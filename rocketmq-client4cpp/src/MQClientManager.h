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

#ifndef __MQCLIENTMANAGER_H__
#define __MQCLIENTMANAGER_H__

#include <string>
#include <map>
#include "Mutex.h"
#include "AtomicValue.h"

namespace rmq
{
	class MQClientFactory;
	class ClientConfig;

	class MQClientManager
	{
	public:
	    ~MQClientManager();
	    static MQClientManager* getInstance();
	    MQClientFactory* getAndCreateMQClientFactory(ClientConfig& clientConfig);
	    void removeClientFactory(const std::string&  clientId);

	private:
	    MQClientManager();

	private:
	    static MQClientManager* s_instance;
	    kpr::AtomicInteger m_factoryIndexGenerator;
	    std::map<std::string, MQClientFactory*> m_factoryTable;
	    kpr::Mutex m_mutex;
	};
}

#endif
