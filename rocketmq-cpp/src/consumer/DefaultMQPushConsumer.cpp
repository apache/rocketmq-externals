/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "DefaultMQPushConsumer.h"
#include "CommunicationMode.h"
#include "ConsumeMsgService.h"
#include "ConsumerRunningInfo.h"
#include "FilterAPI.h"
#include "Logging.h"
#include "MQClientAPIImpl.h"
#include "MQClientFactory.h"
#include "MQClientManager.h"
#include "MQProtos.h"
#include "OffsetStore.h"
#include "PullAPIWrapper.h"
#include "PullSysFlag.h"
#include "Rebalance.h"
#include "UtilAll.h"
#include "Validators.h"
#include "task_queue.h"

namespace rocketmq {

class AsyncPullCallback : public PullCallback {
 public:
  AsyncPullCallback(DefaultMQPushConsumer* pushConsumer, PullRequest* request)
      : m_callbackOwner(pushConsumer),
        m_pullRequest(request),
        m_bShutdown(false) {}
  virtual ~AsyncPullCallback() {
    m_callbackOwner = NULL;
    m_pullRequest = NULL;
  }
  virtual void onSuccess(MQMessageQueue& mq, PullResult& result,
                         bool bProducePullRequest) {
    if (m_bShutdown == true) {
      LOG_INFO("pullrequest for:%s in shutdown, return",
               (m_pullRequest->m_messageQueue).toString().c_str());
      m_pullRequest->removePullMsgEvent();
      return;
    }

    switch (result.pullStatus) {
      case FOUND: {
        if (!m_pullRequest->isDroped())  // if request is setted to dropped,
                                         // don't add msgFoundList to
                                         // m_msgTreeMap and don't call
                                         // producePullMsgTask
        {  // avoid issue: pullMsg is sent out, rebalance is doing concurrently
           // and this request is dropped, and then received pulled msgs.
          m_pullRequest->setNextOffset(result.nextBeginOffset);
          m_pullRequest->putMessage(result.msgFoundList);

          m_callbackOwner->getConsumerMsgService()->submitConsumeRequest(
              m_pullRequest, result.msgFoundList);

          if (bProducePullRequest)
            m_callbackOwner->producePullMsgTask(m_pullRequest);
          else
            m_pullRequest->removePullMsgEvent();

          LOG_DEBUG("FOUND:%s with size:" SIZET_FMT ", nextBeginOffset:%lld",
                    (m_pullRequest->m_messageQueue).toString().c_str(),
                    result.msgFoundList.size(), result.nextBeginOffset);
        } else {
          LOG_INFO("remove pullmsg event of mq:%s",
                   (m_pullRequest->m_messageQueue).toString().c_str());
          m_pullRequest->removePullMsgEvent();
        }
        break;
      }
      case NO_NEW_MSG: {
        m_pullRequest->setNextOffset(result.nextBeginOffset);

        vector<MQMessageExt> msgs;
        m_pullRequest->getMessage(msgs);
        if ((msgs.size() == 0) && (result.nextBeginOffset > 0)) {
          /*if broker losted/cleared msgs of one msgQueue, but the brokerOffset
          is kept, then consumer will enter following situation:
          1>. get pull offset with 0 when do rebalance, and set
          m_offsetTable[mq] to 0;
          2>. NO_NEW_MSG or NO_MATCHED_MSG got when pullMessage, and nextBegin
          offset increase by 800
          3>. request->getMessage(msgs) always NULL
          4>. we need update consumerOffset to nextBeginOffset indicated by
          broker
          but if really no new msg could be pulled, also go to this CASE

          LOG_INFO("maybe misMatch between broker and client happens, update
          consumerOffset to nextBeginOffset indicated by broker");*/
          m_callbackOwner->updateConsumeOffset(m_pullRequest->m_messageQueue,
                                               result.nextBeginOffset);
        }
        if (bProducePullRequest)
          m_callbackOwner->producePullMsgTask(m_pullRequest);
        else
          m_pullRequest->removePullMsgEvent();

        /*LOG_INFO("NO_NEW_MSG:%s,nextBeginOffset:%lld",
                 (m_pullRequest->m_messageQueue).toString().c_str(),
                 result.nextBeginOffset);*/
        break;
      }
      case NO_MATCHED_MSG: {
        m_pullRequest->setNextOffset(result.nextBeginOffset);

        vector<MQMessageExt> msgs;
        m_pullRequest->getMessage(msgs);
        if ((msgs.size() == 0) && (result.nextBeginOffset > 0)) {
          /*if broker losted/cleared msgs of one msgQueue, but the brokerOffset
          is kept, then consumer will enter following situation:
          1>. get pull offset with 0 when do rebalance, and set
          m_offsetTable[mq] to 0;
          2>. NO_NEW_MSG or NO_MATCHED_MSG got when pullMessage, and nextBegin
          offset increase by 800
          3>. request->getMessage(msgs) always NULL
          4>. we need update consumerOffset to nextBeginOffset indicated by
          broker
          but if really no new msg could be pulled, also go to this CASE

          LOG_INFO("maybe misMatch between broker and client happens, update
          consumerOffset to nextBeginOffset indicated by broker");*/
          m_callbackOwner->updateConsumeOffset(m_pullRequest->m_messageQueue,
                                               result.nextBeginOffset);
        }
        if (bProducePullRequest)
          m_callbackOwner->producePullMsgTask(m_pullRequest);
        else
          m_pullRequest->removePullMsgEvent();
        /*LOG_INFO("NO_MATCHED_MSG:%s,nextBeginOffset:%lld",
                 (m_pullRequest->m_messageQueue).toString().c_str(),
                 result.nextBeginOffset);*/
        break;
      }
      case OFFSET_ILLEGAL: {
        m_pullRequest->setNextOffset(result.nextBeginOffset);
        if (bProducePullRequest)
          m_callbackOwner->producePullMsgTask(m_pullRequest);
        else
          m_pullRequest->removePullMsgEvent();

        /*LOG_INFO("OFFSET_ILLEGAL:%s,nextBeginOffset:%lld",
                 (m_pullRequest->m_messageQueue).toString().c_str(),
                 result.nextBeginOffset);*/
        break;
      }
      case BROKER_TIMEOUT: {  // as BROKER_TIMEOUT is defined by client, broker
                              // will not returns this status, so this case
                              // could not be entered.
        LOG_ERROR("impossible BROKER_TIMEOUT Occurs");
        m_pullRequest->setNextOffset(result.nextBeginOffset);
        if (bProducePullRequest)
          m_callbackOwner->producePullMsgTask(m_pullRequest);
        else
          m_pullRequest->removePullMsgEvent();
        break;
      }
    }
  }

