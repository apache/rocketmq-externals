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

#include "MQClient.h"
#include "Logging.h"
#include "MQClientFactory.h"
#include "MQClientManager.h"
#include "TopicPublishInfo.h"
#include "UtilAll.h"

namespace rocketmq {

#define ROCKETMQCPP_VERSION "1.0.1"
#define BUILD_DATE "03-14-2018"
// display version: strings bin/librocketmq.so |grep VERSION
const char *rocketmq_build_time =
    "VERSION: " ROCKETMQCPP_VERSION ", BUILD DATE: " BUILD_DATE " ";

//<!************************************************************************
MQClient::MQClient() {
  string NAMESRV_ADDR_ENV = "NAMESRV_ADDR";
  if (const char *addr = getenv(NAMESRV_ADDR_ENV.c_str()))
    m_namesrvAddr = addr;
  else
    m_namesrvAddr = "";

  m_instanceName = "DEFAULT";
  m_clientFactory = NULL;
  m_serviceState = CREATE_JUST;
  m_pullThreadNum = boost::thread::hardware_concurrency();
  m_tcpConnectTimeout = 3000;        // 3s
  m_tcpTransportTryLockTimeout = 3;  // 3s
  m_unitName = "";
}

MQClient::~MQClient() {}

string MQClient::getMQClientId() const {
  string clientIP = UtilAll::getLocalAddress();
  string processId = UtilAll::to_string(getpid());
  return processId + "-" + clientIP + "@" + m_instanceName;
}

//<!groupName;
const string &MQClient::getGroupName() const { return m_GroupName; }

void MQClient::setGroupName(const string &groupname) {
  m_GroupName = groupname;
}

const string &MQClient::getNamesrvAddr() const { return m_namesrvAddr; }

void MQClient::setNamesrvAddr(const string &namesrvAddr) {
  m_namesrvAddr = namesrvAddr;
}

const string &MQClient::getNamesrvDomain() const { return m_namesrvDomain; }

void MQClient::setNamesrvDomain(const string &namesrvDomain) {
  m_namesrvDomain = namesrvDomain;
}

const string &MQClient::getInstanceName() const { return m_instanceName; }

void MQClient::setInstanceName(const string &instanceName) {
  m_instanceName = instanceName;
}

void MQClient::createTopic(const string &key, const string &newTopic,
                           int queueNum) {
  try {
    getFactory()->createTopic(key, newTopic, queueNum, m_SessionCredentials);
  } catch (MQException &e) {
    LOG_ERROR(e.what());
  }
}

int64 MQClient::earliestMsgStoreTime(const MQMessageQueue &mq) {
  return getFactory()->earliestMsgStoreTime(mq, m_SessionCredentials);
}

QueryResult MQClient::queryMessage(const string &topic, const string &key,
                                   int maxNum, int64 begin, int64 end) {
  return getFactory()->queryMessage(topic, key, maxNum, begin, end,
                                    m_SessionCredentials);
}

int64 MQClient::minOffset(const MQMessageQueue &mq) {
  return getFactory()->minOffset(mq, m_SessionCredentials);
}

int64 MQClient::maxOffset(const MQMessageQueue &mq) {
  return getFactory()->maxOffset(mq, m_SessionCredentials);
}

int64 MQClient::searchOffset(const MQMessageQueue &mq, uint64_t timestamp) {
  return getFactory()->searchOffset(mq, timestamp, m_SessionCredentials);
}

MQMessageExt *MQClient::viewMessage(const string &msgId) {
  return getFactory()->viewMessage(msgId, m_SessionCredentials);
}

vector<MQMessageQueue> MQClient::getTopicMessageQueueInfo(const string &topic) {
  boost::weak_ptr<TopicPublishInfo> weak_topicPublishInfo(
      getFactory()->tryToFindTopicPublishInfo(topic, m_SessionCredentials));
  boost::shared_ptr<TopicPublishInfo> topicPublishInfo(
      weak_topicPublishInfo.lock());
  if (topicPublishInfo) {
    return topicPublishInfo->getMessageQueueList();
  }
  THROW_MQEXCEPTION(
      MQClientException,
      "could not find MessageQueue Info of topic: [" + topic + "].", -1);
}

void MQClient::start() {
  if (getFactory() == NULL) {
    m_clientFactory = MQClientManager::getInstance()->getMQClientFactory(
        getMQClientId(), m_pullThreadNum, m_tcpConnectTimeout,
        m_tcpTransportTryLockTimeout, m_unitName);
  }
  LOG_INFO(
      "MQClient "
      "start,groupname:%s,clientID:%s,instanceName:%s,nameserveraddr:%s",
      getGroupName().c_str(), getMQClientId().c_str(),
      getInstanceName().c_str(), getNamesrvAddr().c_str());
}

void MQClient::shutdown() { m_clientFactory = NULL; }

MQClientFactory *MQClient::getFactory() const { return m_clientFactory; }

bool MQClient::isServiceStateOk() { return m_serviceState == RUNNING; }

void MQClient::setLogLevel(elogLevel inputLevel) {
  ALOG_ADAPTER->setLogLevel(inputLevel);
}

elogLevel MQClient::getLogLevel() { return ALOG_ADAPTER->getLogLevel(); }

void MQClient::setLogFileSizeAndNum(int fileNum, long perFileSize) {
  ALOG_ADAPTER->setLogFileNumAndSize(fileNum, perFileSize);
}

void MQClient::setTcpTransportPullThreadNum(int num) {
  if (num > m_pullThreadNum) {
    m_pullThreadNum = num;
  }
}

const int MQClient::getTcpTransportPullThreadNum() const {
  return m_pullThreadNum;
}

void MQClient::setTcpTransportConnectTimeout(uint64_t timeout) {
  m_tcpConnectTimeout = timeout;
}
const uint64_t MQClient::getTcpTransportConnectTimeout() const {
  return m_tcpConnectTimeout;
}

void MQClient::setTcpTransportTryLockTimeout(uint64_t timeout) {
  if (timeout < 1000) {
    timeout = 1000;
  }
  m_tcpTransportTryLockTimeout = timeout / 1000;
}
const uint64_t MQClient::getTcpTransportTryLockTimeout() const {
  return m_tcpTransportTryLockTimeout;
}

void MQClient::setUnitName(string unitName) { m_unitName = unitName; }
const string &MQClient::getUnitName() { return m_unitName; }

void MQClient::setSessionCredentials(const string &input_accessKey,
                                     const string &input_secretKey,
                                     const string &input_onsChannel) {
  m_SessionCredentials.setAccessKey(input_accessKey);
  m_SessionCredentials.setSecretKey(input_secretKey);
  m_SessionCredentials.setAuthChannel(input_onsChannel);
}

const SessionCredentials &MQClient::getSessionCredentials() const {
  return m_SessionCredentials;
}

//<!************************************************************************
}  //<!end namespace;
