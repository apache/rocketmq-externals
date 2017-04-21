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

#ifndef __MQADMINIMPL_H__
#define __MQADMINIMPL_H__

#include <string>
#include <list>
#include <set>
#include <vector>

#include "MessageExt.h"
#include "QueryResult.h"

namespace rmq
{
    class MQClientFactory;
    class MessageQueue;

    class MQAdminImpl
    {
    public:
        MQAdminImpl(MQClientFactory* pMQClientFactory);
        ~MQAdminImpl();

        void createTopic(const std::string& key, const std::string& newTopic, int queueNum);
		void createTopic(const std::string& key, const std::string& newTopic, int queueNum, int topicSysFlag);

        std::vector<MessageQueue>* fetchPublishMessageQueues(const std::string& topic);
        std::set<MessageQueue>* fetchSubscribeMessageQueues(const std::string& topic);
        long long searchOffset(const MessageQueue& mq, long long timestamp);
        long long maxOffset(const MessageQueue& mq);
        long long minOffset(const MessageQueue& mq);

        long long earliestMsgStoreTime(const MessageQueue& mq);

        MessageExt* viewMessage(const std::string& msgId);

        QueryResult queryMessage(const std::string& topic,
                                 const std::string& key,
                                 int maxNum,
                                 long long begin,
                                 long long end);

    private:
        MQClientFactory* m_pMQClientFactory;
    };
}

#endif
