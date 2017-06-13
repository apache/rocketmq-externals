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
#ifndef __RMQ_PULLRESULT_H__
#define __RMQ_PULLRESULT_H__

#include <list>
#include <string>
#include <sstream>

#include "RocketMQClient.h"
#include "MessageExt.h"

namespace rmq
{
	enum PullStatus
	{
		FOUND,
		NO_NEW_MSG,
		NO_MATCHED_MSG,
		OFFSET_ILLEGAL
	};

	/**
	* PullResult
	*
	*/
	struct PullResult
	{
		PullResult()
		{

		}

		PullResult(PullStatus pullStatus,
				   long long nextBeginOffset,
				   long long minOffset,
				   long long maxOffset,
				   std::list<MessageExt*>& msgFoundList)
			:pullStatus(pullStatus),
			 nextBeginOffset(nextBeginOffset),
			 minOffset(minOffset),
			 maxOffset(maxOffset),
			 msgFoundList(msgFoundList)
		{

		}

		~PullResult()
		{
			std::list<MessageExt*>::iterator it = msgFoundList.begin();

			for (;it!=msgFoundList.end();it++)
			{
				delete *it;
			}
		}

		std::string toString() const
		{
			std::stringstream ss;
			ss 	<< "{pullStatus=" << pullStatus
				<< ",nextBeginOffset=" << nextBeginOffset
				<< ",minOffset=" << nextBeginOffset
				<< ",maxOffset=" << nextBeginOffset
				<< ",msgFoundList.size=" << msgFoundList.size()
				<<"}";
			return ss.str();
		}

		PullStatus pullStatus;
		long long nextBeginOffset;
		long long minOffset;
		long long maxOffset;
		std::list<MessageExt*> msgFoundList;
	};
}

#endif
