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

#include "DefaultMQPushConsumer.h"
#include <list>
#include <string>

#include "DefaultMQPushConsumerImpl.h"
#include "MessageQueue.h"
#include "MessageExt.h"
#include "ClientConfig.h"
#include "ConsumerStatManage.h"
#include "MixAll.h"
#include "AllocateMessageQueueStrategyInner.h"

namespace rmq
{

class AllocateMessageQueueStrategy;

DefaultMQPushConsumer::DefaultMQPushConsumer()
{
    m_consumerGroup = MixAll::DEFAULT_CONSUMER_GROUP;
    m_messageModel = CLUSTERING;
    m_consumeFromWhere = CONSUME_FROM_LAST_OFFSET;
    m_pAllocateMessageQueueStrategy = new AllocateMessageQueueAveragely();
    m_pMessageListener = NULL;
    m_consumeThreadMin = 5;
    m_consumeThreadMax = 25;
    m_consumeConcurrentlyMaxSpan = 2000;
    m_pullThresholdForQueue = 1000;
    m_pullInterval = 0;
    m_consumeMessageBatchMaxSize = 1;
    m_pullBatchSize = 32;
    m_postSubscriptionWhenPull = false;
    m_unitMode = false;
    m_maxReconsumeTimes = 16;
    m_suspendCurrentQueueTimeMillis = 1000;
    m_consumeTimeout = 15;
    m_pOffsetStore = NULL;
    m_pDefaultMQPushConsumerImpl = new DefaultMQPushConsumerImpl(this);
}

DefaultMQPushConsumer::DefaultMQPushConsumer(const std::string& consumerGroup)
{
    m_consumerGroup = consumerGroup;
    m_messageModel = CLUSTERING;
    m_consumeFromWhere = CONSUME_FROM_LAST_OFFSET;
    m_pAllocateMessageQueueStrategy = new AllocateMessageQueueAveragely();
    m_pMessageListener = NULL;
    m_consumeThreadMin = 5;
    m_consumeThreadMax = 25;
    m_consumeConcurrentlyMaxSpan = 2000;
    m_pullThresholdForQueue = 1000;
    m_pullInterval = 0;
    m_consumeMessageBatchMaxSize = 1;
    m_pullBatchSize = 32;
    m_postSubscriptionWhenPull = false;
    m_unitMode = false;
    m_maxReconsumeTimes = 16;
    m_suspendCurrentQueueTimeMillis = 1000;
    m_consumeTimeout = 15;
    m_pOffsetStore = NULL;
    m_pDefaultMQPushConsumerImpl = new DefaultMQPushConsumerImpl(this);
}

DefaultMQPushConsumer::~DefaultMQPushConsumer()
{
    delete m_pAllocateMessageQueueStrategy;
    delete m_pDefaultMQPushConsumerImpl;
}

//MQAdmin
void DefaultMQPushConsumer::createTopic(const std::string& key, const std::string& newTopic, int queueNum)
{
    m_pDefaultMQPushConsumerImpl->createTopic(key, newTopic, queueNum);
}

long long DefaultMQPushConsumer::searchOffset(const MessageQueue& mq, long long timestamp)
{
    return m_pDefaultMQPushConsumerImpl->searchOffset(mq, timestamp);
}

long long DefaultMQPushConsumer::maxOffset(const MessageQueue& mq)
{
    return m_pDefaultMQPushConsumerImpl->maxOffset(mq);
}

long long DefaultMQPushConsumer::minOffset(const MessageQueue& mq)
{
    return m_pDefaultMQPushConsumerImpl->minOffset(mq);
}

long long DefaultMQPushConsumer::earliestMsgStoreTime(const MessageQueue& mq)
{
    return m_pDefaultMQPushConsumerImpl->earliestMsgStoreTime(mq);
}

MessageExt* DefaultMQPushConsumer::viewMessage(const std::string& msgId)
{
    return m_pDefaultMQPushConsumerImpl->viewMessage(msgId);
}

QueryResult DefaultMQPushConsumer::queryMessage(const std::string& topic,
        const std::string&  key,
        int maxNum,
        long long begin,
        long long end)
{
    return m_pDefaultMQPushConsumerImpl->queryMessage(topic, key, maxNum, begin, end);
}
// MQadmin end

AllocateMessageQueueStrategy* DefaultMQPushConsumer::getAllocateMessageQueueStrategy()
{
    return m_pAllocateMessageQueueStrategy;
}

void DefaultMQPushConsumer::setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy)
{
    m_pAllocateMessageQueueStrategy = pAllocateMessageQueueStrategy;
}

