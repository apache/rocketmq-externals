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

#include "CommandHeader.h"
#include <cstdlib>
#include <sstream>
#include "Logging.h"
#include "UtilAll.h"

namespace rocketmq {
//<!************************************************************************
void GetRouteInfoRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
}

void GetRouteInfoRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
}
//<!***************************************************************************
void UnregisterClientRequestHeader::Encode(Json::Value& outData) {
  outData["clientID"] = clientID;
  outData["producerGroup"] = producerGroup;
  outData["consumerGroup"] = consumerGroup;
}

void UnregisterClientRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("clientID", clientID));
  requestMap.insert(pair<string, string>("producerGroup", producerGroup));
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
}
//<!************************************************************************
void CreateTopicRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
  outData["defaultTopic"] = defaultTopic;
  outData["readQueueNums"] = readQueueNums;
  outData["writeQueueNums"] = writeQueueNums;
  outData["perm"] = perm;
  outData["topicFilterType"] = topicFilterType;
}
void CreateTopicRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(pair<string, string>("defaultTopic", defaultTopic));
  requestMap.insert(
      pair<string, string>("readQueueNums", UtilAll::to_string(readQueueNums)));
  requestMap.insert(pair<string, string>("writeQueueNums",
                                         UtilAll::to_string(writeQueueNums)));
  requestMap.insert(pair<string, string>("perm", UtilAll::to_string(perm)));
  requestMap.insert(pair<string, string>("topicFilterType", topicFilterType));
}

//<!************************************************************************
void SendMessageRequestHeader::Encode(Json::Value& outData) {
  outData["producerGroup"] = producerGroup;
  outData["topic"] = topic;
  outData["defaultTopic"] = defaultTopic;
  outData["defaultTopicQueueNums"] = defaultTopicQueueNums;
  outData["queueId"] = queueId;
  outData["sysFlag"] = sysFlag;
  outData["bornTimestamp"] = UtilAll::to_string(bornTimestamp);
  outData["flag"] = flag;
  outData["properties"] = properties;
#ifdef ONS
  outData["reconsumeTimes"] = UtilAll::to_string(reconsumeTimes);
  outData["unitMode"] = UtilAll::to_string(unitMode);
#endif
}

int SendMessageRequestHeader::getReconsumeTimes() { return reconsumeTimes; }

void SendMessageRequestHeader::setReconsumeTimes(int input_reconsumeTimes) {
  reconsumeTimes = input_reconsumeTimes;
}

void SendMessageRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  LOG_DEBUG(
      "SendMessageRequestHeader producerGroup is:%s,topic is:%s, defaulttopic "
      "is:%s, properties is:%s,UtilAll::to_string( defaultTopicQueueNums) "
      "is:%s,UtilAll::to_string( queueId):%s, UtilAll::to_string( sysFlag) "
      "is:%s, UtilAll::to_string( bornTimestamp) is:%s,UtilAll::to_string( "
      "flag) is:%s",
      producerGroup.c_str(), topic.c_str(), defaultTopic.c_str(),
      properties.c_str(), UtilAll::to_string(defaultTopicQueueNums).c_str(),
      UtilAll::to_string(queueId).c_str(), UtilAll::to_string(sysFlag).c_str(),
      UtilAll::to_string(bornTimestamp).c_str(),
      UtilAll::to_string(flag).c_str());

  requestMap.insert(pair<string, string>("producerGroup", producerGroup));
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(pair<string, string>("defaultTopic", defaultTopic));
  requestMap.insert(pair<string, string>(
      "defaultTopicQueueNums", UtilAll::to_string(defaultTopicQueueNums)));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
  requestMap.insert(
      pair<string, string>("sysFlag", UtilAll::to_string(sysFlag)));
  requestMap.insert(
      pair<string, string>("bornTimestamp", UtilAll::to_string(bornTimestamp)));
  requestMap.insert(pair<string, string>("flag", UtilAll::to_string(flag)));
  requestMap.insert(pair<string, string>("properties", properties));
#ifdef ONS
  requestMap.insert(pair<string, string>("reconsumeTimes",
                                         UtilAll::to_string(reconsumeTimes)));
  requestMap.insert(
      pair<string, string>("unitMode", UtilAll::to_string(unitMode)));
#endif
}

