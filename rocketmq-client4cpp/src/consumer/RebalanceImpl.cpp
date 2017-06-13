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

#include "RebalanceImpl.h"
#include "AllocateMessageQueueStrategy.h"
#include "MQClientFactory.h"
#include "MixAll.h"
#include "LockBatchBody.h"
#include "MQClientAPIImpl.h"
#include "KPRUtil.h"
#include "ScopedLock.h"

namespace rmq
{

RebalanceImpl::RebalanceImpl(const std::string& consumerGroup,
                             MessageModel messageModel,
                             AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy,
                             MQClientFactory* pMQClientFactory)
    : m_consumerGroup(consumerGroup),
      m_messageModel(messageModel),
      m_pAllocateMessageQueueStrategy(pAllocateMessageQueueStrategy),
      m_pMQClientFactory(pMQClientFactory)
{

}

RebalanceImpl::~RebalanceImpl()
{
}

void RebalanceImpl::unlock(MessageQueue& mq, bool oneway)
{
    FindBrokerResult findBrokerResult =
        m_pMQClientFactory->findBrokerAddressInSubscribe(mq.getBrokerName(), MixAll::MASTER_ID, true);
    if (!findBrokerResult.brokerAddr.empty())
    {
        UnlockBatchRequestBody* requestBody = new UnlockBatchRequestBody();
        requestBody->setConsumerGroup(m_consumerGroup);
        requestBody->setClientId(m_pMQClientFactory->getClientId());
        requestBody->getMqSet().insert(mq);

        try
        {
            m_pMQClientFactory->getMQClientAPIImpl()->unlockBatchMQ(findBrokerResult.brokerAddr,
                    requestBody, 1000, oneway);
        }
        catch (...)
        {
            RMQ_ERROR("unlockBatchMQ exception, MQ: {%s}" , mq.toString().c_str());
        }
    }
}

void RebalanceImpl::unlockAll(bool oneway)
{
    std::map<std::string, std::set<MessageQueue> > brokerMqs = buildProcessQueueTableByBrokerName();
    std::map<std::string, std::set<MessageQueue> >::iterator it = brokerMqs.begin();

    for (; it != brokerMqs.end(); it++)
    {
        std::string brokerName = it->first;
        std::set<MessageQueue> mqs = it->second;

        if (mqs.empty())
        {
            continue;
        }

        FindBrokerResult findBrokerResult =
            m_pMQClientFactory->findBrokerAddressInSubscribe(brokerName, MixAll::MASTER_ID, true);

        if (!findBrokerResult.brokerAddr.empty())
        {
            UnlockBatchRequestBody* requestBody = new UnlockBatchRequestBody();
            requestBody->setConsumerGroup(m_consumerGroup);
            requestBody->setClientId(m_pMQClientFactory->getClientId());
            requestBody->setMqSet(mqs);

            try
            {
                m_pMQClientFactory->getMQClientAPIImpl()->unlockBatchMQ(findBrokerResult.brokerAddr,
                        requestBody, 1000, oneway);

                kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
                std::set<MessageQueue>::iterator itm = mqs.begin();
                for (; itm != mqs.end(); itm++)
                {
                    std::map<MessageQueue, ProcessQueue*>::iterator itp = m_processQueueTable.find(*itm);
                    if (itp != m_processQueueTable.end())
                    {
                        itp->second->setLocked(false);
                        RMQ_INFO("the message queue unlock OK, Group: {%s}, MQ: {%s}",
                                 m_consumerGroup.c_str(), (*itm).toString().c_str());
                    }
                }
            }
            catch (...)
            {
                RMQ_ERROR("unlockBatchMQ exception, mqs.size: {%u} ", (unsigned)mqs.size());
            }
        }
    }
}

bool RebalanceImpl::lock(MessageQueue& mq)
{
    FindBrokerResult findBrokerResult =
        m_pMQClientFactory->findBrokerAddressInSubscribe(mq.getBrokerName(), MixAll::MASTER_ID, true);
    if (!findBrokerResult.brokerAddr.empty())
    {
        LockBatchRequestBody* requestBody = new LockBatchRequestBody();
        requestBody->setConsumerGroup(m_consumerGroup);
        requestBody->setClientId(m_pMQClientFactory->getClientId());
        requestBody->getMqSet().insert(mq);

        try
        {
            std::set<MessageQueue> lockedMq =
                m_pMQClientFactory->getMQClientAPIImpl()->lockBatchMQ(
                    findBrokerResult.brokerAddr, requestBody, 1000);

            std::set<MessageQueue>::iterator it = lockedMq.begin();
            for (; it != lockedMq.end(); it++)
            {
                kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
                MessageQueue mmqq = *it;
                std::map<MessageQueue, ProcessQueue*>::iterator itt = m_processQueueTable.find(mmqq);
                if (itt != m_processQueueTable.end())
                {
                    itt->second->setLocked(true);
                    itt->second->setLastLockTimestamp(KPRUtil::GetCurrentTimeMillis());
                }
            }

            it = lockedMq.find(mq);
            bool lockOK = (it != lockedMq.end());

            RMQ_INFO("the message queue lock {%s}, {%s}, {%s}",//
                     (lockOK ? "OK" : "Failed"), //
                     m_consumerGroup.c_str(), //
                     mq.toString().c_str());
            return lockOK;
        }
        catch (...)
        {
            RMQ_ERROR("lockBatchMQ exception, MQ: {%s}", mq.toString().c_str());
        }
    }

    return false;
}

void RebalanceImpl::lockAll()
{
    std::map<std::string, std::set<MessageQueue> > brokerMqs = buildProcessQueueTableByBrokerName();

    std::map<std::string, std::set<MessageQueue> >::iterator it = brokerMqs.begin();
    for (; it != brokerMqs.end(); it++)
    {
        std::string brokerName = it->first;
        std::set<MessageQueue> mqs = it->second;

        if (mqs.empty())
        {
            continue;
        }

        FindBrokerResult findBrokerResult =
            m_pMQClientFactory->findBrokerAddressInSubscribe(brokerName, MixAll::MASTER_ID, true);
        if (!findBrokerResult.brokerAddr.empty())
        {
            LockBatchRequestBody* requestBody = new LockBatchRequestBody();
            requestBody->setConsumerGroup(m_consumerGroup);
            requestBody->setClientId(m_pMQClientFactory->getClientId());
            requestBody->setMqSet(mqs);

            try
            {
                std::set<MessageQueue> lockOKMQSet =
                    m_pMQClientFactory->getMQClientAPIImpl()->lockBatchMQ(
                        findBrokerResult.brokerAddr, requestBody, 1000);

                std::set<MessageQueue>::iterator its = lockOKMQSet.begin();
                for (; its != lockOKMQSet.end(); its++)
                {
                    kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
                    MessageQueue mq = *its;
                    std::map<MessageQueue, ProcessQueue*>::iterator itt = m_processQueueTable.find(mq);
                    if (itt != m_processQueueTable.end())
                    {
                        ProcessQueue* processQueue = itt->second;
                        if (!processQueue->isLocked())
                        {
                            RMQ_INFO("the message queue locked OK, Group: {%s}, MQ: %s",
                            	m_consumerGroup.c_str(),
                            	mq.toString().c_str());
                        }

                        processQueue->setLocked(true);
                        processQueue->setLastLockTimestamp(KPRUtil::GetCurrentTimeMillis());
                    }
                }

                its = mqs.begin();
                for (; its != mqs.end(); its++)
                {
                    MessageQueue mq = *its;
                    std::set<MessageQueue>::iterator itf = lockOKMQSet.find(mq);
                    if (itf == lockOKMQSet.end())
                    {
                        kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
                        std::map<MessageQueue, ProcessQueue*>::iterator itt = m_processQueueTable.find(mq);
                        if (itt != m_processQueueTable.end())
                        {
                            itt->second->setLocked(false);
                            RMQ_WARN("the message queue locked Failed, Group: {%s}, MQ: %s",
                            	m_consumerGroup.c_str(),
                            	mq.toString().c_str());
                        }
                    }
                }
            }
            catch (std::exception& e)
            {
                RMQ_ERROR("lockBatchMQ exception: %s", e.what());
            }
        }
    }
}

void RebalanceImpl::doRebalance()
{
    std::map<std::string, SubscriptionData> subTable = getSubscriptionInner();
    std::map<std::string, SubscriptionData>::iterator it = subTable.begin();
    for (; it != subTable.end(); it++)
    {
        std::string topic = it->first;
        try
        {
            rebalanceByTopic(topic);
        }
        catch (std::exception& e)
        {
            if (topic.find(MixAll::RETRY_GROUP_TOPIC_PREFIX) != 0)
            {
                RMQ_WARN("rebalanceByTopic Exception: %s", e.what());
            }
        }
    }

    truncateMessageQueueNotMyTopic();
}

std::map<std::string, SubscriptionData>& RebalanceImpl::getSubscriptionInner()
{
    return m_subscriptionInner;
}

std::map<MessageQueue, ProcessQueue*>& RebalanceImpl::getProcessQueueTable()
{
    return m_processQueueTable;
}


kpr::RWMutex& RebalanceImpl::getProcessQueueTableLock()
{
	return m_processQueueTableLock;
}


std::map<std::string, std::set<MessageQueue> >& RebalanceImpl::getTopicSubscribeInfoTable()
{
    return m_topicSubscribeInfoTable;
}

std::string& RebalanceImpl::getConsumerGroup()
{
    return m_consumerGroup;
}

void RebalanceImpl::setConsumerGroup(const std::string& consumerGroup)
{
    m_consumerGroup = consumerGroup;
}

MessageModel RebalanceImpl::getMessageModel()
{
    return m_messageModel;
}

void RebalanceImpl::setMessageModel(MessageModel messageModel)
{
    m_messageModel = messageModel;
}

AllocateMessageQueueStrategy* RebalanceImpl::getAllocateMessageQueueStrategy()
{
    return m_pAllocateMessageQueueStrategy;
}

void RebalanceImpl::setAllocateMessageQueueStrategy(AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy)
{
    m_pAllocateMessageQueueStrategy = pAllocateMessageQueueStrategy;
}

MQClientFactory* RebalanceImpl::getmQClientFactory()
{
    return m_pMQClientFactory;
}

void RebalanceImpl::setmQClientFactory(MQClientFactory* pMQClientFactory)
{
    m_pMQClientFactory = pMQClientFactory;
}

std::map<std::string, std::set<MessageQueue> > RebalanceImpl::buildProcessQueueTableByBrokerName()
{
	std::map<std::string, std::set<MessageQueue> > result ;
    kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
    std::map<MessageQueue, ProcessQueue*>::iterator it = m_processQueueTable.begin();
    for (; it != m_processQueueTable.end();)
    {
        MessageQueue mq = it->first;
        std::map<std::string, std::set<MessageQueue> >::iterator itm = result.find(mq.getBrokerName());
        if (itm == result.end())
        {
            std::set<MessageQueue> mqs ;
            mqs.insert(mq);
            result[mq.getBrokerName()] = mqs;
        }
        else
        {
            itm->second.insert(mq);
        }
    }

    return result;
}

void RebalanceImpl::rebalanceByTopic(const std::string& topic)
{
	RMQ_DEBUG("rebalanceByTopic begin, topic={%s}", topic.c_str());
    switch (m_messageModel)
    {
        case BROADCASTING:
        {
            //kpr::ScopedLock<kpr::Mutex> lock(m_topicSubscribeInfoTableLock);
            std::map<std::string, std::set<MessageQueue> >::iterator it = m_topicSubscribeInfoTable.find(topic);
            if (it != m_topicSubscribeInfoTable.end())
            {
                std::set<MessageQueue> mqSet = it->second;
                bool changed = updateProcessQueueTableInRebalance(topic, mqSet);
                if (changed)
                {
                    messageQueueChanged(topic, mqSet, mqSet);
                    RMQ_INFO("messageQueueChanged {%s} {%s} {%s} {%s}",
                             m_consumerGroup.c_str(),
                             topic.c_str(),
                             UtilAll::toString(mqSet).c_str(),
                             UtilAll::toString(mqSet).c_str());
                }
            }
            else
            {
                RMQ_WARN("doRebalance, {%s}, but the topic[%s] not exist.", m_consumerGroup.c_str(), topic.c_str());
            }
            break;
        }
        case CLUSTERING:
        {
            //kpr::ScopedLock<kpr::Mutex> lock(m_topicSubscribeInfoTableLock);
            std::map<std::string, std::set<MessageQueue> >::iterator it = m_topicSubscribeInfoTable.find(topic);
            if (it == m_topicSubscribeInfoTable.end())
            {
                if (topic.find(MixAll::RETRY_GROUP_TOPIC_PREFIX) != 0)
                {
                    RMQ_WARN("doRebalance, %s, but the topic[%s] not exist.", m_consumerGroup.c_str(), topic.c_str());
                }
            }

            std::list<std::string> cidAll = m_pMQClientFactory->findConsumerIdList(topic, m_consumerGroup);
            if (cidAll.empty())
            {
                RMQ_WARN("doRebalance, %s:%s, get consumer id list failed.", m_consumerGroup.c_str(), topic.c_str());
            }

            if (it != m_topicSubscribeInfoTable.end() && !cidAll.empty())
            {
                std::vector<MessageQueue> mqAll;
                std::set<MessageQueue> mqSet = it->second;
                std::set<MessageQueue>::iterator its = mqSet.begin();

                for (; its != mqSet.end(); its++)
                {
                    mqAll.push_back(*its);
                }

                cidAll.sort();

                AllocateMessageQueueStrategy* strategy = m_pAllocateMessageQueueStrategy;

                std::vector<MessageQueue>* allocateResult = NULL;
                try
                {
                    allocateResult = strategy->allocate(m_consumerGroup,
                    	m_pMQClientFactory->getClientId(), mqAll, cidAll);
                }
                catch (std::exception& e)
                {
                    RMQ_ERROR("AllocateMessageQueueStrategy.allocate Exception, allocateMessageQueueStrategyName={%s}, mqAll={%s}, cidAll={%s}, %s",
                    	strategy->getName().c_str(), UtilAll::toString(mqAll).c_str(), UtilAll::toString(cidAll).c_str(), e.what());
                    return;
                }

                std::set<MessageQueue> allocateResultSet;
                if (allocateResult != NULL)
                {
                    for (size_t i = 0; i < allocateResult->size(); i++)
                    {
                        allocateResultSet.insert(allocateResult->at(i));
                    }

                    delete allocateResult;
                }

                bool changed = updateProcessQueueTableInRebalance(topic, allocateResultSet);
                if (changed)
                {
                    RMQ_INFO("rebalanced result changed. allocateMessageQueueStrategyName={%s}, group={%s}, topic={%s}, ConsumerId={%s}, "
                             "rebalanceSize={%u}, rebalanceMqSet={%s}, mqAllSize={%u}, cidAllSize={%u}, mqAll={%s}, cidAll={%s}",
                             strategy->getName().c_str(), m_consumerGroup.c_str(), topic.c_str(), m_pMQClientFactory->getClientId().c_str(),
                             (unsigned)allocateResultSet.size(), UtilAll::toString(allocateResultSet).c_str(),
                             (unsigned)mqAll.size(), (unsigned)cidAll.size(), UtilAll::toString(mqAll).c_str(), UtilAll::toString(cidAll).c_str()
                            );

                    messageQueueChanged(topic, mqSet, allocateResultSet);
                }
            }
        }
        break;
        default:
            break;
    }
    RMQ_DEBUG("rebalanceByTopic end");
}


void RebalanceImpl::removeProcessQueue(const MessageQueue& mq)
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
	std::map<MessageQueue, ProcessQueue*>::iterator it = m_processQueueTable.find(mq);
	if (it != m_processQueueTable.end())
	{
		MessageQueue mq = it->first;
        ProcessQueue* pq = it->second;
        bool isDroped = pq->isDropped();

        this->removeUnnecessaryMessageQueue(mq, *pq);
        RMQ_INFO("Fix Offset, {%s}, remove unnecessary mq, {%s} Droped: {%d}",
        	m_consumerGroup.c_str(), mq.toString().c_str(), isDroped);
	}
}


