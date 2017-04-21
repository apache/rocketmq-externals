/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License")=0;
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

#ifndef __RMQ_MQPRODUCER_H__
#define __RMQ_MQPRODUCER_H__

#include <vector>
#include <string>

#include "RocketMQClient.h"
#include "MQAdmin.h"
#include "SendResult.h"

namespace rmq
{
	class MessageQueue;
	class SendCallback;
	class LocalTransactionExecuter;
	class MessageQueueSelector;

	/**
	* Producer interface
	*
	*/
	class MQProducer : public MQAdmin
	{
	public:
		MQProducer()
		{
		}

		virtual ~MQProducer()
		{
		}

		virtual void start()=0;
		virtual void shutdown()=0;

		virtual std::vector<MessageQueue>* fetchPublishMessageQueues(const std::string& topic)=0;

		virtual SendResult send(Message& msg)=0;
		virtual void send(Message& msg, SendCallback* sendCallback)=0;
		virtual void sendOneway(Message& msg)=0;

		virtual SendResult send(Message& msg, MessageQueue& mq)=0;
		virtual void send(Message& msg, MessageQueue& mq, SendCallback* sendCallback)=0;
		virtual void sendOneway(Message& msg, MessageQueue& mq)=0;

		virtual SendResult send(Message& msg, MessageQueueSelector* selector, void* arg)=0;
		virtual void send(Message& msg, MessageQueueSelector* selector, void* arg, SendCallback* sendCallback)=0;
		virtual void sendOneway(Message& msg, MessageQueueSelector* selector, void* arg)=0;

		virtual TransactionSendResult sendMessageInTransaction(Message& msg,
																LocalTransactionExecuter* tranExecuter,
																void* arg)=0;
	};
}
#endif
