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

#include <list>
#include "SocketUtil.h"
#include "MQAdminImpl.h"
#include "MQClientFactory.h"
#include "MQClientAPIImpl.h"
#include "MQClientException.h"
#include "TopicConfig.h"
#include "TopicPublishInfo.h"
#include "MessageId.h"
#include "MessageDecoder.h"

namespace rmq
{


MQAdminImpl::MQAdminImpl(MQClientFactory* pMQClientFactory)
{
    m_pMQClientFactory = pMQClientFactory;
}

MQAdminImpl::~MQAdminImpl()
{

}

void MQAdminImpl::createTopic(const std::string& key, const std::string& newTopic,
	int queueNum)
{
	return createTopic(key, newTopic, queueNum, 0);
}


void MQAdminImpl::createTopic(const std::string& key, const std::string& newTopic,
	int queueNum, int topicSysFlag)
{
    try
    {
        MQClientAPIImpl* api = m_pMQClientFactory->getMQClientAPIImpl();
        TopicRouteDataPtr topicRouteData = api->getTopicRouteInfoFromNameServer(key, 1000 * 3);

        std::list<BrokerData> brokerDataList = topicRouteData->getBrokerDatas();
        if (!brokerDataList.empty())
        {
            brokerDataList.sort();

            MQClientException exception("", 0, "", 0);
            bool hasException = false;

            std::list<BrokerData>::iterator it = brokerDataList.begin();

            for (; it != brokerDataList.end(); it++)
            {
                std::map<int, std::string>::iterator it1 = (*it).brokerAddrs.find(MixAll::MASTER_ID);
                if (it1 != (*it).brokerAddrs.end())
                {
                    std::string addr = it1->second;

                    TopicConfig topicConfig(newTopic);
                    topicConfig.setReadQueueNums(queueNum);
                    topicConfig.setWriteQueueNums(queueNum);
                    topicConfig.setTopicSysFlag(topicSysFlag);

                    try
                    {
                        api->createTopic(addr, key, topicConfig, 1000 * 3);
                    }
                    catch (MQClientException& e)
                    {
                        hasException = true;
                        exception = e;
                    }
                }
            }

            if (hasException)
            {
                throw exception;
            }
        }
        else
        {
            THROW_MQEXCEPTION(MQClientException, "Not found broker, maybe key is wrong", -1);
        }
    }
    catch (MQClientException e)
    {
        THROW_MQEXCEPTION(MQClientException, "create new topic failed", -1);
    }
}

std::vector<MessageQueue>* MQAdminImpl::fetchPublishMessageQueues(const std::string& topic)
{
    try
    {
        MQClientAPIImpl* api = m_pMQClientFactory->getMQClientAPIImpl();
        TopicRouteDataPtr topicRouteData = api->getTopicRouteInfoFromNameServer(topic, 1000 * 3);

        if (topicRouteData.ptr() != NULL)
        {
            TopicPublishInfoPtr topicPublishInfo =
                MQClientFactory::topicRouteData2TopicPublishInfo(topic, *topicRouteData);
            if (topicPublishInfo.ptr() != NULL && topicPublishInfo->ok())
            {
                std::vector<MessageQueue>* ret = new std::vector<MessageQueue>();
                (*ret) = topicPublishInfo->getMessageQueueList();

				/*
                std::vector<MessageQueue>& mqs = ;
                std::vector<MessageQueue>::iterator it = mqs.begin();
                for (; it != mqs.end(); it++)
                {
                    ret->push_back(*it);
                }
                */

                return ret;
            }
        }
    }
    catch (MQClientException e)
    {
        THROW_MQEXCEPTION(MQClientException, "Can not find Message Queue for this topic" + topic, -1);
    }

    THROW_MQEXCEPTION(MQClientException, "Unknow why, Can not find Message Queue for this topic, " + topic, -1);
}

std::set<MessageQueue>* MQAdminImpl::fetchSubscribeMessageQueues(const std::string& topic)
{
    try
    {
        TopicRouteDataPtr topicRouteData =
            m_pMQClientFactory->getMQClientAPIImpl()->getTopicRouteInfoFromNameServer(topic, 1000 * 3);
        if (topicRouteData.ptr() != NULL)
        {
            std::set<MessageQueue>* mqList =
                MQClientFactory::topicRouteData2TopicSubscribeInfo(topic, *topicRouteData);
            if (!mqList->empty())
            {
                return mqList;
            }
            else
            {
                THROW_MQEXCEPTION(MQClientException, "Can not find Message Queue for this topic" + topic, -1);
            }
        }
    }
    catch (MQClientException e)
    {
        THROW_MQEXCEPTION(MQClientException, "Can not find Message Queue for this topic" + topic, -1);
    }

    THROW_MQEXCEPTION(MQClientException, "Unknow why, Can not find Message Queue for this topic: " + topic, -1);
}

long long MQAdminImpl::searchOffset(const MessageQueue& mq, long long timestamp)
{
    std::string brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    if (brokerAddr.empty())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    }

