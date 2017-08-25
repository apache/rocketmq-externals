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
#include "PullAPIWrapper.h"
#include "CommunicationMode.h"
#include "MQClientFactory.h"
#include "PullResultExt.h"
#include "PullSysFlag.h"
namespace rocketmq {
//<!************************************************************************
PullAPIWrapper::PullAPIWrapper(MQClientFactory* mQClientFactory,
                               const string& consumerGroup) {
  m_MQClientFactory = mQClientFactory;
  m_consumerGroup = consumerGroup;
}

PullAPIWrapper::~PullAPIWrapper() {
  m_MQClientFactory = NULL;
  m_pullFromWhichNodeTable.clear();
}

void PullAPIWrapper::updatePullFromWhichNode(const MQMessageQueue& mq,
                                             int brokerId) {
  boost::lock_guard<boost::mutex> lock(m_lock);
  m_pullFromWhichNodeTable[mq] = brokerId;
}

int PullAPIWrapper::recalculatePullFromWhichNode(const MQMessageQueue& mq) {
  boost::lock_guard<boost::mutex> lock(m_lock);
  if (m_pullFromWhichNodeTable.find(mq) != m_pullFromWhichNodeTable.end()) {
    return m_pullFromWhichNodeTable[mq];
  }
  return MASTER_ID;
}

PullResult PullAPIWrapper::processPullResult(
    const MQMessageQueue& mq, PullResult* pullResult,
    SubscriptionData* subscriptionData) {
  PullResultExt* pResultExt = static_cast<PullResultExt*>(pullResult);
  if (pResultExt == NULL) {
    string errMsg("The pullResult NULL of");
    errMsg.append(mq.toString());
    THROW_MQEXCEPTION(MQClientException, errMsg, -1);
  }

  //<!update;
  updatePullFromWhichNode(mq, pResultExt->suggestWhichBrokerId);

  vector<MQMessageExt> msgFilterList;
  if (pResultExt->pullStatus == FOUND) {
    //<!decode all msg list;
    vector<MQMessageExt> msgAllList;
    MQDecoder::decodes(&pResultExt->msgMemBlock, msgAllList);

    //<!filter msg list again;
    if (subscriptionData != NULL && !subscriptionData->getTagsSet().empty()) {
      msgFilterList.reserve(msgAllList.size());
      vector<MQMessageExt>::iterator it = msgAllList.begin();
      for (; it != msgAllList.end(); ++it) {
        string msgTag = (*it).getTags();
        if (subscriptionData->containTag(msgTag)) {
          msgFilterList.push_back(*it);
        }
      }
    } else
    {
      msgFilterList.swap(msgAllList);
    }
  }

  return PullResult(pResultExt->pullStatus, pResultExt->nextBeginOffset,
                    pResultExt->minOffset, pResultExt->maxOffset,
                    msgFilterList);
}

PullResult* PullAPIWrapper::pullKernelImpl(
    const MQMessageQueue& mq,        // 1
    string subExpression,            // 2
    int64 subVersion,                // 3
    int64 offset,                    // 4
    int maxNums,                     // 5
    int sysFlag,                     // 6
    int64 commitOffset,              // 7
    int brokerSuspendMaxTimeMillis,  // 8
    int timeoutMillis,               // 9
    int communicationMode,           // 10
    PullCallback* pullCallback, const SessionCredentials& session_credentials,
    void* pArg /*= NULL*/) {
  unique_ptr<FindBrokerResult> pFindBrokerResult(
      m_MQClientFactory->findBrokerAddressInSubscribe(
          mq.getBrokerName(), recalculatePullFromWhichNode(mq), false));
  //<!goto nameserver;
  if (pFindBrokerResult == NULL) {
    m_MQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic(),
                                                          session_credentials);
    pFindBrokerResult.reset(m_MQClientFactory->findBrokerAddressInSubscribe(
        mq.getBrokerName(), recalculatePullFromWhichNode(mq), false));
  }

  if (pFindBrokerResult != NULL) {
    int sysFlagInner = sysFlag;

    if (pFindBrokerResult->slave) {
      sysFlagInner = PullSysFlag::clearCommitOffsetFlag(sysFlagInner);
    }

    PullMessageRequestHeader* pRequestHeader = new PullMessageRequestHeader();
    pRequestHeader->consumerGroup = m_consumerGroup;
    pRequestHeader->topic = mq.getTopic();
    pRequestHeader->queueId = mq.getQueueId();
    pRequestHeader->queueOffset = offset;
    pRequestHeader->maxMsgNums = maxNums;
    pRequestHeader->sysFlag = sysFlagInner;
    pRequestHeader->commitOffset = commitOffset;
    pRequestHeader->suspendTimeoutMillis = brokerSuspendMaxTimeMillis;
    pRequestHeader->subscription = subExpression;
    pRequestHeader->subVersion = subVersion;

    return m_MQClientFactory->getMQClientAPIImpl()->pullMessage(
        pFindBrokerResult->brokerAddr, pRequestHeader, timeoutMillis,
        communicationMode, pullCallback, pArg, session_credentials);
  }
  THROW_MQEXCEPTION(MQClientException, "The broker not exist", -1);
}

}  //<!end namespace;
