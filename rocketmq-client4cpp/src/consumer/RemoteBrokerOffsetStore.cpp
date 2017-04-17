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

#include "RemoteBrokerOffsetStore.h"
#include "MQClientFactory.h"
#include "ScopedLock.h"
#include "MQClientException.h"
#include "CommandCustomHeader.h"
#include "MQClientAPIImpl.h"

namespace rmq
{

RemoteBrokerOffsetStore::RemoteBrokerOffsetStore(MQClientFactory* pMQClientFactory, const std::string& groupName)
{
    m_pMQClientFactory = pMQClientFactory;
    m_groupName = groupName;
}

void RemoteBrokerOffsetStore::load()
{

}

void RemoteBrokerOffsetStore::updateOffset(const MessageQueue& mq, long long offset, bool increaseOnly)
{
    kpr::ScopedWLock<kpr::RWMutex> lock(m_tableMutex);
    typeof(m_offsetTable.begin()) it = m_offsetTable.find(mq);
    if (it == m_offsetTable.end())
    {
        m_offsetTable[mq] = offset;
        it = m_offsetTable.find(mq);
    }

    kpr::AtomicLong& offsetOld = it->second;
    if (increaseOnly)
    {
        MixAll::compareAndIncreaseOnly(offsetOld, offset);
    }
    else
    {
        offsetOld.set(offset);
    }
}

long long RemoteBrokerOffsetStore::readOffset(const MessageQueue& mq, ReadOffsetType type)
{
    RMQ_DEBUG("readOffset, MQ:%s, type:%d", mq.toString().c_str(), type);
    switch (type)
    {
        case MEMORY_FIRST_THEN_STORE:
        case READ_FROM_MEMORY:
        {
            kpr::ScopedRLock<kpr::RWMutex> lock(m_tableMutex);
            typeof(m_offsetTable.begin()) it = m_offsetTable.find(mq);
            if (it != m_offsetTable.end())
            {
                return it->second.get();
            }
            else if (READ_FROM_MEMORY == type)
            {
                RMQ_DEBUG("No offset in memory, MQ:%s", mq.toString().c_str());
                return -1;
            }
        }
        case READ_FROM_STORE:
        {
            try
            {
                long long brokerOffset = this->fetchConsumeOffsetFromBroker(mq);
                RMQ_DEBUG("fetchConsumeOffsetFromBroker, MQ:%s, brokerOffset:%lld",
                          mq.toString().c_str(), brokerOffset);
                if (brokerOffset >= 0)
                {
                    this->updateOffset(mq, brokerOffset, false);
                }
                return brokerOffset;
            }
            // No offset in broker
            catch (const MQBrokerException& e)
            {
                RMQ_WARN("No offset in broker, MQ:%s, exception:%s", mq.toString().c_str(), e.what());
                return -1;
            }
            catch (const std::exception& e)
            {
                RMQ_ERROR("fetchConsumeOffsetFromBroker exception, MQ:%s, msg:%s",
                          mq.toString().c_str(), e.what());
                return -2;
            }
            catch (...)
            {
                RMQ_ERROR("fetchConsumeOffsetFromBroker unknow exception, MQ:%s",
                          mq.toString().c_str());
                return -2;
            }
        }
        default:
            break;
    }

    return -1;
}

void RemoteBrokerOffsetStore::persistAll(std::set<MessageQueue>& mqs)
{
    if (mqs.empty())
    {
        return;
    }

    std::set<MessageQueue> unusedMQ;
    long long times = m_storeTimesTotal.fetchAndAdd(1);

    kpr::ScopedRLock<kpr::RWMutex> lock(m_tableMutex);
    for (typeof(m_offsetTable.begin()) it = m_offsetTable.begin();
         it != m_offsetTable.end(); it++)
    {
        MessageQueue mq = it->first;
        kpr::AtomicLong& offset = it->second;
        if (mqs.find(mq) != mqs.end())
        {
            try
            {
                this->updateConsumeOffsetToBroker(mq, offset.get());
                if ((times % 12) == 0)
                {
                    RMQ_INFO("updateConsumeOffsetToBroker, Group: {%s} ClientId: {%s}  mq:{%s} offset {%llu}",
                             m_groupName.c_str(),
                             m_pMQClientFactory->getClientId().c_str(),
                             mq.toString().c_str(),
                             offset.get());
                }
            }
            catch (...)
            {
                RMQ_ERROR("updateConsumeOffsetToBroker exception, mq=%s", mq.toString().c_str());
            }
        }
        else
        {
            unusedMQ.insert(mq);
        }
    }

    if (!unusedMQ.empty())
    {
        for (typeof(unusedMQ.begin()) it = unusedMQ.begin(); it != unusedMQ.end(); it++)
        {
            m_offsetTable.erase(*it);
            RMQ_INFO("remove unused mq, %s, %s", it->toString().c_str(), m_groupName.c_str());
        }
    }
}

void RemoteBrokerOffsetStore::persist(const MessageQueue& mq)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_tableMutex);
    typeof(m_offsetTable.begin()) it = m_offsetTable.find(mq);
    if (it != m_offsetTable.end())
    {
        try
        {
            this->updateConsumeOffsetToBroker(mq, it->second.get());
            RMQ_DEBUG("updateConsumeOffsetToBroker ok, mq=%s, offset=%lld", mq.toString().c_str(), it->second.get());
        }
        catch (...)
        {
            RMQ_ERROR("updateConsumeOffsetToBroker exception, mq=%s", mq.toString().c_str());
        }
    }
}

