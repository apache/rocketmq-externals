/**
* Copyright (C) 2013 suwenkuang ,hooligan_520@qq.com
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

#ifndef __LOCALFILEOFFSETSTORE_H__
#define __LOCALFILEOFFSETSTORE_H__
#include <map>
#include <string>
#include <set>

#include "RocketMQClient.h"
#include "OffsetStore.h"
#include "MessageQueue.h"
#include "AtomicValue.h"
#include "Mutex.h"

namespace rmq
{
    class MQClientFactory;
    class MessageQueue;
    class OffsetSerializeWrapper;

    class LocalFileOffsetStore : public OffsetStore
    {
    public:
        LocalFileOffsetStore(MQClientFactory* pMQClientFactory, const std::string& groupName);

        void load();
        void updateOffset(const MessageQueue& mq, long long offset, bool increaseOnly);
        long long readOffset(const MessageQueue& mq, ReadOffsetType type);
        void persistAll(std::set<MessageQueue>& mqs);
        void persist(const MessageQueue& mq);
        void removeOffset(const MessageQueue& mq) ;
        std::map<MessageQueue, long long> cloneOffsetTable(const std::string& topic);

    private:
        OffsetSerializeWrapper* readLocalOffset();
        OffsetSerializeWrapper* readLocalOffsetBak();

    private:
        MQClientFactory* m_pMQClientFactory;
        std::string m_groupName;
        std::string m_storePath;
        std::map<MessageQueue, kpr::AtomicLong> m_offsetTable;
        kpr::RWMutex m_tableMutex;
    };
}

#endif
