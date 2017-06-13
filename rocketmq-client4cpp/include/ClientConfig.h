/**
 * Copyright (C) 2010-2013 kangliqiang <kangliq@163.com>
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
#ifndef __RMQ_CLIENTCONFIG_H__
#define __RMQ_CLIENTCONFIG_H__

#include <string>
#include "RocketMQClient.h"

namespace rmq
{
	/**
	 * Producer and Consumer common configuration
	 *
	 */
	class ClientConfig
	{
	public:
		ClientConfig();
		virtual ~ClientConfig();

		std::string buildMQClientId();
		void changeInstanceNameToPID();

		void resetClientConfig(const ClientConfig& cc);
		ClientConfig cloneClientConfig();

		std::string getNamesrvAddr();
		void setNamesrvAddr(const std::string& namesrvAddr);

		std::string getClientIP();
		void setClientIP(const std::string& clientIP);

		std::string getInstanceName();
		void setInstanceName(const std::string& instanceName);

		int getClientCallbackExecutorThreads();
		void setClientCallbackExecutorThreads(int clientCallbackExecutorThreads);

		int getPollNameServerInterval();

		void setPollNameServerInterval(int pollNameServerInterval);

		int getHeartbeatBrokerInterval();
		void setHeartbeatBrokerInterval(int heartbeatBrokerInterval);

		int getPersistConsumerOffsetInterval();
		void setPersistConsumerOffsetInterval(int persistConsumerOffsetInterval);

		std::string toString() const;

	private:
		int m_clientCallbackExecutorThreads;
		int m_pollNameServerInterval;
		int m_heartbeatBrokerInterval;
		int m_persistConsumerOffsetInterval;
		std::string m_clientIP;
		std::string m_instanceName;
		std::string m_namesrvAddr;
	};
}

#endif
