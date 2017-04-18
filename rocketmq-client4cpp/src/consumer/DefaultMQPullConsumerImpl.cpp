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

#include "DefaultMQPullConsumerImpl.h"

#include <iostream>
#include <string>
#include <set>
#include "DefaultMQPullConsumer.h"
#include "DefaultMQProducer.h"
#include "MQClientFactory.h"
#include "MQAdminImpl.h"
#include "RebalancePullImpl.h"
#include "MQClientAPIImpl.h"
#include "OffsetStore.h"
#include "MixAll.h"
#include "MQClientManager.h"
#include "LocalFileOffsetStore.h"
#include "RemoteBrokerOffsetStore.h"
#include "PullSysFlag.h"
#include "FilterAPI.h"
#include "PullAPIWrapper.h"
#include "MQClientException.h"
#include "Validators.h"
#include "ScopedLock.h"

namespace rmq
{

DefaultMQPullConsumerImpl::DefaultMQPullConsumerImpl(DefaultMQPullConsumer* pDefaultMQPullConsumer)
    : m_pDefaultMQPullConsumer(pDefaultMQPullConsumer),
      m_serviceState(CREATE_JUST)
{
	m_pMQClientFactory = NULL;
	m_pPullAPIWrapper = NULL;
    m_pOffsetStore = NULL;
    m_pRebalanceImpl = new RebalancePullImpl(this);
}

DefaultMQPullConsumerImpl::~DefaultMQPullConsumerImpl()
{
	if (m_pRebalanceImpl)
		delete m_pRebalanceImpl;
	if (m_pPullAPIWrapper)
		delete m_pPullAPIWrapper;
	if (m_pOffsetStore)
		delete m_pOffsetStore;
	//delete m_pMQClientFactory;
}

void  DefaultMQPullConsumerImpl::start()
{
    RMQ_INFO("DefaultMQPullConsumerImpl::start()");
    switch (m_serviceState)
    {
        case CREATE_JUST:
	        {
	        	RMQ_INFO("the consumer [{%s}] start beginning. messageModel={%s}",
                	m_pDefaultMQPullConsumer->getConsumerGroup().c_str(),
                	getMessageModelString(m_pDefaultMQPullConsumer->getMessageModel()));

	            m_serviceState = START_FAILED;
	            checkConfig();
	            copySubscription();

	            if (m_pDefaultMQPullConsumer->getMessageModel() == CLUSTERING)
	            {
	                m_pDefaultMQPullConsumer->changeInstanceNameToPID();
	            }

	            m_pMQClientFactory = MQClientManager::getInstance()->getAndCreateMQClientFactory(*m_pDefaultMQPullConsumer);

	            m_pRebalanceImpl->setConsumerGroup(m_pDefaultMQPullConsumer->getConsumerGroup());
	            m_pRebalanceImpl->setMessageModel(m_pDefaultMQPullConsumer->getMessageModel());
	            m_pRebalanceImpl->setAllocateMessageQueueStrategy(m_pDefaultMQPullConsumer->getAllocateMessageQueueStrategy());
	            m_pRebalanceImpl->setmQClientFactory(m_pMQClientFactory);

	            m_pPullAPIWrapper = new PullAPIWrapper(m_pMQClientFactory, m_pDefaultMQPullConsumer->getConsumerGroup());

	            if (m_pDefaultMQPullConsumer->getOffsetStore() != NULL)
	            {
	                m_pOffsetStore = m_pDefaultMQPullConsumer->getOffsetStore();
	            }
	            else
	            {
	                switch (m_pDefaultMQPullConsumer->getMessageModel())
	                {
	                    case BROADCASTING:
	                        m_pOffsetStore = new LocalFileOffsetStore(m_pMQClientFactory, m_pDefaultMQPullConsumer->getConsumerGroup());
	                        break;
	                    case CLUSTERING:
	                        m_pOffsetStore = new RemoteBrokerOffsetStore(m_pMQClientFactory, m_pDefaultMQPullConsumer->getConsumerGroup());
	                        break;
	                    default:
	                        break;
	                }
	            }

	            m_pOffsetStore->load();

	            bool registerOK =
	                m_pMQClientFactory->registerConsumer(m_pDefaultMQPullConsumer->getConsumerGroup(), this);
	            if (!registerOK)
	            {
	                m_serviceState = CREATE_JUST;
	                std::string str = "The consumer group[" + m_pDefaultMQPullConsumer->getConsumerGroup();
	                str += "] has been created before, specify another name please.";
	                THROW_MQEXCEPTION(MQClientException, str, -1);
	            }

	            m_pMQClientFactory->start();

	            m_serviceState = RUNNING;
	        }
        	break;
        case RUNNING:
        case START_FAILED:
        case SHUTDOWN_ALREADY:
            THROW_MQEXCEPTION(MQClientException, "The PullConsumer service state not OK, maybe started once, ", -1);
        default:
            break;
    }
}


void  DefaultMQPullConsumerImpl::shutdown()
{
    RMQ_DEBUG("DefaultMQPullConsumerImpl::shutdown()");
    switch (m_serviceState)
    {
        case CREATE_JUST:
            break;
        case RUNNING:
            persistConsumerOffset();
            m_pMQClientFactory->unregisterConsumer(m_pDefaultMQPullConsumer->getConsumerGroup());
            m_pMQClientFactory->shutdown();

            m_serviceState = SHUTDOWN_ALREADY;
            break;
        case SHUTDOWN_ALREADY:
            break;
        default:
            break;
    }
}


void DefaultMQPullConsumerImpl::createTopic(const std::string& key, const std::string& newTopic, int queueNum)
{
    makeSureStateOK();
    m_pMQClientFactory->getMQAdminImpl()->createTopic(key, newTopic, queueNum);
}

long long DefaultMQPullConsumerImpl::fetchConsumeOffset(MessageQueue& mq, bool fromStore)
{
    makeSureStateOK();
    return m_pOffsetStore->readOffset(mq, fromStore ? READ_FROM_STORE : MEMORY_FIRST_THEN_STORE);
}

std::set<MessageQueue>* DefaultMQPullConsumerImpl::fetchMessageQueuesInBalance(const std::string& topic)
{
    makeSureStateOK();
    std::set<MessageQueue>* mqResult = new std::set<MessageQueue>;

	kpr::ScopedRLock<kpr::RWMutex> lock(m_pRebalanceImpl->getProcessQueueTableLock());
    std::map<MessageQueue, ProcessQueue*>& mqTable = m_pRebalanceImpl->getProcessQueueTable();
    RMQ_FOR_EACH(mqTable, it)
    {
        if (it->first.getTopic() == topic)
        {
            mqResult->insert(it->first);
        }
    }

    return mqResult;
}

std::vector<MessageQueue>* DefaultMQPullConsumerImpl::fetchPublishMessageQueues(const std::string&  topic)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->fetchPublishMessageQueues(topic);
}

