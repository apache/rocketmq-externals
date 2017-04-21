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

#include "DefaultMQPushConsumerImpl.h"

#include <string>
#include <set>
#include "DefaultMQPushConsumer.h"
#include "ConsumerStatManage.h"
#include "DefaultMQPullConsumer.h"
#include "DefaultMQProducer.h"
#include "MQClientFactory.h"
#include "MQAdminImpl.h"
#include "RebalancePushImpl.h"
#include "MQClientAPIImpl.h"
#include "OffsetStore.h"
#include "MixAll.h"
#include "MQClientManager.h"
#include "LocalFileOffsetStore.h"
#include "RemoteBrokerOffsetStore.h"
#include "PullSysFlag.h"
#include "FilterAPI.h"
#include "PullAPIWrapper.h"
#include "MQClientException.h"
#include "Validators.h"
#include "MessageListener.h"
#include "ConsumeMessageHook.h"
#include "PullMessageService.h"
#include "ConsumeMessageOrderlyService.h"
#include "ConsumeMessageConcurrentlyService.h"
#include "KPRUtil.h"
#include "TimerThread.h"

namespace rmq
{

/* RemoveProcessQueueLater */
class RemoveProcessQueueLater : public kpr::TimerHandler
{
public:
    RemoveProcessQueueLater(DefaultMQPushConsumerImpl* pConsumerImp, PullRequest* pPullRequest)
        : m_pConsumerImp(pConsumerImp), m_pPullRequest(pPullRequest)
    {
    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
            m_pConsumerImp->getOffsetStore()->updateOffset(m_pPullRequest->getMessageQueue(), m_pPullRequest->getNextOffset(), false);
            m_pConsumerImp->getOffsetStore()->persist(m_pPullRequest->getMessageQueue());
            m_pConsumerImp->getRebalanceImpl()->removeProcessQueue(m_pPullRequest->getMessageQueue());

            RMQ_WARN("fix the pull request offset, {%s}", m_pPullRequest->toString().c_str());
        }
        catch(...)
        {
        	RMQ_ERROR("RemoveProcessQueueLater OnTimeOut Exception");
        }

        delete this;
    }

private:
	DefaultMQPushConsumerImpl* m_pConsumerImp;
    PullRequest* m_pPullRequest;
};


/* DefaultMQPushConsumerImplCallback */
class DefaultMQPushConsumerImplCallback : public PullCallback
{
public:
    DefaultMQPushConsumerImplCallback(SubscriptionData& subscriptionData,
        DefaultMQPushConsumerImpl* pDefaultMQPushConsumerImpl,
        PullRequest* pPullRequest)
	    : m_subscriptionData(subscriptionData),
	      m_pDefaultMQPushConsumerImpl(pDefaultMQPushConsumerImpl),
	      m_pPullRequest(pPullRequest)
	{
	    m_beginTimestamp = KPRUtil::GetCurrentTimeMillis();
	}

