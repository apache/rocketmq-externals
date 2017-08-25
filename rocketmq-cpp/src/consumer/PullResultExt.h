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
#include "dataBlock.h"

namespace rocketmq {
/**
 * 只在内部使用，不对外公开
 */
//<!***************************************************************************
class PullResultExt : public PullResult {
 public:
  PullResultExt(PullStatus pullStatus, int64 nextBeginOffset, int64 minOffset,
                int64 maxOffset, int suggestWhichBrokerId,
                const MemoryBlock& messageBinary)
      : PullResult(pullStatus, nextBeginOffset, minOffset, maxOffset),
        suggestWhichBrokerId(suggestWhichBrokerId),
        msgMemBlock(messageBinary) {}
  PullResultExt(PullStatus pullStatus, int64 nextBeginOffset, int64 minOffset,
                int64 maxOffset, int suggestWhichBrokerId)
      : PullResult(pullStatus, nextBeginOffset, minOffset, maxOffset),
        suggestWhichBrokerId(suggestWhichBrokerId) {}
  virtual ~PullResultExt() {}

 public:
  int suggestWhichBrokerId;
  MemoryBlock msgMemBlock;
};

}  //<!end namespace;
