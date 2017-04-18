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
#include <assert.h>

#include "MQClientAPIImpl.h"
#include "MQClientException.h"
#include "SocketUtil.h"
#include "UtilAll.h"
#include "TcpRemotingClient.h"
#include "MQProtos.h"
#include "PullResultExt.h"
#include "ConsumerInvokeCallback.h"
#include "NamesrvUtil.h"
#include "VirtualEnvUtil.h"
#include "ClientRemotingProcessor.h"
#include "CommandCustomHeader.h"
#include "TopicList.h"
#include "ProducerInvokeCallback.h"
#include "MessageDecoder.h"
#include "MessageSysFlag.h"
#include "GetConsumerListByGroupResponseBody.h"


namespace rmq
{


MQClientAPIImpl::MQClientAPIImpl(ClientConfig& clientConfig,
								const RemoteClientConfig& remoteClientConfig,
                                ClientRemotingProcessor* pClientRemotingProcessor)
    : m_pClientRemotingProcessor(pClientRemotingProcessor)
{
    m_pRemotingClient = new TcpRemotingClient(remoteClientConfig);

    m_pRemotingClient->registerProcessor(CHECK_TRANSACTION_STATE_VALUE, m_pClientRemotingProcessor);
    m_pRemotingClient->registerProcessor(NOTIFY_CONSUMER_IDS_CHANGED_VALUE, m_pClientRemotingProcessor);
    m_pRemotingClient->registerProcessor(RESET_CONSUMER_CLIENT_OFFSET_VALUE, m_pClientRemotingProcessor);
    m_pRemotingClient->registerProcessor(GET_CONSUMER_STATUS_FROM_CLIENT_VALUE, m_pClientRemotingProcessor);
    m_pRemotingClient->registerProcessor(GET_CONSUMER_RUNNING_INFO_VALUE, m_pClientRemotingProcessor);
    m_pRemotingClient->registerProcessor(CONSUME_MESSAGE_DIRECTLY_VALUE, m_pClientRemotingProcessor);
}

MQClientAPIImpl::~MQClientAPIImpl()
{
}

std::string MQClientAPIImpl::getProjectGroupPrefix()
{
    return m_projectGroupPrefix;
}

std::vector<std::string> MQClientAPIImpl::getNameServerAddressList()
{
    return m_pRemotingClient->getNameServerAddressList();
}

TcpRemotingClient* MQClientAPIImpl::getRemotingClient()
{
    return m_pRemotingClient;
}

std::string MQClientAPIImpl::fetchNameServerAddr()
{
    try
    {
        std::string addrs = m_topAddressing.fetchNSAddr();
        if (!addrs.empty())
        {
            if (addrs != m_nameSrvAddr)
            {
            	RMQ_INFO("name server address changed, %s -> %s",
            		m_nameSrvAddr.c_str(), addrs.c_str());
                updateNameServerAddressList(addrs);
                m_nameSrvAddr = addrs;
                return m_nameSrvAddr;
            }
        }
    }
    catch (...)
    {
		RMQ_ERROR("fetchNameServerAddr Exception");
    }

    return m_nameSrvAddr;
}

void MQClientAPIImpl::updateNameServerAddressList(const std::string& addrs)
{
    m_nameSrvAddr = addrs;
    std::vector<std::string> av;
    UtilAll::Split(av, addrs, ";");
    if (av.size() > 0)
    {
    	m_pRemotingClient->updateNameServerAddressList(av);
    }
}

void MQClientAPIImpl::start()
{
    m_pRemotingClient->start();

    try
    {
        std::string localAddress = getLocalAddress();
        m_projectGroupPrefix = getProjectGroupByIp(localAddress, 3000);
    }
    catch (std::exception e)
    {
    }
}

void MQClientAPIImpl::shutdown()
{
    m_pRemotingClient->shutdown();
}

void MQClientAPIImpl::createSubscriptionGroup(const std::string& addr,
        SubscriptionGroupConfig config,
        int timeoutMillis)
{
    //TODO
}


void MQClientAPIImpl::createTopic(const std::string& addr,
                                  const std::string& defaultTopic,
                                  TopicConfig topicConfig,
                                  int timeoutMillis)
{
    std::string topicWithProjectGroup = topicConfig.getTopicName();
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup =
            VirtualEnvUtil::buildWithProjectGroup(topicConfig.getTopicName(), m_projectGroupPrefix);
    }

