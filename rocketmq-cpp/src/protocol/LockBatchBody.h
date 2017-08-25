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

#ifndef __LOCKBATCHBODY_H__
#define __LOCKBATCHBODY_H__
#include <set>
#include <string>
#include "MQMessageQueue.h"
#include "RemotingSerializable.h"
#include "dataBlock.h"
#include "json/json.h"
#include "UtilAll.h"

namespace rocketmq {
//<!***************************************************************************

class LockBatchRequestBody {
 public:
  virtual ~LockBatchRequestBody() { mqSet.clear(); }
  string getConsumerGroup();
  void setConsumerGroup(string consumerGroup);
  string getClientId();
  void setClientId(string clientId);
  vector<MQMessageQueue> getMqSet();
  void setMqSet(vector<MQMessageQueue> mqSet);
  void Encode(string& outData);
  Json::Value toJson(const MQMessageQueue& mq) const;

 private:
  string consumerGroup;
  string clientId;
  vector<MQMessageQueue> mqSet;
};

class LockBatchResponseBody {
 public:
  virtual ~LockBatchResponseBody() { lockOKMQSet.clear(); }
  vector<MQMessageQueue> getLockOKMQSet();
  void setLockOKMQSet(vector<MQMessageQueue> lockOKMQSet);
  static void Decode(const MemoryBlock* mem,
                     vector<MQMessageQueue>& messageQueues);

 private:
  vector<MQMessageQueue> lockOKMQSet;
};

class UnlockBatchRequestBody {
 public:
  virtual ~UnlockBatchRequestBody() { mqSet.clear(); }
  string getConsumerGroup();
  void setConsumerGroup(string consumerGroup);
  string getClientId();
  void setClientId(string clientId);
  vector<MQMessageQueue> getMqSet();
  void setMqSet(vector<MQMessageQueue> mqSet);
  void Encode(string& outData);
  Json::Value toJson(const MQMessageQueue& mq) const;

 private:
  string consumerGroup;
  string clientId;
  vector<MQMessageQueue> mqSet;
};

}  //<!end namespace;
#endif
