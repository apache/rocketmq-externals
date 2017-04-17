/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License")
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
#include "DefaultMQProducerImpl.h"
#include "DefaultMQProducer.h"
#include "MessageExt.h"
#include "QueryResult.h"
#include "TopicPublishInfo.h"
#include "MQClientException.h"
#include "LocalTransactionExecuter.h"
#include "SendMessageHook.h"
#include "MQClientManager.h"
#include "MQClientFactory.h"
#include "Validators.h"
#include "MQAdminImpl.h"
#include "MQClientAPIImpl.h"
#include "MessageSysFlag.h"
#include "CommandCustomHeader.h"
#include "KPRUtil.h"
#include "MessageDecoder.h"
#include "MessageQueueSelector.h"
#include "MQProtos.h"
#include "RemotingCommand.h"
#include "UtilAll.h"



namespace rmq
{

DefaultMQProducerImpl::DefaultMQProducerImpl(DefaultMQProducer
        *pDefaultMQProducer)
    : m_pDefaultMQProducer(pDefaultMQProducer),
      m_serviceState(CREATE_JUST),
      m_pMQClientFactory(NULL)
{
}

DefaultMQProducerImpl::~DefaultMQProducerImpl()
{
	//delete m_pMQClientFactory;
}


void DefaultMQProducerImpl::start()
{
    start(true);
}

void DefaultMQProducerImpl::start(bool startFactory)
{
    RMQ_DEBUG("DefaultMQProducerImpl::start()");

    switch (m_serviceState)
    {
        case CREATE_JUST:
        {
        	RMQ_INFO("the producer [{%s}] start beginning.",
                	m_pDefaultMQProducer->getProducerGroup().c_str());

            m_serviceState = START_FAILED;
            checkConfig();

            if (m_pDefaultMQProducer->getProducerGroup() !=
                MixAll::CLIENT_INNER_PRODUCER_GROUP)
            {
                m_pDefaultMQProducer->changeInstanceNameToPID();
            }

            m_pMQClientFactory =
                MQClientManager::getInstance()->getAndCreateMQClientFactory(
                    *m_pDefaultMQProducer);
            bool registerOK = m_pMQClientFactory->registerProducer(
                                  m_pDefaultMQProducer->getProducerGroup(), this);

            if (!registerOK)
            {
                m_serviceState = CREATE_JUST;
                THROW_MQEXCEPTION(MQClientException,
                                  "The producer group[" + m_pDefaultMQProducer->getProducerGroup()
                                  + "] has been created before, specify another name please.", -1);
            }

            m_topicPublishInfoTable[m_pDefaultMQProducer->getCreateTopicKey()] =
                TopicPublishInfo();

            if (startFactory)
            {
                m_pMQClientFactory->start();
            }

			RMQ_INFO("the producer [%s] start OK", m_pDefaultMQProducer->getProducerGroup().c_str());
            m_serviceState = RUNNING;
        }
        break;

        case RUNNING:
            RMQ_ERROR("This client is already running.");

        case START_FAILED:
            RMQ_ERROR("This client failed to start previously.");

        case SHUTDOWN_ALREADY:
            RMQ_ERROR("This client has been shutted down.");
            THROW_MQEXCEPTION(MQClientException,
                              "The producer service state not OK, maybe started once, ", -1);

        default:
            break;
    }

    m_pMQClientFactory->sendHeartbeatToAllBrokerWithLock();
}

void DefaultMQProducerImpl::shutdown()
{
    shutdown(true);
}

void DefaultMQProducerImpl::shutdown(bool shutdownFactory)
{
    RMQ_DEBUG("DefaultMQProducerImpl::shutdown()");

    switch (m_serviceState)
    {
        case CREATE_JUST:
            break;

        case RUNNING:
            m_pMQClientFactory->unregisterProducer(
                m_pDefaultMQProducer->getProducerGroup());

            if (shutdownFactory)
            {
                m_pMQClientFactory->shutdown();
            }

			RMQ_INFO("the producer [%s] shutdown OK", m_pDefaultMQProducer->getProducerGroup().c_str());
            m_serviceState = SHUTDOWN_ALREADY;
            break;

        case SHUTDOWN_ALREADY:
            break;

        default:
            break;
    }
}


void DefaultMQProducerImpl::initTransactionEnv()
{
    //TODO
}

void DefaultMQProducerImpl::destroyTransactionEnv()
{
    //TODO
}

bool DefaultMQProducerImpl::hasHook()
{
    return !m_hookList.empty();
}

void DefaultMQProducerImpl::registerHook(SendMessageHook* pHook)
{
    m_hookList.push_back(pHook);
}

void DefaultMQProducerImpl::executeHookBefore(const SendMessageContext& context)
{
    std::list<SendMessageHook*>::iterator it = m_hookList.begin();

    for (; it != m_hookList.end(); it++)
    {
        try
        {
            (*it)->sendMessageBefore(context);
        }
        catch (...)
        {
        	RMQ_WARN("sendMessageBefore exception");
        }
    }
}

void DefaultMQProducerImpl::executeHookAfter(const SendMessageContext& context)
{
    std::list<SendMessageHook*>::iterator it = m_hookList.begin();

    for (; it != m_hookList.end(); it++)
    {
        try
        {
            (*it)->sendMessageAfter(context);
        }
        catch (...)
        {
        	RMQ_WARN("sendMessageAfter exception");
        }
    }
}


std::set<std::string> DefaultMQProducerImpl::getPublishTopicList()
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
    std::set<std::string> toplist;
    std::map<std::string, TopicPublishInfo>::iterator it =
        m_topicPublishInfoTable.begin();
    for (; it != m_topicPublishInfoTable.end(); it++)
    {
        toplist.insert(it->first);
    }

