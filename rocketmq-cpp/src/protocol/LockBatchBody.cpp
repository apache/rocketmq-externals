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

#include "LockBatchBody.h"
#include "Logging.h"
namespace rocketmq {  //<!end namespace;

string LockBatchRequestBody::getConsumerGroup() { return consumerGroup; }
void LockBatchRequestBody::setConsumerGroup(string in_consumerGroup) {
  consumerGroup = in_consumerGroup;
}
string LockBatchRequestBody::getClientId() { return clientId; }
void LockBatchRequestBody::setClientId(string in_clientId) {
  clientId = in_clientId;
}
vector<MQMessageQueue> LockBatchRequestBody::getMqSet() { return mqSet; }
void LockBatchRequestBody::setMqSet(vector<MQMessageQueue> in_mqSet) {
  mqSet.swap(in_mqSet);
}
void LockBatchRequestBody::Encode(string& outData) {
  Json::Value root;
  root["consumerGroup"] = consumerGroup;
  root["clientId"] = clientId;

  vector<MQMessageQueue>::const_iterator it = mqSet.begin();
  for (; it != mqSet.end(); it++) {
    root["mqSet"].append(toJson(*it));
  }

  Json::FastWriter fastwrite;
  outData = fastwrite.write(root);
}

Json::Value LockBatchRequestBody::toJson(const MQMessageQueue& mq) const {
  Json::Value outJson;
  outJson["topic"] = mq.getTopic();
  outJson["brokerName"] = mq.getBrokerName();
  outJson["queueId"] = mq.getQueueId();
  return outJson;
}

vector<MQMessageQueue> LockBatchResponseBody::getLockOKMQSet() {
  return lockOKMQSet;
}
void LockBatchResponseBody::setLockOKMQSet(
    vector<MQMessageQueue> in_lockOKMQSet) {
  lockOKMQSet.swap(in_lockOKMQSet);
}

void LockBatchResponseBody::Decode(const MemoryBlock* mem,
                                   vector<MQMessageQueue>& messageQueues) {
  messageQueues.clear();
  //<! decode;
  const char* const pData = static_cast<const char*>(mem->getData());

  Json::Reader reader;
  Json::Value root;
  if (!reader.parse(pData, root)) {
    LOG_WARN("decode LockBatchResponseBody error");
    return;
  }

  Json::Value mqs = root["lockOKMQSet"];
  LOG_DEBUG("LockBatchResponseBody mqs size:%d", mqs.size());
  for (unsigned int i = 0; i < mqs.size(); i++) {
    MQMessageQueue mq;
    Json::Value qd = mqs[i];
    mq.setTopic(qd["topic"].asString());
    mq.setBrokerName(qd["brokerName"].asString());
    mq.setQueueId(qd["queueId"].asInt());
    LOG_INFO("LockBatchResponseBody MQ:%s", mq.toString().c_str());
    messageQueues.push_back(mq);
  }
}

string UnlockBatchRequestBody::getConsumerGroup() { return consumerGroup; }
void UnlockBatchRequestBody::setConsumerGroup(string in_consumerGroup) {
  consumerGroup = in_consumerGroup;
}
string UnlockBatchRequestBody::getClientId() { return clientId; }
void UnlockBatchRequestBody::setClientId(string in_clientId) {
  clientId = in_clientId;
}
vector<MQMessageQueue> UnlockBatchRequestBody::getMqSet() { return mqSet; }
void UnlockBatchRequestBody::setMqSet(vector<MQMessageQueue> in_mqSet) {
  mqSet.swap(in_mqSet);
}
void UnlockBatchRequestBody::Encode(string& outData) {
  Json::Value root;
  root["consumerGroup"] = consumerGroup;
  root["clientId"] = clientId;

  vector<MQMessageQueue>::const_iterator it = mqSet.begin();
  for (; it != mqSet.end(); it++) {
    root["mqSet"].append(toJson(*it));
  }

  Json::FastWriter fastwrite;
  outData = fastwrite.write(root);
}

Json::Value UnlockBatchRequestBody::toJson(
    const MQMessageQueue& mq) const {
  Json::Value outJson;
  outJson["topic"] = mq.getTopic();
  outJson["brokerName"] = mq.getBrokerName();
  outJson["queueId"] = mq.getQueueId();
  return outJson;
}
}
