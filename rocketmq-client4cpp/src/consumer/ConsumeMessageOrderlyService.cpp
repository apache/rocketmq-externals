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
#include <string>

#include "ConsumeMessageOrderlyService.h"
#include "DefaultMQPushConsumerImpl.h"
#include "MQClientFactory.h"
#include "DefaultMQProducer.h"
#include "MessageListener.h"
#include "MessageQueue.h"
#include "RebalanceImpl.h"
#include "DefaultMQPushConsumer.h"
#include "OffsetStore.h"
#include "ScopedLock.h"
#include "KPRUtil.h"
#include "MixAll.h"
#include "UtilAll.h"

namespace rmq
{

class LockMq : public kpr::TimerHandler
{
public:
    LockMq(ConsumeMessageOrderlyService* pService)
        : m_pService(pService)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
        m_pService->lockMQPeriodically();

        // can not delete
        //delete this;
    }

private:
    ConsumeMessageOrderlyService* m_pService;
};

class SubmitConsumeRequestLaterOrderly : public kpr::TimerHandler
{
public:
    SubmitConsumeRequestLaterOrderly(ProcessQueue* pProcessQueue,
                                     const MessageQueue& messageQueue,
                                     ConsumeMessageOrderlyService* pService)
        : m_pProcessQueue(pProcessQueue),
          m_messageQueue(messageQueue),
          m_pService(pService)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
        	std::list<MessageExt*> msgs;
       		m_pService->submitConsumeRequest(msgs, m_pProcessQueue, m_messageQueue, true);
        }
        catch(...)
        {
        	RMQ_ERROR("SubmitConsumeRequestLaterOrderly OnTimeOut exception");
        }

        delete this;
    }

private:
    ProcessQueue* m_pProcessQueue;
    MessageQueue m_messageQueue;
    ConsumeMessageOrderlyService* m_pService;
};


class TryLockLaterAndReconsume : public kpr::TimerHandler
{
public:
    TryLockLaterAndReconsume(ProcessQueue* pProcessQueue,
                             MessageQueue& messageQueue,
                             ConsumeMessageOrderlyService* pService)
        : m_pProcessQueue(pProcessQueue),
          m_messageQueue(messageQueue),
          m_pService(pService)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
	        bool lockOK = m_pService->lockOneMQ(m_messageQueue);
	        if (lockOK)
	        {
	            m_pService->submitConsumeRequestLater(m_pProcessQueue, m_messageQueue, 10);
	        }
	        else
	        {
	            m_pService->submitConsumeRequestLater(m_pProcessQueue, m_messageQueue, 3000);
	        }
        }
        catch(...)
        {
        	RMQ_ERROR("TryLockLaterAndReconsume OnTimeOut exception");
        }

        delete this;
    }

private:
    ProcessQueue* m_pProcessQueue;
    MessageQueue m_messageQueue;
    ConsumeMessageOrderlyService* m_pService;
};



ConsumeMessageOrderlyService::ConsumeMessageOrderlyService(DefaultMQPushConsumerImpl* pDefaultMQPushConsumerImpl,
        MessageListenerOrderly* pMessageListener)
{
	m_stoped = false;
    m_pDefaultMQPushConsumerImpl = pDefaultMQPushConsumerImpl;
    m_pMessageListener = pMessageListener;
    m_pDefaultMQPushConsumer = m_pDefaultMQPushConsumerImpl->getDefaultMQPushConsumer();
    m_consumerGroup = m_pDefaultMQPushConsumer->getConsumerGroup();
    m_pConsumeExecutor = new kpr::ThreadPool("ConsumeMessageThreadPool", 1,
    m_pDefaultMQPushConsumer->getConsumeThreadMin(), m_pDefaultMQPushConsumer->getConsumeThreadMax());
    m_scheduledExecutorService = new kpr::TimerThread("ConsumeMessageConcurrentlyService", 10);
}

ConsumeMessageOrderlyService::~ConsumeMessageOrderlyService()
{
}


void ConsumeMessageOrderlyService::start()
{
    m_scheduledExecutorService->Start();

    LockMq* lm = new LockMq(this);
    m_scheduledExecutorService->RegisterTimer(0, ProcessQueue::s_RebalanceLockInterval, lm, true);
}

void ConsumeMessageOrderlyService::shutdown()
{
    m_stoped = true;
    m_pConsumeExecutor->Destroy();
    m_scheduledExecutorService->Stop();
    m_scheduledExecutorService->Join();
    unlockAllMQ();
}

