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
#ifndef __PULLRESULT_H__
#define __PULLRESULT_H__

#include <sstream>
#include "MQMessageExt.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!***************************************************************************
enum PullStatus {
  FOUND,
  NO_NEW_MSG,
  NO_MATCHED_MSG,
  OFFSET_ILLEGAL,
  BROKER_TIMEOUT  // indicate pull request timeout or received NULL response
};

static const char* EnumStrings[] = {"FOUND", "NO_NEW_MSG", "NO_MATCHED_MSG",
                                    "OFFSET_ILLEGAL", "BROKER_TIMEOUT"};

//<!***************************************************************************
class ROCKETMQCLIENT_API PullResult {
 public:
  PullResult();
  PullResult(PullStatus status);
  PullResult(PullStatus pullStatus, int64 nextBeginOffset,
             int64 minOffset, int64 maxOffset);

  PullResult(PullStatus pullStatus, int64 nextBeginOffset,
             int64 minOffset, int64 maxOffset,
             const std::vector<MQMessageExt>& src);

  virtual ~PullResult();

  std::string toString() {
    std::stringstream ss;
    ss << "PullResult [ pullStatus=" << EnumStrings[pullStatus]
       << ", nextBeginOffset=" << nextBeginOffset << ", minOffset=" << minOffset
       << ", maxOffset=" << maxOffset
       << ", msgFoundList=" << msgFoundList.size() << " ]";
    return ss.str();
  }

 public:
  PullStatus pullStatus;
  int64 nextBeginOffset;
  int64 minOffset;
  int64 maxOffset;
  std::vector<MQMessageExt> msgFoundList;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
