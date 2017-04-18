/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#ifndef __DEFAULTMQPULLCONSUMERIMPL_H__
#define __DEFAULTMQPULLCONSUMERIMPL_H__

#include <string>
#include <set>
#include <map>
#include <vector>
#include "MQConsumerInner.h"
#include "MessageExt.h"
#include "QueryResult.h"
#include "ServiceState.h"
#include "PullRequest.h"
#include "MessageQueue.h"
#include "PullResult.h"
#include "PullCallback.h"
#include "PullAPIWrapper.h"

namespace rmq
{
class DefaultMQPullConsumer;
class PullCallback;
class OffsetStore;
class RebalanceImpl;
class MQClientFactory;
class PullAPIWrapper;

/**
* PullConsumer imp
*/
class DefaultMQPullConsumerImpl : public  MQConsumerInner
{
public:
    DefaultMQPullConsumerImpl(DefaultMQPullConsumer *pDefaultMQPullConsumer);
    ~DefaultMQPullConsumerImpl();
    void createTopic(const std::string &key, const std::string &newTopic,
                     int queueNum);
    long long fetchConsumeOffset(MessageQueue &mq, bool fromStore);
    std::set<MessageQueue> *fetchMessageQueuesInBalance(const std::string &topic);
    std::vector<MessageQueue> *fetchPublishMessageQueues(const std::string  &topic);
    std::set<MessageQueue> *fetchSubscribeMessageQueues(const std::string &topic);
    long long earliestMsgStoreTime(const MessageQueue &mq);
    std::string groupName();
    MessageModel messageModel();
    ConsumeType consumeType();
    ConsumeFromWhere consumeFromWhere();
    std::set<SubscriptionData> subscriptions();
    void doRebalance();
    void persistConsumerOffset();
    void updateTopicSubscribeInfo(const std::string &topic,
                                  const std::set<MessageQueue> &info);
    bool isSubscribeTopicNeedUpdate(const std::string &topic);
    long long maxOffset(const MessageQueue &mq);
    long long minOffset(const MessageQueue &mq);

    PullResult *pull(MessageQueue &mq,
                     const std::string &subExpression,
                     long long offset,
                     int maxNums);

    void pull(MessageQueue &mq,
              const std::string &subExpression,
              long long offset,
              int maxNums,
              PullCallback *pPullCallback);

    PullResult *pullBlockIfNotFound(MessageQueue &mq,
                                    const std::string &subExpression,
                                    long long offset, int maxNums);

    void pullBlockIfNotFound(MessageQueue &mq,
                             const std::string &subExpression,
                             long long offset, int maxNums,
                             PullCallback *pPullCallback);

    QueryResult queryMessage(const std::string &topic,
                             const std::string  &key,
                             int maxNum,
                             long long begin,
                             long long end);

    long long searchOffset(const MessageQueue &mq, long long timestamp);
    void sendMessageBack(MessageExt &msg, int delayLevel,
                         const std::string &brokerName);
    void sendMessageBack(MessageExt &msg, int delayLevel,
                         const std::string &brokerName, const std::string &consumerGroup);
    void shutdown();
    void updateConsumeOffset(MessageQueue &mq, long long offset);
    MessageExt *viewMessage(const std::string &msgId);
    DefaultMQPullConsumer *getDefaultMQPullConsumer();
    OffsetStore *getOffsetStore();
    void setOffsetStore(OffsetStore *pOffsetStore);
    void start();

    ServiceState getServiceState();
    void setServiceState(ServiceState serviceState);

private:
    void makeSureStateOK();
    void subscriptionAutomatically(const std::string &topic);
    void copySubscription();
    void checkConfig();

    PullResult *pullSyncImpl(MessageQueue &mq,
                             const std::string &subExpression,
                             long long offset,
                             int maxNums,
                             bool block) ;
    void pullAsyncImpl(MessageQueue &mq,
                       const std::string &subExpression,
                       long long offset,
                       int maxNums,
                       PullCallback *pPullCallback,
                       bool block);

private:
    DefaultMQPullConsumer *m_pDefaultMQPullConsumer;
    ServiceState m_serviceState;
    MQClientFactory *m_pMQClientFactory;
    PullAPIWrapper *m_pPullAPIWrapper;
    OffsetStore *m_pOffsetStore;
    RebalanceImpl *m_pRebalanceImpl;
    friend class DefaultMQPullConsumerImplCallback;
};

class DefaultMQPullConsumerImplCallback : public PullCallback
{
public:
    DefaultMQPullConsumerImplCallback(SubscriptionData &subscriptionData,
                                      MessageQueue &mq,
                                      DefaultMQPullConsumerImpl *pDefaultMQPullConsumerImpl,
                                      PullCallback *pCallback)
        : m_subscriptionData(subscriptionData),
          m_mq(mq),
          m_pDefaultMQPullConsumerImpl(pDefaultMQPullConsumerImpl),
          m_pCallback(pCallback)
    {
    }

    void onSuccess(PullResult &pullResult)
    {
        m_pCallback->onSuccess(
            *m_pDefaultMQPullConsumerImpl->m_pPullAPIWrapper->
            processPullResult(m_mq, pullResult, m_subscriptionData));
    }

    void onException(MQException &e)
    {
        m_pCallback->onException(e);
    }

private:
    SubscriptionData m_subscriptionData;
    MessageQueue m_mq;
    DefaultMQPullConsumerImpl *m_pDefaultMQPullConsumerImpl;
    PullCallback *m_pCallback;
};
}

#endif
