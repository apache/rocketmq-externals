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

#ifndef __LOCKBATCHBODY_H__
#define __LOCKBATCHBODY_H__

#include <string>
#include <set>

#include "RemotingSerializable.h"
#include "MessageQueue.h"

namespace rmq
{
    class LockBatchRequestBody : public RemotingSerializable
    {
    public:
        LockBatchRequestBody();
        ~LockBatchRequestBody();

        void encode(std::string& outData);
		std::string toString() const;

        std::string getConsumerGroup();
        void setConsumerGroup(const std::string& consumerGroup);

        std::string getClientId();
        void setClientId(const std::string& clientId);

        std::set<MessageQueue>& getMqSet();
        void setMqSet(const std::set<MessageQueue>& mqSet);

    private:
        std::string m_consumerGroup;
        std::string m_clientId;
        std::set<MessageQueue> m_mqSet;
    };

    class LockBatchResponseBody : public RemotingSerializable
    {
    public:
        LockBatchResponseBody();
        ~LockBatchResponseBody();

        void encode(std::string& outData);
		std::string toString() const;

        static LockBatchResponseBody* decode(const char* pData, int len);

        std::set<MessageQueue> getLockOKMQSet();
        void setLockOKMQSet(const std::set<MessageQueue>& lockOKMQSet);

    private:
        std::set<MessageQueue> m_lockOKMQSet;
    };

    typedef LockBatchRequestBody UnlockBatchRequestBody;
}

#endif