    CreateTopicRequestHeader* requestHeader = new CreateTopicRequestHeader();
    requestHeader->topic = (topicWithProjectGroup);
    requestHeader->defaultTopic = (defaultTopic);
    requestHeader->readQueueNums = (topicConfig.getReadQueueNums());
    requestHeader->writeQueueNums = (topicConfig.getWriteQueueNums());
    requestHeader->perm = (topicConfig.getPerm());
    requestHeader->topicFilterType = (topicConfig.getTopicFilterType());
    requestHeader->topicSysFlag = (topicConfig.getTopicSysFlag());
    requestHeader->order = (topicConfig.isOrder());

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(UPDATE_AND_CREATE_TOPIC_VALUE, requestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                return;
            }
            default:
                break;
        }
        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "createTopic failed", -1);
}

SendResult MQClientAPIImpl::sendMessage(const std::string& addr,
                                        const std::string& brokerName,
                                        Message& msg,
                                        SendMessageRequestHeader* pRequestHeader,
                                        int timeoutMillis,
                                        CommunicationMode communicationMode,
                                        SendCallback* pSendCallback)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        msg.setTopic(VirtualEnvUtil::buildWithProjectGroup(msg.getTopic(), m_projectGroupPrefix));
        pRequestHeader->producerGroup = (VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->producerGroup,
                                         m_projectGroupPrefix));
        pRequestHeader->topic = (VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->topic,
                                 m_projectGroupPrefix));
    }

	bool sendSmartMsg = true;
	RemotingCommandPtr request = NULL;
    if (sendSmartMsg)
    {
        SendMessageRequestHeaderV2* pRequestHeaderV2 = SendMessageRequestHeaderV2::createSendMessageRequestHeaderV2(pRequestHeader);
        request = RemotingCommand::createRequestCommand(SEND_MESSAGE_V2_VALUE, pRequestHeaderV2);
        delete pRequestHeader;
    }
    else
    {
        request = RemotingCommand::createRequestCommand(SEND_MESSAGE_VALUE, pRequestHeader);
    }

    if (msg.getCompressBody() != NULL)
    {
    	request->setBody((char*)msg.getCompressBody(), msg.getCompressBodyLen(), false);
    }
    else
    {
    	request->setBody((char*)msg.getBody(), msg.getBodyLen(), false);
    }

    SendResult result;
    switch (communicationMode)
    {
        case ONEWAY:
            m_pRemotingClient->invokeOneway(addr, request, timeoutMillis);
            return result;
        case ASYNC:
            sendMessageAsync(addr, brokerName, msg, timeoutMillis, request, pSendCallback);
            return result;
        case SYNC:
        {
            SendResult* r = sendMessageSync(addr, brokerName, msg, timeoutMillis, request);
            if (r)
            {
                result = *r;
                delete r;
            }
            return result;
        }
        default:
            break;
    }
    return result;
}

PullResult* MQClientAPIImpl::pullMessage(const std::string& addr,
        PullMessageRequestHeader* pRequestHeader,
        int timeoutMillis,
        CommunicationMode communicationMode,
        PullCallback* pPullCallback)
{

    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestHeader->consumerGroup = (VirtualEnvUtil::buildWithProjectGroup(
                                             pRequestHeader->consumerGroup, m_projectGroupPrefix));
        pRequestHeader->topic = (VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->topic,
                                 m_projectGroupPrefix));
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(PULL_MESSAGE_VALUE, pRequestHeader);

    PullResult* result = NULL;
    switch (communicationMode)
    {
        case ONEWAY:
            break;
        case ASYNC:
            pullMessageAsync(addr, request, timeoutMillis, pPullCallback);
            break;
        case SYNC:
            result =  pullMessageSync(addr, request, timeoutMillis);
            break;
        default:
            assert(false);
            break;
    }

    return result;
}

MessageExt* MQClientAPIImpl::viewMessage(const std::string& addr,  long long phyoffset,  int timeoutMillis)
{
    ViewMessageRequestHeader* requestHeader = new ViewMessageRequestHeader();
    requestHeader->offset = phyoffset;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(VIEW_MESSAGE_BY_ID_VALUE, requestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                if (response->getBody() != NULL)
                {
                	int len = 0;
                	MessageExt* messageExt = MessageDecoder::decode((char*)response->getBody(),
                		response->getBodyLen(), len);
                    if (!UtilAll::isBlank(m_projectGroupPrefix))
				    {
				        messageExt->setTopic(VirtualEnvUtil::clearProjectGroup(messageExt->getTopic(),
				        	m_projectGroupPrefix));
				    }
                    return messageExt;
                }
            }
            default:
                break;
        }
        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "viewMessage failed", -1);
}