//<!************************************************************************
CommandHeader* SendMessageResponseHeader::Decode(Json::Value& ext) {
  SendMessageResponseHeader* h = new SendMessageResponseHeader();

  Json::Value& tempValue = ext["msgId"];
  if (tempValue.isString()) {
    h->msgId = tempValue.asString();
  }

  tempValue = ext["queueId"];
  if (tempValue.isString()) {
    h->queueId = atoi(tempValue.asCString());
  }

  tempValue = ext["queueOffset"];
  if (tempValue.isString()) {
    h->queueOffset = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void SendMessageResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("msgId", msgId));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
  requestMap.insert(
      pair<string, string>("queueOffset", UtilAll::to_string(queueOffset)));
}
//<!************************************************************************
void PullMessageRequestHeader::Encode(Json::Value& outData) {
  outData["consumerGroup"] = consumerGroup;
  outData["topic"] = topic;
  outData["queueId"] = queueId;
  outData["queueOffset"] = UtilAll::to_string(queueOffset);
  ;
  outData["maxMsgNums"] = maxMsgNums;
  outData["sysFlag"] = sysFlag;
  outData["commitOffset"] = UtilAll::to_string(commitOffset);
  ;
  outData["subVersion"] = UtilAll::to_string(subVersion);
  ;
  outData["suspendTimeoutMillis"] = UtilAll::to_string(suspendTimeoutMillis);
  ;
  outData["subscription"] = subscription;
}

void PullMessageRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
  requestMap.insert(
      pair<string, string>("queueOffset", UtilAll::to_string(queueOffset)));
  requestMap.insert(
      pair<string, string>("maxMsgNums", UtilAll::to_string(maxMsgNums)));
  requestMap.insert(
      pair<string, string>("sysFlag", UtilAll::to_string(sysFlag)));
  requestMap.insert(
      pair<string, string>("commitOffset", UtilAll::to_string(commitOffset)));
  requestMap.insert(
      pair<string, string>("subVersion", UtilAll::to_string(subVersion)));
  requestMap.insert(pair<string, string>(
      "suspendTimeoutMillis", UtilAll::to_string(suspendTimeoutMillis)));
  requestMap.insert(pair<string, string>("subscription", subscription));
}
//<!************************************************************************
CommandHeader* PullMessageResponseHeader::Decode(Json::Value& ext) {
  PullMessageResponseHeader* h = new PullMessageResponseHeader();

  Json::Value& tempValue = ext["suggestWhichBrokerId"];
  if (tempValue.isString()) {
    h->suggestWhichBrokerId = UtilAll::str2ll(tempValue.asCString());
  }

  tempValue = ext["nextBeginOffset"];
  if (tempValue.isString()) {
    h->nextBeginOffset = UtilAll::str2ll(tempValue.asCString());
  }

  tempValue = ext["minOffset"];
  if (tempValue.isString()) {
    h->minOffset = UtilAll::str2ll(tempValue.asCString());
  }

  tempValue = ext["maxOffset"];
  if (tempValue.isString()) {
    h->maxOffset = UtilAll::str2ll(tempValue.asCString());
  }

  return h;
}

void PullMessageResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>(
      "suggestWhichBrokerId", UtilAll::to_string(suggestWhichBrokerId)));
  requestMap.insert(pair<string, string>("nextBeginOffset",
                                         UtilAll::to_string(nextBeginOffset)));
  requestMap.insert(
      pair<string, string>("minOffset", UtilAll::to_string(minOffset)));
  requestMap.insert(
      pair<string, string>("maxOffset", UtilAll::to_string(maxOffset)));
}
//<!************************************************************************
void GetConsumerListByGroupResponseHeader::Encode(Json::Value& outData) {
  // outData = "{}";
}

void GetConsumerListByGroupResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {}
//<!***************************************************************************
void GetMinOffsetRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
  outData["queueId"] = queueId;
}

void GetMinOffsetRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
}
//<!***************************************************************************
CommandHeader* GetMinOffsetResponseHeader::Decode(Json::Value& ext) {
  GetMinOffsetResponseHeader* h = new GetMinOffsetResponseHeader();

  Json::Value& tempValue = ext["offset"];
  if (tempValue.isString()) {
    h->offset = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void GetMinOffsetResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void GetMaxOffsetRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
  outData["queueId"] = queueId;
}

void GetMaxOffsetRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
}
//<!***************************************************************************
CommandHeader* GetMaxOffsetResponseHeader::Decode(Json::Value& ext) {
  GetMaxOffsetResponseHeader* h = new GetMaxOffsetResponseHeader();

  Json::Value& tempValue = ext["offset"];
  if (tempValue.isString()) {
    h->offset = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void GetMaxOffsetResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void SearchOffsetRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
  outData["queueId"] = queueId;
  outData["timestamp"] = UtilAll::to_string(timestamp);
}

void SearchOffsetRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
  requestMap.insert(
      pair<string, string>("timestamp", UtilAll::to_string(timestamp)));
}
//<!***************************************************************************
CommandHeader* SearchOffsetResponseHeader::Decode(Json::Value& ext) {
  SearchOffsetResponseHeader* h = new SearchOffsetResponseHeader();

  Json::Value& tempValue = ext["offset"];
  if (tempValue.isString()) {
    h->offset = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void SearchOffsetResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void ViewMessageRequestHeader::Encode(Json::Value& outData) {
  outData["offset"] = UtilAll::to_string(offset);
}

void ViewMessageRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void GetEarliestMsgStoretimeRequestHeader::Encode(Json::Value& outData) {
  outData["topic"] = topic;
  outData["queueId"] = queueId;
}

void GetEarliestMsgStoretimeRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
}
//<!***************************************************************************
CommandHeader* GetEarliestMsgStoretimeResponseHeader::Decode(
    Json::Value& ext) {
  GetEarliestMsgStoretimeResponseHeader* h =
      new GetEarliestMsgStoretimeResponseHeader();

  Json::Value& tempValue = ext["timestamp"];
  if (tempValue.isString()) {
    h->timestamp = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void GetEarliestMsgStoretimeResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(
      pair<string, string>("timestamp", UtilAll::to_string(timestamp)));
}
//<!***************************************************************************
void GetConsumerListByGroupRequestHeader::Encode(Json::Value& outData) {
  outData["consumerGroup"] = consumerGroup;
}

void GetConsumerListByGroupRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
}
//<!***************************************************************************
void QueryConsumerOffsetRequestHeader::Encode(Json::Value& outData) {
  outData["consumerGroup"] = consumerGroup;
  outData["topic"] = topic;
  outData["queueId"] = queueId;
}

void QueryConsumerOffsetRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
}
//<!***************************************************************************
CommandHeader* QueryConsumerOffsetResponseHeader::Decode(
    Json::Value& ext) {
  QueryConsumerOffsetResponseHeader* h =
      new QueryConsumerOffsetResponseHeader();
  Json::Value& tempValue = ext["offset"];
  if (tempValue.isString()) {
    h->offset = UtilAll::str2ll(tempValue.asCString());
  }
  return h;
}

void QueryConsumerOffsetResponseHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void UpdateConsumerOffsetRequestHeader::Encode(Json::Value& outData) {
  outData["consumerGroup"] = consumerGroup;
  outData["topic"] = topic;
  outData["queueId"] = queueId;
  outData["commitOffset"] = UtilAll::to_string(commitOffset);
}

void UpdateConsumerOffsetRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
  requestMap.insert(pair<string, string>("topic", topic));
  requestMap.insert(
      pair<string, string>("queueId", UtilAll::to_string(queueId)));
  requestMap.insert(
      pair<string, string>("commitOffset", UtilAll::to_string(commitOffset)));
}
//<!***************************************************************************
void ConsumerSendMsgBackRequestHeader::Encode(Json::Value& outData) {
  outData["group"] = group;
  outData["delayLevel"] = delayLevel;
  outData["offset"] = UtilAll::to_string(offset);
#ifdef ONS
  outData["originMsgId"] = originMsgId;
  outData["originTopic"] = originTopic;
#endif
}

void ConsumerSendMsgBackRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("group", group));
  requestMap.insert(
      pair<string, string>("delayLevel", UtilAll::to_string(delayLevel)));
  requestMap.insert(pair<string, string>("offset", UtilAll::to_string(offset)));
}
//<!***************************************************************************
void GetConsumerListByGroupResponseBody::Decode(const MemoryBlock* mem,
                                                vector<string>& cids) {
  cids.clear();
  //<! decode;
  const char* const pData = static_cast<const char*>(mem->getData());

  Json::Reader reader;
  Json::Value root;
  if (!reader.parse(pData, root)) {
    LOG_ERROR("GetConsumerListByGroupResponse error");
    return;
  }

  Json::Value ids = root["consumerIdList"];
  for (unsigned int i = 0; i < ids.size(); i++) {
    if (ids[i].isString()) {
      cids.push_back(ids[i].asString());
    }
  }
}

void GetConsumerListByGroupResponseBody::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {}

void ResetOffsetRequestHeader::setTopic(const string& tmp) { topic = tmp; }

