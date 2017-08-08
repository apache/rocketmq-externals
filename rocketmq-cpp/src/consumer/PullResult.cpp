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
#include "PullResult.h"
#include "UtilAll.h"

namespace rocketmq {
//<!************************************************************************
PullResult::PullResult()
    : pullStatus(NO_MATCHED_MSG),
      nextBeginOffset(0),
      minOffset(0),
      maxOffset(0) {}

PullResult::PullResult(PullStatus status)
    : pullStatus(status), nextBeginOffset(0), minOffset(0), maxOffset(0) {}

PullResult::PullResult(PullStatus pullStatus, int64 nextBeginOffset,
                       int64 minOffset, int64 maxOffset)
    : pullStatus(pullStatus),
      nextBeginOffset(nextBeginOffset),
      minOffset(minOffset),
      maxOffset(maxOffset) {}

PullResult::PullResult(PullStatus pullStatus, int64 nextBeginOffset,
                       int64 minOffset, int64 maxOffset,
                       const vector<MQMessageExt>& src)
    : pullStatus(pullStatus),
      nextBeginOffset(nextBeginOffset),
      minOffset(minOffset),
      maxOffset(maxOffset) {
  msgFoundList.reserve(src.size());
  for (size_t i = 0; i < src.size(); i++) {
    msgFoundList.push_back(src[i]);
  }
}

PullResult::~PullResult() { msgFoundList.clear(); }

//<!***************************************************************************
}  //<!end namespace;