    void onSuccess(PullResult& pullResult)
	{
		RMQ_DEBUG("onSuccess begin: %s", pullResult.toString().c_str());
	    PullResult* pPullResult = &pullResult;
	    if (pPullResult != NULL)
	    {
	        pPullResult =
	            m_pDefaultMQPushConsumerImpl->m_pPullAPIWrapper->processPullResult(
	                m_pPullRequest->getMessageQueue(), *pPullResult, m_subscriptionData);

	        switch (pPullResult->pullStatus)
	        {
	            case FOUND:
	            {
	                m_pPullRequest->setNextOffset(pPullResult->nextBeginOffset);

	                long long pullRT = KPRUtil::GetCurrentTimeMillis() - m_beginTimestamp;
	                m_pDefaultMQPushConsumerImpl->getConsumerStatManager()->getConsumertat()
	                .pullTimesTotal++;
	                m_pDefaultMQPushConsumerImpl->getConsumerStatManager()->getConsumertat()
	                .pullRTTotal.fetchAndAdd(pullRT);

	                ProcessQueue* processQueue = m_pPullRequest->getProcessQueue();
	                bool dispatchToConsume = processQueue->putMessage(pPullResult->msgFoundList);

	                m_pDefaultMQPushConsumerImpl->m_pConsumeMessageService->submitConsumeRequest(//
	                    pPullResult->msgFoundList, //
	                    processQueue, //
	                    m_pPullRequest->getMessageQueue(), //
	                    dispatchToConsume);

	                if (m_pDefaultMQPushConsumerImpl->m_pDefaultMQPushConsumer->getPullInterval() > 0)
	                {
	                    m_pDefaultMQPushConsumerImpl->executePullRequestLater(m_pPullRequest,
	                            m_pDefaultMQPushConsumerImpl->m_pDefaultMQPushConsumer->getPullInterval());
	                }
	                else
	                {
	                    m_pDefaultMQPushConsumerImpl->executePullRequestImmediately(m_pPullRequest);
	                }
	            }
	            break;
	            case NO_NEW_MSG:
	                m_pPullRequest->setNextOffset(pPullResult->nextBeginOffset);
	                m_pDefaultMQPushConsumerImpl->correctTagsOffset(*m_pPullRequest);
	                m_pDefaultMQPushConsumerImpl->executePullRequestImmediately(m_pPullRequest);
	                break;
	            case NO_MATCHED_MSG:
	                m_pPullRequest->setNextOffset(pPullResult->nextBeginOffset);
	                m_pDefaultMQPushConsumerImpl->correctTagsOffset(*m_pPullRequest);
	                m_pDefaultMQPushConsumerImpl->executePullRequestImmediately(m_pPullRequest);
	                break;
	            case OFFSET_ILLEGAL:
	                RMQ_WARN("the pull request offset illegal, %s, %s",
	                         m_pPullRequest->toString().c_str(), pPullResult->toString().c_str());

	                /*
	                if (m_pPullRequest->getNextOffset() < pPullResult->minOffset)
	                {
	                    m_pPullRequest->setNextOffset(pPullResult->minOffset);
	                }
	                else if (m_pPullRequest->getNextOffset() > pPullResult->maxOffset)
	                {
	                    m_pPullRequest->setNextOffset(pPullResult->maxOffset);
	                }
	                m_pDefaultMQPushConsumerImpl->m_pOffsetStore->updateOffset(
	                    m_pPullRequest->getMessageQueue(), m_pPullRequest->getNextOffset(), false);
	                m_pDefaultMQPushConsumerImpl->executePullRequestImmediately(m_pPullRequest);
	                */

	                // todo
	                m_pPullRequest->setNextOffset(pPullResult->nextBeginOffset);
					m_pPullRequest->getProcessQueue()->setDropped(true);

					m_pDefaultMQPushConsumerImpl->executeTaskLater(new RemoveProcessQueueLater(
						m_pDefaultMQPushConsumerImpl, m_pPullRequest), 10000);
		            break;
	            default:
	                break;
	        }
	    }
	    else
	    {
	        RMQ_WARN("Warning: PullRequest is null!");
	    }
	    RMQ_DEBUG("onSuccess end");
	}

	void onException(MQException& e)
	{
	    std::string topic = m_pPullRequest->getMessageQueue().getTopic();
	    if (topic.find(MixAll::RETRY_GROUP_TOPIC_PREFIX) != std::string::npos)
	    {
	        RMQ_WARN("execute the pull request exception:%s", e.what());
	    }

	    m_pDefaultMQPushConsumerImpl->executePullRequestLater(m_pPullRequest,
	            DefaultMQPushConsumerImpl::s_PullTimeDelayMillsWhenException);
	}

private:
    SubscriptionData m_subscriptionData;
    DefaultMQPushConsumerImpl* m_pDefaultMQPushConsumerImpl;
    PullRequest* m_pPullRequest;
    unsigned long long m_beginTimestamp;
};


DefaultMQPushConsumerImpl::DefaultMQPushConsumerImpl(DefaultMQPushConsumer* pDefaultMQPushConsumer)
{
    m_pDefaultMQPushConsumer = pDefaultMQPushConsumer;
    m_serviceState = CREATE_JUST;
    flowControlTimes1 = 0;
    flowControlTimes2 = 0;
    m_pause = false;
    m_consumeOrderly = false;

    m_pMQClientFactory = NULL;
    m_pPullAPIWrapper = NULL;
    m_pMessageListenerInner = NULL;
    m_pOffsetStore = NULL;
    m_pRebalanceImpl = new RebalancePushImpl(this);
    m_pConsumerStatManager = new ConsumerStatManager();
    m_pConsumeMessageService = NULL;
}

DefaultMQPushConsumerImpl::~DefaultMQPushConsumerImpl()
{
    //delete m_pMessageListenerInner;
    if (m_pPullAPIWrapper)
		delete m_pPullAPIWrapper;
	if (m_pRebalanceImpl)
    	delete m_pRebalanceImpl;
    if (m_pConsumerStatManager)
    	delete m_pConsumerStatManager;
    if (m_pConsumeMessageService)
    	delete m_pConsumeMessageService;
    if (m_pOffsetStore)
    	delete m_pOffsetStore;
    //delete m_pMQClientFactory;
}

