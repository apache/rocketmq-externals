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
#ifndef __RMQ_MQPUSHCONSUMER_H__
#define __RMQ_MQPUSHCONSUMER_H__

#include <set>
#include <string>

#include "RocketMQClient.h"
#include "MQConsumer.h"
#include "PullResult.h"

namespace rmq
{
	class MessageListener;

	/**
	* Push Consumer
	*
	*/
	class MQPushConsumer : public MQConsumer
	{
	public:
		virtual void registerMessageListener(MessageListener* pMessageListener)=0;


		virtual  void subscribe(const std::string& topic, const std::string& subExpression)=0;
		virtual void unsubscribe(const std::string& topic)=0;


		virtual void updateCorePoolSize(int corePoolSize)=0;
		virtual void suspend()=0;
		virtual void resume()=0;
	};
}
#endif
