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

#include "DefaultMQProducer.h"
#include <assert.h>
#include "CommandHeader.h"
#include "CommunicationMode.h"
#include "Logging.h"
#include "MQClientAPIImpl.h"
#include "MQClientException.h"
#include "MQClientFactory.h"
#include "MQClientManager.h"
#include "MQDecoder.h"
#include "MQProtos.h"
#include "MessageSysFlag.h"
#include "TopicPublishInfo.h"
#include "Validators.h"

namespace rocketmq {

//<!************************************************************************
DefaultMQProducer::DefaultMQProducer(const string& groupname)
    : m_sendMsgTimeout(3000),
      m_compressMsgBodyOverHowmuch(4 * 1024),
      m_maxMessageSize(1024 * 128),
      m_retryAnotherBrokerWhenNotStoreOK(false),
      m_compressLevel(5),
      m_retryTimes(5) {
  //<!set default group name;
  string gname = groupname.empty() ? DEFAULT_PRODUCER_GROUP : groupname;
  setGroupName(gname);
}

DefaultMQProducer::~DefaultMQProducer() {}

void DefaultMQProducer::start() {
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
      LOG_INFO("DefaultMQProducer:%s start", m_GroupName.c_str());

      bool registerOK = getFactory()->registerProducer(this);
      if (!registerOK) {
        m_serviceState = CREATE_JUST;
        THROW_MQEXCEPTION(
            MQClientException,
            "The producer group[" + getGroupName() +
                "] has been created before, specify another name please.",
            -1);
      }

      getFactory()->start();
      getFactory()->sendHeartbeatToAllBroker();
      m_serviceState = RUNNING;
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

void DefaultMQProducer::shutdown() {
  switch (m_serviceState) {
    case RUNNING: {
      LOG_INFO("DefaultMQProducer shutdown");
      getFactory()->unregisterProducer(this);
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

SendResult DefaultMQProducer::send(MQMessage& msg, bool bSelectActiveBroker) {
  Validators::checkMessage(msg, getMaxMessageSize());
  try {
    return sendDefaultImpl(msg, ComMode_SYNC, NULL, bSelectActiveBroker);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
  return SendResult();
}

void DefaultMQProducer::send(MQMessage& msg, SendCallback* pSendCallback,
                             bool bSelectActiveBroker) {
  Validators::checkMessage(msg, getMaxMessageSize());
  try {
    sendDefaultImpl(msg, ComMode_ASYNC, pSendCallback, bSelectActiveBroker);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

SendResult DefaultMQProducer::send(MQMessage& msg, const MQMessageQueue& mq) {
  Validators::checkMessage(msg, getMaxMessageSize());
  if (msg.getTopic() != mq.getTopic()) {
    LOG_WARN("message's topic not equal mq's topic");
  }
  try {
    return sendKernelImpl(msg, mq, ComMode_SYNC, NULL);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
  return SendResult();
}

void DefaultMQProducer::send(MQMessage& msg, const MQMessageQueue& mq,
                             SendCallback* pSendCallback) {
  Validators::checkMessage(msg, getMaxMessageSize());
  if (msg.getTopic() != mq.getTopic()) {
    LOG_WARN("message's topic not equal mq's topic");
  }
  try {
    sendKernelImpl(msg, mq, ComMode_ASYNC, pSendCallback);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

void DefaultMQProducer::sendOneway(MQMessage& msg, bool bSelectActiveBroker) {
  Validators::checkMessage(msg, getMaxMessageSize());
  try {
    sendDefaultImpl(msg, ComMode_ONEWAY, NULL, bSelectActiveBroker);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

void DefaultMQProducer::sendOneway(MQMessage& msg, const MQMessageQueue& mq) {
  Validators::checkMessage(msg, getMaxMessageSize());
  if (msg.getTopic() != mq.getTopic()) {
    LOG_WARN("message's topic not equal mq's topic");
  }
  try {
    sendKernelImpl(msg, mq, ComMode_ONEWAY, NULL);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

SendResult DefaultMQProducer::send(MQMessage& msg,
                                   MessageQueueSelector* pSelector, void* arg) {
  try {
    return sendSelectImpl(msg, pSelector, arg, ComMode_SYNC, NULL);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
  return SendResult();
}

SendResult DefaultMQProducer::send(MQMessage& msg,
                                   MessageQueueSelector* pSelector, void* arg,
                                   int autoRetryTimes, bool bActiveBroker) {
  try {
    return sendAutoRetrySelectImpl(msg, pSelector, arg, ComMode_SYNC, NULL,
                                   autoRetryTimes, bActiveBroker);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
  return SendResult();
}

void DefaultMQProducer::send(MQMessage& msg, MessageQueueSelector* pSelector,
                             void* arg, SendCallback* pSendCallback) {
  try {
    sendSelectImpl(msg, pSelector, arg, ComMode_ASYNC, pSendCallback);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

void DefaultMQProducer::sendOneway(MQMessage& msg,
                                   MessageQueueSelector* pSelector, void* arg) {
  try {
    sendSelectImpl(msg, pSelector, arg, ComMode_ONEWAY, NULL);
  } catch (MQException& e) {
    LOG_ERROR(e.what());
    throw e;
  }
}

int DefaultMQProducer::getSendMsgTimeout() const { return m_sendMsgTimeout; }

void DefaultMQProducer::setSendMsgTimeout(int sendMsgTimeout) {
  m_sendMsgTimeout = sendMsgTimeout;
}

int DefaultMQProducer::getCompressMsgBodyOverHowmuch() const {
  return m_compressMsgBodyOverHowmuch;
}

void DefaultMQProducer::setCompressMsgBodyOverHowmuch(
    int compressMsgBodyOverHowmuch) {
  m_compressMsgBodyOverHowmuch = compressMsgBodyOverHowmuch;
}

int DefaultMQProducer::getMaxMessageSize() const { return m_maxMessageSize; }

void DefaultMQProducer::setMaxMessageSize(int maxMessageSize) {
  m_maxMessageSize = maxMessageSize;
}

int DefaultMQProducer::getCompressLevel() const { return m_compressLevel; }

void DefaultMQProducer::setCompressLevel(int compressLevel) {
  assert(compressLevel >= 0 && compressLevel <= 9 || compressLevel == -1);

  m_compressLevel = compressLevel;
}

//<!************************************************************************
SendResult DefaultMQProducer::sendDefaultImpl(MQMessage& msg,
                                              int communicationMode,
                                              SendCallback* pSendCallback,
                                              bool bActiveMQ) {
  MQMessageQueue lastmq;
  int mq_index = 0;
  for (int times = 1; times <= m_retryTimes; times++) {
    boost::weak_ptr<TopicPublishInfo> weak_topicPublishInfo(
        getFactory()->tryToFindTopicPublishInfo(msg.getTopic(),
                                                getSessionCredentials()));
    boost::shared_ptr<TopicPublishInfo> topicPublishInfo(
        weak_topicPublishInfo.lock());
    if (topicPublishInfo) {
      if (times == 1) {
        mq_index = topicPublishInfo->getWhichQueue();
      } else {
        mq_index++;
      }

      SendResult sendResult;
      MQMessageQueue mq;
      if (bActiveMQ)
        mq = topicPublishInfo->selectOneActiveMessageQueue(lastmq, mq_index);
      else
        mq = topicPublishInfo->selectOneMessageQueue(lastmq, mq_index);

      lastmq = mq;
      if (mq.getQueueId() == -1) {
        // THROW_MQEXCEPTION(MQClientException, "the MQMessageQueue is
        // invalide", -1);
        continue;
      }

      try {
        LOG_DEBUG("send to brokerName:%s", mq.getBrokerName().c_str());
        sendResult = sendKernelImpl(msg, mq, communicationMode, pSendCallback);
        switch (communicationMode) {
          case ComMode_ASYNC:
            return sendResult;
          case ComMode_ONEWAY:
            return sendResult;
          case ComMode_SYNC:
            if (sendResult.getSendStatus() != SEND_OK) {
              if (bActiveMQ) {
                topicPublishInfo->updateNonServiceMessageQueue(
                    mq, getSendMsgTimeout());
              }
              continue;
            }
            return sendResult;
          default:
            break;
        }
      } catch (...) {
        LOG_ERROR("send failed of times:%d,brokerName:%s", times,
                  mq.getBrokerName().c_str());
        if (bActiveMQ) {
          topicPublishInfo->updateNonServiceMessageQueue(mq,
                                                         getSendMsgTimeout());
        }
        continue;
      }
    }  // end of for
    LOG_WARN("Retry many times, still failed");
  }
  THROW_MQEXCEPTION(MQClientException, "No route info of this topic, ", -1);
}

SendResult DefaultMQProducer::sendKernelImpl(MQMessage& msg,
                                             const MQMessageQueue& mq,
                                             int communicationMode,
                                             SendCallback* sendCallback) {
  string brokerAddr =
      getFactory()->findBrokerAddressInPublish(mq.getBrokerName());

  if (brokerAddr.empty()) {
    getFactory()->tryToFindTopicPublishInfo(mq.getTopic(),
                                            getSessionCredentials());
    brokerAddr = getFactory()->findBrokerAddressInPublish(mq.getBrokerName());
  }

  if (!brokerAddr.empty()) {
    try {
      LOG_DEBUG("produce before:%s to %s", msg.toString().c_str(),
                mq.toString().c_str());
      int sysFlag = 0;
      if (tryToCompressMessage(msg)) {
        sysFlag |= MessageSysFlag::CompressedFlag;
      }

      string tranMsg =
          msg.getProperty(MQMessage::PROPERTY_TRANSACTION_PREPARED);
      if (!tranMsg.empty() && tranMsg == "true") {
        sysFlag |= MessageSysFlag::TransactionPreparedType;
      }

      SendMessageRequestHeader* requestHeader = new SendMessageRequestHeader();
      requestHeader->producerGroup = getGroupName();
      requestHeader->topic = (msg.getTopic());
      requestHeader->defaultTopic = DEFAULT_TOPIC;
      requestHeader->defaultTopicQueueNums = 4;
      requestHeader->queueId = (mq.getQueueId());
      requestHeader->sysFlag = (sysFlag);
      requestHeader->bornTimestamp = UtilAll::currentTimeMillis();
      requestHeader->flag = (msg.getFlag());
      requestHeader->properties =
          (MQDecoder::messageProperties2String(msg.getProperties()));

      return getFactory()->getMQClientAPIImpl()->sendMessage(
          brokerAddr, mq.getBrokerName(), msg, requestHeader,
          getSendMsgTimeout(), communicationMode, sendCallback,
          getSessionCredentials());
    } catch (MQException& e) {
      throw e;
    }
  }
  THROW_MQEXCEPTION(MQClientException,
                    "The broker[" + mq.getBrokerName() + "] not exist", -1);
}

SendResult DefaultMQProducer::sendSelectImpl(MQMessage& msg,
                                             MessageQueueSelector* pSelector,
                                             void* pArg, int communicationMode,
                                             SendCallback* sendCallback) {
  Validators::checkMessage(msg, getMaxMessageSize());

  boost::weak_ptr<TopicPublishInfo> weak_topicPublishInfo(
      getFactory()->tryToFindTopicPublishInfo(msg.getTopic(),
                                              getSessionCredentials()));
  boost::shared_ptr<TopicPublishInfo> topicPublishInfo(
      weak_topicPublishInfo.lock());
  if (topicPublishInfo)  //&& topicPublishInfo->ok())
  {
    MQMessageQueue mq =
        pSelector->select(topicPublishInfo->getMessageQueueList(), msg, pArg);
    return sendKernelImpl(msg, mq, communicationMode, sendCallback);
  }
  THROW_MQEXCEPTION(MQClientException, "No route info for this topic", -1);
}

SendResult DefaultMQProducer::sendAutoRetrySelectImpl(
    MQMessage& msg, MessageQueueSelector* pSelector, void* pArg,
    int communicationMode, SendCallback* pSendCallback, int autoRetryTimes,
    bool bActiveMQ) {
  Validators::checkMessage(msg, getMaxMessageSize());

  MQMessageQueue lastmq;
  MQMessageQueue mq;
  int mq_index = 0;
  for (int times = 1; times <= autoRetryTimes + 1; times++) {
    boost::weak_ptr<TopicPublishInfo> weak_topicPublishInfo(
        getFactory()->tryToFindTopicPublishInfo(msg.getTopic(),
                                                getSessionCredentials()));
    boost::shared_ptr<TopicPublishInfo> topicPublishInfo(
        weak_topicPublishInfo.lock());
    if (topicPublishInfo) {
      SendResult sendResult;
      if (times == 1) {  // always send to selected MQ firstly, evenif bActiveMQ
                         // was setted to true
        mq = pSelector->select(topicPublishInfo->getMessageQueueList(), msg,
                               pArg);
        lastmq = mq;
      } else {
        LOG_INFO("sendAutoRetrySelectImpl with times:%d", times);
        vector<MQMessageQueue> mqs(topicPublishInfo->getMessageQueueList());
        for (size_t i = 0; i < mqs.size(); i++) {
          if (mqs[i] == lastmq) mq_index = i;
        }
        if (bActiveMQ)
          mq = topicPublishInfo->selectOneActiveMessageQueue(lastmq, mq_index);
        else
          mq = topicPublishInfo->selectOneMessageQueue(lastmq, mq_index);
        lastmq = mq;
        if (mq.getQueueId() == -1) {
          // THROW_MQEXCEPTION(MQClientException, "the MQMessageQueue is
          // invalide", -1);
          continue;
        }
      }

      try {
        LOG_DEBUG("send to broker:%s", mq.toString().c_str());
        sendResult = sendKernelImpl(msg, mq, communicationMode, pSendCallback);
        switch (communicationMode) {
          case ComMode_ASYNC:
            return sendResult;
          case ComMode_ONEWAY:
            return sendResult;
          case ComMode_SYNC:
            if (sendResult.getSendStatus() != SEND_OK) {
              if (bActiveMQ) {
                topicPublishInfo->updateNonServiceMessageQueue(
                    mq, getSendMsgTimeout());
              }
              continue;
            }
            return sendResult;
          default:
            break;
        }
      } catch (...) {
        LOG_ERROR("send failed of times:%d,mq:%s", times,
                  mq.toString().c_str());
        if (bActiveMQ) {
          topicPublishInfo->updateNonServiceMessageQueue(mq,
                                                         getSendMsgTimeout());
        }
        continue;
      }
    }  // end of for
    LOG_WARN("Retry many times, still failed");
  }
  THROW_MQEXCEPTION(MQClientException, "No route info of this topic, ", -1);
}

bool DefaultMQProducer::tryToCompressMessage(MQMessage& msg) {
  string body = msg.getBody();
  if ((int)body.length() >= getCompressMsgBodyOverHowmuch()) {
    string outBody;
    if (UtilAll::deflate(body, outBody, getCompressLevel())) {
      msg.setBody(outBody);
      return true;
    }
  }

  return false;
}
int DefaultMQProducer::getRetryTimes() const { return m_retryTimes; }
void DefaultMQProducer::setRetryTimes(int times) {
  if (times <= 0) {
    LOG_WARN("set retry times illegal, use default value:5");
    return;
  }

  if (times > 15) {
    LOG_WARN("set retry times illegal, use max value:15");
    m_retryTimes = 15;
    return;
  }
  LOG_WARN("set retry times to:%d", times);
  m_retryTimes = times;
}
//<!***************************************************************************
}  //<!end namespace;
