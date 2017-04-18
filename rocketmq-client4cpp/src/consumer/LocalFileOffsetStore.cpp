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

#include "LocalFileOffsetStore.h"

#include "MQClientFactory.h"
#include "OffsetSerializeWrapper.h"
#include "ScopedLock.h"
#include "FileUtil.h"
#include "MixAll.h"
#include "Exception.h"
#include "MQClientException.h"

namespace rmq
{

LocalFileOffsetStore::LocalFileOffsetStore(MQClientFactory* pMQClientFactory,
        const std::string& groupName)
{
    m_pMQClientFactory = pMQClientFactory;
    m_groupName = groupName;
    std::string homePath = getenv("HOME");
    m_storePath = homePath + "/.rocketmq_offsets/" + m_pMQClientFactory->getClientId()
                  + "/" + m_groupName + "/offsets.json";
}

void  LocalFileOffsetStore::load()
{
    OffsetSerializeWrapperPtr offsetSerializeWrapper = this->readLocalOffset();
    if (offsetSerializeWrapper.ptr() != NULL
        && offsetSerializeWrapper->getOffsetTable().size() > 0)
    {
        kpr::ScopedWLock<kpr::RWMutex> lock(m_tableMutex);
        m_offsetTable = offsetSerializeWrapper->getOffsetTable();
        RMQ_FOR_EACH(m_offsetTable, it)
        {
            const MessageQueue& mq = it->first;
            const kpr::AtomicLong& offset = it->second;
            RMQ_INFO("load consumer's offset, {%s} {%s} {%lld}",
                     m_groupName.c_str(),
                     mq.toString().c_str(),
                     offset.get());
        }
    }
}


void  LocalFileOffsetStore::updateOffset(const MessageQueue& mq, long long offset, bool increaseOnly)
{
    RMQ_DEBUG("updateOffset, MQ:%s, offset:%lld", mq.toString().c_str(), offset);
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

long long  LocalFileOffsetStore::readOffset(const MessageQueue& mq, ReadOffsetType type)
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
                RMQ_WARN("No offset in memory, MQ:%s", mq.toString().c_str());
                return -1;
            }
        }
        case READ_FROM_STORE:
        {
            OffsetSerializeWrapperPtr offsetSerializeWrapper;
            try
            {
                offsetSerializeWrapper = this->readLocalOffset();
            }
            catch (std::exception& e)
            {
                RMQ_WARN("load offset file fail, MQ:%s, exception:%s", mq.toString().c_str(), e.what());
                return -1;
            }

            if (offsetSerializeWrapper.ptr() != NULL)
            {
                std::map<MessageQueue, kpr::AtomicLong>& offsetTable = offsetSerializeWrapper->getOffsetTable();
                typeof(offsetTable.begin()) it = offsetTable.find(mq);
                if (it != offsetTable.end())
                {
                    kpr::ScopedWLock<kpr::RWMutex> lock(m_tableMutex);
                    m_offsetTable[mq] = it->second.get();
                    return it->second.get();
                }
            }
            return -1;
        }
        default:
            break;
    }

    return -1;
}


void  LocalFileOffsetStore::persistAll(std::set<MessageQueue>& mqs)
{
    RMQ_DEBUG("persistAll, mqs.size={%u}, mqs=%s",
              (unsigned)mqs.size(), UtilAll::toString(mqs).c_str());
    if (mqs.empty())
    {
        return;
    }
    RMQ_DEBUG("persistAll, m_offsetTable.size={%u}, m_offsetTable=%s",
              (unsigned)m_offsetTable.size(), UtilAll::toString(m_offsetTable).c_str());

    OffsetSerializeWrapper offsetSerializeWrapper;
    std::map<MessageQueue, kpr::AtomicLong>& offsetTable = offsetSerializeWrapper.getOffsetTable();
    {
        kpr::ScopedRLock<kpr::RWMutex> lock(m_tableMutex);
        RMQ_FOR_EACH(m_offsetTable, it)
        {
            MessageQueue mq = it->first;
            kpr::AtomicLong& offset = it->second;
            if (mqs.find(mq) != mqs.end())
            {
                offsetTable[mq] = offset;
            }
        }
    }

    RMQ_DEBUG("persistAll, offsetTable.size={%u}, offsetTable=%s",
              (unsigned)offsetTable.size(), UtilAll::toString(offsetTable).c_str());

    std::string jsonString;
    offsetSerializeWrapper.encode(jsonString);
    RMQ_DEBUG("persistAll, json=%s", jsonString.c_str());

    if (!jsonString.empty())
    {
        try
        {
            kpr::FileUtil::makeDirRecursive(kpr::FileUtil::extractFilePath(m_storePath));
            MixAll::string2File(m_storePath, jsonString);
        }
        catch (const std::exception& e)
        {
            RMQ_ERROR("persistAll consumer offset Exception, %s, %s", m_storePath.c_str(), e.what());
        }
    }
}

void  LocalFileOffsetStore::persist(const MessageQueue& mq)
{
}

void  LocalFileOffsetStore::removeOffset(const MessageQueue& mq)
{
}


std::map<MessageQueue, long long> LocalFileOffsetStore::cloneOffsetTable(const std::string& topic)
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


OffsetSerializeWrapper* LocalFileOffsetStore::readLocalOffset()
{
    std::string content = MixAll::file2String(m_storePath);
    if (content.length() == 0)
    {
        return this->readLocalOffsetBak();
    }
    else
    {
        OffsetSerializeWrapper* offsetSerializeWrapper = NULL;
        try
        {
            offsetSerializeWrapper = OffsetSerializeWrapper::decode(content.c_str(), content.size());
        }
        catch (const MQException& e)
        {
            RMQ_WARN("readLocalOffset Exception, and try to correct, %s", e.what());
            return this->readLocalOffsetBak();
        }

        return offsetSerializeWrapper;
    }
}


OffsetSerializeWrapper* LocalFileOffsetStore::readLocalOffsetBak()
{
    std::string content = MixAll::file2String(m_storePath + ".bak");
    if (content.length() > 0)
    {
        OffsetSerializeWrapper* offsetSerializeWrapper = NULL;
        try
        {
            offsetSerializeWrapper = OffsetSerializeWrapper::decode(content.c_str(), content.size());
        }
        catch (const MQException& e)
        {
            RMQ_WARN("readLocalOffset Exception, maybe json content invalid, %s", e.what());
        }

        return offsetSerializeWrapper;
    }

    return NULL;
}

}