  virtual void onException(MQException& e) {
    if (m_bShutdown == true) {
      LOG_INFO("pullrequest for:%s in shutdown, return",
               (m_pullRequest->m_messageQueue).toString().c_str());
      m_pullRequest->removePullMsgEvent();
      return;
    }
    LOG_WARN("pullrequest for:%s occurs exception, reproduce it",
             (m_pullRequest->m_messageQueue).toString().c_str());
    m_callbackOwner->producePullMsgTask(m_pullRequest);
  }

  void setShutdownStatus() { m_bShutdown = true; }

 private:
  DefaultMQPushConsumer* m_callbackOwner;
  PullRequest* m_pullRequest;
  bool m_bShutdown;
};

//<!***************************************************************************
static boost::mutex m_asyncCallbackLock;
DefaultMQPushConsumer::DefaultMQPushConsumer(const string& groupname)
    : m_consumeFromWhere(CONSUME_FROM_LAST_OFFSET),
      m_pOffsetStore(NULL),
      m_pPullAPIWrapper(NULL),
      m_pMessageListener(NULL),
      m_consumeMessageBatchMaxSize(1),
      m_maxMsgCacheSize(1000) {
  //<!set default group name;
  string gname = groupname.empty() ? DEFAULT_CONSUMER_GROUP : groupname;
  setGroupName(gname);
  m_asyncPull = true;
  m_asyncPullTimeout = 30 * 1000;
  setMessageModel(CLUSTERING);

  m_startTime = UtilAll::currentTimeMillis();
  m_consumeThreadCount = boost::thread::hardware_concurrency();
  m_pullMsgThreadPoolNum = boost::thread::hardware_concurrency();
  m_async_service_thread.reset(new boost::thread(
      boost::bind(&DefaultMQPushConsumer::boost_asio_work, this)));
}

void DefaultMQPushConsumer::boost_asio_work() {
  LOG_INFO("DefaultMQPushConsumer::boost asio async service runing");
  boost::asio::io_service::work work(m_async_ioService);  // avoid async io
                                                          // service stops after
                                                          // first timer timeout
                                                          // callback
  m_async_ioService.run();
}

DefaultMQPushConsumer::~DefaultMQPushConsumer() {
  m_pMessageListener = NULL;
  deleteAndZero(m_pullmsgQueue);
  deleteAndZero(m_pRebalance);
  deleteAndZero(m_pOffsetStore);
  deleteAndZero(m_pPullAPIWrapper);
  deleteAndZero(m_consumerServeice);
  PullMAP::iterator it = m_PullCallback.begin();
  for (; it != m_PullCallback.end(); ++it) {
    deleteAndZero(it->second);
  }
  m_PullCallback.clear();
  m_subTopics.clear();
}

void DefaultMQPushConsumer::sendMessageBack(MQMessageExt& msg, int delayLevel) {
  try {
    getFactory()->getMQClientAPIImpl()->consumerSendMessageBack(
        msg, getGroupName(), delayLevel, 3000, getSessionCredentials());
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
}

void DefaultMQPushConsumer::fetchSubscribeMessageQueues(
    const string& topic, vector<MQMessageQueue>& mqs) {
  mqs.clear();
  try {
    getFactory()->fetchSubscribeMessageQueues(topic, mqs,
                                              getSessionCredentials());
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
}

void DefaultMQPushConsumer::doRebalance() {
  if (isServiceStateOk()) {
    try {
      m_pRebalance->doRebalance();
    } catch (MQException& e) {
      LOG_ERROR(e.what());
    }
  }
}

void DefaultMQPushConsumer::persistConsumerOffset() {
  if (isServiceStateOk()) {
    m_pRebalance->persistConsumerOffset();
  }
}

void DefaultMQPushConsumer::persistConsumerOffsetByResetOffset() {
  if (isServiceStateOk()) {
    m_pRebalance->persistConsumerOffsetByResetOffset();
  }
}

void DefaultMQPushConsumer::start() {
#ifndef WIN32
  /* Ignore the SIGPIPE */
  struct sigaction sa;
  sa.sa_handler = SIG_IGN;
  sa.sa_flags = 0;
  sigaction(SIGPIPE, &sa, 0);
#endif
  switch (m_serviceState) {
    case CREATE_JUST: {
      m_serviceState = START_FAILED;
      MQClient::start();
      LOG_INFO("DefaultMQPushConsumer:%s start", m_GroupName.c_str());

      //<!data;
      checkConfig();

      //<!create rebalance;
      m_pRebalance = new RebalancePush(this, getFactory());

      string groupname = getGroupName();
      m_pPullAPIWrapper = new PullAPIWrapper(getFactory(), groupname);

      if (m_pMessageListener) {
        if (m_pMessageListener->getMessageListenerType() ==
            messageListenerOrderly) {
          LOG_INFO("start orderly consume service:%s", getGroupName().c_str());
          m_consumerServeice = new ConsumeMessageOrderlyService(
              this, m_consumeThreadCount, m_pMessageListener);
        } else  // for backward compatible, defaultly and concurrently listeners
                // are allocating ConsumeMessageConcurrentlyService
        {
          LOG_INFO("start concurrently consume service:%s",
                   getGroupName().c_str());
          m_consumerServeice = new ConsumeMessageConcurrentlyService(
              this, m_consumeThreadCount, m_pMessageListener);
        }
      }

      m_pullmsgQueue = new TaskQueue(m_pullMsgThreadPoolNum);
      m_pullmsgThread.reset(new boost::thread(boost::bind(
          &DefaultMQPushConsumer::runPullMsgQueue, this, m_pullmsgQueue)));

      copySubscription();

      //<! registe;
      bool registerOK = getFactory()->registerConsumer(this);
      if (!registerOK) {
        m_serviceState = CREATE_JUST;
        THROW_MQEXCEPTION(
            MQClientException,
            "The cousumer group[" + getGroupName() +
                "] has been created before, specify another name please.",
            -1);
      }

      //<!msg model;
      switch (getMessageModel()) {
        case BROADCASTING:
          m_pOffsetStore = new LocalFileOffsetStore(groupname, getFactory());
          break;
        case CLUSTERING:
          m_pOffsetStore = new RemoteBrokerOffsetStore(groupname, getFactory());
          break;
      }
      bool bStartFailed = false;
      string errorMsg;
      try {
        m_pOffsetStore->load();
      } catch (MQClientException& e) {
        bStartFailed = true;
        errorMsg = std::string(e.what());
      }
      m_consumerServeice->start();

      getFactory()->start();

      updateTopicSubscribeInfoWhenSubscriptionChanged();
      getFactory()->sendHeartbeatToAllBroker();

      m_serviceState = RUNNING;
      if (bStartFailed) {
        shutdown();
        THROW_MQEXCEPTION(MQClientException, errorMsg, -1);
      }
      break;
    }
    case RUNNING:
    case START_FAILED:
    case SHUTDOWN_ALREADY:
      break;
    default:
      break;
  }

  getFactory()->rebalanceImmediately();
}

void DefaultMQPushConsumer::shutdown() {
  switch (m_serviceState) {
    case RUNNING: {
      LOG_INFO("DefaultMQPushConsumer shutdown");
      m_async_ioService.stop();
      m_async_service_thread->interrupt();
      m_async_service_thread->join();
      m_pullmsgQueue->close();
      m_pullmsgThread->interrupt();
      m_pullmsgThread->join();
      m_consumerServeice->shutdown();
      persistConsumerOffset();
      shutdownAsyncPullCallBack();  // delete aync pullMsg resources
      getFactory()->unregisterConsumer(this);
      getFactory()->shutdown();
      m_serviceState = SHUTDOWN_ALREADY;
      break;
    }
    case CREATE_JUST:
    case SHUTDOWN_ALREADY:
      break;
    default:
      break;
  }
}

void DefaultMQPushConsumer::registerMessageListener(
    MQMessageListener* pMessageListener) {
  if (NULL != pMessageListener) {
    m_pMessageListener = pMessageListener;
  }
}

MessageListenerType DefaultMQPushConsumer::getMessageListenerType() {
  if (NULL != m_pMessageListener) {
    return m_pMessageListener->getMessageListenerType();
  }
  return messageListenerDefaultly;
}

ConsumeMsgService* DefaultMQPushConsumer::getConsumerMsgService() const {
  return m_consumerServeice;
}

OffsetStore* DefaultMQPushConsumer::getOffsetStore() const {
  return m_pOffsetStore;
}

Rebalance* DefaultMQPushConsumer::getRebalance() const { return m_pRebalance; }

void DefaultMQPushConsumer::subscribe(const string& topic,
                                      const string& subExpression) {
  m_subTopics[topic] = subExpression;
}

void DefaultMQPushConsumer::checkConfig() {
  string groupname = getGroupName();
  // check consumerGroup
  Validators::checkGroup(groupname);

  // consumerGroup
  if (!groupname.compare(DEFAULT_CONSUMER_GROUP)) {
    THROW_MQEXCEPTION(MQClientException,
                      "consumerGroup can not equal DEFAULT_CONSUMER", -1);
  }

  if (getMessageModel() != BROADCASTING && getMessageModel() != CLUSTERING) {
    THROW_MQEXCEPTION(MQClientException, "messageModel is valid ", -1);
  }

  if (m_pMessageListener == NULL) {
    THROW_MQEXCEPTION(MQClientException, "messageListener is null ", -1);
  }
}

void DefaultMQPushConsumer::copySubscription() {
  map<string, string>::iterator it = m_subTopics.begin();
  for (; it != m_subTopics.end(); ++it) {
    LOG_INFO("buildSubscriptionData,:%s,%s", it->first.c_str(),
             it->second.c_str());
    unique_ptr<SubscriptionData> pSData(
        FilterAPI::buildSubscriptionData(it->first, it->second));

    m_pRebalance->setSubscriptionData(it->first, pSData.release());
  }

  switch (getMessageModel()) {
    case BROADCASTING:
      break;
    case CLUSTERING: {
      string retryTopic = UtilAll::getRetryTopic(getGroupName());

      //<!this sub;
      unique_ptr<SubscriptionData> pSData(
          FilterAPI::buildSubscriptionData(retryTopic, SUB_ALL));

      m_pRebalance->setSubscriptionData(retryTopic, pSData.release());
      break;
    }
    default:
      break;
  }
}

void DefaultMQPushConsumer::updateTopicSubscribeInfo(
    const string& topic, vector<MQMessageQueue>& info) {
  m_pRebalance->setTopicSubscribeInfo(topic, info);
}

void DefaultMQPushConsumer::updateTopicSubscribeInfoWhenSubscriptionChanged() {
  map<string, SubscriptionData*>& subTable =
      m_pRebalance->getSubscriptionInner();
  map<string, SubscriptionData*>::iterator it = subTable.begin();
  for (; it != subTable.end(); ++it) {
    bool btopic = getFactory()->updateTopicRouteInfoFromNameServer(
        it->first, getSessionCredentials());
    if (btopic == false) {
      LOG_WARN("The topic:[%s] not exist", it->first.c_str());
    }
  }
}

ConsumeType DefaultMQPushConsumer::getConsumeType() {
  return CONSUME_PASSIVELY;
}

ConsumeFromWhere DefaultMQPushConsumer::getConsumeFromWhere() {
  return m_consumeFromWhere;
}

void DefaultMQPushConsumer::setConsumeFromWhere(
    ConsumeFromWhere consumeFromWhere) {
  m_consumeFromWhere = consumeFromWhere;
}

void DefaultMQPushConsumer::getSubscriptions(vector<SubscriptionData>& result) {
  map<string, SubscriptionData*>& subTable =
      m_pRebalance->getSubscriptionInner();
  map<string, SubscriptionData*>::iterator it = subTable.begin();
  for (; it != subTable.end(); ++it) {
    result.push_back(*(it->second));
  }
}

void DefaultMQPushConsumer::updateConsumeOffset(const MQMessageQueue& mq,
                                                int64 offset) {
  if (offset >= 0) {
    m_pOffsetStore->updateOffset(mq, offset);
  } else {
    LOG_ERROR("updateConsumeOffset of mq:%s error", mq.toString().c_str());
  }
}
void DefaultMQPushConsumer::removeConsumeOffset(const MQMessageQueue& mq) {
  m_pOffsetStore->removeOffset(mq);
}

void DefaultMQPushConsumer::triggerNextPullRequest(
    boost::asio::deadline_timer* t, PullRequest* request) {
  // LOG_INFO("trigger pullrequest for:%s",
  // (request->m_messageQueue).toString().c_str());
  producePullMsgTask(request);
  deleteAndZero(t);
}

void DefaultMQPushConsumer::producePullMsgTask(PullRequest* request) {
  if (m_pullmsgQueue->bTaskQueueStatusOK() && isServiceStateOk()) {
    request->addPullMsgEvent();
    if (m_asyncPull) {
      m_pullmsgQueue->produce(TaskBinder::gen(
          &DefaultMQPushConsumer::pullMessageAsync, this, request));
    } else {
      m_pullmsgQueue->produce(
          TaskBinder::gen(&DefaultMQPushConsumer::pullMessage, this, request));
    }
  } else {
    LOG_WARN("produce pullmsg of mq:%s failed",
             request->m_messageQueue.toString().c_str());
  }
}

void DefaultMQPushConsumer::runPullMsgQueue(TaskQueue* pTaskQueue) {
  pTaskQueue->run();
}

void DefaultMQPushConsumer::pullMessage(PullRequest* request) {
  if (request == NULL || request->isDroped()) {
    LOG_WARN("Pull request is set drop with mq:%s, return",
             (request->m_messageQueue).toString().c_str());
    request->removePullMsgEvent();
    return;
  }

  MQMessageQueue& messageQueue = request->m_messageQueue;
  if (m_consumerServeice->getConsumeMsgSerivceListenerType() ==
      messageListenerOrderly) {
    if (!request->isLocked() || request->isLockExpired()) {
      if (!m_pRebalance->lock(messageQueue)) {
        producePullMsgTask(request);
        return;
      }
    }
  }

  if (request->getCacheMsgCount() > m_maxMsgCacheSize) {
    // LOG_INFO("retry pullrequest for:%s after 1s, as cachMsgSize:%d is larger
    // than:%d",  (request->m_messageQueue).toString().c_str(),
    // request->getCacheMsgCount(), m_maxMsgCacheSize);
    boost::asio::deadline_timer* t = new boost::asio::deadline_timer(
        m_async_ioService, boost::posix_time::milliseconds(1 * 1000));
    t->async_wait(boost::bind(&DefaultMQPushConsumer::triggerNextPullRequest,
                              this, t, request));
    return;
  }

  bool commitOffsetEnable = false;
  int64 commitOffsetValue = 0;
  if (CLUSTERING == getMessageModel()) {
    commitOffsetValue = m_pOffsetStore->readOffset(
        messageQueue, READ_FROM_MEMORY, getSessionCredentials());
    if (commitOffsetValue > 0) {
      commitOffsetEnable = true;
    }
  }

  string subExpression;
  SubscriptionData* pSdata =
      m_pRebalance->getSubscriptionData(messageQueue.getTopic());
  if (pSdata == NULL) {
    producePullMsgTask(request);
    return;
  }
  subExpression = pSdata->getSubString();

  int sysFlag =
      PullSysFlag::buildSysFlag(commitOffsetEnable,      // commitOffset
                                false,                   // suspend
                                !subExpression.empty(),  // subscription
                                false);                  // class filter

  try {
    request->setLastPullTimestamp(UtilAll::currentTimeMillis());
    unique_ptr<PullResult> result(
        m_pPullAPIWrapper->pullKernelImpl(messageQueue,              // 1
                                          subExpression,             // 2
                                          pSdata->getSubVersion(),   // 3
                                          request->getNextOffset(),  // 4
                                          32,                        // 5
                                          sysFlag,                   // 6
                                          commitOffsetValue,         // 7
                                          1000 * 15,                 // 8
                                          1000 * 30,                 // 9
                                          ComMode_SYNC,              // 10
                                          NULL, getSessionCredentials()));

    PullResult pullResult = m_pPullAPIWrapper->processPullResult(
        messageQueue, result.get(), pSdata);

    switch (pullResult.pullStatus) {
      case FOUND: {
        if (!request->isDroped())  // if request is setted to dropped, don't add
                                   // msgFoundList to m_msgTreeMap and don't
                                   // call producePullMsgTask
        {  // avoid issue: pullMsg is sent out, rebalance is doing concurrently
           // and this request is dropped, and then received pulled msgs.
          request->setNextOffset(pullResult.nextBeginOffset);
          request->putMessage(pullResult.msgFoundList);

          m_consumerServeice->submitConsumeRequest(request,
                                                   pullResult.msgFoundList);
          producePullMsgTask(request);

          LOG_DEBUG("FOUND:%s with size:" SIZET_FMT ",nextBeginOffset:%lld",
                    messageQueue.toString().c_str(),
                    pullResult.msgFoundList.size(), pullResult.nextBeginOffset);
        } else {
          request->removePullMsgEvent();
        }
        break;
      }
      case NO_NEW_MSG: {
        request->setNextOffset(pullResult.nextBeginOffset);
        vector<MQMessageExt> msgs;
        request->getMessage(msgs);
        if ((msgs.size() == 0) && (pullResult.nextBeginOffset > 0)) {
          /*if broker losted/cleared msgs of one msgQueue, but the brokerOffset
          is kept, then consumer will enter following situation:
          1>. get pull offset with 0 when do rebalance, and set
          m_offsetTable[mq] to 0;
          2>. NO_NEW_MSG or NO_MATCHED_MSG got when pullMessage, and nextBegin
          offset increase by 800
          3>. request->getMessage(msgs) always NULL
          4>. we need update consumerOffset to nextBeginOffset indicated by
          broker
          but if really no new msg could be pulled, also go to this CASE
       */
          // LOG_DEBUG("maybe misMatch between broker and client happens, update
          // consumerOffset to nextBeginOffset indicated by broker");
          updateConsumeOffset(messageQueue, pullResult.nextBeginOffset);
        }
        producePullMsgTask(request);
        LOG_DEBUG("NO_NEW_MSG:%s,nextBeginOffset:%lld",
                  messageQueue.toString().c_str(), pullResult.nextBeginOffset);
        break;
      }
      case NO_MATCHED_MSG: {
        request->setNextOffset(pullResult.nextBeginOffset);
        vector<MQMessageExt> msgs;
        request->getMessage(msgs);
        if ((msgs.size() == 0) && (pullResult.nextBeginOffset > 0)) {
          // LOG_DEBUG("maybe misMatch between broker and client happens, update
          // consumerOffset to nextBeginOffset indicated by broker");
          updateConsumeOffset(messageQueue, pullResult.nextBeginOffset);
        }
        producePullMsgTask(request);

        LOG_DEBUG("NO_MATCHED_MSG:%s,nextBeginOffset:%lld",
                  messageQueue.toString().c_str(), pullResult.nextBeginOffset);
        break;
      }
      case OFFSET_ILLEGAL: {
        request->setNextOffset(pullResult.nextBeginOffset);
        producePullMsgTask(request);

        LOG_DEBUG("OFFSET_ILLEGAL:%s,nextBeginOffset:%lld",
                  messageQueue.toString().c_str(), pullResult.nextBeginOffset);
        break;
      }
      case BROKER_TIMEOUT: {  // as BROKER_TIMEOUT is defined by client, broker
                              // will not returns this status, so this case
                              // could not be entered.
        LOG_ERROR("impossible BROKER_TIMEOUT Occurs");
        request->setNextOffset(pullResult.nextBeginOffset);
        producePullMsgTask(request);
        break;
      }
    }
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    producePullMsgTask(request);
  }
}

AsyncPullCallback* DefaultMQPushConsumer::getAsyncPullCallBack(
    PullRequest* request, MQMessageQueue msgQueue) {
  boost::lock_guard<boost::mutex> lock(m_asyncCallbackLock);
  if (m_asyncPull && request) {
    PullMAP::iterator it = m_PullCallback.find(msgQueue);
    if (it == m_PullCallback.end()) {
      LOG_INFO("new pull callback for mq:%s", msgQueue.toString().c_str());
      m_PullCallback[msgQueue] = new AsyncPullCallback(this, request);
    }
    return m_PullCallback[msgQueue];
  }

  return NULL;
}

void DefaultMQPushConsumer::shutdownAsyncPullCallBack() {
  boost::lock_guard<boost::mutex> lock(m_asyncCallbackLock);
  if (m_asyncPull) {
    PullMAP::iterator it = m_PullCallback.begin();
    for (; it != m_PullCallback.end(); ++it) {
      if (it->second) {
        it->second->setShutdownStatus();
      } else {
        LOG_ERROR("could not find asyncPullCallback for:%s",
                  it->first.toString().c_str());
      }
    }
  }
}

void DefaultMQPushConsumer::pullMessageAsync(PullRequest* request) {
  if (request == NULL || request->isDroped()) {
    LOG_WARN("Pull request is set drop with mq:%s, return",
             (request->m_messageQueue).toString().c_str());
    request->removePullMsgEvent();
    return;
  }

  MQMessageQueue& messageQueue = request->m_messageQueue;
  if (m_consumerServeice->getConsumeMsgSerivceListenerType() ==
      messageListenerOrderly) {
    if (!request->isLocked() || request->isLockExpired()) {
      if (!m_pRebalance->lock(messageQueue)) {
        producePullMsgTask(request);
        return;
      }
    }
  }

  if (request->getCacheMsgCount() > m_maxMsgCacheSize) {
    // LOG_INFO("retry pullrequest for:%s after 1s, as cachMsgSize:%d is larger
    // than:%d",  (request->m_messageQueue).toString().c_str(),
    // request->getCacheMsgCount(), m_maxMsgCacheSize);
    boost::asio::deadline_timer* t = new boost::asio::deadline_timer(
        m_async_ioService, boost::posix_time::milliseconds(1 * 1000));
    t->async_wait(boost::bind(&DefaultMQPushConsumer::triggerNextPullRequest,
                              this, t, request));
    return;
  }

  bool commitOffsetEnable = false;
  int64 commitOffsetValue = 0;
  if (CLUSTERING == getMessageModel()) {
    commitOffsetValue = m_pOffsetStore->readOffset(
        messageQueue, READ_FROM_MEMORY, getSessionCredentials());
    if (commitOffsetValue > 0) {
      commitOffsetEnable = true;
    }
  }

  string subExpression;
  SubscriptionData* pSdata =
      (m_pRebalance->getSubscriptionData(messageQueue.getTopic()));
  if (pSdata == NULL) {
    producePullMsgTask(request);
    return;
  }
  subExpression = pSdata->getSubString();

  int sysFlag =
      PullSysFlag::buildSysFlag(commitOffsetEnable,      // commitOffset
                                true,                    // suspend
                                !subExpression.empty(),  // subscription
                                false);                  // class filter

  AsyncArg arg;
  arg.mq = messageQueue;
  arg.subData = *pSdata;
  arg.pPullWrapper = m_pPullAPIWrapper;

  try {
    request->setLastPullTimestamp(UtilAll::currentTimeMillis());
    m_pPullAPIWrapper->pullKernelImpl(
        messageQueue,                                 // 1
        subExpression,                                // 2
        pSdata->getSubVersion(),                      // 3
        request->getNextOffset(),                     // 4
        32,                                           // 5
        sysFlag,                                      // 6
        commitOffsetValue,                            // 7
        1000 * 15,                                    // 8
        m_asyncPullTimeout,                           // 9
        ComMode_ASYNC,                                // 10
        getAsyncPullCallBack(request, messageQueue),  // 11
        getSessionCredentials(),                      // 12
        &arg);                                        // 13
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    producePullMsgTask(request);
  }
}

void DefaultMQPushConsumer::setAsyncPull(bool asyncFlag) {
  if (asyncFlag) {
    LOG_INFO("set pushConsumer:%s to async default pull mode",
             getGroupName().c_str());
  } else {
    LOG_INFO("set pushConsumer:%s to sync pull mode", getGroupName().c_str());
  }
  m_asyncPull = asyncFlag;
}

void DefaultMQPushConsumer::setConsumeThreadCount(int threadCount) {
  if (threadCount > 0) {
    m_consumeThreadCount = threadCount;
  } else {
    LOG_ERROR("setConsumeThreadCount with invalid value");
  }
}

int DefaultMQPushConsumer::getConsumeThreadCount() const {
  return m_consumeThreadCount;
}

void DefaultMQPushConsumer::setPullMsgThreadPoolCount(int threadCount) {
  m_pullMsgThreadPoolNum = threadCount;
}

int DefaultMQPushConsumer::getPullMsgThreadPoolCount() const {
  return m_pullMsgThreadPoolNum;
}

int DefaultMQPushConsumer::getConsumeMessageBatchMaxSize() const {
  return m_consumeMessageBatchMaxSize;
}

void DefaultMQPushConsumer::setConsumeMessageBatchMaxSize(
    int consumeMessageBatchMaxSize) {
  if (consumeMessageBatchMaxSize >= 1)
    m_consumeMessageBatchMaxSize = consumeMessageBatchMaxSize;
}

void DefaultMQPushConsumer::setMaxCacheMsgSizePerQueue(int maxCacheSize) {
  if (maxCacheSize > 0 && maxCacheSize < 65535) {
    LOG_INFO("set maxCacheSize to:%d for consumer:%s", maxCacheSize,
             getGroupName().c_str());
    m_maxMsgCacheSize = maxCacheSize;
  }
}

int DefaultMQPushConsumer::getMaxCacheMsgSizePerQueue() const {
  return m_maxMsgCacheSize;
}

ConsumerRunningInfo* DefaultMQPushConsumer::getConsumerRunningInfo() {
  ConsumerRunningInfo* info = new ConsumerRunningInfo();
  if (info) {
    if (m_consumerServeice->getConsumeMsgSerivceListenerType() ==
        messageListenerOrderly)
      info->setProperty(ConsumerRunningInfo::PROP_CONSUME_ORDERLY, "true");
    else
      info->setProperty(ConsumerRunningInfo::PROP_CONSUME_ORDERLY, "flase");
    info->setProperty(ConsumerRunningInfo::PROP_THREADPOOL_CORE_SIZE,
                      UtilAll::to_string(m_consumeThreadCount));
    info->setProperty(ConsumerRunningInfo::PROP_CONSUMER_START_TIMESTAMP,
                      UtilAll::to_string(m_startTime));

    vector<SubscriptionData> result;
    getSubscriptions(result);
    info->setSubscriptionSet(result);

    map<MQMessageQueue, PullRequest*> requestTable =
        m_pRebalance->getPullRequestTable();
    map<MQMessageQueue, PullRequest*>::iterator it = requestTable.begin();

    for (; it != requestTable.end(); ++it) {
      if (!it->second->isDroped()) {
        map<MessageQueue, ProcessQueueInfo> queueTable;
        MessageQueue queue((it->first).getTopic(), (it->first).getBrokerName(),
                           (it->first).getQueueId());
        ProcessQueueInfo processQueue;
        processQueue.cachedMsgMinOffset = it->second->getCacheMinOffset();
        processQueue.cachedMsgMaxOffset = it->second->getCacheMaxOffset();
        processQueue.cachedMsgCount = it->second->getCacheMsgCount();
        processQueue.setCommitOffset(m_pOffsetStore->readOffset(
            it->first, MEMORY_FIRST_THEN_STORE, getSessionCredentials()));
        processQueue.setDroped(it->second->isDroped());
        processQueue.setLocked(it->second->isLocked());
        processQueue.lastLockTimestamp = it->second->getLastLockTimestamp();
        processQueue.lastPullTimestamp = it->second->getLastPullTimestamp();
        processQueue.lastConsumeTimestamp =
            it->second->getLastConsumeTimestamp();
        info->setMqTable(queue, processQueue);
      }
    }

    return info;
  }
  return NULL;
}

//<!************************************************************************
}  //<!end namespace;
