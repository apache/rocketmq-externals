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

#ifndef __ALLOCATEMESSAGEQUEUESTRATEGY_H__
#define __ALLOCATEMESSAGEQUEUESTRATEGY_H__

#include "Logging.h"
#include "MQClientException.h"
#include "MQMessageQueue.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!***************************************************************************
class AllocateMQStrategy {
 public:
  virtual ~AllocateMQStrategy() {}
  virtual void allocate(const std::string& currentCID,
                        std::vector<MQMessageQueue>& mqAll,
                        std::vector<std::string>& cidAll,
                        std::vector<MQMessageQueue>& outReuslt) = 0;
};

//<!************************************************************************
class AllocateMQAveragely : public AllocateMQStrategy {
 public:
  virtual ~AllocateMQAveragely() {}
  virtual void allocate(const std::string& currentCID,
                        std::vector<MQMessageQueue>& mqAll,
                        std::vector<std::string>& cidAll,
                        std::vector<MQMessageQueue>& outReuslt) {
    outReuslt.clear();
    if (currentCID.empty()) {
      THROW_MQEXCEPTION(MQClientException, "currentCID is empty", -1);
    }

    if (mqAll.empty()) {
      THROW_MQEXCEPTION(MQClientException, "mqAll is empty", -1);
    }

    if (cidAll.empty()) {
      THROW_MQEXCEPTION(MQClientException, "cidAll is empty", -1);
    }

    int index = -1;
    int cidAllSize = cidAll.size();
    for (int i = 0; i < cidAllSize; i++) {
      if (cidAll[i] == currentCID) {
        index = i;
        break;
      }
    }

    if (index == -1) {
      LOG_ERROR("could not find clientId from Broker");
      return;
    }

    int mqAllSize = mqAll.size();
    int mod = mqAllSize % cidAllSize;
    int averageSize = mqAllSize <= cidAllSize
                          ? 1
                          : (mod > 0 && index < mod ? mqAllSize / cidAllSize + 1
                                                    : mqAllSize / cidAllSize);
    int startIndex = (mod > 0 && index < mod) ? index * averageSize
                                              : index * averageSize + mod;
    int range = (std::min)(averageSize, mqAllSize - startIndex);
    LOG_INFO(
        "range is:%d, index is:%d, mqAllSize is:%d, averageSize is:%d, "
        "startIndex is:%d",
        range, index, mqAllSize, averageSize, startIndex);
    //<!out;
    if (range >= 0)  // example: range is:-1, index is:1, mqAllSize is:1,
                     // averageSize is:1, startIndex is:2
    {
      for (int i = 0; i < range; i++) {
        if ((startIndex + i) >= 0) {
          outReuslt.push_back(mqAll.at((startIndex + i) % mqAllSize));
        }
      }
    }
  }
};

//<!***************************************************************************
}  //<!end namespace;
#endif