void DefaultMQPushConsumerImpl::start()
{
    RMQ_DEBUG("DefaultMQPushConsumerImpl::start()");
    switch (m_serviceState)
    {
        case CREATE_JUST:
        {
        	RMQ_INFO("the consumer [{%s}] start beginning. messageModel={%s}",
                m_pDefaultMQPushConsumer->getConsumerGroup().c_str(),
                getMessageModelString(m_pDefaultMQPushConsumer->getMessageModel()));

            m_serviceState = START_FAILED;
            checkConfig();
            copySubscription();

            if (m_pDefaultMQPushConsumer->getMessageModel() == CLUSTERING)
            {
                m_pDefaultMQPushConsumer->changeInstanceNameToPID();
            }

            m_pMQClientFactory = MQClientManager::getInstance()->getAndCreateMQClientFactory(*m_pDefaultMQPushConsumer);

            m_pRebalanceImpl->setConsumerGroup(m_pDefaultMQPushConsumer->getConsumerGroup());
            m_pRebalanceImpl->setMessageModel(m_pDefaultMQPushConsumer->getMessageModel());
            m_pRebalanceImpl->setAllocateMessageQueueStrategy(m_pDefaultMQPushConsumer->getAllocateMessageQueueStrategy());
            m_pRebalanceImpl->setmQClientFactory(m_pMQClientFactory);

            m_pPullAPIWrapper = new PullAPIWrapper(m_pMQClientFactory, m_pDefaultMQPushConsumer->getConsumerGroup());

            if (m_pDefaultMQPushConsumer->getOffsetStore() != NULL)
            {
                m_pOffsetStore = m_pDefaultMQPushConsumer->getOffsetStore();
            }
            else
            {
                switch (m_pDefaultMQPushConsumer->getMessageModel())
                {
                    case BROADCASTING:
                        m_pOffsetStore = new LocalFileOffsetStore(m_pMQClientFactory, m_pDefaultMQPushConsumer->getConsumerGroup());
                        break;
                    case CLUSTERING:
                        m_pOffsetStore = new RemoteBrokerOffsetStore(m_pMQClientFactory, m_pDefaultMQPushConsumer->getConsumerGroup());
                        break;
                    default:
                        break;
                }
            }

            m_pOffsetStore->load();

            if (dynamic_cast<MessageListenerOrderly*>(m_pMessageListenerInner) != NULL)
            {
                m_consumeOrderly = true;
                m_pConsumeMessageService =
                    new ConsumeMessageOrderlyService(this, (MessageListenerOrderly*)m_pMessageListenerInner);
            }
            else if (dynamic_cast<MessageListenerConcurrently*>(m_pMessageListenerInner) != NULL)
            {
                m_consumeOrderly = false;
                m_pConsumeMessageService =
                    new ConsumeMessageConcurrentlyService(this, (MessageListenerConcurrently*)m_pMessageListenerInner);
            }
            m_pConsumeMessageService->start();

            bool registerOK = m_pMQClientFactory->registerConsumer(m_pDefaultMQPushConsumer->getConsumerGroup(), this);
            if (!registerOK)
            {
                m_serviceState = CREATE_JUST;
                m_pConsumeMessageService->shutdown();
                std::string str = "The consumer group[" + m_pDefaultMQPushConsumer->getConsumerGroup();
                str += "] has been created before, specify another name please.";
                THROW_MQEXCEPTION(MQClientException, str, -1);
            }
            m_pMQClientFactory->start();

			RMQ_INFO("the consumer [%s] start OK.", m_pDefaultMQPushConsumer->getConsumerGroup().c_str());
            m_serviceState = RUNNING;
        }
        break;
        case RUNNING:
        case START_FAILED:
        case SHUTDOWN_ALREADY:
            THROW_MQEXCEPTION(MQClientException, "The PullConsumer service state not OK, maybe started once, ", -1);
        default:
            break;
    }

    updateTopicSubscribeInfoWhenSubscriptionChanged();
    m_pMQClientFactory->sendHeartbeatToAllBrokerWithLock();
    m_pMQClientFactory->rebalanceImmediately();
}


void DefaultMQPushConsumerImpl::shutdown()
{
    RMQ_DEBUG("DefaultMQPushConsumerImpl::shutdown()");
    switch (m_serviceState)
    {
        case CREATE_JUST:
            break;
        case RUNNING:
            m_pConsumeMessageService->shutdown();
            persistConsumerOffset();
            m_pMQClientFactory->unregisterConsumer(m_pDefaultMQPushConsumer->getConsumerGroup());
            m_pMQClientFactory->shutdown();

            m_serviceState = SHUTDOWN_ALREADY;
            break;
        case SHUTDOWN_ALREADY:
            break;
        default:
            break;
    }
}




bool DefaultMQPushConsumerImpl::hasHook()
{
    return !m_hookList.empty();
}

void DefaultMQPushConsumerImpl::registerHook(ConsumeMessageHook* pHook)
{
    m_hookList.push_back(pHook);
}

