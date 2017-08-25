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
#include "MQMessageExt.h"
#include "MessageSysFlag.h"
#include "SocketUtil.h"
#include "TopicFilterType.h"

namespace rocketmq {
//<!************************************************************************
MQMessageExt::MQMessageExt()
    : m_queueOffset(0),
      m_commitLogOffset(0),
      m_bornTimestamp(0),
      m_storeTimestamp(0),
      m_preparedTransactionOffset(0),
      m_queueId(0),
      m_storeSize(0),
      m_sysFlag(0),
      m_bodyCRC(0),
      m_reconsumeTimes(3),
      m_msgId("") {}

MQMessageExt::MQMessageExt(int queueId, int64 bornTimestamp, sockaddr bornHost,
                           int64 storeTimestamp, sockaddr storeHost,
                           string msgId)
    : m_queueOffset(0),
      m_commitLogOffset(0),
      m_bornTimestamp(bornTimestamp),
      m_storeTimestamp(storeTimestamp),
      m_preparedTransactionOffset(0),
      m_queueId(queueId),
      m_storeSize(0),
      m_sysFlag(0),
      m_bodyCRC(0),
      m_reconsumeTimes(3),
      m_bornHost(bornHost),
      m_storeHost(storeHost),
      m_msgId(msgId) {}

MQMessageExt::~MQMessageExt() {}

int MQMessageExt::getQueueId() const { return m_queueId; }

void MQMessageExt::setQueueId(int queueId) { m_queueId = queueId; }

int64 MQMessageExt::getBornTimestamp() const { return m_bornTimestamp; }

void MQMessageExt::setBornTimestamp(int64 bornTimestamp) {
  m_bornTimestamp = bornTimestamp;
}

sockaddr MQMessageExt::getBornHost() const { return m_bornHost; }

string MQMessageExt::getBornHostString() const {
  return socketAddress2String(m_bornHost);
}

string MQMessageExt::getBornHostNameString() const {
  return getHostName(m_bornHost);
}

void MQMessageExt::setBornHost(const sockaddr& bornHost) {
  m_bornHost = bornHost;
}

int64 MQMessageExt::getStoreTimestamp() const { return m_storeTimestamp; }

void MQMessageExt::setStoreTimestamp(int64 storeTimestamp) {
  m_storeTimestamp = storeTimestamp;
}

sockaddr MQMessageExt::getStoreHost() const { return m_storeHost; }

string MQMessageExt::getStoreHostString() const {
  return socketAddress2String(m_storeHost);
}

void MQMessageExt::setStoreHost(const sockaddr& storeHost) {
  m_storeHost = storeHost;
}

const string& MQMessageExt::getMsgId() const { return m_msgId; }

void MQMessageExt::setMsgId(const string& msgId) { m_msgId = msgId; }

int MQMessageExt::getSysFlag() const { return m_sysFlag; }

void MQMessageExt::setSysFlag(int sysFlag) { m_sysFlag = sysFlag; }

int MQMessageExt::getBodyCRC() const { return m_bodyCRC; }

void MQMessageExt::setBodyCRC(int bodyCRC) { m_bodyCRC = bodyCRC; }

int64 MQMessageExt::getQueueOffset() const { return m_queueOffset; }

void MQMessageExt::setQueueOffset(int64 queueOffset) {
  m_queueOffset = queueOffset;
}

int64 MQMessageExt::getCommitLogOffset() const { return m_commitLogOffset; }

void MQMessageExt::setCommitLogOffset(int64 physicOffset) {
  m_commitLogOffset = physicOffset;
}

int MQMessageExt::getStoreSize() const { return m_storeSize; }

void MQMessageExt::setStoreSize(int storeSize) { m_storeSize = storeSize; }

int MQMessageExt::parseTopicFilterType(int sysFlag) {
  if ((sysFlag & MessageSysFlag::MultiTagsFlag) ==
      MessageSysFlag::MultiTagsFlag) {
    return MULTI_TAG;
  }
  return SINGLE_TAG;
}

int MQMessageExt::getReconsumeTimes() const { return m_reconsumeTimes; }

void MQMessageExt::setReconsumeTimes(int reconsumeTimes) {
  m_reconsumeTimes = reconsumeTimes;
}

int64 MQMessageExt::getPreparedTransactionOffset() const {
  return m_preparedTransactionOffset;
}

void MQMessageExt::setPreparedTransactionOffset(
    int64 preparedTransactionOffset) {
  m_preparedTransactionOffset = preparedTransactionOffset;
}

//<!***************************************************************************
}  //<!end namespace;