long long MQClientAPIImpl::searchOffset(const std::string& addr,
                                        const std::string& topic,
                                        int queueId,
                                        long long timestamp,
                                        int timeoutMillis)
{
    std::string topicWithProjectGroup = topic;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup = VirtualEnvUtil::buildWithProjectGroup(topic, m_projectGroupPrefix);
    }

	SearchOffsetRequestHeader* pRequestHeader = new SearchOffsetRequestHeader();
    pRequestHeader->topic = topicWithProjectGroup;
    pRequestHeader->queueId = queueId;
    pRequestHeader->timestamp = timestamp;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(SEARCH_OFFSET_BY_TIMESTAMP_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            SearchOffsetResponseHeader* ret = (SearchOffsetResponseHeader*)response->getCommandCustomHeader();
	            return ret->offset;
	        }
	        default:
	            break;
	    }
	    //THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
	}

    //THROW_MQEXCEPTION(MQClientException, "searchOffset failed", -1);
    return -1;
}

long long MQClientAPIImpl::getMaxOffset(const std::string& addr,
                                        const std::string& topic,
                                        int queueId,
                                        int timeoutMillis)
{
    std::string topicWithProjectGroup = topic;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup = VirtualEnvUtil::buildWithProjectGroup(topic, m_projectGroupPrefix);
    }

	GetMaxOffsetRequestHeader* pRequestHeader = new GetMaxOffsetRequestHeader();
    pRequestHeader->topic = topicWithProjectGroup;
    pRequestHeader->queueId = queueId;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(GET_MAX_OFFSET_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            GetMaxOffsetResponseHeader* ret = (GetMaxOffsetResponseHeader*)response->getCommandCustomHeader();
	            return ret->offset;
	        }
	        default:
	            break;
	    }
	    //THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
	}

    //THROW_MQEXCEPTION(MQClientException, "getMaxOffset failed", -1);
    return -1;
}


std::list<std::string> MQClientAPIImpl::getConsumerIdListByGroup(const std::string& addr,
        const std::string& consumerGroup,
        int timeoutMillis)
{
    std::string consumerGroupWithProjectGroup = consumerGroup;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        consumerGroupWithProjectGroup =
            VirtualEnvUtil::buildWithProjectGroup(consumerGroup, m_projectGroupPrefix);
    }

    GetConsumerListByGroupRequestHeader* requestHeader = new GetConsumerListByGroupRequestHeader();
    requestHeader->consumerGroup = consumerGroupWithProjectGroup;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(GET_CONSUMER_LIST_BY_GROUP_VALUE, requestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                if (response->getBody() != NULL)
                {
                    GetConsumerListByGroupResponseBody* body =
                        GetConsumerListByGroupResponseBody::decode((char*)response->getBody(), response->getBodyLen());
                    std::list<std::string> ret = body->getConsumerIdList();
                    delete body;
                    return ret;
                }
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "getConsumerIdListByGroup failed", -1);
}

long long MQClientAPIImpl::getMinOffset(const std::string& addr,
                                        const std::string& topic,
                                        int queueId,
                                        int timeoutMillis)
{
    std::string topicWithProjectGroup = topic;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup = VirtualEnvUtil::buildWithProjectGroup(topic, m_projectGroupPrefix);
    }

	GetMinOffsetRequestHeader* pRequestHeader = new GetMinOffsetRequestHeader();
    pRequestHeader->topic = topicWithProjectGroup;
    pRequestHeader->queueId = queueId;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(GET_MIN_OFFSET_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            GetMinOffsetResponseHeader* ret = (GetMinOffsetResponseHeader*)response->getCommandCustomHeader();
	            return ret->offset;
	        }
	        default:
	            break;
	    }
	    //THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
	}

    //THROW_MQEXCEPTION(MQClientException, "getMinOffset failed", -1);
    return -1;
}

long long MQClientAPIImpl::getEarliestMsgStoretime(const std::string& addr,
        const std::string& topic,
        int queueId,
        int timeoutMillis)
{
    std::string topicWithProjectGroup = topic;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup = VirtualEnvUtil::buildWithProjectGroup(topic, m_projectGroupPrefix);
    }

	GetEarliestMsgStoretimeRequestHeader* pRequestHeader = new GetEarliestMsgStoretimeRequestHeader();
    pRequestHeader->topic = topicWithProjectGroup;
    pRequestHeader->queueId = queueId;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(GET_EARLIEST_MSG_STORETIME_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            GetEarliestMsgStoretimeResponseHeader* ret = (GetEarliestMsgStoretimeResponseHeader*)response->getCommandCustomHeader();
	            return ret->timestamp;
	        }
	        default:
	            break;
	    }
	    THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
	}

    THROW_MQEXCEPTION(MQClientException, "getEarliestMsgStoretime failed", -1);
}

