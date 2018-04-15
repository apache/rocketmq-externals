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
#include "Rebalance.h"
#include "DefaultMQPushConsumer.h"
#include "LockBatchBody.h"
#include "Logging.h"
#include "MQClientAPIImpl.h"
#include "MQClientFactory.h"
#include "OffsetStore.h"

namespace rocketmq {
//<!************************************************************************
Rebalance::Rebalance(MQConsumer* consumer, MQClientFactory* pfactory)
    : m_pConsumer(consumer), m_pClientFactory(pfactory) {
  m_pAllocateMQStrategy = new AllocateMQAveragely();
}

Rebalance::~Rebalance() {
  {
    map<string, SubscriptionData*>::iterator it = m_subscriptionData.begin();
    for (; it != m_subscriptionData.end(); ++it) deleteAndZero(it->second);
    m_subscriptionData.clear();
  }
  {
    MQ2PULLREQ::iterator it = m_requestQueueTable.begin();
    for (; it != m_requestQueueTable.end(); ++it) {
      delete it->second;
      it->second = NULL;
    }
    m_requestQueueTable.clear();
  }
  m_topicSubscribeInfoTable.clear();
  m_pConsumer = NULL;
  m_pClientFactory = NULL;
  deleteAndZero(m_pAllocateMQStrategy);
}

void Rebalance::doRebalance() {
  LOG_DEBUG("start doRebalance");
  try {
    map<string, SubscriptionData*>::iterator it = m_subscriptionData.begin();
    for (; it != m_subscriptionData.end(); ++it) {
      string topic = (it->first);
      LOG_INFO("current topic is:%s", topic.c_str());
      //<!topic -> mqs
      vector<MQMessageQueue> mqAll;
      if (!getTopicSubscribeInfo(topic, mqAll)) {
        continue;
      }
      if (mqAll.empty()) {
        if (!UtilAll::startsWith_retry(topic))
          THROW_MQEXCEPTION(MQClientException, "doRebalance the topic is empty",
                            -1);
      }

      //<!msg model;
      switch (m_pConsumer->getMessageModel()) {
        case BROADCASTING: {
          bool changed = updateRequestTableInRebalance(topic, mqAll);
          if (changed) {
            messageQueueChanged(topic, mqAll, mqAll);
          }
          break;
        }
        case CLUSTERING: {
          vector<string> cidAll;
          m_pClientFactory->findConsumerIds(
              topic, m_pConsumer->getGroupName(), cidAll,
              m_pConsumer->getSessionCredentials());

          if (cidAll.empty()) {
            /*remove the droping pullRequest changes for recovery consume fastly
                from network broken
                //drop all pullRequest
                MQ2PULLREQ::iterator it = m_requestQueueTable.begin();
                for (; it != m_requestQueueTable.end(); ++it)
                {
                    if(!(it->second->isDroped()))
                    {
                        MQMessageQueue mqtemp = it->first;
                        it->second->setDroped(true);
                        removeUnnecessaryMessageQueue(mqtemp);
                        it->second->clearAllMsgs();//add clear operation to
            avoid bad
                state when dropped pullRequest returns normal
                        LOG_INFO("find consumer failed, drop undropped mq:%s",
                mqtemp.toString().c_str());
                    }
            }*/

            THROW_MQEXCEPTION(MQClientException,
                              "doRebalance the cidAll is empty", -1);
          }
          // log
          for (int i = 0; i < (int)cidAll.size(); ++i) {
            LOG_INFO("client id:%s of topic:%s", cidAll[i].c_str(),
                     topic.c_str());
          }
          //<! sort;
          sort(mqAll.begin(), mqAll.end());
          sort(cidAll.begin(), cidAll.end());

          //<! allocate;
          vector<MQMessageQueue> allocateResult;
          try {
            m_pAllocateMQStrategy->allocate(m_pConsumer->getMQClientId(), mqAll,
                                            cidAll, allocateResult);
          } catch (MQException& e) {
            THROW_MQEXCEPTION(MQClientException, "allocate error", -1);
          }

          // log
          for (int i = 0; i < (int)allocateResult.size(); ++i) {
            LOG_INFO("allocate mq:%s", allocateResult[i].toString().c_str());
          }

          //<!update local;
          bool changed = updateRequestTableInRebalance(topic, allocateResult);
          if (changed) {
            messageQueueChanged(topic, mqAll, allocateResult);
            break;
          }
        }
        default:
          break;
      }
    }
  } catch (MQException& e) {
    LOG_ERROR(e.what());
  }
}

void Rebalance::persistConsumerOffset() {
  DefaultMQPushConsumer* pConsumer =
      static_cast<DefaultMQPushConsumer*>(m_pConsumer);
  OffsetStore* pOffsetStore = pConsumer->getOffsetStore();
  vector<MQMessageQueue> mqs;
  {
    boost::lock_guard<boost::mutex> lock(m_requestTableMutex);
    MQ2PULLREQ::iterator it = m_requestQueueTable.begin();
    for (; it != m_requestQueueTable.end(); ++it) {
      if (it->second && (!it->second->isDroped())) {
        mqs.push_back(it->first);
      }
    }
  }

  if (pConsumer->getMessageModel() == BROADCASTING) {
    pOffsetStore->persistAll(mqs);
  } else {
    vector<MQMessageQueue>::iterator it2 = mqs.begin();
    for (; it2 != mqs.end(); ++it2) {
      pOffsetStore->persist(*it2, m_pConsumer->getSessionCredentials());
    }
  }
}

void Rebalance::persistConsumerOffsetByResetOffset() {
  DefaultMQPushConsumer* pConsumer =
      static_cast<DefaultMQPushConsumer*>(m_pConsumer);
  OffsetStore* pOffsetStore = pConsumer->getOffsetStore();
  vector<MQMessageQueue> mqs;
  {
    boost::lock_guard<boost::mutex> lock(m_requestTableMutex);
    MQ2PULLREQ::iterator it = m_requestQueueTable.begin();
    for (; it != m_requestQueueTable.end(); ++it) {
      if (it->second) {  // even if it was dropped, also need update offset when
                         // rcv resetOffset cmd
        mqs.push_back(it->first);
      }
    }
  }
  vector<MQMessageQueue>::iterator it2 = mqs.begin();
  for (; it2 != mqs.end(); ++it2) {
    pOffsetStore->persist(*it2, m_pConsumer->getSessionCredentials());
  }
}

SubscriptionData* Rebalance::getSubscriptionData(const string& topic) {
  if (m_subscriptionData.find(topic) != m_subscriptionData.end()) {
    return m_subscriptionData[topic];
  }
  return NULL;
}

map<string, SubscriptionData*>& Rebalance::getSubscriptionInner() {
  return m_subscriptionData;
}

void Rebalance::setSubscriptionData(const string& topic,
                                    SubscriptionData* pdata) {
  if (pdata != NULL &&
      m_subscriptionData.find(topic) == m_subscriptionData.end())
    m_subscriptionData[topic] = pdata;
}

void Rebalance::setTopicSubscribeInfo(const string& topic,
                                      vector<MQMessageQueue>& mqs) {
  if (m_subscriptionData.find(topic) != m_subscriptionData.end()) {
    {
      boost::lock_guard<boost::mutex> lock(m_topicSubscribeInfoTableMutex);
      if (m_topicSubscribeInfoTable.find(topic) !=
          m_topicSubscribeInfoTable.end())
        m_topicSubscribeInfoTable.erase(topic);
      m_topicSubscribeInfoTable[topic] = mqs;
    }
    // log
    vector<MQMessageQueue>::iterator it = mqs.begin();
    for (; it != mqs.end(); ++it) {
      LOG_DEBUG("topic [%s] has :%s", topic.c_str(), (*it).toString().c_str());
    }
  }
}

bool Rebalance::getTopicSubscribeInfo(const string& topic,
                                      vector<MQMessageQueue>& mqs) {
  boost::lock_guard<boost::mutex> lock(m_topicSubscribeInfoTableMutex);
  if (m_topicSubscribeInfoTable.find(topic) !=
      m_topicSubscribeInfoTable.end()) {
    mqs = m_topicSubscribeInfoTable[topic];
    return true;
  }
  return false;
}

void Rebalance::addPullRequest(MQMessageQueue mq, PullRequest* pPullRequest) {
  boost::lock_guard<boost::mutex> lock(m_requestTableMutex);
  m_requestQueueTable[mq] = pPullRequest;
}

PullRequest* Rebalance::getPullRequest(MQMessageQueue mq) {
  boost::lock_guard<boost::mutex> lock(m_requestTableMutex);
  if (m_requestQueueTable.find(mq) != m_requestQueueTable.end()) {
    return m_requestQueueTable[mq];
  }
  return NULL;
}

map<MQMessageQueue, PullRequest*> Rebalance::getPullRequestTable() {
  boost::lock_guard<boost::mutex> lock(m_requestTableMutex);
  return m_requestQueueTable;
}

void Rebalance::unlockAll(bool oneway) {
  map<string, vector<MQMessageQueue>*> brokerMqs;
  MQ2PULLREQ requestQueueTable = getPullRequestTable();
  for (MQ2PULLREQ::iterator it = requestQueueTable.begin();
       it != requestQueueTable.end(); ++it) {
    if (!(it->second->isDroped())) {
      if (brokerMqs.find(it->first.getBrokerName()) == brokerMqs.end()) {
        vector<MQMessageQueue>* mqs = new vector<MQMessageQueue>;
        brokerMqs[it->first.getBrokerName()] = mqs;
      } else {
        brokerMqs[it->first.getBrokerName()]->push_back(it->first);
      }
    }
  }
  LOG_INFO("unLockAll " SIZET_FMT " broker mqs", brokerMqs.size());
  for (map<string, vector<MQMessageQueue>*>::iterator itb = brokerMqs.begin();
       itb != brokerMqs.end(); ++itb) {
    unique_ptr<FindBrokerResult> pFindBrokerResult(
        m_pClientFactory->findBrokerAddressInSubscribe(itb->first, MASTER_ID,
                                                       true));
    unique_ptr<UnlockBatchRequestBody> unlockBatchRequest(
        new UnlockBatchRequestBody());
    vector<MQMessageQueue> mqs(*(itb->second));
    unlockBatchRequest->setClientId(m_pConsumer->getMQClientId());
    unlockBatchRequest->setConsumerGroup(m_pConsumer->getGroupName());
    unlockBatchRequest->setMqSet(mqs);

    try {
      m_pClientFactory->getMQClientAPIImpl()->unlockBatchMQ(
          pFindBrokerResult->brokerAddr, unlockBatchRequest.get(), 1000,
          m_pConsumer->getSessionCredentials());
      for (unsigned int i = 0; i != mqs.size(); ++i) {
        PullRequest* pullreq = getPullRequest(mqs[i]);
        if (pullreq) {
          LOG_INFO("unlockBatchMQ success of mq:%s", mqs[i].toString().c_str());
          pullreq->setLocked(true);
        } else {
          LOG_ERROR("unlockBatchMQ fails of mq:%s", mqs[i].toString().c_str());
        }
      }
    } catch (MQException& e) {
      LOG_ERROR("unlockBatchMQ fails");
    }
    deleteAndZero(itb->second);
  }
  brokerMqs.clear();
}

void Rebalance::unlock(MQMessageQueue mq) {
  unique_ptr<FindBrokerResult> pFindBrokerResult(
      m_pClientFactory->findBrokerAddressInSubscribe(mq.getBrokerName(),
                                                     MASTER_ID, true));
  unique_ptr<UnlockBatchRequestBody> unlockBatchRequest(
      new UnlockBatchRequestBody());
  vector<MQMessageQueue> mqs;
  mqs.push_back(mq);
  unlockBatchRequest->setClientId(m_pConsumer->getMQClientId());
  unlockBatchRequest->setConsumerGroup(m_pConsumer->getGroupName());
  unlockBatchRequest->setMqSet(mqs);

  try {
    m_pClientFactory->getMQClientAPIImpl()->unlockBatchMQ(
        pFindBrokerResult->brokerAddr, unlockBatchRequest.get(), 1000,
        m_pConsumer->getSessionCredentials());
    for (unsigned int i = 0; i != mqs.size(); ++i) {
      PullRequest* pullreq = getPullRequest(mqs[i]);
      if (pullreq) {
        LOG_INFO("unlock success of mq:%s", mqs[i].toString().c_str());
        pullreq->setLocked(true);
      } else {
        LOG_ERROR("unlock fails of mq:%s", mqs[i].toString().c_str());
      }
    }
  } catch (MQException& e) {
    LOG_ERROR("unlock fails of mq:%s", mq.toString().c_str());
  }
}

void Rebalance::lockAll() {
  map<string, vector<MQMessageQueue>*> brokerMqs;
  MQ2PULLREQ requestQueueTable = getPullRequestTable();
  for (MQ2PULLREQ::iterator it = requestQueueTable.begin();
       it != requestQueueTable.end(); ++it) {
    if (!(it->second->isDroped())) {
      string brokerKey = it->first.getBrokerName() + it->first.getTopic();
      if (brokerMqs.find(brokerKey) == brokerMqs.end()) {
        vector<MQMessageQueue>* mqs = new vector<MQMessageQueue>;
        brokerMqs[brokerKey] = mqs;
        brokerMqs[brokerKey]->push_back(it->first);
      } else {
        brokerMqs[brokerKey]->push_back(it->first);
      }
    }
  }
  LOG_INFO("LockAll " SIZET_FMT " broker mqs", brokerMqs.size());
  for (map<string, vector<MQMessageQueue>*>::iterator itb = brokerMqs.begin();
       itb != brokerMqs.end(); ++itb) {
    unique_ptr<FindBrokerResult> pFindBrokerResult(
        m_pClientFactory->findBrokerAddressInSubscribe(
            (*(itb->second))[0].getBrokerName(), MASTER_ID, true));
    unique_ptr<LockBatchRequestBody> lockBatchRequest(
        new LockBatchRequestBody());
    lockBatchRequest->setClientId(m_pConsumer->getMQClientId());
    lockBatchRequest->setConsumerGroup(m_pConsumer->getGroupName());
    lockBatchRequest->setMqSet(*(itb->second));
    LOG_INFO("try to lock:" SIZET_FMT " mqs of broker:%s", itb->second->size(),
             itb->first.c_str());
    try {
      vector<MQMessageQueue> messageQueues;
      m_pClientFactory->getMQClientAPIImpl()->lockBatchMQ(
          pFindBrokerResult->brokerAddr, lockBatchRequest.get(), messageQueues,
          1000, m_pConsumer->getSessionCredentials());
      for (unsigned int i = 0; i != messageQueues.size(); ++i) {
        PullRequest* pullreq = getPullRequest(messageQueues[i]);
        if (pullreq) {
          LOG_INFO("lockBatchMQ success of mq:%s",
                   messageQueues[i].toString().c_str());
          pullreq->setLocked(true);
          pullreq->setLastLockTimestamp(UtilAll::currentTimeMillis());
        } else {
          LOG_ERROR("lockBatchMQ fails of mq:%s",
                    messageQueues[i].toString().c_str());
        }
      }
      messageQueues.clear();
    } catch (MQException& e) {
      LOG_ERROR("lockBatchMQ fails");
    }
    deleteAndZero(itb->second);
  }
  brokerMqs.clear();
}
bool Rebalance::lock(MQMessageQueue mq) {
  unique_ptr<FindBrokerResult> pFindBrokerResult(
      m_pClientFactory->findBrokerAddressInSubscribe(mq.getBrokerName(),
                                                     MASTER_ID, true));
  unique_ptr<LockBatchRequestBody> lockBatchRequest(new LockBatchRequestBody());
  lockBatchRequest->setClientId(m_pConsumer->getMQClientId());
  lockBatchRequest->setConsumerGroup(m_pConsumer->getGroupName());
  vector<MQMessageQueue> in_mqSet;
  in_mqSet.push_back(mq);
  lockBatchRequest->setMqSet(in_mqSet);
  bool lockResult = false;

  try {
    vector<MQMessageQueue> messageQueues;
    LOG_DEBUG("try to lock mq:%s", mq.toString().c_str());
    m_pClientFactory->getMQClientAPIImpl()->lockBatchMQ(
        pFindBrokerResult->brokerAddr, lockBatchRequest.get(), messageQueues,
        1000, m_pConsumer->getSessionCredentials());
    if (messageQueues.size() == 0) {
      LOG_ERROR("lock mq on broker:%s failed",
                pFindBrokerResult->brokerAddr.c_str());
      return false;
    }
    for (unsigned int i = 0; i != messageQueues.size(); ++i) {
      PullRequest* pullreq = getPullRequest(messageQueues[i]);
      if (pullreq) {
        LOG_INFO("lock success of mq:%s", messageQueues[i].toString().c_str());
        pullreq->setLocked(true);
        pullreq->setLastLockTimestamp(UtilAll::currentTimeMillis());
        lockResult = true;
      } else {
        LOG_ERROR("lock fails of mq:%s", messageQueues[i].toString().c_str());
      }
    }
    messageQueues.clear();
    return lockResult;
  } catch (MQException& e) {
    LOG_ERROR("lock fails of mq:%s", mq.toString().c_str());
    return false;
  }
}

//<!************************************************************************
RebalancePull::RebalancePull(MQConsumer* consumer, MQClientFactory* pfactory)
    : Rebalance(consumer, pfactory) {}

bool RebalancePull::updateRequestTableInRebalance(
    const string& topic, vector<MQMessageQueue>& mqsSelf) {
  return false;
}

int64 RebalancePull::computePullFromWhere(const MQMessageQueue& mq) {
  return 0;
}

void RebalancePull::messageQueueChanged(const string& topic,
                                        vector<MQMessageQueue>& mqAll,
                                        vector<MQMessageQueue>& mqDivided) {}

void RebalancePull::removeUnnecessaryMessageQueue(const MQMessageQueue& mq) {}

//<!***************************************************************************
RebalancePush::RebalancePush(MQConsumer* consumer, MQClientFactory* pfactory)
    : Rebalance(consumer, pfactory) {}

bool RebalancePush::updateRequestTableInRebalance(
    const string& topic, vector<MQMessageQueue>& mqsSelf) {
  LOG_DEBUG("updateRequestTableInRebalance Enter");
  if (mqsSelf.empty()) {
    LOG_WARN("allocated queue is empty for topic:%s", topic.c_str());
  }

  bool changed = false;

  //<!remove
  MQ2PULLREQ requestQueueTable(getPullRequestTable());
  MQ2PULLREQ::iterator it = requestQueueTable.begin();
  for (; it != requestQueueTable.end(); ++it) {
    MQMessageQueue mqtemp = it->first;
    if (mqtemp.getTopic().compare(topic) == 0) {
      if (mqsSelf.empty() ||
          (find(mqsSelf.begin(), mqsSelf.end(), mqtemp) == mqsSelf.end())) {
        if (!(it->second->isDroped())) {
          it->second->setDroped(true);
          removeUnnecessaryMessageQueue(mqtemp);
          it->second->clearAllMsgs();  // add clear operation to avoid bad state
                                       // when dropped pullRequest returns
                                       // normal
          LOG_INFO("drop mq:%s", mqtemp.toString().c_str());
        }
        changed = true;
      }
    }
  }

  //<!add
  vector<PullRequest*> pullrequestAdd;
  DefaultMQPushConsumer* pConsumer =
      static_cast<DefaultMQPushConsumer*>(m_pConsumer);
  vector<MQMessageQueue>::iterator it2 = mqsSelf.begin();
  for (; it2 != mqsSelf.end(); ++it2) {
    PullRequest* pPullRequest(getPullRequest(*it2));
    if (pPullRequest && pPullRequest->isDroped()) {
      LOG_DEBUG(
          "before resume the pull handle of this pullRequest, its mq is:%s, "
          "its offset is:%lld",
          (it2->toString()).c_str(), pPullRequest->getNextOffset());
      pConsumer->getOffsetStore()->removeOffset(
          *it2);  // remove dirty offset which maybe update to
                  // OffsetStore::m_offsetTable by consuming After last
                  // drop
      int64 nextOffset = computePullFromWhere(*it2);
      if (nextOffset >= 0) {
        /*
          Fix issue with following scenario:
          1. pullRequest was dropped
          2. the pullMsgEvent was not executed by taskQueue, so the PullMsgEvent
          was not stop
          3. pullReuest was resumed by next doRebalance, then mulitple
          pullMsgEvent were produced for pullRequest
        */
        bool bPullMsgEvent = pPullRequest->addPullMsgEvent();
        while (!bPullMsgEvent) {
          boost::this_thread::sleep_for(boost::chrono::milliseconds(50));
          LOG_INFO("pullRequest with mq :%s has unfinished pullMsgEvent",
                   (it2->toString()).c_str());
          bPullMsgEvent = pPullRequest->addPullMsgEvent();
        }
        pPullRequest->setDroped(false);
        pPullRequest->clearAllMsgs();  // avoid consume accumulation and consume
                                       // dumplication issues
        pPullRequest->setNextOffset(nextOffset);
        pPullRequest->updateQueueMaxOffset(nextOffset);
        LOG_INFO(
            "after resume the pull handle of this pullRequest, its mq is:%s, "
            "its offset is:%lld",
            (it2->toString()).c_str(), pPullRequest->getNextOffset());
        changed = true;
        pConsumer->producePullMsgTask(pPullRequest);
      } else {
        LOG_ERROR(
            "get fatel error QueryOffset of mq:%s, do not reconsume this queue",
            (it2->toString()).c_str());
      }
    }

    if (!pPullRequest) {
      LOG_INFO("updateRequestTableInRebalance Doesn't find old mq");
      PullRequest* pullRequest = new PullRequest(m_pConsumer->getGroupName());
      pullRequest->m_messageQueue = *it2;

      int64 nextOffset = computePullFromWhere(*it2);
      if (nextOffset >= 0) {
        pullRequest->setNextOffset(nextOffset);
        pullRequest->clearAllMsgs();  // avoid consume accumulation and consume
                                      // dumplication issues
        changed = true;
        //<! mq-> pq;
        addPullRequest(*it2, pullRequest);
        pullrequestAdd.push_back(pullRequest);
        LOG_INFO("add mq:%s, request initiall offset:%lld",
                 (*it2).toString().c_str(), nextOffset);
      }
    }
  }

  vector<PullRequest*>::iterator it3 = pullrequestAdd.begin();
  for (; it3 != pullrequestAdd.end(); ++it3) {
    LOG_DEBUG("start pull request");
    pConsumer->producePullMsgTask(*it3);
  }

  LOG_DEBUG("updateRequestTableInRebalance exit");
  return changed;
}

int64 RebalancePush::computePullFromWhere(const MQMessageQueue& mq) {
  int64 result = -1;
  DefaultMQPushConsumer* pConsumer =
      static_cast<DefaultMQPushConsumer*>(m_pConsumer);
  ConsumeFromWhere consumeFromWhere = pConsumer->getConsumeFromWhere();
  OffsetStore* pOffsetStore = pConsumer->getOffsetStore();
  switch (consumeFromWhere) {
    case CONSUME_FROM_LAST_OFFSET: {
      int64 lastOffset = pOffsetStore->readOffset(
          mq, READ_FROM_STORE, m_pConsumer->getSessionCredentials());
      if (lastOffset >= 0) {
        LOG_INFO("CONSUME_FROM_LAST_OFFSET, lastOffset of mq:%s is:%lld",
                 mq.toString().c_str(), lastOffset);
        result = lastOffset;
      } else if (-1 == lastOffset) {
        LOG_WARN("CONSUME_FROM_LAST_OFFSET, lastOffset of mq:%s is -1",
                 mq.toString().c_str());
        if (UtilAll::startsWith_retry(mq.getTopic())) {
          LOG_INFO("CONSUME_FROM_LAST_OFFSET, lastOffset of mq:%s is 0",
                   mq.toString().c_str());
          result = 0;
        } else {
          try {
            result = pConsumer->maxOffset(mq);
            LOG_INFO("CONSUME_FROM_LAST_OFFSET, maxOffset of mq:%s is:%lld",
                     mq.toString().c_str(), result);
          } catch (MQException& e) {
            LOG_ERROR(
                "CONSUME_FROM_LAST_OFFSET error, lastOffset  of mq:%s is -1",
                mq.toString().c_str());
            result = -1;
          }
        }
      } else {
        LOG_ERROR("CONSUME_FROM_LAST_OFFSET error, lastOffset  of mq:%s is -1",
                  mq.toString().c_str());
        result = -1;
      }
      break;
    }
    case CONSUME_FROM_FIRST_OFFSET: {
      int64 lastOffset = pOffsetStore->readOffset(
          mq, READ_FROM_STORE, m_pConsumer->getSessionCredentials());
      if (lastOffset >= 0) {
        LOG_INFO("CONSUME_FROM_FIRST_OFFSET, lastOffset of mq:%s is:%lld",
                 mq.toString().c_str(), lastOffset);
        result = lastOffset;
      } else if (-1 == lastOffset) {
        LOG_INFO("CONSUME_FROM_FIRST_OFFSET, lastOffset of mq:%s, return 0",
                 mq.toString().c_str());
        result = 0;
      } else {
        LOG_ERROR("CONSUME_FROM_FIRST_OFFSET, lastOffset of mq:%s, return -1",
                  mq.toString().c_str());
        result = -1;
      }
      break;
    }
    case CONSUME_FROM_TIMESTAMP: {
      int64 lastOffset = pOffsetStore->readOffset(
          mq, READ_FROM_STORE, m_pConsumer->getSessionCredentials());
      if (lastOffset >= 0) {
        LOG_INFO("CONSUME_FROM_TIMESTAMP, lastOffset of mq:%s is:%lld",
                 mq.toString().c_str(), lastOffset);
        result = lastOffset;
      } else if (-1 == lastOffset) {
        if (UtilAll::startsWith_retry(mq.getTopic())) {
          try {
            result = pConsumer->maxOffset(mq);
            LOG_INFO("CONSUME_FROM_TIMESTAMP, maxOffset  of mq:%s is:%lld",
                     mq.toString().c_str(), result);
          } catch (MQException& e) {
            LOG_ERROR(
                "CONSUME_FROM_TIMESTAMP error, lastOffset  of mq:%s is -1",
                mq.toString().c_str());
            result = -1;
          }
        } else {
          try {
          } catch (MQException& e) {
            LOG_ERROR(
                "CONSUME_FROM_TIMESTAMP error, lastOffset  of mq:%s, return 0",
                mq.toString().c_str());
            result = -1;
          }
        }
      } else {
        LOG_ERROR(
            "CONSUME_FROM_TIMESTAMP error, lastOffset  of mq:%s, return -1",
            mq.toString().c_str());
        result = -1;
      }
      break;
    }
    default:
      break;
  }
  return result;
}

void RebalancePush::messageQueueChanged(const string& topic,
                                        vector<MQMessageQueue>& mqAll,
                                        vector<MQMessageQueue>& mqDivided) {}

void RebalancePush::removeUnnecessaryMessageQueue(const MQMessageQueue& mq) {
  DefaultMQPushConsumer* pConsumer =
      static_cast<DefaultMQPushConsumer*>(m_pConsumer);
  OffsetStore* pOffsetStore = pConsumer->getOffsetStore();

  pOffsetStore->persist(mq, m_pConsumer->getSessionCredentials());
  pOffsetStore->removeOffset(mq);
  if (pConsumer->getMessageListenerType() == messageListenerOrderly) {
    unlock(mq);
  }
}

//<!************************************************************************
}  //<!end namespace;
