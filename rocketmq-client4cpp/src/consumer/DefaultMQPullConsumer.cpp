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

#include "DefaultMQPullConsumer.h"

#include <list>
#include <string>

#include "MessageQueue.h"
#include "MessageExt.h"
#include "ClientConfig.h"
#include "DefaultMQPullConsumerImpl.h"
#include "MixAll.h"
#include "AllocateMessageQueueStrategyInner.h"

namespace rmq
{

DefaultMQPullConsumer::DefaultMQPullConsumer()
    : m_consumerGroup(MixAll::DEFAULT_CONSUMER_GROUP),
      m_brokerSuspendMaxTimeMillis(1000 * 20),
      m_consumerTimeoutMillisWhenSuspend(1000 * 30),
      m_consumerPullTimeoutMillis(1000 * 10),
      m_messageModel(CLUSTERING),
      m_pMessageQueueListener(NULL),
      m_pOffsetStore(NULL),
      m_pAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely()),
      m_unitMode(false),
      m_maxReconsumeTimes(16)
{
    m_pDefaultMQPullConsumerImpl = new DefaultMQPullConsumerImpl(this);
}

DefaultMQPullConsumer::DefaultMQPullConsumer(const std::string& consumerGroup)
    : m_consumerGroup(consumerGroup),
      m_brokerSuspendMaxTimeMillis(1000 * 20),
      m_consumerTimeoutMillisWhenSuspend(1000 * 30),
      m_consumerPullTimeoutMillis(1000 * 10),
      m_messageModel(CLUSTERING),
      m_pMessageQueueListener(NULL),
      m_pOffsetStore(NULL),
      m_pAllocateMessageQueueStrategy(new AllocateMessageQueueAveragely()),
      m_unitMode(false),
      m_maxReconsumeTimes(16)
{
    m_pDefaultMQPullConsumerImpl = new DefaultMQPullConsumerImpl(this);
}

DefaultMQPullConsumer::~DefaultMQPullConsumer()
{
	//memleak or coredump
	if (m_pAllocateMessageQueueStrategy)
		delete m_pAllocateMessageQueueStrategy;
	if (m_pDefaultMQPullConsumerImpl)
		delete m_pDefaultMQPullConsumerImpl;
}

//MQAdmin
void DefaultMQPullConsumer::createTopic(const std::string& key, const std::string& newTopic, int queueNum)
{
    m_pDefaultMQPullConsumerImpl->createTopic(key, newTopic, queueNum);
}

long long DefaultMQPullConsumer::searchOffset(const MessageQueue& mq, long long timestamp)
{
    return m_pDefaultMQPullConsumerImpl->searchOffset(mq, timestamp);
}

long long DefaultMQPullConsumer::maxOffset(const MessageQueue& mq)
{
    return m_pDefaultMQPullConsumerImpl->maxOffset(mq);
}

long long DefaultMQPullConsumer::minOffset(const MessageQueue& mq)
{
    return m_pDefaultMQPullConsumerImpl->minOffset(mq);
}

long long DefaultMQPullConsumer::earliestMsgStoreTime(const MessageQueue& mq)
{
    return m_pDefaultMQPullConsumerImpl->earliestMsgStoreTime(mq);
}

MessageExt* DefaultMQPullConsumer::viewMessage(const std::string& msgId)
{
    return m_pDefaultMQPullConsumerImpl->viewMessage(msgId);
}

QueryResult DefaultMQPullConsumer::queryMessage(const std::string& topic,
        const std::string&  key,
        int maxNum,
        long long begin,
        long long end)
{
    return m_pDefaultMQPullConsumerImpl->queryMessage(topic, key, maxNum, begin, end);
}
// MQadmin end

AllocateMessageQueueStrategy* DefaultMQPullConsumer::getAllocateMessageQueueStrategy()
{
    return m_pAllocateMessageQueueStrategy;
}

void DefaultMQPullConsumer::setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy)
{
    m_pAllocateMessageQueueStrategy = pAllocateMessageQueueStrategy;
}

int DefaultMQPullConsumer::getBrokerSuspendMaxTimeMillis()
{
    return m_brokerSuspendMaxTimeMillis;
}

void DefaultMQPullConsumer::setBrokerSuspendMaxTimeMillis(int brokerSuspendMaxTimeMillis)
{
    m_brokerSuspendMaxTimeMillis = brokerSuspendMaxTimeMillis;
}

std::string DefaultMQPullConsumer::getConsumerGroup()
{
    return m_consumerGroup;
}

void DefaultMQPullConsumer::setConsumerGroup(const std::string& consumerGroup)
{
    m_consumerGroup = consumerGroup;
}

int DefaultMQPullConsumer::getConsumerPullTimeoutMillis()
{
    return m_consumerPullTimeoutMillis;
}

void DefaultMQPullConsumer::setConsumerPullTimeoutMillis(int consumerPullTimeoutMillis)
{
    m_consumerPullTimeoutMillis = consumerPullTimeoutMillis;
}

