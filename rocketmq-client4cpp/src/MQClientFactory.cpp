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

#include <math.h>
#include <set>
#include <string>
#include <iostream>
#include <vector>

#include "MQClientFactory.h"
#include "RemoteClientConfig.h"
#include "ClientRemotingProcessor.h"
#include "MQClientAPIImpl.h"
#include "MQAdminImpl.h"
#include "DefaultMQProducer.h"
#include "PullMessageService.h"
#include "RebalanceService.h"
#include "ScopedLock.h"
#include "KPRUtil.h"
#include "DefaultMQProducerImpl.h"
#include "DefaultMQPushConsumerImpl.h"
#include "MQClientException.h"
#include "MQConsumerInner.h"
#include "MQProducerInner.h"
#include "UtilAll.h"
#include "PermName.h"
#include "MQClientManager.h"
#include "ConsumerStatManage.h"
#include "TopicPublishInfo.h"
#include "MQVersion.h"

namespace rmq
{


long MQClientFactory::LockTimeoutMillis = 3000;

MQClientFactory::MQClientFactory(ClientConfig& clientConfig, int factoryIndex, const std::string& clientId)
{
    m_clientConfig = clientConfig;
    m_factoryIndex = factoryIndex;
    m_pRemoteClientConfig = new RemoteClientConfig();
    m_pRemoteClientConfig->clientCallbackExecutorThreads = clientConfig.getClientCallbackExecutorThreads();
    m_pClientRemotingProcessor = new ClientRemotingProcessor(this);
    m_pMQClientAPIImpl = new MQClientAPIImpl(m_clientConfig, *m_pRemoteClientConfig, m_pClientRemotingProcessor);

    if (!m_clientConfig.getNamesrvAddr().empty())
    {
        m_pMQClientAPIImpl->updateNameServerAddressList(m_clientConfig.getNamesrvAddr());
        RMQ_INFO("user specified name server address: {%s}", m_clientConfig.getNamesrvAddr().c_str());
    }

    m_clientId = clientId;

    m_pMQAdminImpl = new MQAdminImpl(this);
    m_pPullMessageService = new PullMessageService(this);
    m_pRebalanceService = new RebalanceService(this);
    m_pDefaultMQProducer = new DefaultMQProducer(MixAll::CLIENT_INNER_PRODUCER_GROUP);
    m_pDefaultMQProducer->resetClientConfig(clientConfig);
    m_bootTimestamp = KPRUtil::GetCurrentTimeMillis();

    m_pFetchNameServerAddrTask = new ScheduledTask(this, &MQClientFactory::fetchNameServerAddr);
    m_pUpdateTopicRouteInfoFromNameServerTask = new ScheduledTask(this, &MQClientFactory::updateTopicRouteInfoFromNameServerTask);
    m_pCleanBrokerTask = new ScheduledTask(this, &MQClientFactory::cleanBroker);
    m_pPersistAllConsumerOffsetTask = new ScheduledTask(this, &MQClientFactory::persistAllConsumerOffsetTask);
    m_pRecordSnapshotPeriodicallyTask = new ScheduledTask(this, &MQClientFactory::recordSnapshotPeriodicallyTask);
    m_pLogStatsPeriodicallyTask = new ScheduledTask(this, &MQClientFactory::logStatsPeriodicallyTask);

    m_serviceState = CREATE_JUST;

    RMQ_INFO("created a new client Instance, FactoryIndex: {%d} ClinetID: {%s} Config: {%s} Version: {%s}",
                m_factoryIndex,
                m_clientId.c_str(),
                m_clientConfig.toString().c_str(),
                MQVersion::getVersionDesc(MQVersion::s_CurrentVersion));
}

MQClientFactory::~MQClientFactory()
{
    delete m_pRemoteClientConfig;
    delete m_pClientRemotingProcessor;
    delete m_pMQClientAPIImpl;
    delete m_pMQAdminImpl;
    delete m_pPullMessageService;
    delete m_pRebalanceService;
    delete m_pDefaultMQProducer;
}

void MQClientFactory::start()
{
    RMQ_DEBUG("MQClientFactory::start()");
    kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
    switch (m_serviceState)
    {
        case CREATE_JUST:
            makesureInstanceNameIsOnly(m_clientConfig.getInstanceName());

            m_serviceState = START_FAILED;
            if (m_clientConfig.getNamesrvAddr().empty())
            {
                m_clientConfig.setNamesrvAddr(m_pMQClientAPIImpl->fetchNameServerAddr());
            }

            m_pMQClientAPIImpl->start();
            m_timerTaskManager.Init(5, 1000);
            startScheduledTask();
            m_pPullMessageService->Start();
            m_pRebalanceService->Start();
            m_pDefaultMQProducer->getDefaultMQProducerImpl()->start(false);

			RMQ_INFO("the client factory [%s] start OK", m_clientId.c_str());
            m_serviceState = RUNNING;
            break;
        case RUNNING:
            RMQ_WARN("MQClientFactory is already running.");
            break;
        case SHUTDOWN_ALREADY:
            RMQ_ERROR("MQClientFactory should have already been shutted down");
            break;
        case START_FAILED:
            RMQ_ERROR("MQClientFactory started failed.");
            THROW_MQEXCEPTION(MQClientException, "The Factory object start failed", -1);
        default:
            break;
    }
}


void MQClientFactory::shutdown()
{
    RMQ_DEBUG("MQClientFactory::shutdown()");
    // Consumer
    if (!m_consumerTable.empty())
    {
        return;
    }

    // AdminExt
    if (!m_adminExtTable.empty())
    {
        return;
    }

    // Producer
    if (m_producerTable.size() > 1)
    {
        return;
    }

    RMQ_DEBUG("MQClientFactory::shutdown_begin");
    {
        kpr::ScopedLock<kpr::Mutex> lock(m_mutex);
        switch (m_serviceState)
        {
            case CREATE_JUST:
                break;
            case RUNNING:
                m_pDefaultMQProducer->getDefaultMQProducerImpl()->shutdown(false);

                for (int i = 0; i < 6; i++)
                {
                    m_timerTaskManager.UnRegisterTimer(m_scheduledTaskIds[i]);
                }

                m_timerTaskManager.Stop();

                m_pPullMessageService->stop();
                m_pPullMessageService->Join();

                m_pMQClientAPIImpl->shutdown();
                m_pRebalanceService->stop();
                m_pRebalanceService->Join();

                //closesocket(m_datagramSocket);

                MQClientManager::getInstance()->removeClientFactory(m_clientId);
                m_serviceState = SHUTDOWN_ALREADY;
                break;
            case SHUTDOWN_ALREADY:
                break;
            default:
                break;
        }
    }
}


void MQClientFactory::sendHeartbeatToAllBrokerWithLock()
{
	RMQ_DEBUG("TryLock m_lockHeartbeat: %p", &m_lockHeartbeat);
    if (m_lockHeartbeat.TryLock())
    {
        try
        {
            RMQ_DEBUG("TryLock m_lockHeartbeat ok");
            sendHeartbeatToAllBroker();
        }
        catch (...)
        {
            RMQ_ERROR("sendHeartbeatToAllBroker exception");
        }
        m_lockHeartbeat.Unlock();
    }
    else
    {
        RMQ_WARN("TryLock heartBeat fail");
    }
}

void MQClientFactory::updateTopicRouteInfoFromNameServer()
{
    std::set<std::string> topicList;

    // Consumer
    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
        std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
        for (; it != m_consumerTable.end(); it++)
        {
            MQConsumerInner* inner = it->second;
            std::set<SubscriptionData> subList = inner->subscriptions();
            std::set<SubscriptionData>::iterator it1 = subList.begin();
            for (; it1 != subList.end(); it1++)
            {
                topicList.insert((*it1).getTopic());
            }
        }
    }

