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

namespace rocketmq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MQMessage {
 public:
  MQMessage();
  MQMessage(const std::string& topic, const std::string& body);
  MQMessage(const std::string& topic, const std::string& tags, const std::string& body);
  MQMessage(const std::string& topic, const std::string& tags, const std::string& keys,
            const std::string& body);
  MQMessage(const std::string& topic, const std::string& tags, const std::string& keys,
            const int flag, const std::string& body, bool waitStoreMsgOK);

  virtual ~MQMessage();
  MQMessage(const MQMessage& other);
  MQMessage& operator=(const MQMessage& other);

  void setProperty(const std::string& name, const std::string& value);
  std::string getProperty(const std::string& name) const;

  std::string getTopic() const;
  void setTopic(const std::string& topic);
  void setTopic(const char* body, int len);

  std::string getTags() const;
  void setTags(const std::string& tags);

  std::string getKeys() const;
  void setKeys(const std::string& keys);
  void setKeys(const std::vector<std::string>& keys);

  int getDelayTimeLevel() const;
  void setDelayTimeLevel(int level);

  bool isWaitStoreMsgOK();
  void setWaitStoreMsgOK(bool waitStoreMsgOK);

  int getFlag() const;
  void setFlag(int flag);

  std::string getBody() const;
  void setBody(const char* body, int len);
  void setBody(const std::string& body);

  std::map<std::string, std::string> getProperties() const;
  void setProperties(std::map<std::string, std::string>& properties);

  const std::string toString() const {
    std::stringstream ss;
    ss << "Message [topic=" << m_topic << ", flag=" << m_flag
       << ", tag=" << getTags() << "]";
    return ss.str();
  }

 protected:
  void Init(const std::string& topic, const std::string& tags, const std::string& keys,
            const int flag, const std::string& body, bool waitStoreMsgOK);

 public:
  static const std::string PROPERTY_KEYS;
  static const std::string PROPERTY_TAGS;
  static const std::string PROPERTY_WAIT_STORE_MSG_OK;
  static const std::string PROPERTY_DELAY_TIME_LEVEL;
  static const std::string PROPERTY_RETRY_TOPIC;
  static const std::string PROPERTY_REAL_TOPIC;
  static const std::string PROPERTY_REAL_QUEUE_ID;
  static const std::string PROPERTY_TRANSACTION_PREPARED;
  static const std::string PROPERTY_PRODUCER_GROUP;
  static const std::string PROPERTY_MIN_OFFSET;
  static const std::string PROPERTY_MAX_OFFSET;
  static const std::string KEY_SEPARATOR;

 private:
  std::string m_topic;
  int m_flag;
  std::string m_body;
  std::map<std::string, std::string> m_properties;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