long long MQClientAPIImpl::queryConsumerOffset(const std::string& addr,
        QueryConsumerOffsetRequestHeader* pRequestHeader,
        int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestHeader->consumerGroup = VirtualEnvUtil::buildWithProjectGroup(
                                            pRequestHeader->consumerGroup, m_projectGroupPrefix);
        pRequestHeader->topic = VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->topic,
                                m_projectGroupPrefix);
    }

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(QUERY_CONSUMER_OFFSET_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            QueryConsumerOffsetResponseHeader* ret = (QueryConsumerOffsetResponseHeader*)response->getCommandCustomHeader();
	            long long offset = ret->offset;
	            return offset;
	        }
	        default:
	            break;
	    }
	    THROW_MQEXCEPTION(MQBrokerException, response->getRemark(), response->getCode());
	}

    THROW_MQEXCEPTION(MQClientException, "queryConsumerOffset failed", -1);
    return -1;
}

void MQClientAPIImpl::updateConsumerOffset(const std::string& addr,
        UpdateConsumerOffsetRequestHeader* pRequestHeader,
        int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestHeader->consumerGroup = VirtualEnvUtil::buildWithProjectGroup(
                                            pRequestHeader->consumerGroup, m_projectGroupPrefix);
        pRequestHeader->topic = VirtualEnvUtil::buildWithProjectGroup(
                                    pRequestHeader->topic, m_projectGroupPrefix);
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(UPDATE_CONSUMER_OFFSET_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                return;
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "updateConsumerOffset failed", -1);
}

void MQClientAPIImpl::updateConsumerOffsetOneway(const std::string& addr,
        UpdateConsumerOffsetRequestHeader* pRequestHeader,
        int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestHeader->consumerGroup = VirtualEnvUtil::buildWithProjectGroup(
                                            pRequestHeader->consumerGroup, m_projectGroupPrefix);
        pRequestHeader->topic = VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->topic,
                                m_projectGroupPrefix);
    }

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(UPDATE_CONSUMER_OFFSET_VALUE, pRequestHeader);

    m_pRemotingClient->invokeOneway(addr, request, timeoutMillis);
}

void MQClientAPIImpl::sendHearbeat(const std::string& addr, HeartbeatData* pHeartbeatData, int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        std::set<ConsumerData>& consumerDatas = pHeartbeatData->getConsumerDataSet();
        std::set<ConsumerData>::iterator it = consumerDatas.begin();
        for (; it != consumerDatas.end(); it++)
        {
            ConsumerData& consumerData = (ConsumerData&)(*it);
            consumerData.groupName = VirtualEnvUtil::buildWithProjectGroup(consumerData.groupName,
                                     m_projectGroupPrefix);

            std::set<SubscriptionData>& subscriptionDatas = consumerData.subscriptionDataSet;
            std::set<SubscriptionData>::iterator itsub = subscriptionDatas.begin();
            for (; itsub != subscriptionDatas.end(); itsub++)
            {
                SubscriptionData& subscriptionData = (SubscriptionData&)(*itsub);
                subscriptionData.setTopic(VirtualEnvUtil::buildWithProjectGroup(
                                              subscriptionData.getTopic(), m_projectGroupPrefix));
            }
        }

        std::set<ProducerData>& producerDatas = pHeartbeatData->getProducerDataSet();
        std::set<ProducerData>::iterator itp = producerDatas.begin();
        for (; itp != producerDatas.end(); itp++)
        {
            ProducerData& producerData = (ProducerData&)(*itp);
            producerData.groupName = VirtualEnvUtil::buildWithProjectGroup(producerData.groupName,
                                     m_projectGroupPrefix);
        }
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(HEART_BEAT_VALUE, NULL);

    std::string body;
    pHeartbeatData->encode(body);
    request->setBody((char*)body.data(), body.length(), true);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                return;
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "sendHearbeat failed", -1);
}

void MQClientAPIImpl::unregisterClient(const std::string& addr,
                                       const std::string& clientID,
                                       const std::string& producerGroup,
                                       const std::string& consumerGroup,
                                       int timeoutMillis)
{
    std::string producerGroupWithProjectGroup = producerGroup;
    std::string consumerGroupWithProjectGroup = consumerGroup;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        producerGroupWithProjectGroup =
            VirtualEnvUtil::buildWithProjectGroup(producerGroup, m_projectGroupPrefix);
        consumerGroupWithProjectGroup =
            VirtualEnvUtil::buildWithProjectGroup(consumerGroup, m_projectGroupPrefix);
    }

    UnregisterClientRequestHeader* requestHeader = new UnregisterClientRequestHeader();
    requestHeader->clientID = (clientID);
    requestHeader->producerGroup = (producerGroupWithProjectGroup);
    requestHeader->consumerGroup = (consumerGroupWithProjectGroup);

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(UNREGISTER_CLIENT_VALUE, requestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
                return;
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "unregisterClient failed", -1);
}

void MQClientAPIImpl::endTransactionOneway(const std::string& addr,
        EndTransactionRequestHeader* pRequestHeader,
        const std::string& remark,
        int timeoutMillis)
{
    //TODO
}

