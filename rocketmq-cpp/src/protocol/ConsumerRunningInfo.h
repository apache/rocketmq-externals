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
#ifndef __CONSUMERRUNNINGINFO_H__
#define __CONSUMERRUNNINGINFO_H__

#include "MessageQueue.h"
#include "ProcessQueueInfo.h"
#include "SubscriptionData.h"

namespace rocketmq {

class ConsumerRunningInfo {
 public:
  ConsumerRunningInfo() {}
  virtual ~ConsumerRunningInfo() {
    properties.clear();
    mqTable.clear();
    subscriptionSet.clear();
  }

 public:
  static const string PROP_NAMESERVER_ADDR;
  static const string PROP_THREADPOOL_CORE_SIZE;
  static const string PROP_CONSUME_ORDERLY;
  static const string PROP_CONSUME_TYPE;
  static const string PROP_CLIENT_VERSION;
  static const string PROP_CONSUMER_START_TIMESTAMP;

 public:
  const map<string, string> getProperties() const;
  void setProperties(const map<string, string>& input_properties);
  void setProperty(const string& key, const string& value);
  const map<MessageQueue, ProcessQueueInfo> getMqTable() const;
  void setMqTable(MessageQueue queue, ProcessQueueInfo queueInfo);
  // const map<string, ConsumeStatus> getStatusTable() const;
  // void setStatusTable(const map<string, ConsumeStatus>& input_statusTable) ;
  const vector<SubscriptionData> getSubscriptionSet() const;
  void setSubscriptionSet(
      const vector<SubscriptionData>& input_subscriptionSet);
  const string getJstack() const;
  void setJstack(const string& input_jstack);
  string encode();

 private:
  map<string, string> properties;
  vector<SubscriptionData> subscriptionSet;
  map<MessageQueue, ProcessQueueInfo> mqTable;
  // map<string, ConsumeStatus> statusTable;
  string jstack;
};
}
#endif
