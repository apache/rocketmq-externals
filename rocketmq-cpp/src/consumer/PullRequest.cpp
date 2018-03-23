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
#include "PullRequest.h"
#include "Logging.h"

namespace rocketmq {
//<!***************************************************************************
const uint64 PullRequest::RebalanceLockInterval = 20 * 1000;
const uint64 PullRequest::RebalanceLockMaxLiveTime = 30 * 1000;

PullRequest::PullRequest(const string& groupname)
    : m_groupname(groupname),
      m_nextOffset(0),
      m_queueOffsetMax(0),
      m_bDroped(false),
      m_bLocked(false),
      m_bPullMsgEventInprogress(false) {}

PullRequest::~PullRequest() {
  m_msgTreeMapTemp.clear();
  m_msgTreeMap.clear();
}

PullRequest& PullRequest::operator=(const PullRequest& other) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  if (this != &other) {
    m_groupname = other.m_groupname;
    m_nextOffset = other.m_nextOffset;
    m_bDroped.store(other.m_bDroped.load());
    m_queueOffsetMax = other.m_queueOffsetMax;
    m_messageQueue = other.m_messageQueue;
    m_msgTreeMap = other.m_msgTreeMap;
    m_msgTreeMapTemp = other.m_msgTreeMapTemp;
  }
  return *this;
}

void PullRequest::putMessage(vector<MQMessageExt>& msgs) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);

  vector<MQMessageExt>::iterator it = msgs.begin();
  for (; it != msgs.end(); it++) {
    m_msgTreeMap[it->getQueueOffset()] = *it;
    m_queueOffsetMax = (std::max)(m_queueOffsetMax, it->getQueueOffset());
  }
  LOG_DEBUG("PullRequest: putMessage m_queueOffsetMax:%lld ", m_queueOffsetMax);
}

void PullRequest::getMessage(vector<MQMessageExt>& msgs) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);

  map<int64, MQMessageExt>::iterator it = m_msgTreeMap.begin();
  for (; it != m_msgTreeMap.end(); it++) {
    msgs.push_back(it->second);
  }
}

int64 PullRequest::getCacheMinOffset() {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  if (m_msgTreeMap.empty()) {
    return 0;
  } else {
    map<int64, MQMessageExt>::iterator it = m_msgTreeMap.begin();
    MQMessageExt msg = it->second;
    return msg.getQueueOffset();
  }
}

int64 PullRequest::getCacheMaxOffset() { return m_queueOffsetMax; }

int PullRequest::getCacheMsgCount() {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  return m_msgTreeMap.size();
}

void PullRequest::getMessageByQueueOffset(vector<MQMessageExt>& msgs,
                                          int64 minQueueOffset,
                                          int64 maxQueueOffset) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);

  int64 it = minQueueOffset;
  for (; it <= maxQueueOffset; it++) {
    msgs.push_back(m_msgTreeMap[it]);
  }
}

int64 PullRequest::removeMessage(vector<MQMessageExt>& msgs) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);

  int64 result = -1;
  LOG_DEBUG("m_queueOffsetMax is:%lld", m_queueOffsetMax);
  if (!m_msgTreeMap.empty()) {
    result = m_queueOffsetMax + 1;
    LOG_DEBUG(
        " offset result is:%lld, m_queueOffsetMax is:%lld, msgs size:" SIZET_FMT
        "",
        result, m_queueOffsetMax, msgs.size());
    vector<MQMessageExt>::iterator it = msgs.begin();
    for (; it != msgs.end(); it++) {
      LOG_DEBUG("remove these msg from m_msgTreeMap, its offset:%lld",
                it->getQueueOffset());
      m_msgTreeMap.erase(it->getQueueOffset());
    }

    if (!m_msgTreeMap.empty()) {
      map<int64, MQMessageExt>::iterator it = m_msgTreeMap.begin();
      result = it->first;
      LOG_INFO("cache msg size:" SIZET_FMT
               " of pullRequest:%s, return offset result is:%lld",
               m_msgTreeMap.size(), m_messageQueue.toString().c_str(), result);
    }
  }

  return result;
}

void PullRequest::clearAllMsgs() {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);

  if (isDroped()) {
    LOG_DEBUG("clear m_msgTreeMap as PullRequest had been dropped.");
    m_msgTreeMap.clear();
    m_msgTreeMapTemp.clear();
  }
}