    // Producer
    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_producerTableLock);
        std::map<std::string, MQProducerInner*>::iterator it = m_producerTable.begin();
        for (; it != m_producerTable.end(); it++)
        {
            MQProducerInner* inner = it->second;
            std::set<std::string> pubList = inner->getPublishTopicList();
            topicList.insert(pubList.begin(), pubList.end());
        }
    }

    std::set<std::string>::iterator it2 = topicList.begin();
    for (; it2 != topicList.end(); it2++)
    {
        updateTopicRouteInfoFromNameServer(*it2);
    }
}

bool MQClientFactory::updateTopicRouteInfoFromNameServer(const std::string& topic)
{
    return updateTopicRouteInfoFromNameServer(topic, false, NULL);
}

bool MQClientFactory::updateTopicRouteInfoFromNameServer(const std::string& topic,
        bool isDefault,
        DefaultMQProducer* pDefaultMQProducer)
{
    RMQ_DEBUG("TryLock m_lockNamesrv: 0x%p, topic: [%s]", &m_lockNamesrv, topic.c_str());
    if (m_lockNamesrv.TryLock(MQClientFactory::LockTimeoutMillis))
    {
        RMQ_DEBUG("TryLock m_lockNamesrv ok");
        TopicRouteDataPtr topicRouteData = NULL;
        try
        {
            if (isDefault && pDefaultMQProducer != NULL)
            {
                topicRouteData =
                    m_pMQClientAPIImpl->getDefaultTopicRouteInfoFromNameServer(
                        pDefaultMQProducer->getCreateTopicKey(), 1000 * 3);
                if (topicRouteData.ptr() != NULL)
                {
                    std::list<QueueData> dataList = topicRouteData->getQueueDatas();

                    std::list<QueueData>::iterator it = dataList.begin();
                    for (; it != dataList.end(); it++)
                    {
                        QueueData data = *it;

                        int queueNums =
                            std::min<int>(pDefaultMQProducer->getDefaultTopicQueueNums(),
                                          data.readQueueNums);
                        data.readQueueNums = (queueNums);
                        data.writeQueueNums = (queueNums);
                    }
                }
            }
            else
            {
                topicRouteData =
                    m_pMQClientAPIImpl->getTopicRouteInfoFromNameServer(topic, 1000 * 3);
            }

            if (topicRouteData.ptr() != NULL)
            {
                kpr::ScopedWLock<kpr::RWMutex> lock(m_topicRouteTableLock);
                std::map<std::string, TopicRouteData>::iterator it = m_topicRouteTable.find(topic);
                bool changed = false;

                if (it != m_topicRouteTable.end())
                {
                    changed = topicRouteDataIsChange(it->second, *topicRouteData);
                    if (!changed)
                    {
                        changed = isNeedUpdateTopicRouteInfo(topic);
						if (changed)
						{
                        	RMQ_INFO("the topic[{%s}] route info changed, old[{%s}] ,new[{%s}]",
                                 topic.c_str(), it->second.toString().c_str(),
                                 topicRouteData->toString().c_str());
                        }
                    }
                }
                else
                {
                    changed = true;
                }

                if (changed)
                {
                    TopicRouteData cloneTopicRouteData = *topicRouteData;

                    std::list<BrokerData> dataList = topicRouteData->getBrokerDatas();

                    std::list<BrokerData>::iterator it = dataList.begin();
                    for (; it != dataList.end(); it++)
                    {
                        kpr::ScopedWLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
                        m_brokerAddrTable[(*it).brokerName] = (*it).brokerAddrs;
                    }

                    {
                        TopicPublishInfoPtr publishInfo =
                            topicRouteData2TopicPublishInfo(topic, *topicRouteData);
                        publishInfo->setHaveTopicRouterInfo(true);

                        kpr::ScopedRLock<kpr::RWMutex> lock(m_producerTableLock);
                        std::map<std::string, MQProducerInner*>::iterator it = m_producerTable.begin();
                        for (; it != m_producerTable.end(); it++)
                        {
                            MQProducerInner* impl = it->second;
                            if (impl)
                            {
                                impl->updateTopicPublishInfo(topic, *publishInfo);
                            }
                        }
                    }

                    {
                        std::set<MessageQueue>* subscribeInfo =
                            topicRouteData2TopicSubscribeInfo(topic, *topicRouteData);

                        kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
                        std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
                        for (; it != m_consumerTable.end(); it++)
                        {
                            MQConsumerInner* impl = it->second;
                            if (impl)
                            {
                                impl->updateTopicSubscribeInfo(topic, *subscribeInfo);
                            }
                        }
                        delete subscribeInfo;
                    }

                    m_topicRouteTable[topic] = cloneTopicRouteData;
                    m_lockNamesrv.Unlock();
                    RMQ_DEBUG("UnLock m_lockNamesrv ok");

                    RMQ_INFO("topicRouteTable.put[%s] = TopicRouteData[%s]",
                    	topic.c_str(), cloneTopicRouteData.toString().c_str());
                    return true;
                }
            }
            else
            {
                //TODO log?
                RMQ_WARN("updateTopicRouteInfoFromNameServer, getTopicRouteInfoFromNameServer return null, Topic: {%s}",
                         topic.c_str());
            }
        }
        catch (const std::exception& e)
        {
        	if (!(topic.find(MixAll::RETRY_GROUP_TOPIC_PREFIX) == 0) && topic != MixAll::DEFAULT_TOPIC)
        	{
            	RMQ_WARN("updateTopicRouteInfoFromNameServer Exception: %s", e.what());
            }
        }
        catch (...)
        {
            RMQ_WARN("updateTopicRouteInfoFromNameServer unknow Exception");
        }

        m_lockNamesrv.Unlock();
        RMQ_DEBUG("UnLock m_lockNamesrv ok");
    }
    else
    {
        RMQ_WARN("TryLock m_lockNamesrv timeout %ldms", MQClientFactory::LockTimeoutMillis);
    }

    return false;
}