void MQClientAPIImpl::queryMessage(const std::string& addr,
                                   QueryMessageRequestHeader* pRequestHeader,
                                   int timeoutMillis,
                                   InvokeCallback* pInvokeCallback)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestHeader->topic = VirtualEnvUtil::buildWithProjectGroup(pRequestHeader->topic,
        	m_projectGroupPrefix);
    }

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(QUERY_MESSAGE_VALUE, pRequestHeader);

    m_pRemotingClient->invokeAsync(addr, request, timeoutMillis, pInvokeCallback);
    return;
}

bool MQClientAPIImpl::registerClient(const std::string& addr, HeartbeatData& heartbeat, int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        std::set<ConsumerData>& consumerDatas = heartbeat.getConsumerDataSet();
        std::set<ConsumerData>::iterator it = consumerDatas.begin();

        for (; it != consumerDatas.end(); it++)
        {
            ConsumerData& consumerData = (ConsumerData&)(*it);

            consumerData.groupName = VirtualEnvUtil::buildWithProjectGroup(consumerData.groupName,
                                     m_projectGroupPrefix);
            std::set<SubscriptionData>& subscriptionDatas = consumerData.subscriptionDataSet;
            std::set<SubscriptionData>::iterator itsub = subscriptionDatas.begin();

            for (; itsub != subscriptionDatas.end(); itsub++)
            {
                SubscriptionData& subscriptionData = (SubscriptionData&)(*itsub);
                subscriptionData.setTopic(VirtualEnvUtil::buildWithProjectGroup(
                                              subscriptionData.getTopic(), m_projectGroupPrefix));
            }
        }

        std::set<ProducerData>& producerDatas = heartbeat.getProducerDataSet();
        std::set<ProducerData>::iterator itp = producerDatas.begin();
        for (; itp != producerDatas.end(); itp++)
        {
            ProducerData& producerData = (ProducerData&)(*itp);
            producerData.groupName = VirtualEnvUtil::buildWithProjectGroup(producerData.groupName,
                                     m_projectGroupPrefix);
        }
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(HEART_BEAT_VALUE, NULL);

    std::string body;
    heartbeat.encode(body);

    request->setBody((char*)body.data(), body.length(), true);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    return (response && response->getCode() == SUCCESS_VALUE);
}

void MQClientAPIImpl::consumerSendMessageBack(
		const std::string& addr,
		MessageExt& msg,
        const std::string& consumerGroup,
        int delayLevel,
        int timeoutMillis)
{
    std::string consumerGroupWithProjectGroup = consumerGroup;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        consumerGroupWithProjectGroup =
            VirtualEnvUtil::buildWithProjectGroup(consumerGroup, m_projectGroupPrefix);
        msg.setTopic(VirtualEnvUtil::buildWithProjectGroup(msg.getTopic(), m_projectGroupPrefix));
    }

    ConsumerSendMsgBackRequestHeader* requestHeader = new ConsumerSendMsgBackRequestHeader();
    requestHeader->group = consumerGroupWithProjectGroup;
    requestHeader->offset = msg.getCommitLogOffset();
    requestHeader->delayLevel = delayLevel;

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(CONSUMER_SEND_MSG_BACK_VALUE, requestHeader);

	std::string brokerAddr = addr.empty() ? socketAddress2IPPort(msg.getStoreHost()) : addr;
    RemotingCommandPtr response = m_pRemotingClient->invokeSync(brokerAddr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
                return;
                break;
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "consumerSendMessageBack failed", -1);
}

std::set<MessageQueue> MQClientAPIImpl::lockBatchMQ(const std::string& addr,
        LockBatchRequestBody* pRequestBody,
        int timeoutMillis)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestBody->setConsumerGroup((VirtualEnvUtil::buildWithProjectGroup(
                                            pRequestBody->getConsumerGroup(), m_projectGroupPrefix)));
        std::set<MessageQueue>& messageQueues = pRequestBody->getMqSet();
        std::set<MessageQueue>::iterator it = messageQueues.begin();

        for (; it != messageQueues.end(); it++)
        {
            MessageQueue& messageQueue = (MessageQueue&)(*it);
            messageQueue.setTopic(VirtualEnvUtil::buildWithProjectGroup(messageQueue.getTopic(),
                                  m_projectGroupPrefix));
        }
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(LOCK_BATCH_MQ_VALUE, NULL);

    std::string body;
    pRequestBody->encode(body);
    request->setBody((char*)body.data(), body.length(), true);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                LockBatchResponseBody* responseBody =
                    LockBatchResponseBody::decode(response->getBody(), response->getBodyLen());
                std::set<MessageQueue> messageQueues = responseBody->getLockOKMQSet();

                if (!UtilAll::isBlank(m_projectGroupPrefix))
                {
                    std::set<MessageQueue>::iterator it = messageQueues.begin();

                    for (; it != messageQueues.end(); it++)
                    {
                        MessageQueue& messageQueue = (MessageQueue&)(*it);
                        messageQueue.setTopic(VirtualEnvUtil::clearProjectGroup(messageQueue.getTopic(),
                                              m_projectGroupPrefix));
                    }
                }
                return messageQueues;
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    THROW_MQEXCEPTION(MQClientException, "lockBatchMQ failed", -1);
}