void DefaultMQPushConsumerImpl::executeHookBefore(ConsumeMessageContext& context)
{
    std::list<ConsumeMessageHook*>::iterator it = m_hookList.begin();
    for (; it != m_hookList.end(); it++)
    {
        try
        {
            (*it)->consumeMessageBefore(context);
        }
        catch (...)
        {
        	RMQ_WARN("consumeMessageBefore exception");
        }
    }
}

void DefaultMQPushConsumerImpl::executeHookAfter(ConsumeMessageContext& context)
{
    std::list<ConsumeMessageHook*>::iterator it = m_hookList.begin();
    for (; it != m_hookList.end(); it++)
    {
        try
        {
            (*it)->consumeMessageAfter(context);
        }
        catch (...)
        {
        	RMQ_WARN("consumeMessageAfter exception");
        }
    }
}

void DefaultMQPushConsumerImpl::createTopic(const std::string& key, const std::string& newTopic, int queueNum)
{
    m_pMQClientFactory->getMQAdminImpl()->createTopic(key, newTopic, queueNum);
}

std::set<MessageQueue>* DefaultMQPushConsumerImpl::fetchSubscribeMessageQueues(const std::string& topic)
{
    std::map<std::string, std::set<MessageQueue> >& mqs =  m_pRebalanceImpl->getTopicSubscribeInfoTable();
    std::map<std::string, std::set<MessageQueue> >::iterator it = mqs.find(topic);

    if (it == mqs.end())
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(topic);
        mqs =  m_pRebalanceImpl->getTopicSubscribeInfoTable();
        it = mqs.find(topic);
    }

    if (it == mqs.end())
    {
        THROW_MQEXCEPTION(MQClientException, "The topic[" + topic + "] not exist", -1);
    }

    std::set<MessageQueue>* result = new std::set<MessageQueue>(it->second.begin(), it->second.end());
    return result;
}

DefaultMQPushConsumer* DefaultMQPushConsumerImpl::getDefaultMQPushConsumer()
{
    return m_pDefaultMQPushConsumer;
}

long long DefaultMQPushConsumerImpl::earliestMsgStoreTime(const MessageQueue& mq)
{
    return m_pMQClientFactory->getMQAdminImpl()->earliestMsgStoreTime(mq);
}

long long DefaultMQPushConsumerImpl::maxOffset(const MessageQueue& mq)
{
    return m_pMQClientFactory->getMQAdminImpl()->maxOffset(mq);
}

long long DefaultMQPushConsumerImpl::minOffset(const MessageQueue& mq)
{
    return m_pMQClientFactory->getMQAdminImpl()->minOffset(mq);
}

OffsetStore* DefaultMQPushConsumerImpl::getOffsetStore()
{
    return m_pOffsetStore;
}

void DefaultMQPushConsumerImpl::setOffsetStore(OffsetStore* pOffsetStore)
{
    m_pOffsetStore = pOffsetStore;
}

//MQConsumerInner
std::string DefaultMQPushConsumerImpl::groupName()
{
    return m_pDefaultMQPushConsumer->getConsumerGroup();
}

MessageModel DefaultMQPushConsumerImpl::messageModel()
{
    return m_pDefaultMQPushConsumer->getMessageModel();
}

ConsumeType DefaultMQPushConsumerImpl::consumeType()
{
    return CONSUME_PASSIVELY;
}

ConsumeFromWhere DefaultMQPushConsumerImpl::consumeFromWhere()
{
    return m_pDefaultMQPushConsumer->getConsumeFromWhere();
}

std::set<SubscriptionData> DefaultMQPushConsumerImpl::subscriptions()
{
    std::set<SubscriptionData> sds;
    std::map<std::string, SubscriptionData>& subscription = m_pRebalanceImpl->getSubscriptionInner();
    std::map<std::string, SubscriptionData>::iterator it = subscription.begin();
    for (; it != subscription.end(); it++)
    {
        sds.insert(it->second);
    }

    return sds;
}

void DefaultMQPushConsumerImpl::doRebalance()
{
    if (m_pRebalanceImpl != NULL)
    {
        m_pRebalanceImpl->doRebalance();
    }
}

void DefaultMQPushConsumerImpl::persistConsumerOffset()
{
    try
    {
        makeSureStateOK();

        std::set<MessageQueue> mqs;
        {
	        kpr::ScopedRLock<kpr::RWMutex> lock(m_pRebalanceImpl->getProcessQueueTableLock());
	        std::map<MessageQueue, ProcessQueue*>& processQueueTable = m_pRebalanceImpl->getProcessQueueTable();
	        RMQ_FOR_EACH(processQueueTable, it)
	        {
	            mqs.insert(it->first);
	        }
        }

        m_pOffsetStore->persistAll(mqs);
    }
    catch (...)
    {
    	RMQ_ERROR("persistConsumerOffset exception, group: %s",
    		m_pDefaultMQPushConsumer->getConsumerGroup().c_str());
    }
}

