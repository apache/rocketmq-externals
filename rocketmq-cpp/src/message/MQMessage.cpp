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
#include "MQMessage.h"
#include "UtilAll.h"

namespace rocketmq {
//<!***************************************************************************

const string MQMessage::PROPERTY_KEYS = "KEYS";
const string MQMessage::PROPERTY_TAGS = "TAGS";
const string MQMessage::PROPERTY_WAIT_STORE_MSG_OK = "WAIT";
const string MQMessage::PROPERTY_DELAY_TIME_LEVEL = "DELAY";
const string MQMessage::PROPERTY_RETRY_TOPIC = "RETRY_TOPIC";
const string MQMessage::PROPERTY_REAL_TOPIC = "REAL_TOPIC";
const string MQMessage::PROPERTY_REAL_QUEUE_ID = "REAL_QID";
const string MQMessage::PROPERTY_TRANSACTION_PREPARED = "TRAN_MSG";
const string MQMessage::PROPERTY_PRODUCER_GROUP = "PGROUP";
const string MQMessage::PROPERTY_MIN_OFFSET = "MIN_OFFSET";
const string MQMessage::PROPERTY_MAX_OFFSET = "MAX_OFFSET";
const string MQMessage::KEY_SEPARATOR = " ";
//<!************************************************************************
MQMessage::MQMessage() { Init("", "", "", 0, "", true); }

MQMessage::MQMessage(const string& topic, const string& body) {
  Init(topic, "", "", 0, body, true);
}

MQMessage::MQMessage(const string& topic, const string& tags,
                     const string& body) {
  Init(topic, tags, "", 0, body, true);
}

MQMessage::MQMessage(const string& topic, const string& tags,
                     const string& keys, const string& body) {
  Init(topic, tags, keys, 0, body, true);
}

MQMessage::MQMessage(const string& topic, const string& tags,
                     const string& keys, const int flag, const string& body,
                     bool waitStoreMsgOK) {
  Init(topic, tags, keys, flag, body, waitStoreMsgOK);
}

MQMessage::~MQMessage() { m_properties.clear(); }

MQMessage::MQMessage(const MQMessage& other) {
  m_body = other.m_body;
  m_topic = other.m_topic;
  m_flag = other.m_flag;
  m_properties = other.m_properties;
}

MQMessage& MQMessage::operator=(const MQMessage& other) {
  if (this != &other) {
    m_body = other.m_body;
    m_topic = other.m_topic;
    m_flag = other.m_flag;
    m_properties = other.m_properties;
  }
  return *this;
}

void MQMessage::setProperty(const string& name, const string& value) {
  m_properties[name] = value;
}

string MQMessage::getProperty(const string& name) const {
  map<string, string>::const_iterator it = m_properties.find(name);

  return (it == m_properties.end()) ? "" : (*it).second;
}

string MQMessage::getTopic() const { return m_topic; }

void MQMessage::setTopic(const string& topic) { m_topic = topic; }

void MQMessage::setTopic(const char* body, int len) {
  m_topic.clear();
  m_topic.append(body, len);
}

string MQMessage::getTags() const { return getProperty(PROPERTY_TAGS); }

void MQMessage::setTags(const string& tags) {
  setProperty(PROPERTY_TAGS, tags);
}

string MQMessage::getKeys() const { return getProperty(PROPERTY_KEYS); }

void MQMessage::setKeys(const string& keys) {
  setProperty(PROPERTY_KEYS, keys);
}

void MQMessage::setKeys(const vector<string>& keys) {
  if (keys.empty()) {
    return;
  }

  vector<string>::const_iterator it = keys.begin();
  string str;
  str += *it;
  it++;

  for (; it != keys.end(); it++) {
    str += KEY_SEPARATOR;
    str += *it;
  }

  setKeys(str);
}

int MQMessage::getDelayTimeLevel() const {
  string tmp = getProperty(PROPERTY_DELAY_TIME_LEVEL);
  if (!tmp.empty()) {
    return atoi(tmp.c_str());
  }
  return 0;
}

void MQMessage::setDelayTimeLevel(int level) {
  char tmp[16];
  sprintf(tmp, "%d", level);

  setProperty(PROPERTY_DELAY_TIME_LEVEL, tmp);
}

bool MQMessage::isWaitStoreMsgOK() {
  string tmp = getProperty(PROPERTY_WAIT_STORE_MSG_OK);
  if (tmp.empty()) {
    return true;
  } else {
    return (tmp == "true") ? true : false;
  }
}

void MQMessage::setWaitStoreMsgOK(bool waitStoreMsgOK) {
  if (waitStoreMsgOK) {
    setProperty(PROPERTY_WAIT_STORE_MSG_OK, "true");
  } else {
    setProperty(PROPERTY_WAIT_STORE_MSG_OK, "false");
  }
}

int MQMessage::getFlag() const { return m_flag; }

void MQMessage::setFlag(int flag) { m_flag = flag; }

string MQMessage::getBody() const { return m_body; }

void MQMessage::setBody(const char* body, int len) {
  m_body.clear();
  m_body.append(body, len);
}

void MQMessage::setBody(const string &body) {
  m_body.clear();
  m_body.append(body);
}

map<string, string> MQMessage::getProperties() const { return m_properties; }

void MQMessage::setProperties(map<string, string>& properties) {
  m_properties = properties;
}

void MQMessage::Init(const string& topic, const string& tags,
                     const string& keys, const int flag, const string& body,
                     bool waitStoreMsgOK) {
  m_topic = topic;
  m_flag = flag;
  m_body = body;

  if (tags.length() > 0) {
    setTags(tags);
  }

  if (keys.length() > 0) {
    setKeys(keys);
  }

  setWaitStoreMsgOK(waitStoreMsgOK);
}
}  //<!end namespace;
