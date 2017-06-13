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
#ifndef __RMQ_OFFSETSTORE_H__
#define __RMQ_OFFSETSTORE_H__

#include <set>
#include <map>

#include "RocketMQClient.h"

namespace rmq
{
	class MessageQueue;

	enum ReadOffsetType
	{
		READ_FROM_MEMORY,
		READ_FROM_STORE,
		MEMORY_FIRST_THEN_STORE,
	};

	/**
	* Consumer Offset Store
	*
	*/
	class OffsetStore
	{
	public:
		virtual ~OffsetStore() {}

		virtual void load()=0;

		virtual void updateOffset(const MessageQueue& mq, long long offset, bool increaseOnly)=0;
		virtual long long readOffset(const MessageQueue& mq, ReadOffsetType type)=0;

		virtual void persistAll(std::set<MessageQueue>& mqs)=0;
		virtual void persist(const MessageQueue& mq)=0;

		virtual void removeOffset(const MessageQueue& mq)=0;

		virtual std::map<MessageQueue, long long> cloneOffsetTable(const std::string& topic) = 0;
	};
}

#endif