void MQClientAPIImpl::unlockBatchMQ(const std::string& addr,
                                    UnlockBatchRequestBody* pRequestBody,
                                    int timeoutMillis,
                                    bool oneway)
{
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        pRequestBody->setConsumerGroup((VirtualEnvUtil::buildWithProjectGroup(
                                            pRequestBody->getConsumerGroup(), m_projectGroupPrefix)));
        std::set<MessageQueue>& messageQueues = pRequestBody->getMqSet();
        std::set<MessageQueue>::iterator it = messageQueues.begin();

        for (; it != messageQueues.end(); it++)
        {
            MessageQueue& messageQueue = (MessageQueue&)(*it);
            messageQueue.setTopic(VirtualEnvUtil::buildWithProjectGroup(messageQueue.getTopic(),
                                  m_projectGroupPrefix));
        }
    }

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(UNLOCK_BATCH_MQ_VALUE, NULL);

    std::string body;
    pRequestBody->encode(body);
    request->setBody((char*)body.data(), body.length(), true);

    if (oneway)
    {
        m_pRemotingClient->invokeOneway(addr, request, timeoutMillis);
    }
    else
    {
        RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
        if (response)
        {
            switch (response->getCode())
            {
                case SUCCESS_VALUE:
                    return;
                default:
                    break;
            }

            THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
        }

        THROW_MQEXCEPTION(MQClientException, "unlockBatchMQ failed", -1);
    }
}

TopicStatsTable MQClientAPIImpl::getTopicStatsInfo(const std::string& addr,
        const std::string& topic,
        int timeoutMillis)
{
    //TODO
    TopicStatsTable t;
    return t;
}

ConsumeStats MQClientAPIImpl::getConsumeStats(const std::string& addr,
        const std::string& consumerGroup,
        int timeoutMillis)
{
    //TODO
    ConsumeStats cs;
    return cs;
}

ProducerConnection* MQClientAPIImpl::getProducerConnectionList(const std::string& addr,
        const std::string& producerGroup,
        int timeoutMillis)
{
    //TODO
    return NULL;
}

ConsumerConnection* MQClientAPIImpl::getConsumerConnectionList(const std::string& addr,
        const std::string& consumerGroup,
        int timeoutMillis)
{
    //TODO
    return NULL;
}

KVTable MQClientAPIImpl::getBrokerRuntimeInfo(const std::string& addr,  int timeoutMillis)
{
    //TODO
    KVTable kv;
    return kv;
}

void MQClientAPIImpl::updateBrokerConfig(const std::string& addr,
        const std::map<std::string, std::string>&  properties,
        int timeoutMillis)
{
    //TODO
}

ClusterInfo* MQClientAPIImpl::getBrokerClusterInfo(int timeoutMillis)
{
   //TODO
    return NULL;
}

