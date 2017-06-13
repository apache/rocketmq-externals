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

#ifndef __RMQ_MESSAGEQUEUE_H__
#define __RMQ_MESSAGEQUEUE_H__

#include <iostream>
#include <string>
#include <sstream>

#include "RocketMQClient.h"

namespace rmq
{
	/**
	* Message Queue
	*
	*/
	class MessageQueue
	{
	public:
		MessageQueue();
		~MessageQueue(){};

		MessageQueue(const std::string& topic, const std::string& brokerName, int queueId);

		std::string getTopic()const;
		void setTopic(const std::string& topic);

		std::string getBrokerName()const;
		void setBrokerName(const std::string& brokerName);

		int getQueueId()const;
		void setQueueId(int queueId);

		int hashCode();
		std::string toString() const;
		std::string toJsonString() const;

		bool operator==(const MessageQueue& mq) const;
		bool operator<(const MessageQueue& mq) const;
		int compareTo(const MessageQueue& mq) const;

	private:
		std::string m_topic;
		std::string m_brokerName;
		int m_queueId;
	};

	inline std::ostream& operator<<(std::ostream& os, const MessageQueue& obj)
	{
	    os << obj.toString();
	    return os;
	}
}

#endif