TopicPublishInfo*  MQClientFactory::topicRouteData2TopicPublishInfo(const std::string& topic,
        TopicRouteData& route)
{
    TopicPublishInfo* info = new TopicPublishInfo();
    if (!route.getOrderTopicConf().empty())
    {
        std::vector<std::string> brokers;
        UtilAll::Split(brokers, route.getOrderTopicConf(), ";");
        for (size_t i = 0; i < brokers.size(); i++)
        {
            std::vector<std::string> item;
            UtilAll::Split(item, brokers[i], ":");
            int nums = atoi(item[1].c_str());
            for (int i = 0; i < nums; i++)
            {
                MessageQueue mq(topic, item[0], i);
                info->getMessageQueueList().push_back(mq);
            }
        }

        info->setOrderTopic(true);
    }
    else
    {
        std::list<QueueData> qds = route.getQueueDatas();
        qds.sort();
        std::list<QueueData>::iterator it = qds.begin();
        for (; it != qds.end(); it++)
        {
            QueueData& qd = (*it);
            if (PermName::isWriteable(qd.perm))
            {
                bool find = false;
                BrokerData brokerData;
                std::list<BrokerData> bds = route.getBrokerDatas();
                std::list<BrokerData>::iterator it1 = bds.begin();

                for (; it1 != bds.end(); it1++)
                {
                    BrokerData& bd = (*it1);
                    if (bd.brokerName == qd.brokerName)
                    {
                        brokerData = bd;
                        find = true;
                        break;
                    }
                }

                if (!find)
                {
                    continue;
                }

                if (brokerData.brokerAddrs.find(MixAll::MASTER_ID) == brokerData.brokerAddrs.end())
                {
                    continue;
                }

                for (int i = 0; i < qd.writeQueueNums; i++)
                {
                    MessageQueue mq(topic, qd.brokerName, i);
                    info->getMessageQueueList().push_back(mq);
                }
            }
        }

        info->setOrderTopic(false);
    }

    return info;
}