TopicRouteData* MQClientAPIImpl::getDefaultTopicRouteInfoFromNameServer(const std::string& topic,
        int timeoutMillis)
{
    GetRouteInfoRequestHeader* requestHeader = new GetRouteInfoRequestHeader();
    requestHeader->topic = topic;

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(GET_ROUTEINTO_BY_TOPIC_VALUE, requestHeader);
    RemotingCommandPtr response = m_pRemotingClient->invokeSync("", request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case TOPIC_NOT_EXIST_VALUE:
            {
				// TODO LOG
                break;
            }
            case SUCCESS_VALUE:
            {
                int bodyLen = response->getBodyLen();
                const char* body = response->getBody();
                if (body)
                {
                    TopicRouteData* ret = TopicRouteData::encode(body, bodyLen);
                    return ret;
                }
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    return NULL;
}

TopicRouteData* MQClientAPIImpl::getTopicRouteInfoFromNameServer(const std::string& topic, int timeoutMillis)
{
    std::string topicWithProjectGroup = topic;
    if (!UtilAll::isBlank(m_projectGroupPrefix))
    {
        topicWithProjectGroup = VirtualEnvUtil::buildWithProjectGroup(topic, m_projectGroupPrefix);
    }

    GetRouteInfoRequestHeader* requestHeader = new GetRouteInfoRequestHeader();
    requestHeader->topic = topicWithProjectGroup;

    RemotingCommandPtr request = RemotingCommand::createRequestCommand(GET_ROUTEINTO_BY_TOPIC_VALUE, requestHeader);
    RemotingCommandPtr response = m_pRemotingClient->invokeSync("", request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case TOPIC_NOT_EXIST_VALUE:
            {
            	if (topic != MixAll::DEFAULT_TOPIC)
            	{
            		RMQ_WARN("get Topic [{%s}] RouteInfoFromNameServer is not exist value", topic.c_str());
            	}
                break;
            }
            case SUCCESS_VALUE:
            {
                int bodyLen = response->getBodyLen();
                const char* body = response->getBody();
                if (body)
                {
                    TopicRouteData* ret = TopicRouteData::encode(body, bodyLen);
                    return ret;
                }
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    return NULL;
}

TopicList* MQClientAPIImpl::getTopicListFromNameServer(int timeoutMillis)
{
    RemotingCommandPtr request = RemotingCommand::createRequestCommand(GET_ALL_TOPIC_LIST_FROM_NAMESERVER_VALUE, NULL);
    RemotingCommandPtr response = m_pRemotingClient->invokeSync("", request, timeoutMillis);
    if (response)
    {
        switch (response->getCode())
        {
            case SUCCESS_VALUE:
            {
                char* body = (char*)response->getBody();
                if (body != NULL)
                {
                    TopicList* topicList = TopicList::decode(body, response->getBodyLen());

                    if (!UtilAll::isBlank(m_projectGroupPrefix))
                    {
                        std::set<std::string> newTopicSet;

                        const std::set<std::string>& topics = topicList->getTopicList();
                        std::set<std::string>::const_iterator it = topics.begin();
                        for (; it != topics.end(); it++)
                        {
                            std::string topic = *it;
                            newTopicSet.insert(VirtualEnvUtil::clearProjectGroup(topic, m_projectGroupPrefix));
                        }

                        topicList->setTopicList(newTopicSet);
                    }

                    return topicList;
                }
            }
            default:
                break;
        }

        THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
    }

    return NULL;
}

int MQClientAPIImpl::wipeWritePermOfBroker(const std::string& namesrvAddr,
        const std::string& brokerName,
        int timeoutMillis)
{
    //TODO
    return 0;
}

void MQClientAPIImpl::deleteTopicInBroker(const std::string& addr,
        const std::string& topic,
        int timeoutMillis)
{
    //TODO
}

void MQClientAPIImpl::deleteTopicInNameServer(const std::string& addr,
        const std::string& topic,
        int timeoutMillis)
{
    //TODO
}

void MQClientAPIImpl::deleteSubscriptionGroup(const std::string& addr,
        const std::string& groupName,
        int timeoutMillis)
{
    //TODO
}

std::string MQClientAPIImpl::getKVConfigValue(const std::string& projectNamespace,
        const std::string& key,
        int timeoutMillis)
{
	GetKVConfigRequestHeader* pRequestHeader = new GetKVConfigRequestHeader();
    pRequestHeader->namespace_ = projectNamespace;
    pRequestHeader->key = key;

    RemotingCommandPtr request =
        RemotingCommand::createRequestCommand(GET_KV_CONFIG_VALUE, pRequestHeader);

    RemotingCommandPtr response = m_pRemotingClient->invokeSync("", request, timeoutMillis);
    if (response)
    {
	    switch (response->getCode())
	    {
	        case SUCCESS_VALUE:
	        {
	            GetKVConfigResponseHeader* ret = (GetKVConfigResponseHeader*)response->getCommandCustomHeader();
	            return ret->value;
	        }
	        default:
	            break;
	    }
	    THROW_MQEXCEPTION(MQClientException, response->getRemark(), response->getCode());
	}

    THROW_MQEXCEPTION(MQClientException, "getKVConfigValue failed", -1);
}

void MQClientAPIImpl::putKVConfigValue(const std::string& projectNamespace,
                                       const std::string& key,
                                       const std::string& value,
                                       int timeoutMillis)
{
    //TODO
}

void MQClientAPIImpl::deleteKVConfigValue(const std::string& projectNamespace,
        const std::string& key,
        int timeoutMillis)
{
    //TODO
}

std::string MQClientAPIImpl::getProjectGroupByIp(const std::string& ip,  int timeoutMillis)
{
    return getKVConfigValue(NamesrvUtil::NAMESPACE_PROJECT_CONFIG, ip, timeoutMillis);
}

std::string MQClientAPIImpl::getKVConfigByValue(const std::string& projectNamespace,
        const std::string& projectGroup,
        int timeoutMillis)
{
    //TODO
    return "";
}

KVTable MQClientAPIImpl::getKVListByNamespace(const std::string& projectNamespace,  int timeoutMillis)
{
    //TODO
    return KVTable();
}

void MQClientAPIImpl::deleteKVConfigByValue(const std::string& projectNamespace,
        const std::string& projectGroup,
        int timeoutMillis)
{
    //TODO
}

SendResult* MQClientAPIImpl::sendMessageSync(const std::string& addr,
        const std::string& brokerName,
        Message& msg,
        int timeoutMillis,
        RemotingCommand* request)
{
    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, request, timeoutMillis);
    return processSendResponse(brokerName, msg.getTopic(), response);
}

void MQClientAPIImpl::sendMessageAsync(const std::string& addr,
                                       const std::string& brokerName,
                                       Message& msg,
                                       int timeoutMillis,
                                       RemotingCommand* request,
                                       SendCallback* pSendCallback)
{
    ProducerInvokeCallback* callback = new ProducerInvokeCallback(pSendCallback, this, msg.getTopic(), brokerName);
    m_pRemotingClient->invokeAsync(addr, request, timeoutMillis, callback);
}

SendResult* MQClientAPIImpl::processSendResponse(const std::string& brokerName,
        const std::string& topic,
        RemotingCommand* pResponse)
{
    if (pResponse == NULL)
    {
        return NULL;
    }

    switch (pResponse->getCode())
    {
        case FLUSH_DISK_TIMEOUT_VALUE:
        case FLUSH_SLAVE_TIMEOUT_VALUE:
        case SLAVE_NOT_AVAILABLE_VALUE:
        {
            // TODO LOG
        }
        case SUCCESS_VALUE:
        {
            SendStatus sendStatus = SEND_OK;
            switch (pResponse->getCode())
            {
                case FLUSH_DISK_TIMEOUT_VALUE:
                    sendStatus = FLUSH_DISK_TIMEOUT;
                    break;
                case FLUSH_SLAVE_TIMEOUT_VALUE:
                    sendStatus = FLUSH_SLAVE_TIMEOUT;
                    break;
                case SLAVE_NOT_AVAILABLE_VALUE:
                    sendStatus = SLAVE_NOT_AVAILABLE;
                    break;
                case SUCCESS_VALUE:
                    sendStatus = SEND_OK;
                    break;
                default:
                    //assert false;
                    break;
            }

            SendMessageResponseHeader* responseHeader = (SendMessageResponseHeader*)pResponse->getCommandCustomHeader();
            MessageQueue messageQueue(topic, brokerName, responseHeader->queueId);
            SendResult* ret = new SendResult(sendStatus, responseHeader->msgId, messageQueue,
                                             responseHeader->queueOffset, m_projectGroupPrefix);

            return ret;
        }
        default:
            break;
    }

    THROW_MQEXCEPTION(MQClientException, pResponse->getRemark(), pResponse->getCode());
}

void MQClientAPIImpl::pullMessageAsync(const std::string& addr,
                                       RemotingCommand* pRequest,
                                       int timeoutMillis,
                                       PullCallback* pPullCallback)
{
    ConsumerInvokeCallback* callback = new ConsumerInvokeCallback(pPullCallback, this);
    m_pRemotingClient->invokeAsync(addr, pRequest, timeoutMillis, callback);
}

PullResult* MQClientAPIImpl::processPullResponse(RemotingCommand* pResponse)
{
    PullStatus pullStatus = NO_NEW_MSG;
    switch (pResponse->getCode())
    {
        case SUCCESS_VALUE:
            pullStatus = FOUND;
            break;
        case PULL_NOT_FOUND_VALUE:
            pullStatus = NO_NEW_MSG;
            break;
        case PULL_RETRY_IMMEDIATELY_VALUE:
            pullStatus = NO_MATCHED_MSG;
            break;
        case PULL_OFFSET_MOVED_VALUE:
            pullStatus = OFFSET_ILLEGAL;
            break;
        default:
            THROW_MQEXCEPTION(MQBrokerException, pResponse->getRemark(), pResponse->getCode());
            break;
    }

    PullMessageResponseHeader* responseHeader = (PullMessageResponseHeader*) pResponse->getCommandCustomHeader();
    std::list<MessageExt*> msgFoundList;
    return new PullResultExt(pullStatus, responseHeader->nextBeginOffset,
                             responseHeader->minOffset, responseHeader->maxOffset, msgFoundList,
                             responseHeader->suggestWhichBrokerId, pResponse->getBody(), pResponse->getBodyLen());
}

PullResult* MQClientAPIImpl::pullMessageSync(const std::string& addr,
        RemotingCommand* pRequest,
        int timeoutMillis)
{
    RemotingCommandPtr response = m_pRemotingClient->invokeSync(addr, pRequest, timeoutMillis);
    PullResult* result = processPullResponse(response);

    response->setBody(NULL, 0, false);
    return result;
}

}
