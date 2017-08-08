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
#ifndef __MESSAGE_H__
#define __MESSAGE_H__

#include <map>
#include <sstream>
#include <string>
#include <vector>
#include "RocketMQClient.h"
#include "UtilAll.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MQMessage {
 public:
  MQMessage();
  MQMessage(const string& topic, const string& body);
  MQMessage(const string& topic, const string& tags, const string& body);
  MQMessage(const string& topic, const string& tags, const string& keys,
            const string& body);
  MQMessage(const string& topic, const string& tags, const string& keys,
            const int flag, const string& body, bool waitStoreMsgOK);

  virtual ~MQMessage();
  MQMessage(const MQMessage& other);
  MQMessage& operator=(const MQMessage& other);

  void setProperty(const string& name, const string& value);
  string getProperty(const string& name) const;

  string getTopic() const;
  void setTopic(const string& topic);
  void setTopic(const char* body, int len);

  string getTags() const;
  void setTags(const string& tags);

  string getKeys() const;
  void setKeys(const string& keys);
  void setKeys(const vector<string>& keys);

  int getDelayTimeLevel() const;
  void setDelayTimeLevel(int level);

  bool isWaitStoreMsgOK();
  void setWaitStoreMsgOK(bool waitStoreMsgOK);

  int getFlag() const;
  void setFlag(int flag);

  string getBody() const;
  void setBody(const char* body, int len);
  void setBody(const string& body);

  map<string, string> getProperties() const;
  void setProperties(map<string, string>& properties);

  const string toString() const {
    stringstream ss;
    ss << "Message [topic=" << m_topic << ", flag=" << m_flag
       << ", tag=" << getTags() << "]";
    return ss.str();
  }

 protected:
  void Init(const string& topic, const string& tags, const string& keys,
            const int flag, const string& body, bool waitStoreMsgOK);

 public:
  static const string PROPERTY_KEYS;
  static const string PROPERTY_TAGS;
  static const string PROPERTY_WAIT_STORE_MSG_OK;
  static const string PROPERTY_DELAY_TIME_LEVEL;
  static const string PROPERTY_RETRY_TOPIC;
  static const string PROPERTY_REAL_TOPIC;
  static const string PROPERTY_REAL_QUEUE_ID;
  static const string PROPERTY_TRANSACTION_PREPARED;
  static const string PROPERTY_PRODUCER_GROUP;
  static const string PROPERTY_MIN_OFFSET;
  static const string PROPERTY_MAX_OFFSET;
  static const string KEY_SEPARATOR;

 private:
  string m_topic;
  int m_flag;
  string m_body;
  map<string, string> m_properties;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