    return toplist;
}

bool DefaultMQProducerImpl::isPublishTopicNeedUpdate(const std::string& topic)
{
	kpr::ScopedRLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
    std::map<std::string, TopicPublishInfo>::iterator it =
        m_topicPublishInfoTable.find(topic);
    if (it != m_topicPublishInfoTable.end())
    {
        return !it->second.ok();
    }

    return true;
}

void DefaultMQProducerImpl::checkTransactionState(const std::string& addr, //
        const MessageExt& msg, //
        const CheckTransactionStateRequestHeader& checkRequestHeader)
{
    //TODO
}

void DefaultMQProducerImpl::updateTopicPublishInfo(const std::string& topic,
        TopicPublishInfo& info)
{
	{
		kpr::ScopedWLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
	    std::map<std::string, TopicPublishInfo>::iterator it =
	        m_topicPublishInfoTable.find(topic);
	    if (it != m_topicPublishInfoTable.end())
	    {
	        info.getSendWhichQueue() = it->second.getSendWhichQueue();
	        RMQ_INFO("updateTopicPublishInfo prev is not null, %s", it->second.toString().c_str());
	    }
	    m_topicPublishInfoTable[topic] = info;
    }
}

void DefaultMQProducerImpl::createTopic(const std::string& key,
                                        const std::string& newTopic, int queueNum)
{
    makeSureStateOK();
    Validators::checkTopic(newTopic);

    m_pMQClientFactory->getMQAdminImpl()->createTopic(key, newTopic, queueNum);
}

std::vector<MessageQueue>* DefaultMQProducerImpl::fetchPublishMessageQueues(
    const std::string& topic)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->fetchPublishMessageQueues(topic);
}

long long DefaultMQProducerImpl::searchOffset(const MessageQueue& mq,
        long long timestamp)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->searchOffset(mq, timestamp);
}

long long DefaultMQProducerImpl::maxOffset(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->maxOffset(mq);
}

long long DefaultMQProducerImpl::minOffset(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->minOffset(mq);
}

long long DefaultMQProducerImpl::earliestMsgStoreTime(const MessageQueue& mq)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->earliestMsgStoreTime(mq);
}

MessageExt* DefaultMQProducerImpl::viewMessage(const std::string& msgId)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->viewMessage(msgId);
}

QueryResult DefaultMQProducerImpl::queryMessage(const std::string& topic,
        const std::string& key, int maxNum, long long begin, long long end)
{
    makeSureStateOK();
    return m_pMQClientFactory->getMQAdminImpl()->queryMessage(topic, key, maxNum,
            begin, end);
}


/**
 * DEFAULT ASYNC -------------------------------------------------------
 */
