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
#ifndef __PULLREQUEST_H__
#define __PULLREQUEST_H__

#include <boost/atomic.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include "MQMessageExt.h"
#include "MQMessageQueue.h"
#include "UtilAll.h"
namespace rocketmq {
//<!***************************************************************************
class PullRequest {
 public:
  PullRequest(const string& groupname);
  virtual ~PullRequest();

  void putMessage(vector<MQMessageExt>& msgs);
  void getMessage(vector<MQMessageExt>& msgs);
  int64 getCacheMinOffset();
  int64 getCacheMaxOffset();
  int getCacheMsgCount();
  void getMessageByQueueOffset(vector<MQMessageExt>& msgs, int64 minQueueOffset,
                               int64 maxQueueOffset);
  int64 removeMessage(vector<MQMessageExt>& msgs);
  void clearAllMsgs();

  PullRequest& operator=(const PullRequest& other);

  void setDroped(bool droped);
  bool isDroped() const;

  int64 getNextOffset();
  void setNextOffset(int64 nextoffset);

  string getGroupName() const;

  void updateQueueMaxOffset(int64 queueOffset);

  void setLocked(bool Locked);
  bool isLocked() const;
  bool isLockExpired() const;
  void setLastLockTimestamp(int64 time);
  int64 getLastLockTimestamp() const;
  void setLastPullTimestamp(uint64 time);
  uint64 getLastPullTimestamp() const;
  void setLastConsumeTimestamp(uint64 time);
  uint64 getLastConsumeTimestamp() const;
  void setTryUnlockTimes(int time);
  int getTryUnlockTimes() const;
  void takeMessages(vector<MQMessageExt>& msgs, int batchSize);
  int64 commit();
  void makeMessageToCosumeAgain(vector<MQMessageExt>& msgs);
  boost::timed_mutex& getPullRequestCriticalSection();
  void removePullMsgEvent();
  bool addPullMsgEvent();

 public:
  MQMessageQueue m_messageQueue;
  static const uint64 RebalanceLockInterval;     // ms
  static const uint64 RebalanceLockMaxLiveTime;  // ms

 private:
  string m_groupname;
  int64 m_nextOffset;
  int64 m_queueOffsetMax;
  boost::atomic<bool> m_bDroped;
  boost::atomic<bool> m_bLocked;
  map<int64, MQMessageExt> m_msgTreeMap;
  map<int64, MQMessageExt> m_msgTreeMapTemp;
  boost::mutex m_pullRequestLock;
  uint64 m_lastLockTimestamp;  // ms
  uint64 m_tryUnlockTimes;
  uint64 m_lastPullTimestamp;
  uint64 m_lastConsumeTimestamp;
  boost::timed_mutex m_consumeLock;
  boost::atomic<bool> m_bPullMsgEventInprogress;
};
//<!************************************************************************
}  //<!end namespace;

#endif