void ResetOffsetRequestHeader::setGroup(const string& tmp) { group = tmp; }

void ResetOffsetRequestHeader::setTimeStamp(const int64& tmp) {
  timestamp = tmp;
}

void ResetOffsetRequestHeader::setForceFlag(const bool& tmp) { isForce = tmp; }

const string ResetOffsetRequestHeader::getTopic() const { return topic; }

const string ResetOffsetRequestHeader::getGroup() const { return group; }

const int64 ResetOffsetRequestHeader::getTimeStamp() const { return timestamp; }

const bool ResetOffsetRequestHeader::getForceFlag() const { return isForce; }

CommandHeader* ResetOffsetRequestHeader::Decode(Json::Value& ext) {
  ResetOffsetRequestHeader* h = new ResetOffsetRequestHeader();

  Json::Value& tempValue = ext["topic"];
  if (tempValue.isString()) {
    h->topic = tempValue.asString();
  }

  tempValue = ext["group"];
  if (tempValue.isString()) {
    h->group = tempValue.asString();
  }

  tempValue = ext["timestamp"];
  if (tempValue.isString()) {
    h->timestamp = UtilAll::str2ll(tempValue.asCString());
  }

  tempValue = ext["isForce"];
  if (tempValue.isString()) {
    h->isForce = UtilAll::to_bool(tempValue.asCString());
  }
  LOG_INFO("topic:%s, group:%s, timestamp:%lld, isForce:%d,isForce:%s",
           h->topic.c_str(), h->group.c_str(), h->timestamp, h->isForce,
           tempValue.asCString());
  return h;
}

CommandHeader* GetConsumerRunningInfoRequestHeader::Decode(
    Json::Value& ext) {
  GetConsumerRunningInfoRequestHeader* h =
      new GetConsumerRunningInfoRequestHeader();

  Json::Value& tempValue = ext["consumerGroup"];
  if (tempValue.isString()) {
    h->consumerGroup = tempValue.asString();
  }

  tempValue = ext["clientId"];
  if (tempValue.isString()) {
    h->clientId = tempValue.asString();
  }

  tempValue = ext["jstackEnable"];
  if (tempValue.isString()) {
    h->jstackEnable = UtilAll::to_bool(tempValue.asCString());
  }
  LOG_INFO("consumerGroup:%s, clientId:%s,  jstackEnable:%d",
           h->consumerGroup.c_str(), h->clientId.c_str(), h->jstackEnable);
  return h;
}

void GetConsumerRunningInfoRequestHeader::Encode(Json::Value& outData) {
  outData["consumerGroup"] = consumerGroup;
  outData["clientId"] = clientId;
  outData["jstackEnable"] = jstackEnable;
}

void GetConsumerRunningInfoRequestHeader::SetDeclaredFieldOfCommandHeader(
    map<string, string>& requestMap) {
  requestMap.insert(pair<string, string>("consumerGroup", consumerGroup));
  requestMap.insert(pair<string, string>("clientId", clientId));
  requestMap.insert(
      pair<string, string>("jstackEnable", UtilAll::to_string(jstackEnable)));
}

const string GetConsumerRunningInfoRequestHeader::getConsumerGroup() const {
  return consumerGroup;
}

void GetConsumerRunningInfoRequestHeader::setConsumerGroup(
    const string& Group) {
  consumerGroup = Group;
}

const string GetConsumerRunningInfoRequestHeader::getClientId() const {
  return clientId;
}

void GetConsumerRunningInfoRequestHeader::setClientId(
    const string& input_clientId) {
  clientId = input_clientId;
}

const bool GetConsumerRunningInfoRequestHeader::isJstackEnable() const {
  return jstackEnable;
}

void GetConsumerRunningInfoRequestHeader::setJstackEnable(
    const bool& input_jstackEnable) {
  jstackEnable = input_jstackEnable;
}

CommandHeader* NotifyConsumerIdsChangedRequestHeader::Decode(
    Json::Value& ext) {
  NotifyConsumerIdsChangedRequestHeader* h =
      new NotifyConsumerIdsChangedRequestHeader();

  Json::Value& tempValue = ext["consumerGroup"];
  if (tempValue.isString()) {
    h->consumerGroup = tempValue.asString();
  }

  return h;
}

void NotifyConsumerIdsChangedRequestHeader::setGroup(const string& tmp) {
  consumerGroup = tmp;
}
const string NotifyConsumerIdsChangedRequestHeader::getGroup() const {
  return consumerGroup;
}

//<!************************************************************************
}  //<!end namespace;