void DefaultMQPushConsumerImpl::updateTopicSubscribeInfo(const std::string& topic, const std::set<MessageQueue>& info)
{
    std::map<std::string, SubscriptionData>& subTable = getSubscriptionInner();

    if (subTable.find(topic) != subTable.end())
    {
        m_pRebalanceImpl->getTopicSubscribeInfoTable().insert(std::pair<std::string, std::set<MessageQueue> >(topic, info));
    }
}

std::map<std::string, SubscriptionData>& DefaultMQPushConsumerImpl::getSubscriptionInner()
{
    return m_pRebalanceImpl->getSubscriptionInner();
}

bool DefaultMQPushConsumerImpl::isSubscribeTopicNeedUpdate(const std::string& topic)
{
    std::map<std::string, SubscriptionData>& subTable = getSubscriptionInner();

    if (subTable.find(topic) != subTable.end())
    {
        std::map<std::string, std::set<MessageQueue> >& mqs =
            m_pRebalanceImpl->getTopicSubscribeInfoTable();

        return mqs.find(topic) == mqs.end();
    }

    return false;
}

bool DefaultMQPushConsumerImpl::isPause()
{
    return m_pause;
}

void DefaultMQPushConsumerImpl::setPause(bool pause)
{
    m_pause = pause;
}


void DefaultMQPushConsumerImpl::correctTagsOffset(PullRequest& pullRequest)
{
    if (pullRequest.getProcessQueue()->getMsgCount().get() == 0)
    {
        m_pOffsetStore->updateOffset(pullRequest.getMessageQueue(), pullRequest.getNextOffset(), true);
    }
}

void DefaultMQPushConsumerImpl::pullMessage(PullRequest* pPullRequest)
{
	RMQ_DEBUG("pullMessage begin: %s", pPullRequest->toString().c_str());

    ProcessQueue* processQueue = pPullRequest->getProcessQueue();
    if (processQueue->isDropped())
    {
        RMQ_WARN("the pull request[%s] is dropped.", pPullRequest->toString().c_str());
        delete pPullRequest;
        return;
    }

    pPullRequest->getProcessQueue()->setLastPullTimestamp(KPRUtil::GetCurrentTimeMillis());

    try
    {
        makeSureStateOK();
    }
    catch (const MQException& e)
    {
        RMQ_WARN("pullMessage exception [%s], consumer state not ok", e.what());
        executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenException);
        return;
    }

    if (isPause())
    {
    	RMQ_WARN("consumer was paused, execute pull request later. instanceName={%s}",
                m_pDefaultMQPushConsumer->getInstanceName().c_str());
        executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenSuspend);
        return;
    }

    long size = processQueue->getMsgCount().get();
    if (size > m_pDefaultMQPushConsumer->getPullThresholdForQueue())
    {
        executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenFlowControl);
        if ((flowControlTimes1++ % 3000) == 0)
        {
            RMQ_WARN("the consumer message buffer is full, so do flow control, {%ld} {%s} {%lld}", size,
                    pPullRequest->toString().c_str(), flowControlTimes1);
        }
        return;
    }

    if (!m_consumeOrderly)
    {
        if (processQueue->getMaxSpan() > m_pDefaultMQPushConsumer->getConsumeConcurrentlyMaxSpan())
        {
            executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenFlowControl);
            if ((flowControlTimes2++ % 3000) == 0)
            {
                RMQ_WARN("the queue's messages, span too long, so do flow control, size: {%ld}, pullRequest: {%s}, times: {%lld}, maxspan: {%lld}",
                	size, pPullRequest->toString().c_str(), flowControlTimes2, processQueue->getMaxSpan());
            }
            return;
        }
    }

    std::map<std::string, SubscriptionData>& subTable = getSubscriptionInner();
    std::string topic = pPullRequest->getMessageQueue().getTopic();
    std::map<std::string, SubscriptionData>::iterator it = subTable.find(topic);
    if (it == subTable.end())
    {
        executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenException);
        RMQ_WARN("find the consumer's subscription failed, {%s}", pPullRequest->toString().c_str());
        return;
    }

    SubscriptionData subscriptionData = it->second;
    PullCallback* pullCallback = new DefaultMQPushConsumerImplCallback(subTable[topic], this, pPullRequest);

    bool commitOffsetEnable = false;
    long commitOffsetValue = 0L;
    if (CLUSTERING == m_pDefaultMQPushConsumer->getMessageModel())
    {
        commitOffsetValue = m_pOffsetStore->readOffset(pPullRequest->getMessageQueue(),
                            READ_FROM_MEMORY);
        if (commitOffsetValue > 0)
        {
            commitOffsetEnable = true;
        }
    }

    int sysFlag = PullSysFlag::buildSysFlag(
                      commitOffsetEnable, // commitOffset
                      true, // suspend
                      false// subscription
                  );
    try
    {
        m_pPullAPIWrapper->pullKernelImpl(
            pPullRequest->getMessageQueue(), // 1
            "", // 2
            subscriptionData.getSubVersion(), // 3
            pPullRequest->getNextOffset(), // 4
            m_pDefaultMQPushConsumer->getPullBatchSize(), // 5
            sysFlag, // 6
            commitOffsetValue,// 7
            s_BrokerSuspendMaxTimeMillis, // 8
            s_ConsumerTimeoutMillisWhenSuspend, // 9
            ASYNC, // 10
            pullCallback// 11
        );
    }
    catch (...)
    {
        RMQ_ERROR("pullKernelImpl exception");
        executePullRequestLater(pPullRequest, s_PullTimeDelayMillsWhenException);
    }

    RMQ_DEBUG("pullMessage end");
}