std::set<MessageQueue>*  DefaultMQPullConsumerImpl::fetchSubscribeMessageQueues(const std::string& topic)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->fetchSubscribeMessageQueues(topic);
}

long long  DefaultMQPullConsumerImpl::earliestMsgStoreTime(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->earliestMsgStoreTime(mq);
}

std::string  DefaultMQPullConsumerImpl::groupName()
{
    return m_pDefaultMQPullConsumer->getConsumerGroup();
}

MessageModel  DefaultMQPullConsumerImpl::messageModel()
{
    return m_pDefaultMQPullConsumer->getMessageModel();
}

ConsumeType  DefaultMQPullConsumerImpl::consumeType()
{
    return CONSUME_ACTIVELY;
}

ConsumeFromWhere  DefaultMQPullConsumerImpl::consumeFromWhere()
{
    return CONSUME_FROM_LAST_OFFSET;
}

std::set<SubscriptionData>  DefaultMQPullConsumerImpl::subscriptions()
{
    //TODO
    std::set<SubscriptionData> result;
    return result;
}

void DefaultMQPullConsumerImpl::doRebalance()
{
    if (m_pRebalanceImpl != NULL)
    {
        m_pRebalanceImpl->doRebalance();
    }
}

void  DefaultMQPullConsumerImpl::persistConsumerOffset()
{
    try
    {
        makeSureStateOK();

        std::set<MessageQueue> mqs;
		{
	        kpr::ScopedRLock<kpr::RWMutex> lock(m_pRebalanceImpl->getProcessQueueTableLock());
	        std::map<MessageQueue, ProcessQueue*> processQueueTable = m_pRebalanceImpl->getProcessQueueTable();
	        RMQ_FOR_EACH(processQueueTable, it)
	        {
	            mqs.insert(it->first);
	        }
        }

        m_pOffsetStore->persistAll(mqs);
    }
    catch (...)
    {
        RMQ_ERROR("group {%s} persistConsumerOffset exception",
                  m_pDefaultMQPullConsumer->getConsumerGroup().c_str());
    }
}

