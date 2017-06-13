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
#ifndef __RMQ_MQPULLCONSUMER_H__
#define __RMQ_MQPULLCONSUMER_H__

#include <set>
#include <string>

#include "RocketMQClient.h"
#include "MQConsumer.h"
#include "PullResult.h"

namespace rmq
{
	class MessageQueueListener;
	class MessageQueue;
	class PullCallback;

	/**
	* Pull Consumer
	*
	*/
	class MQPullConsumer : public MQConsumer
	{
	public:
		virtual ~MQPullConsumer(){}
		virtual void registerMessageQueueListener(const std::string& topic, MessageQueueListener* pListener)=0;

		virtual PullResult* pull(MessageQueue& mq, const std::string& subExpression, long long offset,int maxNums)=0;
		virtual void pull(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums, PullCallback* pPullCallback)=0;

		virtual PullResult* pullBlockIfNotFound(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums)=0;
		virtual void pullBlockIfNotFound(MessageQueue& mq, const std::string& subExpression, long long offset, int maxNums, PullCallback* pPullCallback)=0;

		virtual void updateConsumeOffset(MessageQueue& mq, long long offset)=0;
		virtual long long fetchConsumeOffset(MessageQueue& mq, bool fromStore)=0;

		virtual std::set<MessageQueue>* fetchMessageQueuesInBalance(const std::string& topic)=0;
	};
}
#endif