void ConsumeMessageOrderlyService::unlockAllMQ()
{
    m_pDefaultMQPushConsumerImpl->getRebalanceImpl()->unlockAll(false);
}

void ConsumeMessageOrderlyService::lockMQPeriodically()
{
    if (!m_stoped)
    {
        m_pDefaultMQPushConsumerImpl->getRebalanceImpl()->lockAll();
    }
}

bool ConsumeMessageOrderlyService::lockOneMQ(MessageQueue& mq)
{
    if (!m_stoped)
    {
        return m_pDefaultMQPushConsumerImpl->getRebalanceImpl()->lock(mq);
    }

    return false;
}

void ConsumeMessageOrderlyService::tryLockLaterAndReconsume(MessageQueue& messageQueue,
        ProcessQueue* pProcessQueue,
        long long delayMills)
{
    TryLockLaterAndReconsume* consume = new TryLockLaterAndReconsume(pProcessQueue, messageQueue, this);
    m_scheduledExecutorService->RegisterTimer(0, int(delayMills), consume, false);
}

ConsumerStat& ConsumeMessageOrderlyService::getConsumerStat()
{
    return m_pDefaultMQPushConsumerImpl->getConsumerStatManager()->getConsumertat();
}

void ConsumeMessageOrderlyService::submitConsumeRequestLater(ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue,
        long long suspendTimeMillis)
{
    long timeMillis = long(suspendTimeMillis);
    if (timeMillis < 10)
    {
        timeMillis = 10;
    }
    else if (timeMillis > 30000)
    {
        timeMillis = 30000;
    }

    SubmitConsumeRequestLaterOrderly* sc = new SubmitConsumeRequestLaterOrderly(pProcessQueue, messageQueue, this);
    m_scheduledExecutorService->RegisterTimer(0, timeMillis, sc, false);
}

void ConsumeMessageOrderlyService::submitConsumeRequest(std::list<MessageExt*>& msgs,
        ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue,
        bool dispathToConsume)
{
    if (dispathToConsume)
    {
        kpr::ThreadPoolWorkPtr consumeRequest = new ConsumeOrderlyRequest(pProcessQueue, messageQueue, this);
        m_pConsumeExecutor->AddWork(consumeRequest);
    }
}

void ConsumeMessageOrderlyService::updateCorePoolSize(int corePoolSize)
{
}


std::string& ConsumeMessageOrderlyService::getConsumerGroup()
{
    return m_consumerGroup;
}

MessageListenerOrderly* ConsumeMessageOrderlyService::getMessageListener()
{
    return m_pMessageListener;
}

DefaultMQPushConsumerImpl* ConsumeMessageOrderlyService::getDefaultMQPushConsumerImpl()
{
    return m_pDefaultMQPushConsumerImpl;
}

bool ConsumeMessageOrderlyService::processConsumeResult(std::list<MessageExt*>& msgs,
        ConsumeOrderlyStatus status,
        ConsumeOrderlyContext& context,
        ConsumeOrderlyRequest& consumeRequest)
{
    bool continueConsume = true;
    long long commitOffset = -1L;
    int msgsSize = msgs.size();

    if (context.autoCommit)
    {
        switch (status)
        {
            case COMMIT:
            case ROLLBACK:
                RMQ_WARN("the message queue consume result is illegal, we think you want to ack these message: %s",
                	consumeRequest.getMessageQueue().toString().c_str());
            case SUCCESS:
                getConsumerStat().consumeMsgOKTotal.fetchAndAdd(msgsSize);
                commitOffset = consumeRequest.getProcessQueue()->commit();
                break;
            case SUSPEND_CURRENT_QUEUE_A_MOMENT:
                getConsumerStat().consumeMsgFailedTotal.fetchAndAdd(msgsSize);
	            if (checkReconsumeTimes(msgs))
	            {
	                consumeRequest.getProcessQueue()->makeMessageToCosumeAgain(msgs);
	                submitConsumeRequestLater(consumeRequest.getProcessQueue(),
	                                          consumeRequest.getMessageQueue(),
	                                          context.suspendCurrentQueueTimeMillis);
	                continueConsume = false;
				}
				else
				{
					commitOffset = consumeRequest.getProcessQueue()->commit();
				}

                break;
            default:
                break;
        }
    }
    else
    {
        switch (status)
        {
            case SUCCESS:
                getConsumerStat().consumeMsgOKTotal.fetchAndAdd(msgsSize);
                break;
            case COMMIT:
                commitOffset = consumeRequest.getProcessQueue()->commit();
                break;
            case ROLLBACK:
                consumeRequest.getProcessQueue()->rollback();
                submitConsumeRequestLater(consumeRequest.getProcessQueue(),
                                          consumeRequest.getMessageQueue(),
                                          context.suspendCurrentQueueTimeMillis);
                continueConsume = false;
                break;
            case SUSPEND_CURRENT_QUEUE_A_MOMENT:
                getConsumerStat().consumeMsgFailedTotal.fetchAndAdd(msgsSize);
	            if (checkReconsumeTimes(msgs))
	            {
	                consumeRequest.getProcessQueue()->makeMessageToCosumeAgain(msgs);
	                submitConsumeRequestLater(consumeRequest.getProcessQueue(),
	                                          consumeRequest.getMessageQueue(),
	                                          context.suspendCurrentQueueTimeMillis);
	                continueConsume = false;
                }
                break;
            default:
                break;
        }
    }

    if (commitOffset >= 0 && !consumeRequest.getProcessQueue()->isDropped())
    {
        m_pDefaultMQPushConsumerImpl->getOffsetStore()->updateOffset(consumeRequest.getMessageQueue(),
                commitOffset, false);
    }

    return continueConsume;
}

