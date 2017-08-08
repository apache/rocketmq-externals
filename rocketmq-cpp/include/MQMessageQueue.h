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
#ifndef __MQMESSAGEQUEUE_H__
#define __MQMESSAGEQUEUE_H__

#include <iomanip>
#include <sstream>
#include <string>
#include "RocketMQClient.h"

namespace rocketmq {
//<!************************************************************************/
//<!* MQ(T,B,ID);
//<!************************************************************************/
class ROCKETMQCLIENT_API MQMessageQueue {
 public:
  MQMessageQueue();
  MQMessageQueue(const std::string& topic, const std::string& brokerName, int queueId);
  MQMessageQueue(const MQMessageQueue& other);
  MQMessageQueue& operator=(const MQMessageQueue& other);

  std::string getTopic() const;
  void setTopic(const std::string& topic);

  std::string getBrokerName() const;
  void setBrokerName(const std::string& brokerName);

  int getQueueId() const;
  void setQueueId(int queueId);

  bool operator==(const MQMessageQueue& mq) const;
  bool operator<(const MQMessageQueue& mq) const;
  int compareTo(const MQMessageQueue& mq) const;

  const std::string toString() const {
    std::stringstream ss;
    ss << "MessageQueue [topic=" << m_topic << ", brokerName=" << m_brokerName
       << ", queueId=" << m_queueId << "]";

    return ss.str();
  }

 private:
  std::string m_topic;
  std::string m_brokerName;
  int m_queueId;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
