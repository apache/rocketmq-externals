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
#include "MQClientManager.h"
#include "ScopedLock.h"
#include "MQClientFactory.h"
#include "ClientConfig.h"

namespace rmq
{


MQClientManager* MQClientManager::s_instance = new MQClientManager();

MQClientManager::MQClientManager()
{

}

MQClientManager::~MQClientManager()
{

}

MQClientManager* MQClientManager::getInstance()
{
    return s_instance;
}

MQClientFactory* MQClientManager::getAndCreateMQClientFactory(ClientConfig& clientConfig)
{
    std::string clientId = clientConfig.buildMQClientId();
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    std::map<std::string, MQClientFactory*>::iterator it = m_factoryTable.find(clientId);

    if (it != m_factoryTable.end())
    {
        return it->second;
    }
    else
    {
        MQClientFactory* factory = new MQClientFactory(clientConfig, m_factoryIndexGenerator++, clientId);

        m_factoryTable[clientId] = factory;

        return factory;
    }
}

void MQClientManager::removeClientFactory(const std::string&  clientId)
{
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    std::map<std::string, MQClientFactory*>::iterator it = m_factoryTable.find(clientId);

    if (it != m_factoryTable.end())
    {
        //delete it->second;
        m_factoryTable.erase(it);
    }
}

}

