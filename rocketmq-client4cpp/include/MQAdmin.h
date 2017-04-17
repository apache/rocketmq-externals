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

#ifndef __RMQ_MQADMIN_H__
#define __RMQ_MQADMIN_H__

#include <string>

#include "RocketMQClient.h"
#include "MessageExt.h"

namespace rmq
{
	class MQClientException;
	class RemotingException;
	class MQBrokerException;
	class InterruptedException;
	class MessageQueue;
	class QueryResult;

	/**
	* MQ Admin
	*
	*/
	class MQAdmin
	{
	public:
		MQAdmin()
		{
		}

		virtual ~MQAdmin()
		{
		}

		virtual void createTopic(const std::string& key, const std::string& newTopic, int queueNum)=0;

		virtual long long searchOffset(const MessageQueue& mq, long long timestamp)=0;
		virtual long long maxOffset(const MessageQueue& mq)=0;
		virtual long long minOffset(const MessageQueue& mq)=0;

		virtual long long earliestMsgStoreTime(const MessageQueue& mq)=0;

		virtual MessageExt* viewMessage(const std::string& msgId)=0;
		virtual QueryResult queryMessage(const std::string& topic,
										 const std::string&  key,
										 int maxNum,
										 long long begin,
										 long long end)=0;
	};
}

#endif
