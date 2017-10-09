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

#include "DefaultMQPullConsumer.h"
#include "AsyncArg.h"
#include "CommunicationMode.h"
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
#include "Validators.h"

namespace rocketmq {
//<!***************************************************************************
DefaultMQPullConsumer::DefaultMQPullConsumer(const string& groupname)
    : m_pMessageQueueListener(NULL),
      m_pOffsetStore(NULL),
      m_pRebalance(NULL),
      m_pPullAPIWrapper(NULL)

{
  //<!set default group name;
  string gname = groupname.empty() ? DEFAULT_CONSUMER_GROUP : groupname;
  setGroupName(gname);

  setMessageModel(BROADCASTING);
}

DefaultMQPullConsumer::~DefaultMQPullConsumer() {
  m_pMessageQueueListener = NULL;
  deleteAndZero(m_pRebalance);
  deleteAndZero(m_pOffsetStore);
  deleteAndZero(m_pPullAPIWrapper);
}

// MQConsumer
//<!************************************************************************
void DefaultMQPullConsumer::start() {
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
      LOG_INFO("DefaultMQPullConsumer:%s start", m_GroupName.c_str());

      //<!create rebalance;
      m_pRebalance = new RebalancePull(this, getFactory());

      string groupname = getGroupName();
      m_pPullAPIWrapper = new PullAPIWrapper(getFactory(), groupname);

      //<!data;
      checkConfig();
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

      getFactory()->start();
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
}

void DefaultMQPullConsumer::shutdown() {
  switch (m_serviceState) {
    case RUNNING: {
      LOG_INFO("DefaultMQPullConsumer:%s shutdown", m_GroupName.c_str());
      persistConsumerOffset();
      getFactory()->unregisterConsumer(this);
      getFactory()->shutdown();
      m_serviceState = SHUTDOWN_ALREADY;
      break;
    }
    case SHUTDOWN_ALREADY:
    case CREATE_JUST:
      break;
    default:
      break;
  }
}

void DefaultMQPullConsumer::sendMessageBack(MQMessageExt& msg, int delayLevel) {

}

void DefaultMQPullConsumer::fetchSubscribeMessageQueues(
    const string& topic, vector<MQMessageQueue>& mqs) {
  mqs.clear();
  try {
    getFactory()->fetchSubscribeMessageQueues(topic, mqs,
                                              getSessionCredentials());
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
}

void DefaultMQPullConsumer::updateTopicSubscribeInfo(
    const string& topic, vector<MQMessageQueue>& info) {}

void DefaultMQPullConsumer::registerMessageQueueListener(
    const string& topic, MQueueListener* pListener) {
  m_registerTopics.insert(topic);
  if (pListener) {
    m_pMessageQueueListener = pListener;
  }
}

PullResult DefaultMQPullConsumer::pull(const MQMessageQueue& mq,
                                       const string& subExpression,
                                       int64 offset, int maxNums) {
  return pullSyncImpl(mq, subExpression, offset, maxNums, false);
}

void DefaultMQPullConsumer::pull(const MQMessageQueue& mq,
                                 const string& subExpression, int64 offset,
                                 int maxNums, PullCallback* pPullCallback) {
  pullAsyncImpl(mq, subExpression, offset, maxNums, false, pPullCallback);
}

PullResult DefaultMQPullConsumer::pullBlockIfNotFound(
    const MQMessageQueue& mq, const string& subExpression, int64 offset,
    int maxNums) {
  return pullSyncImpl(mq, subExpression, offset, maxNums, true);
}

void DefaultMQPullConsumer::pullBlockIfNotFound(const MQMessageQueue& mq,
                                                const string& subExpression,
                                                int64 offset, int maxNums,
                                                PullCallback* pPullCallback) {
  pullAsyncImpl(mq, subExpression, offset, maxNums, true, pPullCallback);
}

PullResult DefaultMQPullConsumer::pullSyncImpl(const MQMessageQueue& mq,
                                               const string& subExpression,
                                               int64 offset, int maxNums,
                                               bool block) {
  if (offset < 0) THROW_MQEXCEPTION(MQClientException, "offset < 0", -1);

  if (maxNums <= 0) THROW_MQEXCEPTION(MQClientException, "maxNums <= 0", -1);

  //<!auto subscript,all sub;
  subscriptionAutomatically(mq.getTopic());

  int sysFlag = PullSysFlag::buildSysFlag(false, block, true, false);

  //<!this sub;
  unique_ptr<SubscriptionData> pSData(
      FilterAPI::buildSubscriptionData(mq.getTopic(), subExpression));

  int timeoutMillis = block ? 1000 * 30 : 1000 * 10;

  try {
    unique_ptr<PullResult> pullResult(
        m_pPullAPIWrapper->pullKernelImpl(mq,                      // 1
                                          pSData->getSubString(),  // 2
                                          0L,                      // 3
                                          offset,                  // 4
                                          maxNums,                 // 5
                                          sysFlag,                 // 6
                                          0,                       // 7
                                          1000 * 20,               // 8
                                          timeoutMillis,           // 9
                                          ComMode_SYNC,            // 10
                                          NULL,                    //<!callback;
                                          getSessionCredentials(), NULL));
    return m_pPullAPIWrapper->processPullResult(mq, pullResult.get(),
                                                pSData.get());
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
  return PullResult(BROKER_TIMEOUT);
}

void DefaultMQPullConsumer::pullAsyncImpl(const MQMessageQueue& mq,
                                          const string& subExpression,
                                          int64 offset, int maxNums, bool block,
                                          PullCallback* pPullCallback) {
  if (offset < 0) THROW_MQEXCEPTION(MQClientException, "offset < 0", -1);

  if (maxNums <= 0) THROW_MQEXCEPTION(MQClientException, "maxNums <= 0", -1);

  if (!pPullCallback)
    THROW_MQEXCEPTION(MQClientException, "pPullCallback is null", -1);

  //<!auto subscript,all sub;
  subscriptionAutomatically(mq.getTopic());

  int sysFlag = PullSysFlag::buildSysFlag(false, block, true, false);

  //<!this sub;
  unique_ptr<SubscriptionData> pSData(
      FilterAPI::buildSubscriptionData(mq.getTopic(), subExpression));

  int timeoutMillis = block ? 1000 * 30 : 1000 * 10;

  //<!�첽����;
  AsyncArg arg;
  arg.mq = mq;
  arg.subData = *pSData;
  arg.pPullWrapper = m_pPullAPIWrapper;

  try {
    unique_ptr<PullResult> pullResult(m_pPullAPIWrapper->pullKernelImpl(
        mq,                      // 1
        pSData->getSubString(),  // 2
        0L,                      // 3
        offset,                  // 4
        maxNums,                 // 5
        sysFlag,                 // 6
        0,                       // 7
        1000 * 20,               // 8
        timeoutMillis,           // 9
        ComMode_ASYNC,           // 10
        pPullCallback, getSessionCredentials(), &arg));
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
}

void DefaultMQPullConsumer::subscriptionAutomatically(const string& topic) {
  SubscriptionData* pSdata = m_pRebalance->getSubscriptionData(topic);
  if (pSdata == NULL) {
    unique_ptr<SubscriptionData> subscriptionData(
        FilterAPI::buildSubscriptionData(topic, SUB_ALL));
    m_pRebalance->setSubscriptionData(topic, subscriptionData.release());
  }
}

void DefaultMQPullConsumer::updateConsumeOffset(const MQMessageQueue& mq,
                                                int64 offset) {
  m_pOffsetStore->updateOffset(mq, offset);
}

void DefaultMQPullConsumer::removeConsumeOffset(const MQMessageQueue& mq) {
  m_pOffsetStore->removeOffset(mq);
}

int64 DefaultMQPullConsumer::fetchConsumeOffset(const MQMessageQueue& mq,
                                                bool fromStore) {
  return m_pOffsetStore->readOffset(
      mq, fromStore ? READ_FROM_STORE : MEMORY_FIRST_THEN_STORE,
      getSessionCredentials());
}

void DefaultMQPullConsumer::persistConsumerOffset() {
  /*As do not execute rebalance for pullConsumer now, requestTable is always
  empty
  map<MQMessageQueue, PullRequest*> requestTable =
  m_pRebalance->getPullRequestTable();
  map<MQMessageQueue, PullRequest*>::iterator it = requestTable.begin();
  vector<MQMessageQueue> mqs;
  for (; it != requestTable.end(); ++it)
  {
      if (it->second)
      {
          mqs.push_back(it->first);
      }
  }
  m_pOffsetStore->persistAll(mqs);*/
}

void DefaultMQPullConsumer::persistConsumerOffsetByResetOffset() {}

void DefaultMQPullConsumer::persistConsumerOffset4PullConsumer(
    const MQMessageQueue& mq) {
  if (isServiceStateOk()) {
    m_pOffsetStore->persist(mq, getSessionCredentials());
  }
}

void DefaultMQPullConsumer::fetchMessageQueuesInBalance(
    const string& topic, vector<MQMessageQueue> mqs) {}

void DefaultMQPullConsumer::checkConfig() {
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
}

void DefaultMQPullConsumer::doRebalance() {}

void DefaultMQPullConsumer::copySubscription() {
  set<string>::iterator it = m_registerTopics.begin();
  for (; it != m_registerTopics.end(); ++it) {
    unique_ptr<SubscriptionData> subscriptionData(
        FilterAPI::buildSubscriptionData((*it), SUB_ALL));
    m_pRebalance->setSubscriptionData((*it), subscriptionData.release());
  }
}

ConsumeType DefaultMQPullConsumer::getConsumeType() { return CONSUME_ACTIVELY; }

ConsumeFromWhere DefaultMQPullConsumer::getConsumeFromWhere() {
  return CONSUME_FROM_LAST_OFFSET;
}

void DefaultMQPullConsumer::getSubscriptions(vector<SubscriptionData>& result) {
  set<string>::iterator it = m_registerTopics.begin();
  for (; it != m_registerTopics.end(); ++it) {
    SubscriptionData ms(*it, SUB_ALL);
    result.push_back(ms);
  }
}

void DefaultMQPullConsumer::producePullMsgTask(PullRequest*) {}

Rebalance* DefaultMQPullConsumer::getRebalance() const { return NULL; }

//<!************************************************************************
}  //<!end namespace;