void DefaultMQPushConsumerImpl::executePullRequestImmediately(PullRequest* pullRequest)
{
    m_pMQClientFactory->getPullMessageService()->executePullRequestImmediately(pullRequest);
}

void DefaultMQPushConsumerImpl::executePullRequestLater(PullRequest* pullRequest, long timeDelay)
{
    m_pMQClientFactory->getPullMessageService()->executePullRequestLater(pullRequest, timeDelay);
}

void DefaultMQPushConsumerImpl::executeTaskLater(kpr::TimerHandler* handler, long timeDelay)
{
    m_pMQClientFactory->getPullMessageService()->executeTaskLater(handler, timeDelay);
}


void DefaultMQPushConsumerImpl::makeSureStateOK()
{
    if (m_serviceState != RUNNING)
    {
        THROW_MQEXCEPTION(MQClientException, "The consumer service state not OK, ", -1);
    }
}

ConsumerStatManager* DefaultMQPushConsumerImpl::getConsumerStatManager()
{
    return m_pConsumerStatManager;
}

QueryResult DefaultMQPushConsumerImpl::queryMessage(const std::string& topic,
        const std::string&  key,
        int maxNum,
        long long begin,
        long long end)
{
    return m_pMQClientFactory->getMQAdminImpl()->queryMessage(topic, key, maxNum, begin, end);
}

void DefaultMQPushConsumerImpl::registerMessageListener(MessageListener* pMessageListener)
{
    m_pMessageListenerInner = pMessageListener;
}

void DefaultMQPushConsumerImpl::resume()
{
    m_pause = false;
}

long long DefaultMQPushConsumerImpl::searchOffset(const MessageQueue& mq, long long timestamp)
{
    return m_pMQClientFactory->getMQAdminImpl()->searchOffset(mq, timestamp);
}

void DefaultMQPushConsumerImpl::sendMessageBack(MessageExt& msg, int delayLevel, const std::string& brokerName)
{
    try
    {
    	std::string brokerAddr = brokerName.empty() ?
    		socketAddress2IPPort(msg.getStoreHost()) : m_pMQClientFactory->findBrokerAddressInPublish(brokerName);

        m_pMQClientFactory->getMQClientAPIImpl()->consumerSendMessageBack(brokerAddr, msg,
                m_pDefaultMQPushConsumer->getConsumerGroup(),
                delayLevel,
                5000);
    }
    catch (...)
    {
    	RMQ_ERROR("sendMessageBack Exception, group: %s", m_pDefaultMQPushConsumer->getConsumerGroup().c_str());
        Message newMsg(MixAll::getRetryTopic(m_pDefaultMQPushConsumer->getConsumerGroup()),
                       msg.getBody(), msg.getBodyLen());

		std::string originMsgId = msg.getProperty(Message::PROPERTY_ORIGIN_MESSAGE_ID);
		newMsg.putProperty(Message::PROPERTY_ORIGIN_MESSAGE_ID, UtilAll::isBlank(originMsgId) ? msg.getMsgId()
                    : originMsgId);

        newMsg.setFlag(msg.getFlag());
        newMsg.setProperties(msg.getProperties());
        newMsg.putProperty(Message::PROPERTY_RETRY_TOPIC, msg.getTopic());

        int reTimes = msg.getReconsumeTimes() + 1;
        newMsg.putProperty(Message::PROPERTY_RECONSUME_TIME, UtilAll::toString(reTimes));
        newMsg.putProperty(Message::PROPERTY_MAX_RECONSUME_TIMES, UtilAll::toString(m_pDefaultMQPushConsumer->getMaxReconsumeTimes()));
        newMsg.setDelayTimeLevel(3 + reTimes);

        m_pMQClientFactory->getDefaultMQProducer()->send(newMsg);
    }
}