std::set<MessageQueue>* MQClientFactory::topicRouteData2TopicSubscribeInfo(const std::string& topic,
        TopicRouteData& route)
{
    std::set<MessageQueue>* mqList = new std::set<MessageQueue>();
    std::list<QueueData> qds = route.getQueueDatas();
    std::list<QueueData>::iterator it = qds.begin();
    for (; it != qds.end(); it++)
    {
        QueueData& qd = (*it);
        if (PermName::isReadable(qd.perm))
        {
            for (int i = 0; i < qd.readQueueNums; i++)
            {
                MessageQueue mq(topic, qd.brokerName, i);
                mqList->insert(mq);
            }
        }
    }

    return mqList;
}

bool MQClientFactory::registerConsumer(const std::string& group, MQConsumerInner* pConsumer)
{
    if (group.empty() || pConsumer == NULL)
    {
        return false;
    }

	kpr::ScopedWLock<kpr::RWMutex> lock(m_consumerTableLock);
    if (m_consumerTable.find(group) != m_consumerTable.end())
    {
        return false;
    }
    m_consumerTable[group] = pConsumer;

    return true;
}

void MQClientFactory::unregisterConsumer(const std::string& group)
{
	{
		kpr::ScopedWLock<kpr::RWMutex> lock(m_consumerTableLock);
	    m_consumerTable.erase(group);
    }
    unregisterClientWithLock("", group);
}

bool MQClientFactory::registerProducer(const std::string& group, DefaultMQProducerImpl* pProducer)
{
    if (group.empty() || pProducer == NULL)
    {
        return false;
    }

	kpr::ScopedWLock<kpr::RWMutex> lock(m_producerTableLock);
    if (m_producerTable.find(group) != m_producerTable.end())
    {
        return false;
    }
    m_producerTable[group] = pProducer;

    return true;
}

void MQClientFactory::unregisterProducer(const std::string& group)
{
	{
		kpr::ScopedWLock<kpr::RWMutex> lock(m_producerTableLock);
	    m_producerTable.erase(group);
    }
    unregisterClientWithLock(group, "");
}

bool MQClientFactory::registerAdminExt(const std::string& group, MQAdminExtInner* pAdmin)
{
    if (group.empty() || pAdmin == NULL)
    {
        return false;
    }

	kpr::ScopedWLock<kpr::RWMutex> lock(m_adminExtTableLock);
    if (m_adminExtTable.find(group) != m_adminExtTable.end())
    {
        return false;
    }
    m_adminExtTable[group] = pAdmin;

    return true;
}

void MQClientFactory::unregisterAdminExt(const std::string& group)
{
	kpr::ScopedWLock<kpr::RWMutex> lock(m_adminExtTableLock);
    m_adminExtTable.erase(group);
}

void MQClientFactory::rebalanceImmediately()
{
    m_pRebalanceService->wakeup();
}

