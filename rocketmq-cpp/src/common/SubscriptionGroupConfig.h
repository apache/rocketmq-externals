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
#ifndef __SUBSCRIPTIONGROUPCONFIG_H__
#define __SUBSCRIPTIONGROUPCONFIG_H__

#include <string>

namespace rocketmq {
//<!***************************************************************************
class SubscriptionGroupConfig {
 public:
  SubscriptionGroupConfig(const string& groupName) {
    this->groupName = groupName;
    consumeEnable = true;
    consumeFromMinEnable = true;
    consumeBroadcastEnable = true;
    retryQueueNums = 1;
    retryMaxTimes = 5;
    brokerId = MASTER_ID;
    whichBrokerWhenConsumeSlowly = 1;
  }

  string groupName;
  bool consumeEnable;
  bool consumeFromMinEnable;
  bool consumeBroadcastEnable;
  int retryQueueNums;
  int retryMaxTimes;
  int brokerId;
  int whichBrokerWhenConsumeSlowly;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