void DefaultMQPushConsumerImpl::checkConfig()
{
    // consumerGroup check
    Validators::checkGroup(m_pDefaultMQPushConsumer->getConsumerGroup());

    // consumerGroup
    if (m_pDefaultMQPushConsumer->getConsumerGroup() == MixAll::DEFAULT_CONSUMER_GROUP)
    {
        THROW_MQEXCEPTION(MQClientException, "consumerGroup can not equal "
                          + MixAll::DEFAULT_CONSUMER_GROUP //
                          + ", please specify another one.", -1);
    }

    if (m_pDefaultMQPushConsumer->getMessageModel() != BROADCASTING
        && m_pDefaultMQPushConsumer->getMessageModel() != CLUSTERING)
    {
        THROW_MQEXCEPTION(MQClientException, "messageModel is invalid ", -1);
    }

    // allocateMessageQueueStrategy
    if (m_pDefaultMQPushConsumer->getAllocateMessageQueueStrategy() == NULL)
    {
        THROW_MQEXCEPTION(MQClientException, "allocateMessageQueueStrategy is null", -1);
    }

    // consumeFromWhereOffset
    if (m_pDefaultMQPushConsumer->getConsumeFromWhere() < CONSUME_FROM_LAST_OFFSET
        || m_pDefaultMQPushConsumer->getConsumeFromWhere() > CONSUME_FROM_MAX_OFFSET)
    {
        THROW_MQEXCEPTION(MQClientException, "consumeFromWhere is invalid", -1);
    }

    // subscription
    /*
    if (m_pDefaultMQPushConsumer->getSubscription().size() == 0)
    {
      THROW_MQEXCEPTION(MQClientException,"subscription is null" ,-1);
    }
    */

    // messageListener
    if (m_pDefaultMQPushConsumer->getMessageListener() == NULL)
    {
        THROW_MQEXCEPTION(MQClientException, "messageListener is null", -1);
    }

    MessageListener* listener = m_pDefaultMQPushConsumer->getMessageListener();
    MessageListener* orderly = (dynamic_cast<MessageListenerOrderly*>(listener)) ;
    MessageListener* concurrently = (dynamic_cast<MessageListenerConcurrently*>(listener)) ;

    if (!orderly && !concurrently)
    {
        THROW_MQEXCEPTION(MQClientException,
                          "messageListener must be instanceof MessageListenerOrderly or MessageListenerConcurrently" ,
                          -1);
    }

    // consumeThreadMin
    if (m_pDefaultMQPushConsumer->getConsumeThreadMin() < 1
        || m_pDefaultMQPushConsumer->getConsumeThreadMin() > 1000
        || m_pDefaultMQPushConsumer->getConsumeThreadMin() > m_pDefaultMQPushConsumer->getConsumeThreadMax()
       )
    {
        THROW_MQEXCEPTION(MQClientException, "consumeThreadMin Out of range [1, 1000]", -1);
    }

    // consumeThreadMax
    if (m_pDefaultMQPushConsumer->getConsumeThreadMax() < 1
        || m_pDefaultMQPushConsumer->getConsumeThreadMax() > 1000)
    {
        THROW_MQEXCEPTION(MQClientException, "consumeThreadMax Out of range [1, 1000]", -1);
    }

    // consumeConcurrentlyMaxSpan
    if (m_pDefaultMQPushConsumer->getConsumeConcurrentlyMaxSpan() < 1
        || m_pDefaultMQPushConsumer->getConsumeConcurrentlyMaxSpan() > 65535)
    {
        THROW_MQEXCEPTION(MQClientException, "consumeConcurrentlyMaxSpan Out of range [1, 65535]" , -1);
    }

    // pullThresholdForQueue
    if (m_pDefaultMQPushConsumer->getPullThresholdForQueue() < 1
        || m_pDefaultMQPushConsumer->getPullThresholdForQueue() > 65535)
    {
        THROW_MQEXCEPTION(MQClientException, "pullThresholdForQueue Out of range [1, 65535]", -1);
    }

    // pullInterval
    if (m_pDefaultMQPushConsumer->getPullInterval() < 0
        || m_pDefaultMQPushConsumer->getPullInterval() > 65535)
    {
        THROW_MQEXCEPTION(MQClientException, "pullInterval Out of range [0, 65535]", -1);
    }

    // consumeMessageBatchMaxSize
    if (m_pDefaultMQPushConsumer->getConsumeMessageBatchMaxSize() < 1
        || m_pDefaultMQPushConsumer->getConsumeMessageBatchMaxSize() > 1024)
    {
        THROW_MQEXCEPTION(MQClientException, "consumeMessageBatchMaxSize Out of range [1, 1024]", -1);
    }

    // pullBatchSize
    if (m_pDefaultMQPushConsumer->getPullBatchSize() < 1
        || m_pDefaultMQPushConsumer->getPullBatchSize() > 1024)
    {
        THROW_MQEXCEPTION(MQClientException, "pullBatchSize Out of range [1, 1024]", -1);
    }
}

