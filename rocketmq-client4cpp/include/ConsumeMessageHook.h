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
#ifndef __RMQ_CONSUMEMESSAGEHOOK_H__
#define __RMQ_CONSUMEMESSAGEHOOK_H__

#include <list>

#include "RocketMQClient.h"
#include "MessageQueue.h"

namespace rmq
{
	typedef struct
	{
		std::string consumerGroup;
		std::list<MessageExt*> msgList;
		MessageQueue mq;
		bool success;
		void* arg;
	} ConsumeMessageContext;

	class ConsumeMessageHook
	{
	public:
		virtual ~ConsumeMessageHook() {}
		virtual std::string hookName()=0;
		virtual void consumeMessageBefore(const ConsumeMessageContext& context)=0;
		virtual void consumeMessageAfter(const ConsumeMessageContext& context)=0;
	};
}

#endif
