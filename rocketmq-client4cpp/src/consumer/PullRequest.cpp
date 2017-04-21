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

#include "PullRequest.h"
#include "UtilAll.h"

namespace rmq
{

PullRequest::~PullRequest()
{

}

std::string PullRequest::getConsumerGroup()
{
    return m_consumerGroup;
}

void PullRequest::setConsumerGroup(const std::string& consumerGroup)
{
    m_consumerGroup = consumerGroup;
}

MessageQueue& PullRequest::getMessageQueue()
{
    return m_messageQueue;
}

void PullRequest::setMessageQueue(const MessageQueue& messageQueue)
{
    m_messageQueue = messageQueue;
}

long long PullRequest::getNextOffset()
{
    return m_nextOffset;
}

void PullRequest::setNextOffset(long long nextOffset)
{
    m_nextOffset = nextOffset;
}

int PullRequest::hashCode()
{
    /*
    final int prime = 31;
    int result = 1;
    result = prime * result + ((consumerGroup == null) ? 0 : consumerGroup.hashCode());
    result = prime * result + ((messageQueue == null) ? 0 : messageQueue.hashCode());
    return result;
    */
    std::stringstream ss;
    ss  << m_consumerGroup
        << m_messageQueue.hashCode();
    return UtilAll::hashCode(ss.str());
}

std::string PullRequest::toString() const
{
    std::stringstream ss;
    ss << "{consumerGroup=" << m_consumerGroup
       << ",messageQueue=" << m_messageQueue.toString()
       << ",nextOffset=" << m_nextOffset << "}";
    return ss.str();
}


bool PullRequest::operator==(const PullRequest& other)
{
    if (m_consumerGroup != other.m_consumerGroup)
    {
        return false;
    }

    if (!(m_messageQueue == other.m_messageQueue))
    {
        return false;
    }

    return true;
}

ProcessQueue* PullRequest::getProcessQueue()
{
    return m_pProcessQueue;
}

void PullRequest::setProcessQueue(ProcessQueue* pProcessQueue)
{
    m_pProcessQueue = pProcessQueue;
}

}
