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
#include "ConsumeMessageConcurrentlyService.h"

#include "DefaultMQPushConsumerImpl.h"
#include "MessageListener.h"
#include "MessageQueue.h"
#include "RebalanceImpl.h"
#include "DefaultMQPushConsumer.h"
#include "MixAll.h"
#include "KPRUtil.h"
#include "UtilAll.h"
#include "OffsetStore.h"

namespace rmq
{


class SubmitConsumeRequestLater : public kpr::TimerHandler
{
public:
    SubmitConsumeRequestLater(std::list<MessageExt*>& msgs,
                              ProcessQueue* pProcessQueue,
                              MessageQueue messageQueue,
                              ConsumeMessageConcurrentlyService* pService)
        : m_msgs(msgs),
          m_pProcessQueue(pProcessQueue),
          m_messageQueue(messageQueue),
          m_pService(pService)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
        	m_pService->submitConsumeRequest(m_msgs, m_pProcessQueue, m_messageQueue, true);
        }
        catch(...)
        {
        	RMQ_ERROR("SubmitConsumeRequestLater OnTimeOut exception");
        }

        delete this;
    }

private:
    std::list<MessageExt*> m_msgs;
    ProcessQueue* m_pProcessQueue;
    MessageQueue m_messageQueue;
    ConsumeMessageConcurrentlyService* m_pService;
};


class CleanExpireMsgTask : public kpr::TimerHandler
{
public:
    CleanExpireMsgTask(ConsumeMessageConcurrentlyService* pService)
        : m_pService(pService)
    {

    }

    void OnTimeOut(unsigned int timerID)
    {
    	try
    	{
        	m_pService->cleanExpireMsg();
        }
        catch(...)
        {
        	RMQ_ERROR("CleanExpireMsgTask OnTimeOut exception");
        }
    }

private:
    ConsumeMessageConcurrentlyService* m_pService;
};



ConsumeMessageConcurrentlyService::ConsumeMessageConcurrentlyService(
    DefaultMQPushConsumerImpl* pDefaultMQPushConsumerImpl,
    MessageListenerConcurrently* pMessageListener)
{
    m_pDefaultMQPushConsumerImpl = pDefaultMQPushConsumerImpl;
    m_pMessageListener = pMessageListener;
    m_pDefaultMQPushConsumer = m_pDefaultMQPushConsumerImpl->getDefaultMQPushConsumer();
    m_consumerGroup = m_pDefaultMQPushConsumer->getConsumerGroup();
    m_pConsumeExecutor = new kpr::ThreadPool("ConsumeMessageThreadPool", 5,
    	m_pDefaultMQPushConsumer->getConsumeThreadMin(), m_pDefaultMQPushConsumer->getConsumeThreadMax());
    m_pScheduledExecutorService = new kpr::TimerThread("ConsumeMessageConcurrentlyService", 1000);
    m_pCleanExpireMsgExecutors = new kpr::TimerThread("CleanExpireMsgService", 1000);
	m_pCleanExpireMsgTask = new CleanExpireMsgTask(this);
}

ConsumeMessageConcurrentlyService::~ConsumeMessageConcurrentlyService()
{
	delete m_pCleanExpireMsgTask;
}


void ConsumeMessageConcurrentlyService::start()
{
	// 每分钟检查1次是否有消费超时消息
	m_pCleanExpireMsgExecutors->RegisterTimer(
		60 * 1000, 60 * 1000,
		m_pCleanExpireMsgTask, true);

    m_pScheduledExecutorService->Start();
    m_pCleanExpireMsgExecutors->Start();
}

void ConsumeMessageConcurrentlyService::shutdown()
{
    m_pConsumeExecutor->Destroy();
    m_pScheduledExecutorService->Stop();
    m_pScheduledExecutorService->Join();

    m_pCleanExpireMsgExecutors->Stop();
    m_pCleanExpireMsgExecutors->Join();
}


