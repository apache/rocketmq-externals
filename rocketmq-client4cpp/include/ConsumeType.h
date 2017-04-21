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
#ifndef __RMQ_CONSUMETYPE_H__
#define __RMQ_CONSUMETYPE_H__

#include "RocketMQClient.h"

namespace rmq
{
	enum ConsumeType
	{
		/**
		* Active comsume
		*/
		CONSUME_ACTIVELY,
		/**
		* Passive comsume
		*/
		CONSUME_PASSIVELY,
	};

	enum ConsumeFromWhere
	{
		CONSUME_FROM_LAST_OFFSET,
		CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST,
		CONSUME_FROM_MIN_OFFSET,
		CONSUME_FROM_MAX_OFFSET,
	    CONSUME_FROM_FIRST_OFFSET,
	    CONSUME_FROM_TIMESTAMP,
	};

	enum MessageModel
	{
		BROADCASTING,
		CLUSTERING,
	};

	const char* getConsumeTypeString(ConsumeType type);
	const char* getConsumeFromWhereString(ConsumeFromWhere type);
	const char* getMessageModelString(MessageModel type);
}

#endif