void RemoteBrokerOffsetStore::removeOffset(const MessageQueue& mq)
{
    kpr::ScopedWLock<kpr::RWMutex> lock(m_tableMutex);
    m_offsetTable.erase(mq);
    RMQ_INFO("remove unnecessary messageQueue offset. mq=%s, offsetTableSize=%u",
             mq.toString().c_str(), (unsigned)m_offsetTable.size());
}


std::map<MessageQueue, long long> RemoteBrokerOffsetStore::cloneOffsetTable(const std::string& topic)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_tableMutex);
    std::map<MessageQueue, long long> cloneOffsetTable;
    RMQ_FOR_EACH(m_offsetTable, it)
    {
        MessageQueue mq = it->first;
        kpr::AtomicLong& offset = it->second;
        if (topic == mq.getTopic())
        {
            cloneOffsetTable[mq] = offset.get();
        }
    }

    return cloneOffsetTable;
}


void RemoteBrokerOffsetStore::updateConsumeOffsetToBroker(const MessageQueue& mq, long long offset)
{
    FindBrokerResult findBrokerResult = m_pMQClientFactory->findBrokerAddressInAdmin(mq.getBrokerName());
    if (findBrokerResult.brokerAddr.empty())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        findBrokerResult = m_pMQClientFactory->findBrokerAddressInAdmin(mq.getBrokerName());
    }

    if (!findBrokerResult.brokerAddr.empty())
    {
        UpdateConsumerOffsetRequestHeader* requestHeader = new UpdateConsumerOffsetRequestHeader();
        requestHeader->topic = mq.getTopic();
        requestHeader->consumerGroup = this->m_groupName;
        requestHeader->queueId = mq.getQueueId();
        requestHeader->commitOffset = offset;

        m_pMQClientFactory->getMQClientAPIImpl()->updateConsumerOffsetOneway(
            findBrokerResult.brokerAddr, requestHeader, 1000 * 5);
    }
    else
    {
        THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
    }
}

long long RemoteBrokerOffsetStore::fetchConsumeOffsetFromBroker(const MessageQueue& mq)
{
    FindBrokerResult findBrokerResult = m_pMQClientFactory->findBrokerAddressInAdmin(mq.getBrokerName());
    if (findBrokerResult.brokerAddr.empty())
    {
        // TODO Here may be heavily overhead for Name Server,need tuning
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        findBrokerResult = m_pMQClientFactory->findBrokerAddressInAdmin(mq.getBrokerName());
    }

    if (!findBrokerResult.brokerAddr.empty())
    {
        QueryConsumerOffsetRequestHeader* requestHeader = new QueryConsumerOffsetRequestHeader();
        requestHeader->topic = mq.getTopic();
        requestHeader->consumerGroup = this->m_groupName;
        requestHeader->queueId = mq.getQueueId();

        return m_pMQClientFactory->getMQClientAPIImpl()->queryConsumerOffset(
                   findBrokerResult.brokerAddr, requestHeader, 1000 * 5);
    }
    else
    {
        THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
    }
}

}