int DefaultMQPushConsumer::getConsumeConcurrentlyMaxSpan()
{
    return m_consumeConcurrentlyMaxSpan;
}

void DefaultMQPushConsumer::setConsumeConcurrentlyMaxSpan(int consumeConcurrentlyMaxSpan)
{
    m_consumeConcurrentlyMaxSpan = consumeConcurrentlyMaxSpan;
}

ConsumeFromWhere DefaultMQPushConsumer::getConsumeFromWhere()
{
    return m_consumeFromWhere;
}

void DefaultMQPushConsumer::setConsumeFromWhere(ConsumeFromWhere consumeFromWhere)
{
    m_consumeFromWhere = consumeFromWhere;
}

int DefaultMQPushConsumer::getConsumeMessageBatchMaxSize()
{
    return m_consumeMessageBatchMaxSize;
}

void DefaultMQPushConsumer::setConsumeMessageBatchMaxSize(int consumeMessageBatchMaxSize)
{
    m_consumeMessageBatchMaxSize = consumeMessageBatchMaxSize;
}

std::string DefaultMQPushConsumer::getConsumerGroup()
{
    return m_consumerGroup;
}

void DefaultMQPushConsumer::setConsumerGroup(const std::string& consumerGroup)
{
    m_consumerGroup = consumerGroup;
}

int DefaultMQPushConsumer::getConsumeThreadMax()
{
    return m_consumeThreadMax;
}

void DefaultMQPushConsumer::setConsumeThreadMax(int consumeThreadMax)
{
    m_consumeThreadMax = consumeThreadMax;
}

int DefaultMQPushConsumer::getConsumeThreadMin()
{
    return m_consumeThreadMin;
}

void DefaultMQPushConsumer::setConsumeThreadMin(int consumeThreadMin)
{
    m_consumeThreadMin = consumeThreadMin;
}

DefaultMQPushConsumerImpl* DefaultMQPushConsumer::getDefaultMQPushConsumerImpl()
{
    return m_pDefaultMQPushConsumerImpl;
}

MessageListener* DefaultMQPushConsumer::getMessageListener()
{
    return m_pMessageListener;
}

void DefaultMQPushConsumer::setMessageListener(MessageListener* pMessageListener)
{
    m_pMessageListener = pMessageListener;
}

MessageModel DefaultMQPushConsumer::getMessageModel()
{
    return m_messageModel;
}

void DefaultMQPushConsumer::setMessageModel(MessageModel messageModel)
{
    m_messageModel = messageModel;
}

int DefaultMQPushConsumer::getPullBatchSize()
{
    return m_pullBatchSize;
}

void DefaultMQPushConsumer::setPullBatchSize(int pullBatchSize)
{
    m_pullBatchSize = pullBatchSize;
}

long DefaultMQPushConsumer::getPullInterval()
{
    return m_pullInterval;
}

void DefaultMQPushConsumer::setPullInterval(long pullInterval)
{
    m_pullInterval = pullInterval;
}

int DefaultMQPushConsumer::getPullThresholdForQueue()
{
    return m_pullThresholdForQueue;
}

void DefaultMQPushConsumer::setPullThresholdForQueue(int pullThresholdForQueue)
{
    m_pullThresholdForQueue = pullThresholdForQueue;
}

std::map<std::string, std::string>& DefaultMQPushConsumer::getSubscription()
{
    return m_subscription;
}

void DefaultMQPushConsumer::setSubscription(const std::map<std::string, std::string>& subscription)
{
    m_subscription = subscription;
}

