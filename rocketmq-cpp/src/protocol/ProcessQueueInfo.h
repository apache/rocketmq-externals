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