void ConsumeMessageConcurrentlyService::cleanExpireMsg()
{
	std::map<MessageQueue, ProcessQueue*>& processQueueTable
		= m_pDefaultMQPushConsumerImpl->getRebalanceImpl()->getProcessQueueTable();
	RMQ_FOR_EACH(processQueueTable, it)
	{
		ProcessQueue* pq = it->second;
        pq->cleanExpiredMsg(m_pDefaultMQPushConsumer);
	}
}


ConsumerStat& ConsumeMessageConcurrentlyService::getConsumerStat()
{
    return m_pDefaultMQPushConsumerImpl->getConsumerStatManager()->getConsumertat();
}

bool ConsumeMessageConcurrentlyService::sendMessageBack(MessageExt& msg,
        ConsumeConcurrentlyContext& context)
{
    // 如果用户没有设置，服务器会根据重试次数自动叠加延时时间
    try
    {
        m_pDefaultMQPushConsumerImpl->sendMessageBack(msg,
        	context.delayLevelWhenNextConsume, context.messageQueue.getBrokerName());
        return true;
    }
    catch (...)
    {
		RMQ_ERROR("sendMessageBack exception, group: %s, msg: %s",
			m_consumerGroup.c_str(), msg.toString().c_str());
    }

    return false;
}

void ConsumeMessageConcurrentlyService::submitConsumeRequestLater(std::list<MessageExt*>& msgs,
        ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue)
{
    SubmitConsumeRequestLater* sc = new SubmitConsumeRequestLater(msgs, pProcessQueue, messageQueue, this);
    m_pScheduledExecutorService->RegisterTimer(0, 5000, sc, false);
}

void ConsumeMessageConcurrentlyService::submitConsumeRequest(std::list<MessageExt*>& msgs,
        ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue,
        bool dispathToConsume)
{
	size_t consumeBatchSize = m_pDefaultMQPushConsumer->getConsumeMessageBatchMaxSize();

	RMQ_DEBUG("submitConsumeRequest begin, msgs.size=%d, messageQueue=%s, consumeBatchSize=%d, dispathToConsume=%d",
		(int)msgs.size(), messageQueue.toString().c_str(), (int)consumeBatchSize, dispathToConsume
    );

    if (msgs.size() <= consumeBatchSize)
    {
        kpr::ThreadPoolWorkPtr consumeRequest = new ConsumeConcurrentlyRequest(msgs, pProcessQueue, messageQueue, this);
        m_pConsumeExecutor->AddWork(consumeRequest);
    }
    else
    {
        std::list<MessageExt*>::iterator it = msgs.begin();
        for (; it != msgs.end();)
        {
            std::list<MessageExt*> msgThis;
            for (size_t i = 0; i < consumeBatchSize; i++, it++)
            {
                if (it != msgs.end())
                {
                    msgThis.push_back(*it);
                }
                else
                {
                    break;
                }
            }

            kpr::ThreadPoolWorkPtr consumeRequest = new ConsumeConcurrentlyRequest(msgThis, pProcessQueue, messageQueue, this);
            m_pConsumeExecutor->AddWork(consumeRequest);
        }
    }

    RMQ_DEBUG("submitConsumeRequest end");
}

void ConsumeMessageConcurrentlyService::updateCorePoolSize(int corePoolSize)
{
	//todo 暂时不支持调整线程池大小
}

