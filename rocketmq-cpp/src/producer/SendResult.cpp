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
#include "SendResult.h"
#include "UtilAll.h"
#include "VirtualEnvUtil.h"

namespace rocketmq {
//<!***************************************************************************
SendResult::SendResult() : m_sendStatus(SEND_OK), m_queueOffset(0) {}

SendResult::SendResult(const SendStatus& sendStatus, const string& msgId,
                       const MQMessageQueue& messageQueue, int64 queueOffset)
    : m_sendStatus(sendStatus),
      m_msgId(msgId),
      m_messageQueue(messageQueue),
      m_queueOffset(queueOffset) {}

SendResult::SendResult(const SendResult& other) {
  m_sendStatus = other.m_sendStatus;
  m_msgId = other.m_msgId;
  m_messageQueue = other.m_messageQueue;
  m_queueOffset = other.m_queueOffset;
}

SendResult& SendResult::operator=(const SendResult& other) {
  if (this != &other) {
    m_sendStatus = other.m_sendStatus;
    m_msgId = other.m_msgId;
    m_messageQueue = other.m_messageQueue;
    m_queueOffset = other.m_queueOffset;
  }
  return *this;
}

SendResult::~SendResult() {}

const string& SendResult::getMsgId() const { return m_msgId; }

SendStatus SendResult::getSendStatus() const { return m_sendStatus; }

MQMessageQueue SendResult::getMessageQueue() const { return m_messageQueue; }

int64 SendResult::getQueueOffset() const { return m_queueOffset; }

//<!************************************************************************
}  //<!end namespace;