void MQClientFactory::doRebalance()
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
    std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
    for (; it != m_consumerTable.end(); it++)
    {
        MQConsumerInner* impl = it->second;
        if (impl != NULL)
        {
            try
            {
                impl->doRebalance();
            }
            catch (std::exception& e)
            {
                RMQ_ERROR("doRebalance exception, %s", e.what());
            }
            catch (...)
            {
                RMQ_ERROR("doRebalance unknow exception");
            }
        }
    }
}

MQProducerInner* MQClientFactory::selectProducer(const std::string& group)
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_producerTableLock);
    std::map<std::string, MQProducerInner*>::iterator it = m_producerTable.find(group);
    if (it != m_producerTable.end())
    {
        return it->second;
    }

    return NULL;
}

MQConsumerInner* MQClientFactory::selectConsumer(const std::string& group)
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
    std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.find(group);
    if (it != m_consumerTable.end())
    {
        return it->second;
    }

    return NULL;
}

FindBrokerResult MQClientFactory::findBrokerAddressInAdmin(const std::string& brokerName)
{
    //TODO
    FindBrokerResult result;
    std::string brokerAddr;
    bool slave = false;
    bool found = false;

    kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
    typeof(m_brokerAddrTable.begin()) it = m_brokerAddrTable.find(brokerName);
    if (it != m_brokerAddrTable.end())
    {
        // TODO more slave
        typeof(it->second.begin()) it1 = it->second.begin();
        for (; it1 != it->second.end(); it1++)
        {
            int brockerId = it1->first;
            brokerAddr = it1->second;
            if (!brokerAddr.empty())
            {
                found = true;
                if (MixAll::MASTER_ID == brockerId)
                {
                    slave = false;
                }
                else
                {
                    slave = true;
                }
                break;
            }
        }
    }

    if (found)
    {
        result.brokerAddr = brokerAddr;
        result.slave = slave;
    }

    return result;
}

std::string MQClientFactory::findBrokerAddressInPublish(const std::string& brokerName)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
    std::map<std::string, std::map<int, std::string> >::iterator it = m_brokerAddrTable.find(brokerName);
    if (it != m_brokerAddrTable.end())
    {
        std::map<int, std::string>::iterator it1 = it->second.find(MixAll::MASTER_ID);
        if (it1 != it->second.end())
        {
            return it1->second;
        }
    }

    return "";
}

FindBrokerResult MQClientFactory::findBrokerAddressInSubscribe(const std::string& brokerName,
        long brokerId,
        bool onlyThisBroker)
{
    std::string brokerAddr = "";
    bool slave = false;
    bool found = false;

    kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
    std::map<std::string, std::map<int, std::string> >::iterator it = m_brokerAddrTable.find(brokerName);
    if (it != m_brokerAddrTable.end())
    {
        std::map<int, std::string>::iterator it1 = it->second.find(brokerId);
        if (it1 != it->second.end())
        {
            brokerAddr = it1->second;
            slave = (brokerId != MixAll::MASTER_ID);
            found = true;
        }
        else
        {
            it1 = it->second.begin();
            brokerAddr = it1->second;
            slave = (brokerId != MixAll::MASTER_ID);
            found = true;
        }
    }

    FindBrokerResult result;
    result.brokerAddr = brokerAddr;
    result.slave = slave;

    return result;
}

std::list<std::string> MQClientFactory::findConsumerIdList(const std::string& topic, const std::string& group)
{
    std::string brokerAddr = findBrokerAddrByTopic(topic);

    if (brokerAddr.empty())
    {
        updateTopicRouteInfoFromNameServer(topic);
        brokerAddr = findBrokerAddrByTopic(topic);
    }

    if (!brokerAddr.empty())
    {
        try
        {
            return m_pMQClientAPIImpl->getConsumerIdListByGroup(brokerAddr, group, 3000);
        }
        catch (...)
        {
			RMQ_WARN("getConsumerIdListByGroup exception, %s, %s", brokerAddr.c_str(), group.c_str());
        }
    }

    std::list<std::string> ids;

    return ids;
}

std::string MQClientFactory::findBrokerAddrByTopic(const std::string& topic)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_topicRouteTableLock);

    std::map<std::string, TopicRouteData>::iterator it = m_topicRouteTable.find(topic);
    if (it != m_topicRouteTable.end())
    {
        const std::list<BrokerData>& brokers = it->second.getBrokerDatas();

        if (!brokers.empty())
        {
            BrokerData bd = brokers.front();
            return TopicRouteData::selectBrokerAddr(bd);
        }
    }

    return "";
}

TopicRouteData MQClientFactory::getAnExistTopicRouteData(const std::string& topic)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_topicRouteTableLock);

    std::map<std::string, TopicRouteData>::iterator it = m_topicRouteTable.find(topic);
    if (it != m_topicRouteTable.end())
    {
        return it->second;
    }

    TopicRouteData data;
    return data;
}