void  DefaultMQPullConsumerImpl::updateTopicSubscribeInfo(const std::string& topic, const std::set<MessageQueue>& info)
{
    std::map<std::string, SubscriptionData>& subTable = m_pRebalanceImpl->getSubscriptionInner();

    if (subTable.find(topic) != subTable.end())
    {
        m_pRebalanceImpl->getTopicSubscribeInfoTable().insert(std::pair<std::string, std::set<MessageQueue> >(topic, info));
    }
}

bool  DefaultMQPullConsumerImpl::isSubscribeTopicNeedUpdate(const std::string& topic)
{
    std::map<std::string, SubscriptionData>& subTable = m_pRebalanceImpl->getSubscriptionInner();
    if (subTable.find(topic) != subTable.end())
    {
        std::map<std::string, std::set<MessageQueue> >& mqs =
            m_pRebalanceImpl->getTopicSubscribeInfoTable();
        return mqs.find(topic) == mqs.end();
    }

    return false;
}

long long  DefaultMQPullConsumerImpl::maxOffset(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->maxOffset(mq);
}

long long  DefaultMQPullConsumerImpl::minOffset(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->minOffset(mq);
}

PullResult* DefaultMQPullConsumerImpl::pull(MessageQueue& mq,
        const std::string& subExpression,
        long long offset,
        int maxNums)
{
    return pullSyncImpl(mq, subExpression, offset, maxNums, false);
}

void  DefaultMQPullConsumerImpl::pull(MessageQueue& mq,
                                      const std::string& subExpression,
                                      long long offset,
                                      int maxNums,
                                      PullCallback* pPullCallback)
{
    pullAsyncImpl(mq, subExpression, offset, maxNums, pPullCallback, false);
}

PullResult* DefaultMQPullConsumerImpl::pullBlockIfNotFound(MessageQueue& mq,
        const std::string& subExpression,
        long long offset,
        int maxNums)
{
    return pullSyncImpl(mq, subExpression, offset, maxNums, true);
}

void  DefaultMQPullConsumerImpl::pullBlockIfNotFound(MessageQueue& mq,
        const std::string& subExpression,
        long long offset,
        int maxNums,
        PullCallback* pPullCallback)
{
    pullAsyncImpl(mq, subExpression, offset, maxNums, pPullCallback, true);
}

QueryResult  DefaultMQPullConsumerImpl::queryMessage(const std::string& topic,
        const std::string&  key,
        int maxNum,
        long long begin,
        long long end)
{
    makeSureStateOK();

    QueryResult result(0, std::list<MessageExt*>());
    return m_pMQClientFactory->getMQAdminImpl()->queryMessage(topic, key, maxNum, begin, end);
}

long long DefaultMQPullConsumerImpl::searchOffset(const MessageQueue& mq, long long timestamp)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->searchOffset(mq, timestamp);
}

void  DefaultMQPullConsumerImpl::sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName)
{
	return sendMessageBack(msg, delayLevel, brokerName, m_pDefaultMQPullConsumer->getConsumerGroup());
}


