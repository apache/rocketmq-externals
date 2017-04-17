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
#ifndef __REMOTEBROKEROFFSETSTORE_H__
#define __REMOTEBROKEROFFSETSTORE_H__

#include "OffsetStore.h"
#include <map>
#include <string>
#include <set>
#include "MessageQueue.h"
#include "AtomicValue.h"
#include "Mutex.h"

namespace rmq
{
    class MQClientFactory;

    /**
    * offset remote store
    *
    */
    class RemoteBrokerOffsetStore : public OffsetStore
    {
    public:
        RemoteBrokerOffsetStore(MQClientFactory* pMQClientFactory, const std::string& groupName) ;

        void load();
        void updateOffset(const MessageQueue& mq, long long offset, bool increaseOnly);
        long long readOffset(const MessageQueue& mq, ReadOffsetType type);
        void persistAll(std::set<MessageQueue>& mqs);
        void persist(const MessageQueue& mq);
        void removeOffset(const MessageQueue& mq) ;
        std::map<MessageQueue, long long> cloneOffsetTable(const std::string& topic);

    private:
        void updateConsumeOffsetToBroker(const MessageQueue& mq, long long offset);
        long long fetchConsumeOffsetFromBroker(const MessageQueue& mq);

    private:
        MQClientFactory* m_pMQClientFactory;
        std::string m_groupName;
        kpr::AtomicInteger m_storeTimesTotal;
        std::map<MessageQueue, kpr::AtomicLong> m_offsetTable;
        kpr::RWMutex m_tableMutex;
    };
}

#endif