void ConsumeMessageConcurrentlyService::processConsumeResult(ConsumeConcurrentlyStatus status,
        ConsumeConcurrentlyContext& context,
        ConsumeConcurrentlyRequest& consumeRequest)
{
    int ackIndex = context.ackIndex;

    if (consumeRequest.getMsgs().empty())
    {
        return;
    }

    int msgsSize = consumeRequest.getMsgs().size();

    switch (status)
    {
        case CONSUME_SUCCESS:
        {
            if (ackIndex >= msgsSize)
            {
                ackIndex = msgsSize - 1;
            }

            int ok = ackIndex + 1;
            int failed = msgsSize - ok;
            // 统计信息
            getConsumerStat().consumeMsgOKTotal.fetchAndAdd(ok);
            getConsumerStat().consumeMsgFailedTotal.fetchAndAdd(failed);
        }

        break;
        case RECONSUME_LATER:
            ackIndex = -1;
            // 统计信息
            getConsumerStat().consumeMsgFailedTotal.fetchAndAdd(msgsSize);
            break;
        default:
            break;
    }

    std::list<MessageExt*>& msgs = consumeRequest.getMsgs();
    std::list<MessageExt*>::iterator it = msgs.begin();

    //跳过已经消费的消息
    for (int i = 0; i < ackIndex + 1 && it != msgs.end(); i++)
    {
        it++;
    }

    switch (m_pDefaultMQPushConsumer->getMessageModel())
    {
        case BROADCASTING:
            // 如果是广播模式，直接丢弃失败消息，需要在文档中告知用户
            // 这样做的原因：广播模式对于失败重试代价过高，对整个集群性能会有较大影响，失败重试功能交由应用处理
            for (; it != msgs.end(); it++)
            {
                MessageExt* msg = *it;
                RMQ_WARN("BROADCASTING, the message consume failed, drop it, %s", msg->toString().c_str());
            }
            break;
        case CLUSTERING:
        {
            // 处理消费失败的消息，直接发回到Broker
            std::list<MessageExt*> msgBackFailed;
            for (; it != msgs.end(); it++)
            {
                MessageExt* msg = *it;
                bool result = sendMessageBack(*msg, context);
                if (!result)
                {
                    msg->setReconsumeTimes(msg->getReconsumeTimes() + 1);
                    msgBackFailed.push_back(msg);
                }
            }

            if (!msgBackFailed.empty())
            {
                // 发回失败的消息仍然要保留
                // 删除consumeRequest中发送失败的消息
                it = msgs.begin();

                for (; it != msgs.end();)
                {
                    bool find = false;
                    std::list<MessageExt*>::iterator itFailed = msgBackFailed.begin();
                    for (; itFailed != msgBackFailed.end(); itFailed++)
                    {
                        if (*it == *itFailed)
                        {
                            it = msgs.erase(it);
                            find = true;
                            break;
                        }
                    }

                    if (!find)
                    {
                        it++;
                    }
                }

                // 此过程处理失败的消息，需要在Client中做定时消费，直到成功
                submitConsumeRequestLater(msgBackFailed, consumeRequest.getProcessQueue(),
                                          consumeRequest.getMessageQueue());
            }
        }
        break;
        default:
            break;
    }

    long long offset = consumeRequest.getProcessQueue()->removeMessage(consumeRequest.getMsgs());
    if (offset >= 0 && !(consumeRequest.getProcessQueue()->isDropped()))
    {
        m_pDefaultMQPushConsumerImpl->getOffsetStore()->updateOffset(consumeRequest.getMessageQueue(),
                offset, true);
    }
}

std::string& ConsumeMessageConcurrentlyService::getConsumerGroup()
{
    return m_consumerGroup;
}

MessageListenerConcurrently* ConsumeMessageConcurrentlyService::getMessageListener()
{
    return m_pMessageListener;
}

DefaultMQPushConsumerImpl* ConsumeMessageConcurrentlyService::getDefaultMQPushConsumerImpl()
{
    return m_pDefaultMQPushConsumerImpl;
}

ConsumeConcurrentlyRequest::ConsumeConcurrentlyRequest(std::list<MessageExt*>& msgs,
        ProcessQueue* pProcessQueue,
        const MessageQueue& messageQueue,
        ConsumeMessageConcurrentlyService* pService)
{
	m_msgs = msgs;
	m_pProcessQueue = pProcessQueue;
	m_pService = pService;
    m_messageQueue = messageQueue;
}

ConsumeConcurrentlyRequest::~ConsumeConcurrentlyRequest()
{
	m_msgs.clear();
}