//MQConsumer
void DefaultMQPushConsumer::sendMessageBack(MessageExt& msg, int delayLevel)
{
    m_pDefaultMQPushConsumerImpl->sendMessageBack(msg, delayLevel, "");
}

void DefaultMQPushConsumer::sendMessageBack(MessageExt& msg, int delayLevel, const std::string brokerName)
{
    m_pDefaultMQPushConsumerImpl->sendMessageBack(msg, delayLevel, brokerName);
}


std::set<MessageQueue>* DefaultMQPushConsumer::fetchSubscribeMessageQueues(const std::string& topic)
{
    return m_pDefaultMQPushConsumerImpl->fetchSubscribeMessageQueues(topic);
}

void DefaultMQPushConsumer::start()
{
    m_pDefaultMQPushConsumerImpl->start();
}

void DefaultMQPushConsumer::shutdown()
{
    m_pDefaultMQPushConsumerImpl->shutdown();
}
//MQConsumer end

//MQPushConsumer
void DefaultMQPushConsumer::registerMessageListener(MessageListener* pMessageListener)
{
    m_pMessageListener = pMessageListener;
    m_pDefaultMQPushConsumerImpl->registerMessageListener(pMessageListener);
}

void DefaultMQPushConsumer::subscribe(const std::string& topic, const std::string& subExpression)
{
    m_pDefaultMQPushConsumerImpl->subscribe(topic, subExpression);
}

void DefaultMQPushConsumer::unsubscribe(const std::string& topic)
{
    m_pDefaultMQPushConsumerImpl->unsubscribe(topic);
}

void DefaultMQPushConsumer::updateCorePoolSize(int corePoolSize)
{
    m_pDefaultMQPushConsumerImpl->updateCorePoolSize(corePoolSize);
}

void DefaultMQPushConsumer::suspend()
{
    m_pDefaultMQPushConsumerImpl->suspend();
}

void DefaultMQPushConsumer::resume()
{
    m_pDefaultMQPushConsumerImpl->resume();
}
//MQPushConsumer end

OffsetStore* DefaultMQPushConsumer::getOffsetStore()
{
    return m_pOffsetStore;
}

void DefaultMQPushConsumer::setOffsetStore(OffsetStore* pOffsetStore)
{
    m_pOffsetStore = pOffsetStore;
}

std::string DefaultMQPushConsumer::getConsumeTimestamp() {
    return m_consumeTimestamp;
}

void DefaultMQPushConsumer::setConsumeTimestamp(std::string consumeTimestamp) {
    m_consumeTimestamp = consumeTimestamp;
}

bool DefaultMQPushConsumer::isPostSubscriptionWhenPull()
{
    return m_postSubscriptionWhenPull;
}


void DefaultMQPushConsumer::setPostSubscriptionWhenPull(bool postSubscriptionWhenPull)
{
    m_postSubscriptionWhenPull = postSubscriptionWhenPull;
}


bool DefaultMQPushConsumer::isUnitMode()
{
    return m_unitMode;
}


void DefaultMQPushConsumer::setUnitMode(bool isUnitMode)
{
    m_unitMode = isUnitMode;
}

int DefaultMQPushConsumer::getMaxReconsumeTimes()
{
    return m_maxReconsumeTimes;
}


void DefaultMQPushConsumer::setMaxReconsumeTimes(int maxReconsumeTimes)
{
    m_maxReconsumeTimes = maxReconsumeTimes;
}


int DefaultMQPushConsumer::getSuspendCurrentQueueTimeMillis()
{
    return m_suspendCurrentQueueTimeMillis;
}


void DefaultMQPushConsumer::setSuspendCurrentQueueTimeMillis(int suspendCurrentQueueTimeMillis)
{
    m_suspendCurrentQueueTimeMillis = suspendCurrentQueueTimeMillis;
}


int DefaultMQPushConsumer::getConsumeTimeout()
{
    return m_consumeTimeout;
}

void DefaultMQPushConsumer::setConsumeTimeout(int consumeTimeout)
{
    m_consumeTimeout = consumeTimeout;
}


}
