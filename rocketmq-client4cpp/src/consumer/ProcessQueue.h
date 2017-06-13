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

#ifndef __PROCESSQUEUE_H__
#define __PROCESSQUEUE_H__

#include <list>
#include <map>
#include "Mutex.h"
#include "AtomicValue.h"

namespace rmq
{
    class MessageExt;
	class DefaultMQPushConsumer;

    class ProcessQueue
    {
    public:
		static const unsigned int s_RebalanceLockMaxLiveTime = 30000;
		static const unsigned int s_RebalanceLockInterval = 20000;
		static const unsigned int s_PullMaxIdleTime = 120000;

    public:
        ProcessQueue();

        bool isLockExpired();
		bool isPullExpired();

		void cleanExpiredMsg(DefaultMQPushConsumer* pPushConsumer);
        bool putMessage(const std::list<MessageExt*>& msgs);

        long long getMaxSpan();
        long long removeMessage(std::list<MessageExt*>& msgs);

		void clear();

        std::map<long long, MessageExt*> getMsgTreeMap();
        kpr::AtomicInteger getMsgCount();
        bool isDropped();
        void setDropped(bool dropped);

		unsigned long long getLastPullTimestamp();
		void setLastPullTimestamp(unsigned long long lastPullTimestamp);

		unsigned long long getLastConsumeTimestamp();
		void setLastConsumeTimestamp(unsigned long long lastConsumeTimestamp);

        /**
        * ========================================================================
        */
		kpr::Mutex& getLockConsume();
        void setLocked(bool locked);
        bool isLocked();
		long long getTryUnlockTimes();
		void incTryUnlockTimes();

        void rollback();
        long long commit();
        void makeMessageToCosumeAgain(const std::list<MessageExt*>& msgs);

        std::list<MessageExt*> takeMessages(int batchSize);

        long long getLastLockTimestamp();
        void setLastLockTimestamp(long long lastLockTimestamp);


    private:
        kpr::RWMutex m_lockTreeMap;
        std::map<long long, MessageExt*> m_msgTreeMap;
        volatile long long m_queueOffsetMax ;
        kpr::AtomicInteger m_msgCount;
        volatile bool m_dropped;
        volatile unsigned long long m_lastPullTimestamp;
		volatile unsigned long long m_lastConsumeTimestamp;

        /**
        * order message
        */
        kpr::Mutex m_lockConsume;
        volatile bool m_locked;
        volatile unsigned long long m_lastLockTimestamp;
        volatile bool m_consuming;
        std::map<long long, MessageExt*> m_msgTreeMapTemp;
        kpr::AtomicInteger m_tryUnlockTimes;
    };
}

#endif