void PullRequest::updateQueueMaxOffset(int64 queueOffset) {
  // following 2 cases which may set queueOffset smaller than m_queueOffsetMax:
  // 1. resetOffset cmd
  // 2. during  rebalance, if configured with CONSUMER_FROM_FIRST_OFFSET, when
  // readOffset called by computePullFromWhere was failed,  m_nextOffset will be
  // setted to 0
  m_queueOffsetMax = queueOffset;
}

void PullRequest::setDroped(bool droped) {
  int temp = (droped == true ? 1 : 0);
  m_bDroped.store(temp);
  /*
  m_queueOffsetMax = 0;
  m_nextOffset = 0;
  //the reason why not clear m_queueOffsetMax and m_nextOffset is due to
  ConsumeMsgService and drop mq are concurrent running.
      consider following situation:
      1>. ConsumeMsgService running
      2>. dorebalance, drop mq, reset m_nextOffset and m_queueOffsetMax
      3>. ConsumeMsgService calls removeMessages, if no other msgs in
  m_msgTreeMap, m_queueOffsetMax(0)+1 will return;
      4>. updateOffset with 1, which is more smaller than correct offset.
  */
}

bool PullRequest::isDroped() const { return m_bDroped.load() == 1; }

int64 PullRequest::getNextOffset() {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  return m_nextOffset;
}

void PullRequest::setLocked(bool Locked) {
  int temp = (Locked == true ? 1 : 0);
  m_bLocked.store(temp);
}
bool PullRequest::isLocked() const { return m_bLocked.load() == 1; }

bool PullRequest::isLockExpired() const {
  return (UtilAll::currentTimeMillis() - m_lastLockTimestamp) >
         RebalanceLockMaxLiveTime;
}

void PullRequest::setLastLockTimestamp(int64 time) {
  m_lastLockTimestamp = time;
}

int64 PullRequest::getLastLockTimestamp() const { return m_lastLockTimestamp; }

void PullRequest::setLastPullTimestamp(uint64 time) {
  m_lastPullTimestamp = time;
}

uint64 PullRequest::getLastPullTimestamp() const { return m_lastPullTimestamp; }

void PullRequest::setLastConsumeTimestamp(uint64 time) {
  m_lastConsumeTimestamp = time;
}

uint64 PullRequest::getLastConsumeTimestamp() const {
  return m_lastConsumeTimestamp;
}

void PullRequest::setTryUnlockTimes(int time) { m_lastLockTimestamp = time; }

int PullRequest::getTryUnlockTimes() const { return m_lastLockTimestamp; }

void PullRequest::setNextOffset(int64 nextoffset) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  m_nextOffset = nextoffset;
}

string PullRequest::getGroupName() const { return m_groupname; }

boost::timed_mutex& PullRequest::getPullRequestCriticalSection() {
  return m_consumeLock;
}

void PullRequest::takeMessages(vector<MQMessageExt>& msgs, int batchSize) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  for (int i = 0; i != batchSize; i++) {
    map<int64, MQMessageExt>::iterator it = m_msgTreeMap.begin();
    if (it != m_msgTreeMap.end()) {
      msgs.push_back(it->second);
      m_msgTreeMapTemp[it->first] = it->second;
      m_msgTreeMap.erase(it);
    }
  }
}

void PullRequest::makeMessageToCosumeAgain(vector<MQMessageExt>& msgs) {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  for (unsigned int it = 0; it != msgs.size(); ++it) {
    m_msgTreeMap[msgs[it].getQueueOffset()] = msgs[it];
    m_msgTreeMapTemp.erase(msgs[it].getQueueOffset());
  }
}

int64 PullRequest::commit() {
  boost::lock_guard<boost::mutex> lock(m_pullRequestLock);
  if (!m_msgTreeMapTemp.empty()) {
    int64 offset = (--m_msgTreeMapTemp.end())->first;
    m_msgTreeMapTemp.clear();
    return offset + 1;
  } else {
    return -1;
  }
}

void PullRequest::removePullMsgEvent() { m_bPullMsgEventInprogress = false; }

bool PullRequest::addPullMsgEvent() {
  if (m_bPullMsgEventInprogress == false) {
    m_bPullMsgEventInprogress = true;
    LOG_INFO("pullRequest with mq :%s set pullMsgEvent",
             m_messageQueue.toString().c_str());
    return true;
  }
  return false;
}

//<!***************************************************************************
}  //<!end namespace;
