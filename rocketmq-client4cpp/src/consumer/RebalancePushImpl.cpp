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

#include "RebalancePushImpl.h"

#include <string.h>
#include <limits.h>

#include "DefaultMQPushConsumerImpl.h"
#include "AllocateMessageQueueStrategy.h"
#include "MQClientFactory.h"
#include "MessageQueueListener.h"
#include "OffsetStore.h"
#include "DefaultMQPushConsumer.h"
#include "MQAdminImpl.h"


namespace rmq
{

RebalancePushImpl::RebalancePushImpl(DefaultMQPushConsumerImpl* pDefaultMQPushConsumerImpl)
    : RebalanceImpl("", BROADCASTING, NULL, NULL),
      m_pDefaultMQPushConsumerImpl(pDefaultMQPushConsumerImpl)
{
}

RebalancePushImpl::RebalancePushImpl(const std::string& consumerGroup,
                                     MessageModel messageModel,
                                     AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy,
                                     MQClientFactory* pMQClientFactory,
                                     DefaultMQPushConsumerImpl* pDefaultMQPushConsumerImpl)
    : RebalanceImpl(consumerGroup, messageModel, pAllocateMessageQueueStrategy, pMQClientFactory),
      m_pDefaultMQPushConsumerImpl(pDefaultMQPushConsumerImpl)
{
}

void RebalancePushImpl::dispatchPullRequest(std::list<PullRequest*>& pullRequestList)
{
    std::list<PullRequest*>::iterator it = pullRequestList.begin();
    for (; it != pullRequestList.end(); it++)
    {
        m_pDefaultMQPushConsumerImpl->executePullRequestImmediately(*it);
        RMQ_INFO("doRebalance, {%s}, add a new pull request {%s}",
        	m_consumerGroup.c_str(), (*it)->toString().c_str());
    }
}

long long RebalancePushImpl::computePullFromWhere(MessageQueue& mq)
{
    long long result = -1;
    ConsumeFromWhere consumeFromWhere =
        m_pDefaultMQPushConsumerImpl->getDefaultMQPushConsumer()->getConsumeFromWhere();
    OffsetStore* offsetStore = m_pDefaultMQPushConsumerImpl->getOffsetStore();

    switch (consumeFromWhere)
    {
    	case CONSUME_FROM_FIRST_OFFSET:
        {
            long long lastOffset = offsetStore->readOffset(mq, READ_FROM_STORE);
            if (lastOffset >= 0)
            {
                result = lastOffset;
            }
            else if (-1 == lastOffset)
            {
                result = 0L;
            }
            else
            {
                result = -1;
            }
            break;
        }
        case CONSUME_FROM_LAST_OFFSET:
        {
            long long lastOffset = offsetStore->readOffset(mq, READ_FROM_STORE);
            if (lastOffset >= 0)
            {
                result = lastOffset;
            }
            else if (-1 == lastOffset)
            {
                if (strncmp(MixAll::RETRY_GROUP_TOPIC_PREFIX.c_str(), mq.getTopic().c_str(), MixAll::RETRY_GROUP_TOPIC_PREFIX.size()) == 0)
                {
                    result = 0L;
                }
                else
                {
                    //result = LLONG_MAX;
                    try
                    {
                    	result = m_pMQClientFactory->getMQAdminImpl()->maxOffset(mq);
                    }
                    catch(...)
                    {
                    	result = -1;
                    }
                }
            }
            else
            {
                result = -1;
            }
            break;
        }

        case CONSUME_FROM_MAX_OFFSET:
            result = LLONG_MAX;
            break;
        case CONSUME_FROM_MIN_OFFSET:
            result = 0L;
            break;
        case CONSUME_FROM_TIMESTAMP:
        	{
	        	long long lastOffset = offsetStore->readOffset(mq, READ_FROM_STORE);
	            if (lastOffset >= 0)
	            {
	                result = lastOffset;
	            }
	            else if (-1 == lastOffset)
	            {
	                if (strncmp(MixAll::RETRY_GROUP_TOPIC_PREFIX.c_str(), mq.getTopic().c_str(), MixAll::RETRY_GROUP_TOPIC_PREFIX.size()) == 0)
	                {
	                	//result = LLONG_MAX;
	                    try
	                    {
	                    	result = m_pMQClientFactory->getMQAdminImpl()->maxOffset(mq);
	                    }
	                    catch(...)
	                    {
	                    	result = -1;
	                    }
	                }
	                else
	                {
	                    try
	                    {
	                    	long timestamp = UtilAll::str2tm(
                                    m_pDefaultMQPushConsumerImpl->getDefaultMQPushConsumer()->getConsumeTimestamp(),
                                    rmq::yyyyMMddHHmmss);
                        	result = m_pMQClientFactory->getMQAdminImpl()->searchOffset(mq, timestamp);
	                    }
	                    catch(...)
	                    {
	                    	result = -1;
	                    }
	                }
	            }
	            else
	            {
	                result = -1;
	            }
	            break;
	        }
        	break;
        default:
            break;
    }

    return result;
}

void RebalancePushImpl::messageQueueChanged(const std::string& topic,
        std::set<MessageQueue>& mqAll,
        std::set<MessageQueue>& mqDivided)
{
}


bool RebalancePushImpl::removeUnnecessaryMessageQueue(MessageQueue& mq, ProcessQueue& pq)
{
    m_pDefaultMQPushConsumerImpl->getOffsetStore()->persist(mq);
    m_pDefaultMQPushConsumerImpl->getOffsetStore()->removeOffset(mq);
    if (m_pDefaultMQPushConsumerImpl->isConsumeOrderly()
    	&& m_pDefaultMQPushConsumerImpl->messageModel() == CLUSTERING)
    {
    	if (pq.getLockConsume().TryLock(1000))
    	{
	    	try
	    	{
	        	this->unlock(mq, true);
	        }
	        catch (std::exception& e)
	        {
	            RMQ_ERROR("removeUnnecessaryMessageQueue Exception: %s", e.what());
	        }
	        pq.getLockConsume().Unlock();
	   	}
	   	else
	   	{
	   		RMQ_WARN("[WRONG]mq is consuming, so can not unlock it, MQ:%s, maybe hanged for a while, times:{%lld}",
            	mq.toString().c_str(),
            	pq.getTryUnlockTimes());

			pq.incTryUnlockTimes();
	   	}

	   	return false;
    }

    return true;
}

}
