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

#ifndef __PULLREQUEST_H__
#define __PULLREQUEST_H__

#include <string>
#include <sstream>

#include "MessageQueue.h"
#include "ProcessQueue.h"

namespace rmq
{
    class PullRequest
    {
    public:
        virtual ~PullRequest();

        std::string getConsumerGroup();
        void setConsumerGroup(const std::string& consumerGroup);

        MessageQueue& getMessageQueue();
        void setMessageQueue(const MessageQueue& messageQueue);

        long long getNextOffset();
        void setNextOffset(long long nextOffset);

        int hashCode();
        std::string toString() const;

        bool operator==(const PullRequest& other);

        ProcessQueue* getProcessQueue();
        void setProcessQueue(ProcessQueue* pProcessQueue);

    private:
        std::string m_consumerGroup;
        MessageQueue m_messageQueue;

        ProcessQueue* m_pProcessQueue;
        long long m_nextOffset;
    };
}

#endif
