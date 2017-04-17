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

#ifndef __RMQ_MESSAGELISTENER_H__
#define __RMQ_MESSAGELISTENER_H__

#include <limits.h>
#include <list>

#include "MessageExt.h"
#include "MessageQueue.h"

namespace rmq
{
	/**
	* Message Listener
	*
	*/
	class MessageListener
	{
	public:
		virtual ~MessageListener(){}
	};

	enum ConsumeOrderlyStatus
	{
		SUCCESS,
		ROLLBACK,
		COMMIT,
		SUSPEND_CURRENT_QUEUE_A_MOMENT,
	};

	typedef struct tagConsumeOrderlyContext
	{
		tagConsumeOrderlyContext(MessageQueue& mq)
			:messageQueue(mq),
			autoCommit(true),
			suspendCurrentQueueTimeMillis(1000)
		{
		}

		MessageQueue messageQueue;///< 要消费的消息属于哪个队列
		bool autoCommit;///< 消息Offset是否自动提交
		long suspendCurrentQueueTimeMillis;
	}ConsumeOrderlyContext;

	class MessageListenerOrderly : public MessageListener
	{
	public:
		virtual ConsumeOrderlyStatus consumeMessage(std::list<MessageExt*>& msgs,
													ConsumeOrderlyContext& context)=0;
	};

	enum ConsumeConcurrentlyStatus
	{
		CONSUME_SUCCESS,
		RECONSUME_LATER,
	};

	struct ConsumeConcurrentlyContext
	{
		ConsumeConcurrentlyContext(MessageQueue& mq)
			:messageQueue(mq),
			delayLevelWhenNextConsume(0),
			ackIndex(INT_MAX)
		{
		}
		MessageQueue messageQueue;
		int delayLevelWhenNextConsume;
		int ackIndex;
	};

	class MessageListenerConcurrently : public MessageListener
	{
	public:
		virtual ConsumeConcurrentlyStatus consumeMessage(std::list<MessageExt*>& msgs,
														ConsumeConcurrentlyContext& context)=0;
	};
}

#endif
