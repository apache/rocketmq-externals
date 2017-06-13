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

#include "RebalancePullImpl.h"
#include "DefaultMQPullConsumerImpl.h"
#include "AllocateMessageQueueStrategy.h"
#include "MQClientFactory.h"
#include "MessageQueueListener.h"
#include "OffsetStore.h"
#include "DefaultMQPullConsumer.h"

namespace rmq
{

RebalancePullImpl::RebalancePullImpl(DefaultMQPullConsumerImpl* pDefaultMQPullConsumerImpl)
    : RebalanceImpl("", BROADCASTING, NULL, NULL),
      m_pDefaultMQPullConsumerImpl(pDefaultMQPullConsumerImpl)
{
}

RebalancePullImpl::RebalancePullImpl(const std::string& consumerGroup,
                                     MessageModel messageModel,
                                     AllocateMessageQueueStrategy* pAllocateMessageQueueStrategy,
                                     MQClientFactory* pMQClientFactory,
                                     DefaultMQPullConsumerImpl* pDefaultMQPullConsumerImpl)
    : RebalanceImpl(consumerGroup, messageModel, pAllocateMessageQueueStrategy, pMQClientFactory),
      m_pDefaultMQPullConsumerImpl(pDefaultMQPullConsumerImpl)
{
}

long long RebalancePullImpl::computePullFromWhere(MessageQueue& mq)
{
    return 0;
}

void RebalancePullImpl::dispatchPullRequest(std::list<PullRequest*>& pullRequestList)
{
}

void RebalancePullImpl::messageQueueChanged(const std::string& topic,
        std::set<MessageQueue>& mqAll,
        std::set<MessageQueue>& mqDivided)
{
    MessageQueueListener* messageQueueListener =
        m_pDefaultMQPullConsumerImpl->getDefaultMQPullConsumer()->getMessageQueueListener();
    if (messageQueueListener != NULL)
    {
        try
        {
            messageQueueListener->messageQueueChanged(topic, mqAll, mqDivided);
        }
        catch (...)
        {
            RMQ_ERROR("messageQueueChanged exception, %s", topic.c_str());
        }
    }
}

bool RebalancePullImpl::removeUnnecessaryMessageQueue(MessageQueue& mq, ProcessQueue& pq)
{
    m_pDefaultMQPullConsumerImpl->getOffsetStore()->persist(mq);
    m_pDefaultMQPullConsumerImpl->getOffsetStore()->removeOffset(mq);
    return true;
}

}
