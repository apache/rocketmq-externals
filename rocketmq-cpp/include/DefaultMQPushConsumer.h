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

#ifndef __DEFAULTMQPUSHCONSUMER_H__
#define __DEFAULTMQPUSHCONSUMER_H__

#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/thread.hpp>
#include <string>
#include "AsyncCallback.h"
#include "MQConsumer.h"
#include "MQMessageListener.h"
#include "MQMessageQueue.h"

namespace rocketmq {

class Rebalance;
class SubscriptionData;
class OffsetStore;
class PullAPIWrapper;
class PullRequest;
class ConsumeMsgService;
class TaskQueue;
class TaskThread;
class AsyncPullCallback;
class ConsumerRunningInfo;
//<!***************************************************************************
class ROCKETMQCLIENT_API DefaultMQPushConsumer : public MQConsumer {
 public:
  DefaultMQPushConsumer(const std::string& groupname);
  void boost_asio_work();
  virtual ~DefaultMQPushConsumer();

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
  void setConsumeFromWhere(ConsumeFromWhere consumeFromWhere);
  virtual void getSubscriptions(std::vector<SubscriptionData>&);
  virtual void updateConsumeOffset(const MQMessageQueue& mq, int64 offset);
  virtual void removeConsumeOffset(const MQMessageQueue& mq);
  virtual PullResult pull(const MQMessageQueue& mq, const std::string& subExpression,
                          int64 offset, int maxNums) {
    return PullResult();
  }
  virtual void pull(const MQMessageQueue& mq, const std::string& subExpression,
                    int64 offset, int maxNums,
                    PullCallback* pPullCallback) {}
  virtual ConsumerRunningInfo* getConsumerRunningInfo();
  //<!end MQConsumer;

  void registerMessageListener(MQMessageListener* pMessageListener);
  MessageListenerType getMessageListenerType();
  void subscribe(const std::string& topic, const std::string& subExpression);

  OffsetStore* getOffsetStore() const;
  virtual Rebalance* getRebalance() const;
  ConsumeMsgService* getConsumerMsgService() const;

  virtual void producePullMsgTask(PullRequest*);
  void triggerNextPullRequest(boost::asio::deadline_timer* t,
                              PullRequest* request);
  void runPullMsgQueue(TaskQueue* pTaskQueue);
  void pullMessage(PullRequest* pullrequest);       // sync pullMsg
  void pullMessageAsync(PullRequest* pullrequest);  // async pullMsg
  void setAsyncPull(bool asyncFlag);
  AsyncPullCallback* getAsyncPullCallBack(PullRequest* request,
                                          MQMessageQueue msgQueue);
  void shutdownAsyncPullCallBack();

  /*
    for orderly consume, set the pull num of message size by each pullMsg,
    default value is 1;
  */
  void setConsumeMessageBatchMaxSize(int consumeMessageBatchMaxSize);
  int getConsumeMessageBatchMaxSize() const;

  /*
    set consuming thread count, default value is cpu cores
  */
  void setConsumeThreadCount(int threadCount);
  int getConsumeThreadCount() const;

  /*
    set pullMsg thread count, default value is cpu cores
  */
  void setPullMsgThreadPoolCount(int threadCount);
  int getPullMsgThreadPoolCount() const;

  /*
    set max cache msg size perQueue in memory if consumer could not consume msgs
    immediately
    default maxCacheMsgSize perQueue is 1000, set range is:1~65535
  */
  void setMaxCacheMsgSizePerQueue(int maxCacheSize);
  int getMaxCacheMsgSizePerQueue() const;

 private:
  void checkConfig();
  void copySubscription();
  void updateTopicSubscribeInfoWhenSubscriptionChanged();

 private:
  uint64_t m_startTime;
  ConsumeFromWhere m_consumeFromWhere;
  std::map<std::string, std::string> m_subTopics;
  int m_consumeThreadCount;
  OffsetStore* m_pOffsetStore;
  Rebalance* m_pRebalance;
  PullAPIWrapper* m_pPullAPIWrapper;
  ConsumeMsgService* m_consumerServeice;
  MQMessageListener* m_pMessageListener;
  int m_consumeMessageBatchMaxSize;
  int m_maxMsgCacheSize;
  boost::asio::io_service m_async_ioService;
  boost::scoped_ptr<boost::thread> m_async_service_thread;

  typedef std::map<MQMessageQueue, AsyncPullCallback*> PullMAP;
  PullMAP m_PullCallback;
  bool m_asyncPull;
  int m_asyncPullTimeout;
  int m_pullMsgThreadPoolNum;

 private:
  TaskQueue* m_pullmsgQueue;
  std::unique_ptr<boost::thread> m_pullmsgThread;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