void DefaultMQPullConsumerImpl::sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName,
	const std::string& consumerGroup)
{
    try
    {
    	std::string brokerAddr = brokerName.empty() ?
    		socketAddress2IPPort(msg.getStoreHost()) : m_pMQClientFactory->findBrokerAddressInPublish(brokerName);

        m_pMQClientFactory->getMQClientAPIImpl()->consumerSendMessageBack(brokerAddr, msg,
                consumerGroup.empty() ? m_pDefaultMQPullConsumer->getConsumerGroup() : consumerGroup,
                delayLevel,
                3000);
    }
    catch (...)
    {
        RMQ_ERROR("sendMessageBack Exception, group: %s", m_pDefaultMQPullConsumer->getConsumerGroup().c_str());
        Message newMsg(MixAll::getRetryTopic(m_pDefaultMQPullConsumer->getConsumerGroup()),
                       msg.getBody(), msg.getBodyLen());

		std::string originMsgId = msg.getProperty(Message::PROPERTY_ORIGIN_MESSAGE_ID);
		newMsg.putProperty(Message::PROPERTY_ORIGIN_MESSAGE_ID, UtilAll::isBlank(originMsgId) ? msg.getMsgId()
                    : originMsgId);

        newMsg.setFlag(msg.getFlag());
        newMsg.setProperties(msg.getProperties());
        newMsg.putProperty(Message::PROPERTY_RETRY_TOPIC, msg.getTopic());

        int reTimes = msg.getReconsumeTimes() + 1;
        newMsg.putProperty(Message::PROPERTY_RECONSUME_TIME, UtilAll::toString(reTimes));
        newMsg.putProperty(Message::PROPERTY_MAX_RECONSUME_TIMES, UtilAll::toString(m_pDefaultMQPullConsumer->getMaxReconsumeTimes()));
        newMsg.setDelayTimeLevel(3 + reTimes);

        m_pMQClientFactory->getDefaultMQProducer()->send(newMsg);
    }
}

void  DefaultMQPullConsumerImpl::updateConsumeOffset(MessageQueue& mq, long long offset)
{
    makeSureStateOK();
    m_pOffsetStore->updateOffset(mq, offset, false);
}

MessageExt*  DefaultMQPullConsumerImpl::viewMessage(const std::string& msgId)
{
    makeSureStateOK();

    return m_pMQClientFactory->getMQAdminImpl()->viewMessage(msgId);
}

DefaultMQPullConsumer*  DefaultMQPullConsumerImpl::getDefaultMQPullConsumer()
{
    return m_pDefaultMQPullConsumer;
}

OffsetStore*  DefaultMQPullConsumerImpl::getOffsetStore()
{
    return m_pOffsetStore;
}

void  DefaultMQPullConsumerImpl::setOffsetStore(OffsetStore* pOffsetStore)
{
    m_pOffsetStore = pOffsetStore;
}

void  DefaultMQPullConsumerImpl::makeSureStateOK()
{
    if (m_serviceState != RUNNING)
    {
        THROW_MQEXCEPTION(MQClientException, "The consumer service state not OK, ", -1);
    }
}

PullResult* DefaultMQPullConsumerImpl::pullSyncImpl(MessageQueue& mq,
        const std::string& subExpression,
        long long offset,
        int maxNums,
        bool block)
{
    makeSureStateOK();

    if (offset < 0)
    {
        THROW_MQEXCEPTION(MQClientException, "offset < 0", -1);
    }

    if (maxNums <= 0)
    {
        THROW_MQEXCEPTION(MQClientException, "maxNums <= 0", -1);
    }

    subscriptionAutomatically(mq.getTopic());

    int sysFlag = PullSysFlag::buildSysFlag(false, block, true);

    SubscriptionDataPtr subscriptionData = NULL;
    try
    {
        subscriptionData = FilterAPI::buildSubscriptionData(mq.getTopic(), subExpression);
    }
    catch (...)
    {
        THROW_MQEXCEPTION(MQClientException, "parse subscription error", -1);
    }

    int timeoutMillis =
        block ? m_pDefaultMQPullConsumer->getConsumerTimeoutMillisWhenSuspend()
        : m_pDefaultMQPullConsumer->getConsumerPullTimeoutMillis();

    PullResult* pullResult = m_pPullAPIWrapper->pullKernelImpl(//
                                 mq, // 1
                                 subscriptionData->getSubString(), // 2
                                 0L, // 3
                                 offset, // 4
                                 maxNums, // 5
                                 sysFlag, // 6
                                 0, // 7
                                 m_pDefaultMQPullConsumer->getBrokerSuspendMaxTimeMillis(), // 8
                                 timeoutMillis, // 9
                                 SYNC, // 10
                                 NULL// 11
                             );

    return m_pPullAPIWrapper->processPullResult(mq, *pullResult, *subscriptionData);
}