void DefaultMQPushConsumerImpl::copySubscription()
{
    try
    {
        std::map<std::string, std::string>& sub = m_pDefaultMQPushConsumer->getSubscription();
        std::map<std::string, std::string>::iterator it = sub.begin();
        for (; it != sub.end(); it++)
        {
            SubscriptionDataPtr subscriptionData = FilterAPI::buildSubscriptionData(it->first, it->second);
            m_pRebalanceImpl->getSubscriptionInner()[it->first] = *subscriptionData;
        }

        if (m_pMessageListenerInner == NULL)
        {
            m_pMessageListenerInner = m_pDefaultMQPushConsumer->getMessageListener();
        }

        switch (m_pDefaultMQPushConsumer->getMessageModel())
        {
            case BROADCASTING:
                break;
            case CLUSTERING:
            {
                std::string retryTopic = MixAll::getRetryTopic(m_pDefaultMQPushConsumer->getConsumerGroup());
                SubscriptionDataPtr subscriptionData =
                    FilterAPI::buildSubscriptionData(retryTopic, SubscriptionData::SUB_ALL);
                m_pRebalanceImpl->getSubscriptionInner()[retryTopic] = *subscriptionData;
            }

            break;
            default:
                break;
        }
    }
    catch (...)
    {
        THROW_MQEXCEPTION(MQClientException, "subscription exception", -1);
    }
}

void DefaultMQPushConsumerImpl::updateTopicSubscribeInfoWhenSubscriptionChanged()
{
    std::map<std::string, SubscriptionData> subTable = getSubscriptionInner();
    std::map<std::string, SubscriptionData>::iterator it = subTable.begin();
    for (; it != subTable.end(); it++)
    {
        m_pMQClientFactory->updateTopicRouteInfoFromNameServer(it->first);
    }
}

MessageListener* DefaultMQPushConsumerImpl::getMessageListenerInner()
{
    return m_pMessageListenerInner;
}

void DefaultMQPushConsumerImpl::subscribe(const std::string& topic, const std::string& subExpression)
{
    try
    {
        SubscriptionDataPtr subscriptionData = FilterAPI::buildSubscriptionData(topic, subExpression);
        m_pRebalanceImpl->getSubscriptionInner()[topic] = *subscriptionData;

        if (m_pMQClientFactory)
        {
            m_pMQClientFactory->sendHeartbeatToAllBrokerWithLock();
        }
    }
    catch (...)
    {
        THROW_MQEXCEPTION(MQClientException, "subscription exception", -1);
    }
}

void DefaultMQPushConsumerImpl::suspend()
{
    m_pause = true;
}

void DefaultMQPushConsumerImpl::unsubscribe(const std::string& topic)
{
    m_pRebalanceImpl->getSubscriptionInner().erase(topic);
}

void DefaultMQPushConsumerImpl::updateConsumeOffset(MessageQueue& mq, long long offset)
{
    m_pOffsetStore->updateOffset(mq, offset, false);
}

void DefaultMQPushConsumerImpl::updateCorePoolSize(int corePoolSize)
{
    m_pConsumeMessageService->updateCorePoolSize(corePoolSize);
}

MessageExt* DefaultMQPushConsumerImpl::viewMessage(const std::string& msgId)
{
    return m_pMQClientFactory->getMQAdminImpl()->viewMessage(msgId);
}

RebalanceImpl* DefaultMQPushConsumerImpl::getRebalanceImpl()
{
    return m_pRebalanceImpl;
}

bool DefaultMQPushConsumerImpl::isConsumeOrderly()
{
    return m_consumeOrderly;
}

void DefaultMQPushConsumerImpl::setConsumeOrderly(bool consumeOrderly)
{
    m_consumeOrderly = consumeOrderly;
}


MQClientFactory* DefaultMQPushConsumerImpl::getmQClientFactory()
{
    return m_pMQClientFactory;
}

void DefaultMQPushConsumerImpl::setmQClientFactory(MQClientFactory* mQClientFactory)
{
    m_pMQClientFactory = mQClientFactory;
}


ServiceState DefaultMQPushConsumerImpl::getServiceState()
{
    return m_serviceState;
}

void DefaultMQPushConsumerImpl::setServiceState(ServiceState serviceState)
{
    m_serviceState = serviceState;
}

}

