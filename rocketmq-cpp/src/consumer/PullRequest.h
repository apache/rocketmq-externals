/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __PULLREQUEST_H__
#define __PULLREQUEST_H__

#include <boost/atomic.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include "ByteOrder.h"
#include "MQMessageExt.h"
#include "MQMessageQueue.h"

namespace metaq {
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
};
//<!************************************************************************
}  //<!end namespace;

#endif
