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

#ifndef __DEFAULTMQPULLCONSUMER_H__
#define __DEFAULTMQPULLCONSUMER_H__

#include <set>
#include <string>
#include "MQConsumer.h"
#include "MQMessageQueue.h"
#include "MQueueListener.h"
#include "RocketMQClient.h"

namespace rocketmq {
class Rebalance;
class SubscriptionData;
class OffsetStore;
class PullAPIWrapper;
class ConsumerRunningInfo;
//<!***************************************************************************
class ROCKETMQCLIENT_API DefaultMQPullConsumer : public MQConsumer {
 public:
  DefaultMQPullConsumer(const std::string& groupname);
  virtual ~DefaultMQPullConsumer();

  //<!begin mqadmin;
  virtual void start();
  virtual void shutdown();
  //<!end mqadmin;

  //<!begin MQConsumer
  virtual void sendMessageBack(MQMessageExt& msg, int delayLevel);
  virtual void fetchSubscribeMessageQueues(const std::string& topic,
                                           std::vector<MQMessageQueue>& mqs);
  virtual void doRebalance();
  virtual void persistConsumerOffset();
  virtual void persistConsumerOffsetByResetOffset();
  virtual void updateTopicSubscribeInfo(const std::string& topic,
                                        std::vector<MQMessageQueue>& info);
  virtual ConsumeType getConsumeType();
  virtual ConsumeFromWhere getConsumeFromWhere();
  virtual void getSubscriptions(std::vector<SubscriptionData>&);
  virtual void updateConsumeOffset(const MQMessageQueue& mq, int64 offset);
  virtual void removeConsumeOffset(const MQMessageQueue& mq);
  virtual void producePullMsgTask(PullRequest*);
  virtual Rebalance* getRebalance() const;
  //<!end MQConsumer;

  void registerMessageQueueListener(const std::string& topic,
                                    MQueueListener* pListener);
  /**
  * pull msg from specified queue, if no msg in queue, return directly
  *
  * @param mq
  *            specify the pulled queue
  * @param subExpression
  *            set filter expression for pulled msg, broker will filter msg actively
  *            Now only OR operation is supported, eg: "tag1 || tag2 || tag3"
  *            if subExpression is setted to "null" or "*"，all msg will be subscribed
  * @param offset
  *            specify the started pull offset
  * @param maxNums
  *            specify max msg num by per pull
  * @return  
  *            accroding to PullResult
  */
  virtual PullResult pull(const MQMessageQueue& mq, const std::string& subExpression,
                          int64 offset, int maxNums);
  virtual void pull(const MQMessageQueue& mq, const std::string& subExpression,
                    int64 offset, int maxNums, PullCallback* pPullCallback);

  /**
  * pull msg from specified queue, if no msg, broker will suspend the pull request 20s
  *
  * @param mq
  *            specify the pulled queue
  * @param subExpression
  *            set filter expression for pulled msg, broker will filter msg actively
  *            Now only OR operation is supported, eg: "tag1 || tag2 || tag3"
  *            if subExpression is setted to "null" or "*"，all msg will be subscribed
  * @param offset
  *            specify the started pull offset
  * @param maxNums
  *            specify max msg num by per pull
  * @return  
  *            accroding to PullResult
  */
  PullResult pullBlockIfNotFound(const MQMessageQueue& mq,
                                 const std::string& subExpression, int64 offset,
                                 int maxNums);
  void pullBlockIfNotFound(const MQMessageQueue& mq,
                           const std::string& subExpression, int64 offset,
                           int maxNums, PullCallback* pPullCallback);

  virtual ConsumerRunningInfo* getConsumerRunningInfo() { return NULL; }
  /**
  * 获取消费进度，返回-1表示出错
  *
  * @param mq
  * @param fromStore
  * @return
  */
  int64 fetchConsumeOffset(const MQMessageQueue& mq, bool fromStore);
  /**
  * 根据topic获取MessageQueue，以均衡方式在组内多个成员之间分配
  *
  * @param topic
  *            消息Topic
  * @return 返回队列集合
  */
  void fetchMessageQueuesInBalance(const std::string& topic,
                                   std::vector<MQMessageQueue> mqs);

  // temp persist consumer offset interface, only valid with
  // RemoteBrokerOffsetStore, updateConsumeOffset should be called before.
  void persistConsumerOffset4PullConsumer(const MQMessageQueue& mq);

 private:
  void checkConfig();
  void copySubscription();

  PullResult pullSyncImpl(const MQMessageQueue& mq, const std::string& subExpression,
                          int64 offset, int maxNums, bool block);

  void pullAsyncImpl(const MQMessageQueue& mq, const std::string& subExpression,
                     int64 offset, int maxNums, bool block,
                     PullCallback* pPullCallback);

  void subscriptionAutomatically(const std::string& topic);

 private:
  std::set<std::string> m_registerTopics;

  MQueueListener* m_pMessageQueueListener;
  OffsetStore* m_pOffsetStore;
  Rebalance* m_pRebalance;
  PullAPIWrapper* m_pPullAPIWrapper;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