int DefaultMQPullConsumer::getConsumerTimeoutMillisWhenSuspend()
{
    return m_consumerTimeoutMillisWhenSuspend;
}

void DefaultMQPullConsumer::setConsumerTimeoutMillisWhenSuspend(int consumerTimeoutMillisWhenSuspend)
{
    m_consumerTimeoutMillisWhenSuspend = consumerTimeoutMillisWhenSuspend;
}

MessageModel DefaultMQPullConsumer::getMessageModel()
{
    return m_messageModel;
}

void DefaultMQPullConsumer::setMessageModel(MessageModel messageModel)
{
    m_messageModel = messageModel;
}

MessageQueueListener* DefaultMQPullConsumer::getMessageQueueListener()
{
    return m_pMessageQueueListener;
}

void DefaultMQPullConsumer::setMessageQueueListener(MessageQueueListener* pMessageQueueListener)
{
    m_pMessageQueueListener = pMessageQueueListener;
}

std::set<std::string> DefaultMQPullConsumer::getRegisterTopics()
{
    return m_registerTopics;
}

void DefaultMQPullConsumer::setRegisterTopics(std::set<std::string> registerTopics)
{
    m_registerTopics = registerTopics;
}

//MQConsumer
void DefaultMQPullConsumer::sendMessageBack(MessageExt& msg, int delayLevel)
{
    m_pDefaultMQPullConsumerImpl->sendMessageBack(msg, delayLevel, "");
}

void DefaultMQPullConsumer::sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName)
{
    m_pDefaultMQPullConsumerImpl->sendMessageBack(msg, delayLevel, brokerName);
}



std::set<MessageQueue>* DefaultMQPullConsumer::fetchSubscribeMessageQueues(const std::string& topic)
{
    return m_pDefaultMQPullConsumerImpl->fetchSubscribeMessageQueues(topic);
}

void DefaultMQPullConsumer::start()
{
    m_pDefaultMQPullConsumerImpl->start();
}

void DefaultMQPullConsumer::shutdown()
{
    m_pDefaultMQPullConsumerImpl->shutdown();
}
//MQConsumer end

//MQPullConsumer
void DefaultMQPullConsumer::registerMessageQueueListener(const std::string& topic, MessageQueueListener* pListener)
{
    m_registerTopics.insert(topic);

    if (pListener)
    {
        m_pMessageQueueListener = pListener;
    }
}

PullResult* DefaultMQPullConsumer::pull(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums)
{
    return m_pDefaultMQPullConsumerImpl->pull(mq, subExpression, offset, maxNums);
}

void DefaultMQPullConsumer::pull(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums, PullCallback* pPullCallback)
{
    m_pDefaultMQPullConsumerImpl->pull(mq, subExpression, offset, maxNums, pPullCallback);
}

PullResult* DefaultMQPullConsumer::pullBlockIfNotFound(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums)
{
    return m_pDefaultMQPullConsumerImpl->pullBlockIfNotFound(mq, subExpression, offset, maxNums);
}

void DefaultMQPullConsumer::pullBlockIfNotFound(MessageQueue& mq,
        const std::string& subExpression,
        long long offset,
        int maxNums,
        PullCallback* pPullCallback)
{
    m_pDefaultMQPullConsumerImpl->pullBlockIfNotFound(mq, subExpression, offset, maxNums, pPullCallback);
}

void DefaultMQPullConsumer::updateConsumeOffset(MessageQueue& mq, long long offset)
{
    m_pDefaultMQPullConsumerImpl->updateConsumeOffset(mq, offset);
}

long long DefaultMQPullConsumer::fetchConsumeOffset(MessageQueue& mq, bool fromStore)
{
    return m_pDefaultMQPullConsumerImpl->fetchConsumeOffset(mq, fromStore);
}

std::set<MessageQueue>* DefaultMQPullConsumer::fetchMessageQueuesInBalance(const std::string& topic)
{
    return m_pDefaultMQPullConsumerImpl->fetchMessageQueuesInBalance(topic);
}
//MQPullConsumer end

OffsetStore* DefaultMQPullConsumer::getOffsetStore()
{
    return m_pOffsetStore;
}

void DefaultMQPullConsumer::setOffsetStore(OffsetStore* offsetStore)
{
    m_pOffsetStore = offsetStore;
}

DefaultMQPullConsumerImpl* DefaultMQPullConsumer::getDefaultMQPullConsumerImpl()
{
    return m_pDefaultMQPullConsumerImpl;
}

bool DefaultMQPullConsumer::isUnitMode()
{
    return m_unitMode;
}

void DefaultMQPullConsumer::setUnitMode(bool isUnitMode)
{
    m_unitMode = isUnitMode;
}

int DefaultMQPullConsumer::getMaxReconsumeTimes()
{
    return m_maxReconsumeTimes;
}

void DefaultMQPullConsumer::setMaxReconsumeTimes(int maxReconsumeTimes)
{
    m_maxReconsumeTimes = maxReconsumeTimes;
}


}

