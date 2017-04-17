/**
 * Copyright (C) 2010-2013 kangliqiang, kangliq@163.com
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
#include <stdlib.h>
#include <sstream>

#include "MQClientException.h"
#include "SocketUtil.h"
#include "ClientConfig.h"
#include "UtilAll.h"
#include "MixAll.h"

namespace rmq
{

ClientConfig::ClientConfig()
{
    char* addr = getenv(MixAll::NAMESRV_ADDR_ENV.c_str());
    if (addr)
    {
        m_namesrvAddr = addr;
    }
    else
    {
        m_namesrvAddr = "";
    }

    m_clientIP = getLocalAddress();
    m_instanceName = "DEFAULT";
    m_clientCallbackExecutorThreads = UtilAll::availableProcessors();
    m_pollNameServerInterval = 1000 * 30;
    m_heartbeatBrokerInterval = 1000 * 30;
    m_persistConsumerOffsetInterval = 1000 * 5;
}

ClientConfig::~ClientConfig()
{
}

std::string ClientConfig::buildMQClientId()
{
    return m_clientIP + "@" + m_instanceName;
}

void ClientConfig::changeInstanceNameToPID()
{
    if (m_instanceName == "DEFAULT")
    {
        m_instanceName = UtilAll::toString(UtilAll::getPid());
    }
}


void ClientConfig::resetClientConfig(const ClientConfig& cc)
{
    m_namesrvAddr = cc.m_namesrvAddr;
    m_clientIP = cc.m_clientIP;
    m_instanceName = cc.m_instanceName;
    m_clientCallbackExecutorThreads = cc.m_clientCallbackExecutorThreads;
    m_pollNameServerInterval = cc.m_pollNameServerInterval;
    m_heartbeatBrokerInterval = cc.m_heartbeatBrokerInterval;
    m_persistConsumerOffsetInterval = cc.m_persistConsumerOffsetInterval;
}

ClientConfig ClientConfig::cloneClientConfig()
{
    return *this;
}

std::string ClientConfig::getNamesrvAddr()
{
    return m_namesrvAddr;
}

void ClientConfig::setNamesrvAddr(const std::string& namesrvAddr)
{
    m_namesrvAddr = namesrvAddr;
}

std::string ClientConfig::getClientIP()
{
    return m_clientIP;
}

void ClientConfig::setClientIP(const std::string& clientIP)
{
    m_clientIP = clientIP;
}

std::string ClientConfig::getInstanceName()
{
    return m_instanceName;
}

void ClientConfig::setInstanceName(const std::string& instanceName)
{
    m_instanceName = instanceName;
}

int ClientConfig::getClientCallbackExecutorThreads()
{
    return m_clientCallbackExecutorThreads;
}

void ClientConfig::setClientCallbackExecutorThreads(int clientCallbackExecutorThreads)
{
    m_clientCallbackExecutorThreads = clientCallbackExecutorThreads;
}

int ClientConfig::getPollNameServerInterval()
{
    return m_pollNameServerInterval;
}

void ClientConfig::setPollNameServerInterval(int pollNameServerInterval)
{
    m_pollNameServerInterval = pollNameServerInterval;
}

int ClientConfig::getHeartbeatBrokerInterval()
{
    return m_heartbeatBrokerInterval;
}

void ClientConfig::setHeartbeatBrokerInterval(int heartbeatBrokerInterval)
{
    m_heartbeatBrokerInterval = heartbeatBrokerInterval;
}

int ClientConfig:: getPersistConsumerOffsetInterval()
{
    return m_persistConsumerOffsetInterval;
}

void ClientConfig::setPersistConsumerOffsetInterval(int persistConsumerOffsetInterval)
{
    m_persistConsumerOffsetInterval = persistConsumerOffsetInterval;
}


std::string ClientConfig::toString() const
{
	std::stringstream ss;
	ss 	<< "{namesrvAddr=" << m_namesrvAddr
		<< ",clientIP=" << m_clientIP
		<< ",instanceName=" << m_instanceName
		<< ",clientCallbackExecutorThreads=" << m_clientCallbackExecutorThreads
		<< ",pollNameServerInteval=" << m_pollNameServerInterval
		<< ",heartbeatBrokerInterval=" << m_heartbeatBrokerInterval
		<< ",persistConsumerOffsetInterval=" << m_persistConsumerOffsetInterval
		<<"}";
	return ss.str();
}


}