void ConsumeConcurrentlyRequest::Do()
{
	RMQ_DEBUG("consumeMessage begin, m_msgs.size=%d", (int)m_msgs.size());

    if (m_pProcessQueue->isDropped())
    {
        RMQ_INFO("the message queue not be able to consume, because it's droped, {%s}",
        	m_messageQueue.toString().c_str());
        return;
    }

	try
	{
	    MessageListenerConcurrently* listener = m_pService->getMessageListener();
	    ConsumeConcurrentlyContext context(m_messageQueue);
	    ConsumeConcurrentlyStatus status = RECONSUME_LATER;

	    // 执行Hook
	    ConsumeMessageContext consumeMessageContext;
	    if (m_pService->getDefaultMQPushConsumerImpl()->hasHook())
	    {
	        consumeMessageContext.consumerGroup = m_pService->getConsumerGroup();
	        consumeMessageContext.mq = m_messageQueue;
	        consumeMessageContext.msgList = m_msgs;
	        consumeMessageContext.success = false;
	        m_pService->getDefaultMQPushConsumerImpl()->executeHookBefore(consumeMessageContext);
	    }

	    long long beginTimestamp = KPRUtil::GetCurrentTimeMillis();
	    try
	    {
	        resetRetryTopic(m_msgs);
	        if (!m_msgs.empty())
	        {
	        	std::list<MessageExt*>::iterator it = m_msgs.begin();
			    for (; it != m_msgs.end(); it++)
			    {
			        MessageExt* msg = (*it);
			        msg->putProperty(Message::PROPERTY_CONSUME_START_TIMESTAMP,
			        	UtilAll::toString(KPRUtil::GetCurrentTimeMillis()));
			    }
            }
	        status = listener->consumeMessage(m_msgs, context);
	    }
	    catch (...)
	    {
	        RMQ_WARN("consumeMessage exception, Group: {%s} Msgs: {%d} MQ: {%s}",
	        	m_pService->getConsumerGroup().c_str(),
	        	(int)m_msgs.size(),
	        	m_messageQueue.toString().c_str()
	        );
	    }

	    long long consumeRT = KPRUtil::GetCurrentTimeMillis() - beginTimestamp;

	    // 执行Hook
	    if (m_pService->getDefaultMQPushConsumerImpl()->hasHook())
	    {
	        consumeMessageContext.success = (status == CONSUME_SUCCESS);
	        m_pService->getDefaultMQPushConsumerImpl()->executeHookAfter(consumeMessageContext);
	    }

	    // 记录统计信息
	    m_pService->getConsumerStat().consumeMsgRTTotal.fetchAndAdd(consumeRT);
	    bool updated = MixAll::compareAndIncreaseOnly(m_pService->getConsumerStat().consumeMsgRTMax, consumeRT);
	    // 耗时最大值新记录
	    if (updated)
	    {
	        RMQ_WARN("consumeMessage RT new max: %lld, Group: %s, Msgs: %d, MQ: %s",
	        	consumeRT,
	        	m_pService->getConsumerGroup().c_str(),
	        	(int)m_msgs.size(),
	        	m_messageQueue.toString().c_str()
	        );
	    }

		if (!m_pProcessQueue->isDropped())
		{
	    	m_pService->processConsumeResult(status, context, *this);
	    }
	    else
	    {
	    	RMQ_WARN("processQueue is dropped without process consume result, messageQueue={%s}, msgs.size={%d}",
	        	m_messageQueue.toString().c_str(), (int)m_msgs.size());
	    }
	}
	catch(...)
	{
		RMQ_WARN("ConsumeConcurrentlyRequest exception");
	}
	RMQ_DEBUG("consumeMessage end, m_msgs.size=%d", (int)m_msgs.size());

    return;
}

void ConsumeConcurrentlyRequest::resetRetryTopic(std::list<MessageExt*>& msgs)
{
    std::string groupTopic = MixAll::getRetryTopic(m_pService->getConsumerGroup());
    std::list<MessageExt*>::iterator it = msgs.begin();

    for (; it != msgs.end(); it++)
    {
        MessageExt* msg = (*it);
        std::string retryTopic = msg->getProperty(Message::PROPERTY_RETRY_TOPIC);
        if (!retryTopic.empty() && groupTopic == msg->getTopic())
        {
            msg->setTopic(retryTopic);
        }
    }
}

}
