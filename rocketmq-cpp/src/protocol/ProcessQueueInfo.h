#ifndef __PROCESSQUEUEINFO_H__
#define __PROCESSQUEUEINFO_H__

#include "UtilAll.h"
#include "json/json.h"

namespace rocketmq {
class ProcessQueueInfo {
 public:
  ProcessQueueInfo() {
    commitOffset = 0;
    cachedMsgMinOffset = 0;
    cachedMsgMaxOffset = 0;
    cachedMsgCount = 0;
    transactionMsgMinOffset = 0;
    transactionMsgMaxOffset = 0;
    transactionMsgCount = 0;
    locked = false;
    tryUnlockTimes = 0;
    lastLockTimestamp = 123;
    droped = false;
    lastPullTimestamp = 0;
    lastConsumeTimestamp = 0;
  }
  virtual ~ProcessQueueInfo() {}

 public:
  const uint64 getCommitOffset() const { return commitOffset; }

  void setCommitOffset(uint64 input_commitOffset) {
    commitOffset = input_commitOffset;
  }

  void setLocked(bool in_locked) { locked = in_locked; }

  const bool isLocked() const { return locked; }

  void setDroped(bool in_dropped) { droped = in_dropped; }

  const bool isDroped() const { return droped; }

  Json::Value toJson() const {
    Json::Value outJson;
    outJson["commitOffset"] = (UtilAll::to_string(commitOffset)).c_str();
    outJson["cachedMsgMinOffset"] =
        (UtilAll::to_string(cachedMsgMinOffset)).c_str();
    outJson["cachedMsgMaxOffset"] =
        (UtilAll::to_string(cachedMsgMaxOffset)).c_str();
    outJson["cachedMsgCount"] = (int)(cachedMsgCount);
    outJson["transactionMsgMinOffset"] =
        (UtilAll::to_string(transactionMsgMinOffset)).c_str();
    outJson["transactionMsgMaxOffset"] =
        (UtilAll::to_string(transactionMsgMaxOffset)).c_str();
    outJson["transactionMsgCount"] = (int)(transactionMsgCount);
    outJson["locked"] = (locked);
    outJson["tryUnlockTimes"] = (int)(tryUnlockTimes);
    outJson["lastLockTimestamp"] =
        (UtilAll::to_string(lastLockTimestamp)).c_str();
    outJson["droped"] = (droped);
    outJson["lastPullTimestamp"] =
        (UtilAll::to_string(lastPullTimestamp)).c_str();
    outJson["lastConsumeTimestamp"] =
        (UtilAll::to_string(lastConsumeTimestamp)).c_str();

    return outJson;
  }

 public:
  uint64 commitOffset;
  uint64 cachedMsgMinOffset;
  uint64 cachedMsgMaxOffset;
  int cachedMsgCount;
  uint64 transactionMsgMinOffset;
  uint64 transactionMsgMaxOffset;
  int transactionMsgCount;
  bool locked;
  int tryUnlockTimes;
  uint64 lastLockTimestamp;

  bool droped;
  uint64 lastPullTimestamp;
  uint64 lastConsumeTimestamp;
};
}

#endif
