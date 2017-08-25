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
#ifndef __MESSAGEQUEUE_H__
#define __MESSAGEQUEUE_H__

#include <string>
#include "json/json.h"

namespace rocketmq {
//<!************************************************************************/
//<!* MQ(T,B,ID);
//<!************************************************************************/
class MessageQueue {
 public:
  MessageQueue();
  MessageQueue(const std::string& topic, const std::string& brokerName,
               int queueId);
  MessageQueue(const MessageQueue& other);
  MessageQueue& operator=(const MessageQueue& other);

  std::string getTopic() const;
  void setTopic(const std::string& topic);

  std::string getBrokerName() const;
  void setBrokerName(const std::string& brokerName);

  int getQueueId() const;
  void setQueueId(int queueId);

  bool operator==(const MessageQueue& mq) const;
  bool operator<(const MessageQueue& mq) const;
  int compareTo(const MessageQueue& mq) const;
  Json::Value toJson() const;

 private:
  std::string m_topic;
  std::string m_brokerName;
  int m_queueId;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