    if (!brokerAddr.empty())
    {
        try
        {
            return m_pMQClientFactory->getMQClientAPIImpl()->searchOffset(brokerAddr, mq.getTopic(),
                    mq.getQueueId(), timestamp, 1000 * 3);
        }
        catch (MQClientException e)
        {
            THROW_MQEXCEPTION(MQClientException, "Invoke Broker[" + brokerAddr + "] exception", -1);
        }
    }
    THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
}

long long MQAdminImpl::maxOffset(const MessageQueue& mq)
{
    std::string brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    if (brokerAddr.empty())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    }

    if (!brokerAddr.empty())
    {
        try
        {
            return m_pMQClientFactory->getMQClientAPIImpl()->getMaxOffset(brokerAddr, mq.getTopic(),
                    mq.getQueueId(), 1000 * 3);
        }
        catch (MQClientException e)
        {
            THROW_MQEXCEPTION(MQClientException, "Invoke Broker[" + brokerAddr + "] exception", -1);
        }
    }
    THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
}

long long MQAdminImpl::minOffset(const MessageQueue& mq)
{
    std::string brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    if (brokerAddr.empty())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    }

    if (!brokerAddr.empty())
    {
        try
        {
            return m_pMQClientFactory->getMQClientAPIImpl()->getMinOffset(brokerAddr, mq.getTopic(),
                    mq.getQueueId(), 1000 * 3);
        }
        catch (MQClientException e)
        {
            THROW_MQEXCEPTION(MQClientException, "Invoke Broker[" + brokerAddr + "] exception", -1);
        }
    }

    THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
}

long long MQAdminImpl::earliestMsgStoreTime(const MessageQueue& mq)
{
    std::string brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    if (brokerAddr.empty())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(mq.getTopic());
        brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    }

    if (!brokerAddr.empty())
    {
        try
        {
            return m_pMQClientFactory->getMQClientAPIImpl()->getEarliestMsgStoretime(brokerAddr,
                    mq.getTopic(), mq.getQueueId(), 1000 * 3);
        }
        catch (MQClientException e)
        {
            THROW_MQEXCEPTION(MQClientException, "Invoke Broker[" + brokerAddr + "] exception", -1);
        }
    }

    THROW_MQEXCEPTION(MQClientException, "The broker[" + mq.getBrokerName() + "] not exist", -1);
}

MessageExt* MQAdminImpl::viewMessage(const std::string& msgId)
{
    try
    {
        MessageId messageId = MessageDecoder::decodeMessageId(msgId);
        return m_pMQClientFactory->getMQClientAPIImpl()->viewMessage(
                   socketAddress2String(messageId.getAddress()), messageId.getOffset(), 1000 * 3);
    }
    catch (UnknownHostException e)
    {
        THROW_MQEXCEPTION(MQClientException, "message id illegal", -1);
    }
}

QueryResult MQAdminImpl::queryMessage(const std::string& topic,
                                      const std::string& key,
                                      int maxNum, long long begin, long long end)
{
    //TODO
    std::list<MessageExt*> messageList;
    QueryResult result(0, messageList);

    return result;
}

}