void  DefaultMQPullConsumerImpl::subscriptionAutomatically(const std::string& topic)
{
    std::map<std::string, SubscriptionData>& sd = m_pRebalanceImpl->getSubscriptionInner();
    std::map<std::string, SubscriptionData>::iterator it = sd.find(topic);

    if (it == sd.end())
    {
        try
        {
            SubscriptionDataPtr subscriptionData =
                FilterAPI::buildSubscriptionData(topic, SubscriptionData::SUB_ALL);
            sd[topic] = *subscriptionData;
        }
        catch (...)
        {
        	RMQ_WARN("FilterAPI::buildSubscriptionData exception");
        }
    }
}

void DefaultMQPullConsumerImpl::pullAsyncImpl(//
    MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums,
    PullCallback* pPullCallback,//
    bool block)
{
    makeSureStateOK();

    if (offset < 0)
    {
        THROW_MQEXCEPTION(MQClientException, "offset < 0", -1);
    }

    if (maxNums <= 0)
    {
        THROW_MQEXCEPTION(MQClientException, "maxNums <= 0", -1);
    }

    if (pPullCallback == NULL)
    {
        THROW_MQEXCEPTION(MQClientException, "pullCallback is null", -1);
    }

    subscriptionAutomatically(mq.getTopic());
    try
    {
        int sysFlag = PullSysFlag::buildSysFlag(false, block, true);

        SubscriptionDataPtr subscriptionData = NULL;
        try
        {
            subscriptionData = FilterAPI::buildSubscriptionData(mq.getTopic(), subExpression);
        }
        catch (...)
        {
            THROW_MQEXCEPTION(MQClientException, "parse subscription error", -1);
        }

        int timeoutMillis =
            block ? m_pDefaultMQPullConsumer->getConsumerTimeoutMillisWhenSuspend()
            : m_pDefaultMQPullConsumer->getConsumerPullTimeoutMillis();
        DefaultMQPullConsumerImplCallback* callback =
            new DefaultMQPullConsumerImplCallback(*subscriptionData,
                    mq, this, pPullCallback);

        m_pPullAPIWrapper->pullKernelImpl(
			mq, // 1
			subscriptionData->getSubString(), // 2
			0L, // 3
			offset, // 4
			maxNums, // 5
			sysFlag, // 6
			0, // 7
			m_pDefaultMQPullConsumer->getBrokerSuspendMaxTimeMillis(), // 8
			timeoutMillis, // 9
			ASYNC, // 10
			callback// 11
		);
    }
    catch (const MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, "pullAsync unknow exception", -1);
    }
}


void  DefaultMQPullConsumerImpl::copySubscription()
{
    try
    {
        std::set<std::string> registerTopics = m_pDefaultMQPullConsumer->getRegisterTopics();
        std::set<std::string>::iterator it = registerTopics.begin();

        for (; it != registerTopics.end(); it++)
        {
            SubscriptionDataPtr subscriptionData =
                FilterAPI::buildSubscriptionData(*it, SubscriptionData::SUB_ALL);
            m_pRebalanceImpl->getSubscriptionInner()[*it] = *subscriptionData;
        }
    }
    catch (...)
    {
        THROW_MQEXCEPTION(MQClientException, "subscription exception", -1);
    }
}


void  DefaultMQPullConsumerImpl::checkConfig()
{
    // check consumerGroup
    Validators::checkGroup(m_pDefaultMQPullConsumer->getConsumerGroup());

    // consumerGroup
    if (m_pDefaultMQPullConsumer->getConsumerGroup() == MixAll::DEFAULT_CONSUMER_GROUP)
    {
        THROW_MQEXCEPTION(MQClientException, "consumerGroup can not equal "
                          + MixAll::DEFAULT_CONSUMER_GROUP //
                          + ", please specify another one.", -1);
    }

    if (m_pDefaultMQPullConsumer->getMessageModel() != BROADCASTING
        && m_pDefaultMQPullConsumer->getMessageModel() != CLUSTERING)
    {
        THROW_MQEXCEPTION(MQClientException, "messageModel is valid ", -1);
    }

    // allocateMessageQueueStrategy
    if (m_pDefaultMQPullConsumer->getAllocateMessageQueueStrategy() == NULL)
    {
        THROW_MQEXCEPTION(MQClientException, "allocateMessageQueueStrategy is null", -1);
    }
}

ServiceState DefaultMQPullConsumerImpl::getServiceState()
{
    return m_serviceState;
}

void DefaultMQPullConsumerImpl::setServiceState(ServiceState serviceState)
{
    m_serviceState = serviceState;
}


}