bool ConsumeMessageOrderlyService::checkReconsumeTimes(std::list<MessageExt*>& msgs)
{
    bool suspend = false;

    if (!msgs.empty())
    {
    	std::list<MessageExt*>::iterator it = msgs.begin();
        for (; it != msgs.end(); it++)
        {
            MessageExt* msg = *it;
            if (msg->getReconsumeTimes() >= m_pDefaultMQPushConsumer->getMaxReconsumeTimes())
            {
            	msg->putProperty(Message::PROPERTY_RECONSUME_TIME, UtilAll::toString(msg->getReconsumeTimes()));

                if (!sendMessageBack(*msg))
                {
                    suspend = true;
                    msg->setReconsumeTimes(msg->getReconsumeTimes() + 1);
                }
            }
            else
            {
                suspend = true;
                msg->setReconsumeTimes(msg->getReconsumeTimes() + 1);
            }
        }
    }

    return suspend;
}

bool ConsumeMessageOrderlyService::sendMessageBack(MessageExt& msg)
{
    try
    {
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

        m_pDefaultMQPushConsumerImpl->getmQClientFactory()->getDefaultMQProducer()->send(newMsg);

        return true;
    }
    catch (...)
    {
        RMQ_ERROR("sendMessageBack exception, group: %s, msg: %s",
                  m_consumerGroup.c_str(), msg.toString().c_str());
    }

    return false;
}


MessageQueueLock& ConsumeMessageOrderlyService::getMessageQueueLock()
{
    return m_messageQueueLock;
}

DefaultMQPushConsumer* ConsumeMessageOrderlyService::getDefaultMQPushConsumer()
{
    return m_pDefaultMQPushConsumer;
}

ConsumeOrderlyRequest::ConsumeOrderlyRequest(ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue,
        ConsumeMessageOrderlyService* pService)
{
	m_pProcessQueue = pProcessQueue;
	m_messageQueue = messageQueue;
	m_pService = pService;
}

ConsumeOrderlyRequest::~ConsumeOrderlyRequest()
{
}