MQClientAPIImpl* MQClientFactory::getMQClientAPIImpl()
{
    return m_pMQClientAPIImpl;
}

MQAdminImpl* MQClientFactory::getMQAdminImpl()
{
    return m_pMQAdminImpl;
}

std::string MQClientFactory::getClientId()
{
    return m_clientId;
}

long long MQClientFactory::getBootTimestamp()
{
    return m_bootTimestamp;
}

PullMessageService* MQClientFactory::getPullMessageService()
{
    return m_pPullMessageService;
}


DefaultMQProducer* MQClientFactory::getDefaultMQProducer()
{
    return m_pDefaultMQProducer;
}

void MQClientFactory::sendHeartbeatToAllBroker()
{
    RMQ_DEBUG("sendHeartbeatToAllBroker begin");

    HeartbeatData heartbeatData;
    this->prepareHeartbeatData(heartbeatData);

    bool producerEmpty = heartbeatData.getProducerDataSet().empty();
    bool consumerEmpty = heartbeatData.getConsumerDataSet().empty();
    if (producerEmpty && consumerEmpty)
    {
        RMQ_ERROR("sending hearbeat, but no consumer and no producer");
        return;
    }

    RMQ_DEBUG("clientId=%s, m_brokerAddrTable=%u", heartbeatData.getClientID().c_str(), (unsigned)m_brokerAddrTable.size());

    kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
    std::map<std::string, std::map<int, std::string> >::iterator it = m_brokerAddrTable.begin();
    for (; it != m_brokerAddrTable.end(); it++)
    {
        std::map<int, std::string>::iterator it1 = it->second.begin();
        for (; it1 != it->second.end(); it1++)
        {
            std::string& addr = it1->second;
            if (!addr.empty())
            {
                if (consumerEmpty)
                {
                    if (it1->first != MixAll::MASTER_ID)
                    {
                        continue;
                    }
                }

                try
                {
                    m_pMQClientAPIImpl->sendHearbeat(addr, &heartbeatData, 3000);
                    RMQ_INFO("send heartbeat to broker[{%s} {%d} {%s}] success",
                    	it->first.c_str(), it1->first, addr.c_str());
                    RMQ_INFO("HeartbeatData %s", heartbeatData.toString().c_str());
                }
                catch (...)
                {
                    RMQ_ERROR("send heart beat to broker exception");
                }
            }
        }
    }

    RMQ_DEBUG("sendHeartbeatToAllBroker end");
}

void MQClientFactory::prepareHeartbeatData(HeartbeatData& heartbeatData)
{
    // clientID
    heartbeatData.setClientID(m_clientId);

    // Consumer
    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
        std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
        for (; it != m_consumerTable.end(); it++)
        {
            MQConsumerInner* inner = it->second;
            if (inner)
            {
                ConsumerData consumerData;
                consumerData.groupName = inner->groupName();
                consumerData.consumeType = inner->consumeType();
                consumerData.messageModel = inner->messageModel();
                consumerData.consumeFromWhere = inner->consumeFromWhere();
                consumerData.subscriptionDataSet = inner->subscriptions();

                heartbeatData.getConsumerDataSet().insert(consumerData);
            }
        }
    }

    // Producer
    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_producerTableLock);
        std::map<std::string, MQProducerInner*>::iterator it = m_producerTable.begin();
        for (; it != m_producerTable.end(); it++)
        {
            MQProducerInner* inner = it->second;
            if (inner)
            {
                ProducerData producerData;
                producerData.groupName = (it->first);

                heartbeatData.getProducerDataSet().insert(producerData);
            }
        }
    }

    return;
}

void MQClientFactory::makesureInstanceNameIsOnly(const std::string& instanceName)
{
    //TODO
}


void MQClientFactory::fetchNameServerAddr()
{
    //1000 * 10, 1000 * 60 * 2
    try
    {
        RMQ_DEBUG("Task: fetchNameServerAddr");
        m_pMQClientAPIImpl->fetchNameServerAddr();
    }
    catch (...)
    {
    	RMQ_ERROR("Task: fetchNameServerAddr exception");
    }
}

void MQClientFactory::updateTopicRouteInfoFromNameServerTask()
{
    //10, 1000 * 30, m_clientConfig.getPollNameServerInteval()
    try
    {
        RMQ_DEBUG("Task: updateTopicRouteInfoFromNameServerTask");
        updateTopicRouteInfoFromNameServer();
    }
    catch (...)
    {
		RMQ_ERROR("Task: fetchNameServerAddr exception");
    }
}

