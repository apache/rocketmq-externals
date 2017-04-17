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

#ifndef __ConsumerRunningInfo_H__
#define __ConsumerRunningInfo_H__

#include <string>
#include <set>
#include <map>

#include "RemotingSerializable.h"
#include "MessageQueue.h"
#include "SubscriptionData.h"
#include "ConsumerStatManage.h"

namespace rmq
{
    class ConsumerRunningInfo : public RemotingSerializable
    {
    public:
        ConsumerRunningInfo();
        ~ConsumerRunningInfo();

		/*
		std::map<std::string, std::string>& getProperties()
		{
			return m_properties;
		}
        void setProperties(const std::map<std::string, std::string>& properties)
		{
			m_properties = properties;
		}

        std::map<MessageQueue, ProcessQueueInfo>& getMqTable()
		{
			return m_mqTable;
		}
        void setMqTable(const std::map<MessageQueue, ProcessQueueInfo>& mqTable)
		{
			m_mqTable = mqTable;
		}

        std::map<std::string, ConsumeStatus>& getStatusTable()
		{
			return m_statusTable;
		}
        void setStatusTable(const std::map<std::string, ConsumeStatus>& statusTable)
		{
		    m_statusTable = statusTable;
		}

        std::set<SubscriptionData>& getSubscriptionSet()
		{
			return m_subscriptionSet;
		}
        void setSubscriptionSet(const std::set<SubscriptionData>& subscriptionSet)
		{
			m_subscriptionSet = subscriptionSet;
		}
		*/

		void encode(std::string& outData);
		std::string formatString();

	public:
		static const std::string PROP_NAMESERVER_ADDR;
		static const std::string PROP_THREADPOOL_CORE_SIZE;
		static const std::string PROP_CONSUME_ORDERLY;
		static const std::string PROP_CONSUME_TYPE;
		static const std::string PROP_CLIENT_VERSION;
		static const std::string PROP_CONSUMER_START_TIMESTAMP;

    private:
		/*
		std::map<std::string, std::string> m_properties;
		std::set<SubscriptionData> m_subscriptionSet;
		std::map<MessageQueue, ProcessQueueInfo> m_mqTable;
		std::map<string, ConsumerStat> m_statusTable;
		std::string m_jstack;
		*/
    };
}

#endif