void DefaultMQProducerImpl::send(Message& msg, SendCallback* pSendCallback)
{
    send(msg, pSendCallback, m_pDefaultMQProducer->getSendMsgTimeout());
}
void DefaultMQProducerImpl::send(Message& msg, SendCallback* pSendCallback, int timeout)
{
    try
    {
        sendDefaultImpl(msg, ASYNC, pSendCallback, timeout);
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}


/**
 * DEFAULT ONEWAY -------------------------------------------------------
 */
void DefaultMQProducerImpl::sendOneway(Message& msg)
{
    try
    {
        sendDefaultImpl(msg, ONEWAY, NULL, m_pDefaultMQProducer->getSendMsgTimeout());
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}


/**
 * KERNEL SYNC -------------------------------------------------------
 */
SendResult DefaultMQProducerImpl::send(Message& msg, MessageQueue& mq)
{
    return send(msg, mq, m_pDefaultMQProducer->getSendMsgTimeout());
}
SendResult DefaultMQProducerImpl::send(Message& msg, MessageQueue& mq, int timeout)
{
    makeSureStateOK();
    Validators::checkMessage(msg, m_pDefaultMQProducer);

    if (msg.getTopic() != mq.getTopic())
    {
        THROW_MQEXCEPTION(MQClientException, "message's topic not equal mq's topic", -1);
    }

    return sendKernelImpl(msg, mq, SYNC, NULL, timeout);
}


/**
 * KERNEL ASYNC -------------------------------------------------------
 */
void DefaultMQProducerImpl::send(Message& msg, MessageQueue& mq,
                                 SendCallback* pSendCallback)
{
    return send(msg, mq, pSendCallback, m_pDefaultMQProducer->getSendMsgTimeout());
}
void DefaultMQProducerImpl::send(Message& msg, MessageQueue& mq,
	SendCallback* pSendCallback, int timeout)
{
    makeSureStateOK();
    Validators::checkMessage(msg, m_pDefaultMQProducer);

    if (msg.getTopic() != mq.getTopic())
    {
        THROW_MQEXCEPTION(MQClientException, "message's topic not equal mq's topic", -1);
    }

    try
    {
        sendKernelImpl(msg, mq, ASYNC, pSendCallback, timeout);
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}

/**
 * KERNEL ONEWAY -------------------------------------------------------
 */
void DefaultMQProducerImpl::sendOneway(Message& msg, MessageQueue& mq)
{
    makeSureStateOK();
    Validators::checkMessage(msg, m_pDefaultMQProducer);

    if (msg.getTopic() != mq.getTopic())
    {
        THROW_MQEXCEPTION(MQClientException, "message's topic not equal mq's topic", -1);
    }

    try
    {
        sendKernelImpl(msg, mq, ONEWAY, NULL, m_pDefaultMQProducer->getSendMsgTimeout());
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}


/**
 * SELECT SYNC -------------------------------------------------------
 */
SendResult DefaultMQProducerImpl::send(Message& msg,
                                       MessageQueueSelector* pSelector, void* arg)
{
    return send(msg, pSelector, arg, m_pDefaultMQProducer->getSendMsgTimeout());
}
SendResult DefaultMQProducerImpl::send(Message& msg,
                                       MessageQueueSelector* pSelector, void* arg, int timeout)
{
    return sendSelectImpl(msg, pSelector, arg, SYNC, NULL, timeout);
}


/**
 * SELECT ASYNC -------------------------------------------------------
 */
void DefaultMQProducerImpl::send(Message& msg,
                                 MessageQueueSelector* pSelector,
                                 void* arg,
                                 SendCallback* pSendCallback)
{
    return send(msg, pSelector, arg, pSendCallback, m_pDefaultMQProducer->getSendMsgTimeout());
}
void DefaultMQProducerImpl::send(Message& msg,
                                 MessageQueueSelector* pSelector,
                                 void* arg,
                                 SendCallback* pSendCallback,
                                 int timeout)
{
    try
    {
        sendSelectImpl(msg, pSelector, arg, ASYNC, pSendCallback, timeout);
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}


/**
 * SELECT ONEWAY -------------------------------------------------------
 */
void DefaultMQProducerImpl::sendOneway(Message& msg,
	MessageQueueSelector* pSelector, void* arg)
{
    try
    {
        sendSelectImpl(msg, pSelector, arg, ONEWAY, NULL,
        	m_pDefaultMQProducer->getSendMsgTimeout());
    }
    catch (MQBrokerException& e)
    {
        THROW_MQEXCEPTION(MQClientException, std::string("unknow exception: ") + e.what(), -1);
    }
}


/*
 * Send with Transaction
 */
TransactionSendResult DefaultMQProducerImpl::sendMessageInTransaction(
    Message& msg,
    LocalTransactionExecuter* tranExecuter, void* arg)
{
    //TODO
    TransactionSendResult result;
    return result;
}

void DefaultMQProducerImpl::endTransaction(//
    SendResult sendResult, //
    LocalTransactionState localTransactionState, //
    MQClientException localException)
{
    //TODO
}

/**
 * DEFAULT SYNC -------------------------------------------------------
 */
SendResult DefaultMQProducerImpl::send(Message& msg)
{
    return send(msg, m_pDefaultMQProducer->getSendMsgTimeout());
}
SendResult DefaultMQProducerImpl::send(Message& msg, int timeout)
{
    return sendDefaultImpl(msg, SYNC, NULL, timeout);
}


std::map<std::string, TopicPublishInfo> DefaultMQProducerImpl::getTopicPublishInfoTable()
{
    return m_topicPublishInfoTable;
}

MQClientFactory* DefaultMQProducerImpl::getMQClientFactory()
{
    return m_pMQClientFactory;
}

int DefaultMQProducerImpl::getZipCompressLevel()
{
    return m_zipCompressLevel;
}

void DefaultMQProducerImpl::setZipCompressLevel(int zipCompressLevel)
{
    m_zipCompressLevel = zipCompressLevel;
}

ServiceState DefaultMQProducerImpl::getServiceState() {
    return m_serviceState;
}


void DefaultMQProducerImpl::setServiceState(ServiceState serviceState) {
    m_serviceState = serviceState;
}


SendResult DefaultMQProducerImpl::sendDefaultImpl(Message& msg,
        CommunicationMode communicationMode,
        SendCallback* pSendCallback,
        int timeout)
{
    makeSureStateOK();
    Validators::checkMessage(msg, m_pDefaultMQProducer);

	long long maxTimeout = m_pDefaultMQProducer->getSendMsgTimeout() + 1000;
    long long beginTimestamp = KPRUtil::GetCurrentTimeMillis();
    long long endTimestamp = beginTimestamp;
    TopicPublishInfo& topicPublishInfo = tryToFindTopicPublishInfo(msg.getTopic());
    SendResult sendResult;

    if (topicPublishInfo.ok())
    {
        MessageQueue* mq = NULL;

		int times = 0;
		int timesTotal = 1 + m_pDefaultMQProducer->getRetryTimesWhenSendFailed();
		std::vector<std::string> brokersSent;
        for (; times < timesTotal && int(endTimestamp - beginTimestamp) < maxTimeout; times++)
        {
            std::string lastBrokerName = (NULL == mq) ? "" : mq->getBrokerName();
            MessageQueue* tmpmq = topicPublishInfo.selectOneMessageQueue(lastBrokerName);

            if (tmpmq != NULL)
            {
                mq = tmpmq;
                brokersSent.push_back(mq->getBrokerName());

                try
                {
                    sendResult = sendKernelImpl(msg, *mq, communicationMode, pSendCallback, timeout);
                    endTimestamp = KPRUtil::GetCurrentTimeMillis();

                    switch (communicationMode)
                    {
                        case ASYNC:
                            return sendResult;

                        case ONEWAY:
                            return sendResult;

                        case SYNC:
                            if (sendResult.getSendStatus() != SEND_OK)
                            {
                                if (m_pDefaultMQProducer->isRetryAnotherBrokerWhenNotStoreOK())
                                {
                                    continue;
                                }
                            }

                            return sendResult;

                        default:
                            break;
                    }
                }
                catch (RemotingException& e)
                {
                    endTimestamp = KPRUtil::GetCurrentTimeMillis();
                    continue;
                }
                catch (MQClientException& e)
                {
                    endTimestamp = KPRUtil::GetCurrentTimeMillis();
                    continue;
                }
                catch (MQBrokerException& e)
                {
                    endTimestamp = KPRUtil::GetCurrentTimeMillis();

                    switch (e.GetError())
                    {
                        case TOPIC_NOT_EXIST_VALUE:
                        case SERVICE_NOT_AVAILABLE_VALUE:
                        case SYSTEM_ERROR_VALUE:
                        case NO_PERMISSION_VALUE:
                        case NO_BUYER_ID_VALUE:
                        case NOT_IN_CURRENT_UNIT_VALUE:
                            continue;
                        default:
                        	if (sendResult.hasResult())
                        	{
                            	return sendResult;
                            }
                            throw;
                    }
                }
                catch (InterruptedException& e)
                {
                	endTimestamp = KPRUtil::GetCurrentTimeMillis();
                    throw;
                }
            }
            else
            {
                break;
            }
        } // end of for

		std::string info = RocketMQUtil::str2fmt("Send [%d] times, still failed, cost [%d]ms, Topic: %s, BrokersSent: %s",
			times, int(endTimestamp - beginTimestamp), msg.getTopic().c_str(), UtilAll::toString(brokersSent).c_str());
		RMQ_WARN("%s", info.c_str());
        THROW_MQEXCEPTION(MQClientException, info, -1);
        return sendResult;
    }

    std::vector<std::string> nsList =
        getMQClientFactory()->getMQClientAPIImpl()->getNameServerAddressList();
    if (nsList.empty())
    {
        THROW_MQEXCEPTION(MQClientException, "No name server address, please set it", -1);
    }

    THROW_MQEXCEPTION(MQClientException, std::string("No route info of this topic, ") + msg.getTopic(), -1);
}

SendResult DefaultMQProducerImpl::sendKernelImpl(Message& msg,
        const MessageQueue& mq,
        CommunicationMode communicationMode,
        SendCallback* sendCallback,
        int timeout)
{
    std::string brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    if (brokerAddr.empty())
    {
        tryToFindTopicPublishInfo(mq.getTopic());
        brokerAddr = m_pMQClientFactory->findBrokerAddressInPublish(mq.getBrokerName());
    }

    SendMessageContext context;
    if (!brokerAddr.empty())
    {
        try
        {
            int sysFlag = 0;

            if (tryToCompressMessage(msg))
            {
                sysFlag |= MessageSysFlag::CompressedFlag;
            }

            std::string tranMsg = msg.getProperty(Message::PROPERTY_TRANSACTION_PREPARED);
            if (!tranMsg.empty() && tranMsg == "true")
            {
                sysFlag |= MessageSysFlag::TransactionPreparedType;
            }

            // Ö´ÐÐhook
            if (hasHook())
            {
                context.producerGroup = (m_pDefaultMQProducer->getProducerGroup());
                context.communicationMode = (communicationMode);
                context.brokerAddr = (brokerAddr);
                context.msg = (msg);
                context.mq = (mq);
                executeHookBefore(context);
            }

            SendMessageRequestHeader* requestHeader = new SendMessageRequestHeader();
            requestHeader->producerGroup = (m_pDefaultMQProducer->getProducerGroup());
            requestHeader->topic = (msg.getTopic());
            requestHeader->defaultTopic = (m_pDefaultMQProducer->getCreateTopicKey());
            requestHeader->defaultTopicQueueNums = (m_pDefaultMQProducer->getDefaultTopicQueueNums());
            requestHeader->queueId = (mq.getQueueId());
            requestHeader->sysFlag = (sysFlag);
            requestHeader->bornTimestamp = (KPRUtil::GetCurrentTimeMillis());
            requestHeader->flag = (msg.getFlag());
            requestHeader->properties = (MessageDecoder::messageProperties2String(msg.getProperties()));
            requestHeader->reconsumeTimes = 0;

			if (requestHeader->topic.find(MixAll::RETRY_GROUP_TOPIC_PREFIX) == 0)
			{
                std::string reconsumeTimes = msg.getProperty(Message::PROPERTY_RECONSUME_TIME);
                if (!reconsumeTimes.empty())
                {
                    requestHeader->reconsumeTimes = int(UtilAll::str2ll(reconsumeTimes.c_str()));
                    msg.clearProperty(Message::PROPERTY_RECONSUME_TIME);
                }

				/*
				3.5.8 new features
                std::string maxReconsumeTimes = msg.getProperty(Message::PROPERTY_MAX_RECONSUME_TIMES);
                if (!maxReconsumeTimes.empty())
                {
                    requestHeader->maxReconsumeTimes = int(UtilAll::str2ll(maxReconsumeTimes.c_str()));
                    msg.clearProperty(Message::PROPERTY_MAX_RECONSUME_TIMES);
                }
                */
            }

            SendResult sendResult = m_pMQClientFactory->getMQClientAPIImpl()->sendMessage(
			    brokerAddr,
			    mq.getBrokerName(),
			    msg,
			    requestHeader,
			    timeout,
			    communicationMode,
			    sendCallback
			);

            if (hasHook())
            {
                context.sendResult = (sendResult);
                executeHookAfter(context);
            }

            return sendResult;
        }
        catch (RemotingException& e)
        {
            if (hasHook())
            {
                context.pException = (&e);
                executeHookAfter(context);
            }
			RMQ_WARN("sendKernelImpl exception: %s, msg: %s", e.what(), msg.toString().c_str());
            throw;
        }
        catch (MQBrokerException& e)
        {
            if (hasHook())
            {
                context.pException = (&e);
                executeHookAfter(context);
            }
			RMQ_WARN("sendKernelImpl exception: %s, msg: %s", e.what(), msg.toString().c_str());
            throw;
        }
        catch (InterruptedException& e)
        {
            if (hasHook())
            {
                context.pException = (&e);
                executeHookAfter(context);
            }
			RMQ_WARN("sendKernelImpl exception: %s, msg: %s", e.what(), msg.toString().c_str());
            throw;
        }
    }

    THROW_MQEXCEPTION(MQClientException, std::string("The broker[") + mq.getBrokerName() + "] not exist", -1);
}

SendResult DefaultMQProducerImpl::sendSelectImpl(Message& msg,
        MessageQueueSelector* selector,
        void* pArg,
        CommunicationMode communicationMode,
        SendCallback* sendCallback,
        int timeout)
{
    makeSureStateOK();
    Validators::checkMessage(msg, m_pDefaultMQProducer);

    SendResult result;
    TopicPublishInfo& topicPublishInfo = tryToFindTopicPublishInfo(msg.getTopic());
    SendResult sendResult;

    if (topicPublishInfo.ok())
    {
        MessageQueue* mq = NULL;

		try
		{
			mq = selector->select(topicPublishInfo.getMessageQueueList(), msg, pArg);
		}
		catch (std::exception& e)
		{
            THROW_MQEXCEPTION(MQClientException,
            	std::string("select message queue throwed exception, ") + e.what(), -1);
        }
        catch (...)
		{
            THROW_MQEXCEPTION(MQClientException, "select message queue throwed exception, ", -1);
        }

        if (mq != NULL)
        {
            return sendKernelImpl(msg, *mq, communicationMode, sendCallback, timeout);
        }
        else
        {
            THROW_MQEXCEPTION(MQClientException, "select message queue return null", -1);
        }
    }

    THROW_MQEXCEPTION(MQClientException, std::string("No route info of this topic, ") + msg.getTopic(), -1);
}

void DefaultMQProducerImpl::makeSureStateOK()
{
    if (m_serviceState != RUNNING)
    {
        THROW_MQEXCEPTION(MQClientException, "The producer service state not OK, ", -1);
    }
}

void DefaultMQProducerImpl::checkConfig()
{
	Validators::checkGroup(m_pDefaultMQProducer->getProducerGroup());

    if (m_pDefaultMQProducer->getProducerGroup().empty())
    {
        THROW_MQEXCEPTION(MQClientException, "producerGroup is null", -1);
    }

    if (m_pDefaultMQProducer->getProducerGroup() == MixAll::DEFAULT_PRODUCER_GROUP)
    {
    	THROW_MQEXCEPTION(MQClientException,
    		std::string("producerGroup can not equal [") + MixAll::DEFAULT_PRODUCER_GROUP + "], please specify another one",
    		-1);
    }
}

TopicPublishInfo& DefaultMQProducerImpl::tryToFindTopicPublishInfo(
    const std::string& topic)
{
	std::map<std::string, TopicPublishInfo>::iterator it;
	{
		kpr::ScopedRLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
	    it = m_topicPublishInfoTable.find(topic);
    }

    if (it == m_topicPublishInfoTable.end() || !it->second.ok())
    {
    	{
	    	kpr::ScopedWLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
	        m_topicPublishInfoTable[topic] = TopicPublishInfo();
        }

        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(topic);

        {
	        kpr::ScopedRLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
	        it = m_topicPublishInfoTable.find(topic);
        }
    }

    if (it != m_topicPublishInfoTable.end()
    	&& (it->second.ok() || it->second.isHaveTopicRouterInfo()))
    {
    	return (it->second);
    }
    else
    {
    	m_pMQClientFactory->updateTopicRouteInfoFromNameServer(topic, true,
                m_pDefaultMQProducer);
        {
	        kpr::ScopedRLock<kpr::RWMutex> lock(m_topicPublishInfoTableLock);
    	    it = m_topicPublishInfoTable.find(topic);
    	}
    	return (it->second);
    }
}

bool DefaultMQProducerImpl::tryToCompressMessage(Message& msg)
{
    if (msg.getBodyLen() >= m_pDefaultMQProducer->getCompressMsgBodyOverHowmuch())
    {
        if (msg.tryToCompress(m_pDefaultMQProducer->getCompressLevel()))
        {
            return true;
        }
    }

    return false;
}

TransactionCheckListener* DefaultMQProducerImpl::checkListener()
{
    return NULL;
}

}