void MQClientFactory::cleanBroker()
{
    //1000, 1000 * 30, m_clientConfig.getHeartbeatBrokerInterval()
    try
    {
        RMQ_DEBUG("Task: cleanBroker");
        cleanOfflineBroker();
        sendHeartbeatToAllBrokerWithLock();
    }
    catch (...)
    {
		RMQ_ERROR("Task: cleanBroker exception");
    }
}

void MQClientFactory::persistAllConsumerOffsetTask()
{
    //1000 * 10, 1000 * 5, m_clientConfig.getPersistConsumerOffsetInterval()
    try
    {
        RMQ_DEBUG("Task: persistAllConsumerOffsetTask");
        persistAllConsumerOffset();
    }
    catch (...)
    {
		RMQ_ERROR("Task: persistAllConsumerOffsetTask exception");
    }
}

void MQClientFactory::recordSnapshotPeriodicallyTask()
{
    // 1000 * 10, 1000,
    try
    {
        //RMQ_DEBUG("Task: recordSnapshotPeriodicallyTask");
        recordSnapshotPeriodically();
    }
    catch (...)
    {
		RMQ_ERROR("Task: recordSnapshotPeriodically exception");
    }
}

void MQClientFactory::logStatsPeriodicallyTask()
{
    //  1000 * 10, 1000 * 60
    try
    {
        RMQ_DEBUG("Task: logStatsPeriodicallyTask");
        logStatsPeriodically();
    }
    catch (...)
    {
		RMQ_ERROR("Task: logStatsPeriodicallyTask exception");
    }
}

void MQClientFactory::startScheduledTask()
{
    m_scheduledTaskIds[0] = m_timerTaskManager.RegisterTimer(1000 * 10, 1000 * 60 * 2, m_pFetchNameServerAddrTask);

    m_scheduledTaskIds[1] = m_timerTaskManager.RegisterTimer(10, m_clientConfig.getPollNameServerInterval(), m_pUpdateTopicRouteInfoFromNameServerTask);

    m_scheduledTaskIds[2] = m_timerTaskManager.RegisterTimer(1000, m_clientConfig.getHeartbeatBrokerInterval(), m_pCleanBrokerTask);

    m_scheduledTaskIds[3] = m_timerTaskManager.RegisterTimer(1000 * 10, m_clientConfig.getPersistConsumerOffsetInterval(), m_pPersistAllConsumerOffsetTask);

    m_scheduledTaskIds[4] = m_timerTaskManager.RegisterTimer(1000 * 10, 1000, m_pRecordSnapshotPeriodicallyTask);
    m_scheduledTaskIds[5] = m_timerTaskManager.RegisterTimer(1000 * 10, 1000 * 60, m_pLogStatsPeriodicallyTask);
}

void MQClientFactory::cleanOfflineBroker()
{
    RMQ_DEBUG("TryLock m_lockNamesrv: 0x%p", &m_lockNamesrv);
    if (m_lockNamesrv.TryLock(MQClientFactory::LockTimeoutMillis))
    {
        RMQ_DEBUG("TryLock m_lockNamesrv ok");
        std::map<std::string, std::map<int, std::string> > updatedTable;
        {
            kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
            std::map<std::string, std::map<int, std::string> >::iterator it = m_brokerAddrTable.begin();

            for (; it != m_brokerAddrTable.end(); it++)
            {
                std::map<int, std::string> cloneTable = it->second;

                std::map<int, std::string>::iterator it1 = cloneTable.begin();

                for (; it1 != cloneTable.end();)
                {
                    std::string& addr = it1->second;
                    if (!isBrokerAddrExistInTopicRouteTable(addr))
                    {
                        std::map<int, std::string>::iterator itTmp = it1;
                        it1++;
                        cloneTable.erase(itTmp);
                        continue;
                    }

                    it1++;
                }

                if (!cloneTable.empty())
                {
                    updatedTable[it->first] = cloneTable;
                }
            }
        }

        {
            kpr::ScopedWLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
            m_brokerAddrTable.clear();
            m_brokerAddrTable = updatedTable;
        }

        m_lockNamesrv.Unlock();
        RMQ_DEBUG("UnLock m_lockNamesrv ok");
    }
    else
    {
        RMQ_DEBUG("TryLock m_lockNamesrv fail");
    }
}

bool MQClientFactory::isBrokerAddrExistInTopicRouteTable(const std::string& addr)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_topicRouteTableLock);

    std::map<std::string, TopicRouteData>::iterator it = m_topicRouteTable.begin();
    for (; it != m_topicRouteTable.end(); it++)
    {
        const std::list<BrokerData>& brokers = it->second.getBrokerDatas();
        std::list<BrokerData>::const_iterator it1 = brokers.begin();

        for (; it1 != brokers.end(); it1++)
        {
            std::map<int, std::string>::const_iterator it2 = (*it1).brokerAddrs.begin();
            for (; it2 != (*it1).brokerAddrs.end(); it2++)
            {
                if (it2->second.find(addr) != std::string::npos)
                {
                    return true;
                }
            }
        }
    }

    return false;
}

