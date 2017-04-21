/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License")
{
}
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
#include "DefaultMQProducer.h"

#include <assert.h>
#include "MessageExt.h"
#include "QueryResult.h"
#include "DefaultMQProducerImpl.h"
#include "MixAll.h"
#include "MQClientException.h"

namespace rmq
{

DefaultMQProducer::DefaultMQProducer()
    : m_producerGroup(MixAll::DEFAULT_PRODUCER_GROUP),
      m_createTopicKey(MixAll::DEFAULT_TOPIC),
  	  m_defaultTopicQueueNums(4),
      m_sendMsgTimeout(3000),
      m_compressMsgBodyOverHowmuch(1024 * 4),
	  m_retryTimesWhenSendFailed(2),
      m_retryAnotherBrokerWhenNotStoreOK(false),
      m_maxMessageSize(1024 * 128),
      m_compressLevel(5)
{
    m_pDefaultMQProducerImpl = new DefaultMQProducerImpl(this);
}

DefaultMQProducer::DefaultMQProducer(const std::string& producerGroup)
    : m_producerGroup(producerGroup),
      m_createTopicKey(MixAll::DEFAULT_TOPIC),
  	  m_defaultTopicQueueNums(4),
      m_sendMsgTimeout(3000),
      m_compressMsgBodyOverHowmuch(1024 * 4),
      m_retryTimesWhenSendFailed(2),
      m_retryAnotherBrokerWhenNotStoreOK(false),
      m_maxMessageSize(1024 * 128),
      m_compressLevel(5)
{
    m_pDefaultMQProducerImpl = new DefaultMQProducerImpl(this);
}

DefaultMQProducer::~DefaultMQProducer()
{
    // memleak: maybe core
    delete m_pDefaultMQProducerImpl;
}


void DefaultMQProducer::start()
{
    m_pDefaultMQProducerImpl->start();
}

void DefaultMQProducer::shutdown()
{
    m_pDefaultMQProducerImpl->shutdown();
}

std::vector<MessageQueue>* DefaultMQProducer::fetchPublishMessageQueues(const std::string& topic)
{
    return m_pDefaultMQProducerImpl->fetchPublishMessageQueues(topic);
}

SendResult DefaultMQProducer::send(Message& msg)
{
    return m_pDefaultMQProducerImpl->send(msg);
}

void DefaultMQProducer::send(Message& msg, SendCallback* pSendCallback)
{
    m_pDefaultMQProducerImpl->send(msg, pSendCallback);
}

void DefaultMQProducer::sendOneway(Message& msg)
{
    m_pDefaultMQProducerImpl->sendOneway(msg);
}

SendResult DefaultMQProducer::send(Message& msg, MessageQueue& mq)
{
    return m_pDefaultMQProducerImpl->send(msg, mq);
}

void DefaultMQProducer::send(Message& msg, MessageQueue& mq, SendCallback* pSendCallback)
{
    m_pDefaultMQProducerImpl->send(msg, mq, pSendCallback);
}

void DefaultMQProducer::sendOneway(Message& msg, MessageQueue& mq)
{
    m_pDefaultMQProducerImpl->sendOneway(msg, mq);
}

SendResult DefaultMQProducer::send(Message& msg, MessageQueueSelector* pSelector, void* arg)
{
    return m_pDefaultMQProducerImpl->send(msg, pSelector, arg);
}

void DefaultMQProducer::send(Message& msg,
                             MessageQueueSelector* pSelector,
                             void* arg,
                             SendCallback* pSendCallback)
{
    m_pDefaultMQProducerImpl->send(msg, pSelector, arg, pSendCallback);
}

void DefaultMQProducer::sendOneway(Message& msg, MessageQueueSelector* pSelector, void* arg)
{
    m_pDefaultMQProducerImpl->sendOneway(msg, pSelector, arg);
}

TransactionSendResult DefaultMQProducer::sendMessageInTransaction(Message& msg,
        LocalTransactionExecuter* tranExecuter, void* arg)
{
    THROW_MQEXCEPTION(MQClientException,
                      "sendMessageInTransaction not implement, please use TransactionMQProducer class", -1);
    TransactionSendResult result;

    return result;
}

void DefaultMQProducer::createTopic(const std::string& key, const std::string& newTopic, int queueNum)
{
    m_pDefaultMQProducerImpl->createTopic(key, newTopic, queueNum);
}

long long DefaultMQProducer::searchOffset(const MessageQueue& mq, long long timestamp)
{
    return m_pDefaultMQProducerImpl->searchOffset(mq, timestamp);
}

long long DefaultMQProducer::maxOffset(const MessageQueue& mq)
{
    return m_pDefaultMQProducerImpl->maxOffset(mq);
}

long long DefaultMQProducer::minOffset(const MessageQueue& mq)
{
    return m_pDefaultMQProducerImpl->minOffset(mq);
}

long long DefaultMQProducer::earliestMsgStoreTime(const MessageQueue& mq)
{
    return m_pDefaultMQProducerImpl->earliestMsgStoreTime(mq);
}

MessageExt* DefaultMQProducer::viewMessage(const std::string& msgId)
{
    return m_pDefaultMQProducerImpl->viewMessage(msgId);
}

QueryResult DefaultMQProducer::queryMessage(const std::string& topic,
        const std::string& key,
        int maxNum,
        long long begin,
        long long end)
{

    return m_pDefaultMQProducerImpl->queryMessage(topic, key, maxNum, begin, end);
}

std::string DefaultMQProducer::getProducerGroup()
{
    return m_producerGroup;
}

void DefaultMQProducer::setProducerGroup(const std::string& producerGroup)
{
    m_producerGroup = producerGroup;
}

std::string DefaultMQProducer::getCreateTopicKey()
{
    return m_createTopicKey;
}

void DefaultMQProducer::setCreateTopicKey(const std::string& createTopicKey)
{
    m_createTopicKey = createTopicKey;
}

int DefaultMQProducer::getSendMsgTimeout()
{
    return m_sendMsgTimeout;
}

void DefaultMQProducer::setSendMsgTimeout(int sendMsgTimeout)
{
    m_sendMsgTimeout = sendMsgTimeout;
}

int DefaultMQProducer::getCompressMsgBodyOverHowmuch()
{
    return m_compressMsgBodyOverHowmuch;
}

void DefaultMQProducer::setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch)
{
    m_compressMsgBodyOverHowmuch = compressMsgBodyOverHowmuch;
}

