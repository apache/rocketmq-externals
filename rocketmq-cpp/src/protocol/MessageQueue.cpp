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

#include "MessageQueue.h"
#include "UtilAll.h"

namespace rocketmq {
//<!************************************************************************
MessageQueue::MessageQueue() {
  m_queueId = -1;  // invalide mq
  m_topic.clear();
  m_brokerName.clear();
}

MessageQueue::MessageQueue(const string& topic, const string& brokerName,
                           int queueId)
    : m_topic(topic), m_brokerName(brokerName), m_queueId(queueId) {}

MessageQueue::MessageQueue(const MessageQueue& other)
    : m_topic(other.m_topic),
      m_brokerName(other.m_brokerName),
      m_queueId(other.m_queueId) {}

MessageQueue& MessageQueue::operator=(const MessageQueue& other) {
  if (this != &other) {
    m_brokerName = other.m_brokerName;
    m_topic = other.m_topic;
    m_queueId = other.m_queueId;
  }
  return *this;
}

string MessageQueue::getTopic() const { return m_topic; }

void MessageQueue::setTopic(const string& topic) { m_topic = topic; }

string MessageQueue::getBrokerName() const { return m_brokerName; }

void MessageQueue::setBrokerName(const string& brokerName) {
  m_brokerName = brokerName;
}

int MessageQueue::getQueueId() const { return m_queueId; }

void MessageQueue::setQueueId(int queueId) { m_queueId = queueId; }

bool MessageQueue::operator==(const MessageQueue& mq) const {
  if (this == &mq) {
    return true;
  }

  if (m_brokerName != mq.m_brokerName) {
    return false;
  }

  if (m_queueId != mq.m_queueId) {
    return false;
  }

  if (m_topic != mq.m_topic) {
    return false;
  }

  return true;
}

int MessageQueue::compareTo(const MessageQueue& mq) const {
  int result = m_topic.compare(mq.m_topic);
  if (result != 0) {
    return result;
  }

  result = m_brokerName.compare(mq.m_brokerName);
  if (result != 0) {
    return result;
  }

  return m_queueId - mq.m_queueId;
}

bool MessageQueue::operator<(const MessageQueue& mq) const {
  return compareTo(mq) < 0;
}

Json::Value MessageQueue::toJson() const {
  Json::Value outJson;
  outJson["topic"] = m_topic;
  outJson["brokerName"] = m_brokerName;
  outJson["queueId"] = m_queueId;
  return outJson;
}

//<!***************************************************************************
}  //<!end namespace;
