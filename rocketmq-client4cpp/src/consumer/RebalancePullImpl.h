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
#ifndef __REBALANCEPULLIMPL_H__
#define __REBALANCEPULLIMPL_H__

#include "RebalanceImpl.h"

namespace rmq
{
class DefaultMQPullConsumerImpl;

class RebalancePullImpl : public RebalanceImpl
{
public:
    RebalancePullImpl(DefaultMQPullConsumerImpl *pDefaultMQPullConsumerImpl);

    RebalancePullImpl(const std::string &consumerGroup,
                      MessageModel messageModel,
                      AllocateMessageQueueStrategy *pAllocateMessageQueueStrategy,
                      MQClientFactory *pMQClientFactory,
                      DefaultMQPullConsumerImpl *pDefaultMQPullConsumerImpl);

    long long computePullFromWhere(MessageQueue &mq);

    void dispatchPullRequest(std::list<PullRequest *> &pullRequestList);

    void messageQueueChanged(const std::string &topic,
                             std::set<MessageQueue> &mqAll,
                             std::set<MessageQueue> &mqDivided);

    bool removeUnnecessaryMessageQueue(MessageQueue &mq, ProcessQueue &pq);

    ConsumeType consumeType()
    {
        return CONSUME_ACTIVELY;
    };

private:
    DefaultMQPullConsumerImpl *m_pDefaultMQPullConsumerImpl;
};
}

#endif
