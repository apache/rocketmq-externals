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

#ifndef __MQCONSUMERINNER_H__
#define __MQCONSUMERINNER_H__

#include <string>
#include <set>

#include "ConsumeType.h"
#include "SubscriptionData.h"

namespace rmq
{
    class MessageQueue;

    class MQConsumerInner
    {
    public:
        virtual ~MQConsumerInner() {}
        virtual std::string groupName() = 0;
        virtual MessageModel messageModel() = 0;
        virtual ConsumeType consumeType() = 0;
        virtual ConsumeFromWhere consumeFromWhere() = 0;
        virtual std::set<SubscriptionData> subscriptions() = 0;
        virtual void doRebalance() = 0;
        virtual void persistConsumerOffset() = 0;
        virtual void updateTopicSubscribeInfo(const std::string& topic, const std::set<MessageQueue>& info) = 0;
        virtual bool isSubscribeTopicNeedUpdate(const std::string& topic) = 0;
    };
}

#endif