void ConsumeOrderlyRequest::Do()
{
	if (m_pProcessQueue->isDropped())
	{
        RMQ_WARN("run, the message queue not be able to consume, because it's dropped, MQ: %s",
            m_messageQueue.toString().c_str());
        return;
    }

	try
	{
	    kpr::Mutex* objLock = m_pService->getMessageQueueLock().fetchLockObject(m_messageQueue);
	    {
	        kpr::ScopedLock<kpr::Mutex> lock(*objLock);

	        MessageModel messageModel = m_pService->getDefaultMQPushConsumerImpl()->messageModel();
	        if (BROADCASTING == messageModel
	        	|| (m_pProcessQueue->isLocked() || !m_pProcessQueue->isLockExpired()))
	        {
	            long long beginTime = KPRUtil::GetCurrentTimeMillis();
	            for (bool continueConsume = true; continueConsume;)
	            {
	                if (m_pProcessQueue->isDropped())
	                {
	                    RMQ_INFO("the message queue not be able to consume, because it's droped, MQ: %s",
	                             m_messageQueue.toString().c_str());
	                    break;
	                }

	                if (CLUSTERING == messageModel
	                 	&& !m_pProcessQueue->isLocked())
	                {
	                    RMQ_WARN("the message queue not locked, so consume later, MQ: %s", m_messageQueue.toString().c_str());
	                    m_pService->tryLockLaterAndReconsume(m_messageQueue, m_pProcessQueue, 10);
	                    break;
	                }

	                if (CLUSTERING == messageModel
	                 	&& m_pProcessQueue->isLockExpired())
	                {
	                    RMQ_WARN("the message queue lock expired, so consume later, MQ: %s", m_messageQueue.toString().c_str());
	                    m_pService->tryLockLaterAndReconsume(m_messageQueue, m_pProcessQueue, 10);
	                    break;
	                }

	                long interval = long(KPRUtil::GetCurrentTimeMillis() - beginTime);
	                if (interval > ConsumeMessageOrderlyService::s_MaxTimeConsumeContinuously)
	                {
	                    m_pService->submitConsumeRequestLater(m_pProcessQueue, m_messageQueue, 10);
	                    break;
	                }

	                int consumeBatchSize =
	                    m_pService->getDefaultMQPushConsumer()->getConsumeMessageBatchMaxSize();

	                std::list<MessageExt*> msgs = m_pProcessQueue->takeMessages(consumeBatchSize);
	                if (!msgs.empty())
	                {
	                    ConsumeOrderlyContext context(m_messageQueue);

	                    ConsumeOrderlyStatus status = SUSPEND_CURRENT_QUEUE_A_MOMENT;

	                    ConsumeMessageContext consumeMessageContext;
	                    if (m_pService->getDefaultMQPushConsumerImpl()->hasHook())
	                    {
	                        consumeMessageContext.consumerGroup = m_pService->getConsumerGroup();
	                        consumeMessageContext.mq = m_messageQueue;
	                        consumeMessageContext.msgList = msgs;
	                        consumeMessageContext.success = false;
	                        m_pService->getDefaultMQPushConsumerImpl()->executeHookBefore(consumeMessageContext);
	                    }

	                    long long beginTimestamp = KPRUtil::GetCurrentTimeMillis();
	                    try
	                    {
	                    	kpr::ScopedLock<kpr::Mutex> lock(m_pProcessQueue->getLockConsume());
	                    	if (m_pProcessQueue->isDropped())
							{
						        RMQ_WARN("consumeMessage, the message queue not be able to consume, because it's dropped, MQ: %s",
						            m_messageQueue.toString().c_str());
						        break;
						    }

	                        status = m_pService->getMessageListener()->consumeMessage(msgs, context);
	                    }
	                    catch (...)
	                    {
	                        RMQ_WARN("consumeMessage exception, Group: {%s}, Msgs: {%u}, MQ: %s",//
	                                 m_pService->getConsumerGroup().c_str(),
	                                 (unsigned)msgs.size(),
	                                 m_messageQueue.toString().c_str());
	                    }

	                    long long consumeRT = KPRUtil::GetCurrentTimeMillis() - beginTimestamp;

	                    if (SUSPEND_CURRENT_QUEUE_A_MOMENT == status
	                    	|| ROLLBACK == status)
	                    {
	                    	RMQ_WARN("consumeMessage Orderly return not OK, Group: {%s} Msgs: {%u} MQ: %s",//
	                                    m_pService->getConsumerGroup().c_str(),
	                                    (unsigned)msgs.size(),
	                                     m_messageQueue.toString().c_str());
	                    	//status = ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
	                    }

	                    if (m_pService->getDefaultMQPushConsumerImpl()->hasHook())
	                    {
	                        consumeMessageContext.success = (SUCCESS == status
	                                                         || COMMIT == status);
	                        m_pService->getDefaultMQPushConsumerImpl()->executeHookAfter(consumeMessageContext);
	                    }

	                    m_pService->getConsumerStat().consumeMsgRTTotal.fetchAndAdd(consumeRT);
	                    MixAll::compareAndIncreaseOnly(m_pService->getConsumerStat()
	                                                   .consumeMsgRTMax, consumeRT);

	                    continueConsume = m_pService->processConsumeResult(msgs, status, context, *this);
	                }
	                else
	                {
	                    continueConsume = false;
	                }
	            }
	        }
	        else
	        {
	        	if (m_pProcessQueue->isDropped())
				{
			        RMQ_WARN("consumeMessage, the message queue not be able to consume, because it's dropped, MQ: %s",
			            m_messageQueue.toString().c_str());
			        return;
			    }

	            m_pService->tryLockLaterAndReconsume(m_messageQueue, m_pProcessQueue, 100);
	        }
	    }
	}
	catch(...)
	{
		RMQ_WARN("ConsumeOrderlyRequest exception");
	}

    return;
}

}

