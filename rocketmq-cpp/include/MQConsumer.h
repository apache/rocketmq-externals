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
#ifndef __MQCONSUMER_H__
#define __MQCONSUMER_H__

#include <string>
#include "AsyncCallback.h"
#include "ConsumeType.h"
#include "MQClient.h"
#include "RocketMQClient.h"

namespace rocketmq {
class SubscriptionData;
class PullRequest;
class Rebalance;
class ConsumerRunningInfo;
//<!************************************************************************
class ROCKETMQCLIENT_API MQConsumer : public MQClient {
 public:
  virtual ~MQConsumer() {}
  virtual void sendMessageBack(MQMessageExt& msg, int delayLevel) = 0;
  virtual void fetchSubscribeMessageQueues(const std::string& topic,
                                           std::vector<MQMessageQueue>& mqs) = 0;
  virtual void doRebalance() = 0;
  virtual void persistConsumerOffset() = 0;
  virtual void persistConsumerOffsetByResetOffset() = 0;
  virtual void updateTopicSubscribeInfo(const std::string& topic,
                                        std::vector<MQMessageQueue>& info) = 0;
  virtual void updateConsumeOffset(const MQMessageQueue& mq,
                                   int64 offset) = 0;
  virtual void removeConsumeOffset(const MQMessageQueue& mq) = 0;
  virtual ConsumeType getConsumeType() = 0;
  virtual ConsumeFromWhere getConsumeFromWhere() = 0;
  virtual void getSubscriptions(std::vector<SubscriptionData>&) = 0;
  virtual void producePullMsgTask(PullRequest*) = 0;
  virtual Rebalance* getRebalance() const = 0;
  virtual PullResult pull(const MQMessageQueue& mq, const std::string& subExpression,
                          int64 offset, int maxNums) = 0;
  virtual void pull(const MQMessageQueue& mq, const std::string& subExpression,
                    int64 offset, int maxNums,
                    PullCallback* pPullCallback) = 0;
  virtual ConsumerRunningInfo* getConsumerRunningInfo() = 0;

 public:
  MessageModel getMessageModel() const { return m_messageModel; }
  void setMessageModel(MessageModel messageModel) {
    m_messageModel = messageModel;
  }

 protected:
  MessageModel m_messageModel;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