bool RebalanceImpl::updateProcessQueueTableInRebalance(const std::string& topic, std::set<MessageQueue>& mqSet)
{
	RMQ_DEBUG("updateProcessQueueTableInRebalance begin, topic={%s}", topic.c_str());
    bool changed = false;

    {
        kpr::ScopedWLock<kpr::RWMutex> lock(m_processQueueTableLock);
        std::map<MessageQueue, ProcessQueue*>::iterator it = m_processQueueTable.begin();
        for (; it != m_processQueueTable.end();)
        {
        	std::map<MessageQueue, ProcessQueue*>::iterator itCur = it++;
            MessageQueue mq = itCur->first;
            ProcessQueue* pq = itCur->second;
            if (mq.getTopic() == topic)
            {
                std::set<MessageQueue>::iterator itMq = mqSet.find(mq);
                if (itMq == mqSet.end())
                {
                    pq->setDropped(true);
        			if (this->removeUnnecessaryMessageQueue(mq, *pq))
        			{
        				changed = true;
        				m_processQueueTable.erase(itCur);

        				RMQ_WARN("doRebalance, {%s}, remove unnecessary mq, {%s}",
        					m_consumerGroup.c_str(), mq.toString().c_str());
        			}
                }
                else if (pq->isPullExpired())
                {
                	switch(this->consumeType())
                	{
                		case CONSUME_ACTIVELY:
                            break;
                        case CONSUME_PASSIVELY:
                        	pq->setDropped(true);
        					if (this->removeUnnecessaryMessageQueue(mq, *pq))
        					{
        						changed = true;
                            	m_processQueueTable.erase(itCur);

                            	RMQ_ERROR("[BUG]doRebalance, {%s}, remove unnecessary mq, {%s}, because pull is pause, so try to fixed it",
                                    m_consumerGroup.c_str(), mq.toString().c_str());
        					}
                            break;
                        default:
                            break;
                	}
                }
            }
        }
    }

    std::list<PullRequest*> pullRequestList;
    std::set<MessageQueue>::iterator its = mqSet.begin();
    for (; its != mqSet.end(); its++)
    {
        MessageQueue mq = *its;
        bool find = false;
        {
            kpr::ScopedRLock<kpr::RWMutex> lock(m_processQueueTableLock);
            std::map<MessageQueue, ProcessQueue*>::iterator itm = m_processQueueTable.find(mq);
            if (itm != m_processQueueTable.end())
            {
                find = true;
            }
        }

        if (!find)
        {
        	//todo: memleak
            PullRequest* pullRequest = new PullRequest();
            pullRequest->setConsumerGroup(m_consumerGroup);
            pullRequest->setMessageQueue(mq);
            pullRequest->setProcessQueue(new ProcessQueue());//todo: memleak

            long long nextOffset = computePullFromWhere(mq);
            if (nextOffset >= 0)
            {
                pullRequest->setNextOffset(nextOffset);
                pullRequestList.push_back(pullRequest);
                changed = true;

				{
	                kpr::ScopedWLock<kpr::RWMutex> lock(m_processQueueTableLock);
	                m_processQueueTable[mq] = pullRequest->getProcessQueue();
	                RMQ_INFO("doRebalance, {%s}, add a new mq, {%s}, pullRequst: %s",
	                	m_consumerGroup.c_str(), mq.toString().c_str(), pullRequest->toString().c_str());
                }
            }
            else
            {
                RMQ_WARN("doRebalance, {%s}, add new mq failed, {%s}",
                	m_consumerGroup.c_str(), mq.toString().c_str());
            }
        }
    }

    //todo memleak
    dispatchPullRequest(pullRequestList);
    RMQ_DEBUG("updateProcessQueueTableInRebalance end");

    return changed;
}

void RebalanceImpl::truncateMessageQueueNotMyTopic()
{
    std::map<std::string, SubscriptionData> subTable = getSubscriptionInner();

    kpr::ScopedWLock<kpr::RWMutex> lock(m_processQueueTableLock);
    std::map<MessageQueue, ProcessQueue*>::iterator it = m_processQueueTable.begin();
    for (; it != m_processQueueTable.end();)
    {
        MessageQueue mq = it->first;
        std::map<std::string, SubscriptionData>::iterator itt = subTable.find(mq.getTopic());

        if (itt == subTable.end())
        {
            ProcessQueue* pq = it->second;
            if (pq != NULL)
            {
                pq->setDropped(true);
                RMQ_WARN("doRebalance, {%s}, truncateMessageQueueNotMyTopic remove unnecessary mq, {%s}",
                         m_consumerGroup.c_str(), mq.toString().c_str());
            }
            m_processQueueTable.erase(it++);
        }
        else
        {
            it++;
        }
    }
}

}
