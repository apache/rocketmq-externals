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

#ifndef __RMQ_MQCONSUMER_H__
#define __RMQ_MQCONSUMER_H__

#include <set>
#include <string>

#include "RocketMQClient.h"
#include "MQAdmin.h"
#include "ConsumeType.h"


namespace rmq
{
	class MessageExt;

	/**
	* Consumer interface
	*
	*/
	class MQConsumer : public MQAdmin
	{
	public:
		virtual ~MQConsumer(){}

		virtual void  start()=0;
		virtual void shutdown()=0;

		virtual void sendMessageBack(MessageExt& msg, int delayLevel)=0;
		virtual std::set<MessageQueue>* fetchSubscribeMessageQueues(const std::string& topic)=0;
	};
}
#endif