DefaultMQProducerImpl* DefaultMQProducer::getDefaultMQProducerImpl()
{
    return m_pDefaultMQProducerImpl;
}

bool DefaultMQProducer::isRetryAnotherBrokerWhenNotStoreOK()
{
    return m_retryAnotherBrokerWhenNotStoreOK;
}

void DefaultMQProducer::setRetryAnotherBrokerWhenNotStoreOK(bool retryAnotherBrokerWhenNotStoreOK)
{
    m_retryAnotherBrokerWhenNotStoreOK = retryAnotherBrokerWhenNotStoreOK;
}

int DefaultMQProducer::getMaxMessageSize()
{
    return m_maxMessageSize;
}

void DefaultMQProducer::setMaxMessageSize(int maxMessageSize)
{
    m_maxMessageSize = maxMessageSize;
}

int DefaultMQProducer::getDefaultTopicQueueNums()
{
    return m_defaultTopicQueueNums;
}

void DefaultMQProducer::setDefaultTopicQueueNums(int defaultTopicQueueNums)
{
    m_defaultTopicQueueNums = defaultTopicQueueNums;
}

int DefaultMQProducer::getRetryTimesWhenSendFailed()
{
    return m_retryTimesWhenSendFailed;
}

void DefaultMQProducer::setRetryTimesWhenSendFailed(int retryTimesWhenSendFailed)
{
    m_retryTimesWhenSendFailed = retryTimesWhenSendFailed;
}

int DefaultMQProducer::getCompressLevel()
{
    return m_compressLevel;
}

void DefaultMQProducer::setCompressLevel(int compressLevel)
{
    assert(compressLevel >= 0 && compressLevel <= 9 || compressLevel == -1);

    m_compressLevel = compressLevel;
}


}