void MQClientFactory::recordSnapshotPeriodically()
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
    std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
    for (; it != m_consumerTable.end(); it++)
    {
        MQConsumerInner* inner = it->second;
        if (inner)
        {
            DefaultMQPushConsumerImpl* consumer = dynamic_cast<DefaultMQPushConsumerImpl*>(inner);
            if (consumer)
            {
                consumer->getConsumerStatManager()->recordSnapshotPeriodically();
            }
        }
    }
}

void MQClientFactory::logStatsPeriodically()
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
    std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
    for (; it != m_consumerTable.end(); it++)
    {
        MQConsumerInner* inner = it->second;
        if (inner)
        {
            DefaultMQPushConsumerImpl* consumer = dynamic_cast<DefaultMQPushConsumerImpl*>(inner);
            if (consumer)
            {
                std::string group = it->first;
                consumer->getConsumerStatManager()->logStatsPeriodically(group, m_clientId);
            }
        }
    }
}

void MQClientFactory::persistAllConsumerOffset()
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
    RMQ_DEBUG("persistAllConsumerOffset, m_consumerTable.size=%u", (unsigned)m_consumerTable.size());
    std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
    for (; it != m_consumerTable.end(); it++)
    {
        MQConsumerInner* inner = it->second;
        if (inner)
        {
            inner->persistConsumerOffset();
        }
    }
}

bool MQClientFactory::topicRouteDataIsChange(TopicRouteData& olddata, TopicRouteData& nowdata)
{
    TopicRouteData old = olddata;
    TopicRouteData now = nowdata;

    old.getQueueDatas().sort();
    old.getBrokerDatas().sort();
    now.getQueueDatas().sort();
    now.getBrokerDatas().sort();

    return !(old == now);

}

bool MQClientFactory::isNeedUpdateTopicRouteInfo(const std::string& topic)
{
    bool result = false;
    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_producerTableLock);
        std::map<std::string, MQProducerInner*>::iterator it = m_producerTable.begin();
        for (; it != m_producerTable.end(); it++)
        {
            MQProducerInner* inner = it->second;
            if (inner)
            {
                result = inner->isPublishTopicNeedUpdate(topic);
            }
        }
    }

    {
    	kpr::ScopedRLock<kpr::RWMutex> lock(m_consumerTableLock);
        std::map<std::string, MQConsumerInner*>::iterator it = m_consumerTable.begin();
        for (; it != m_consumerTable.end(); it++)
        {
            MQConsumerInner* inner = it->second;
            if (inner)
            {
                result = inner->isSubscribeTopicNeedUpdate(topic);
            }
        }
    }

    return result;
}

void MQClientFactory::unregisterClientWithLock(const std::string& producerGroup, const std::string& consumerGroup)
{
    RMQ_DEBUG("TryLock m_lockHeartbeat: 0x%p", &m_lockHeartbeat);
    if (m_lockHeartbeat.TryLock(MQClientFactory::LockTimeoutMillis))
    {
        try
        {
            RMQ_DEBUG("TryLock m_lockHeartbeat ok");
            unregisterClient(producerGroup, consumerGroup);
        }
        catch (...)
        {
            RMQ_ERROR("unregisterClientWithLock exception, %s %s",
                      producerGroup.c_str(), consumerGroup.c_str());
        }
        m_lockHeartbeat.Unlock();
        RMQ_DEBUG("Unlock m_lockHeartbeat ok");
    }
    else
    {
        RMQ_WARN("TryLock m_lockHeartbeat fail");
    }
}

void MQClientFactory::unregisterClient(const std::string& producerGroup, const std::string& consumerGroup)
{
    kpr::ScopedRLock<kpr::RWMutex> lock(m_brokerAddrTableLock);
    std::map<std::string, std::map<int, std::string> >::iterator it = m_brokerAddrTable.begin();
    for (; it != m_brokerAddrTable.end(); it++)
    {
        std::map<int, std::string>::iterator it1 = it->second.begin();

        for (; it1 != it->second.end(); it1++)
        {
            std::string& addr = it1->second;

            if (!addr.empty())
            {
                try
                {
                    m_pMQClientAPIImpl->unregisterClient(addr, m_clientId, producerGroup,
                                                         consumerGroup, 3000);
                }
                catch (...)
                {
                    RMQ_ERROR("unregister client exception from broker: %s", addr.c_str());
                }
            }
        }
    }
}

}
