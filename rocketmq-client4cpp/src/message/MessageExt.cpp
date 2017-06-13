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
#include "MessageExt.h"

#include <sstream>
#include "MessageSysFlag.h"
#include "SocketUtil.h"

namespace rmq
{

MessageExt::MessageExt()
    : m_queueOffset(0),
      m_commitLogOffset(0),
      m_bornTimestamp(0),
      m_storeTimestamp(0),
      m_preparedTransactionOffset(0),
      m_queueId(0),
      m_storeSize(0),
      m_sysFlag(0),
      m_bodyCRC(0),
      m_reconsumeTimes(3),
      m_msgId("")
{
}

MessageExt::MessageExt(int queueId,
                       long long bornTimestamp,
                       sockaddr bornHost,
                       long long storeTimestamp,
                       sockaddr storeHost,
                       std::string msgId)
    : m_queueOffset(0),
      m_commitLogOffset(0),
      m_bornTimestamp(bornTimestamp),
      m_storeTimestamp(storeTimestamp),
      m_preparedTransactionOffset(0),
      m_queueId(queueId),
      m_storeSize(0),
      m_sysFlag(0),
      m_bodyCRC(0),
      m_reconsumeTimes(3),
      m_bornHost(bornHost),
      m_storeHost(storeHost),
      m_msgId(msgId)
{

}

MessageExt::~MessageExt()
{

}

int MessageExt::getQueueId()
{
    return m_queueId;
}

void MessageExt::setQueueId(int queueId)
{
    m_queueId = queueId;
}

long long MessageExt::getBornTimestamp()
{
    return m_bornTimestamp;
}

void MessageExt::setBornTimestamp(long long bornTimestamp)
{
    m_bornTimestamp = bornTimestamp;
}

sockaddr MessageExt::getBornHost()
{
    return m_bornHost;
}

std::string MessageExt::getBornHostString()
{
    return socketAddress2String(m_bornHost);
}

std::string MessageExt::getBornHostNameString()
{
    return getHostName(m_bornHost);
}

void MessageExt::setBornHost(const sockaddr& bornHost)
{
    m_bornHost = bornHost;
}

long long MessageExt::getStoreTimestamp()
{
    return m_storeTimestamp;
}

void MessageExt::setStoreTimestamp(long long storeTimestamp)
{
    m_storeTimestamp = storeTimestamp;
}

sockaddr MessageExt::getStoreHost()
{
    return m_storeHost;
}

std::string MessageExt::getStoreHostString()
{
    return socketAddress2String(m_storeHost);
}

void MessageExt::setStoreHost(const sockaddr& storeHost)
{
    m_storeHost = storeHost;
}

std::string MessageExt::getMsgId()
{
    return m_msgId;
}

void MessageExt::setMsgId(const std::string& msgId)
{
    m_msgId = msgId;
}

int MessageExt::getSysFlag()
{
    return m_sysFlag;
}

void MessageExt::setSysFlag(int sysFlag)
{
    m_sysFlag = sysFlag;
}

int MessageExt::getBodyCRC()
{
    return m_bodyCRC;
}

void MessageExt::setBodyCRC(int bodyCRC)
{
    m_bodyCRC = bodyCRC;
}

long long MessageExt::getQueueOffset()
{
    return m_queueOffset;
}

void MessageExt::setQueueOffset(long long queueOffset)
{
    m_queueOffset = queueOffset;
}

long long MessageExt::getCommitLogOffset()
{
    return m_commitLogOffset;
}

void MessageExt::setCommitLogOffset(long long physicOffset)
{
    m_commitLogOffset = physicOffset;
}

int MessageExt::getStoreSize()
{
    return m_storeSize;
}

void MessageExt::setStoreSize(int storeSize)
{
    m_storeSize = storeSize;
}

TopicFilterType MessageExt::parseTopicFilterType(int sysFlag)
{
    if ((sysFlag & MessageSysFlag::MultiTagsFlag) == MessageSysFlag::MultiTagsFlag)
    {
        return MULTI_TAG;
    }

    return SINGLE_TAG;
}

int MessageExt::getReconsumeTimes()
{
    return m_reconsumeTimes;
}

void MessageExt::setReconsumeTimes(int reconsumeTimes)
{
    m_reconsumeTimes = reconsumeTimes;
}

long long MessageExt::getPreparedTransactionOffset()
{
    return  m_preparedTransactionOffset;
}

void MessageExt::setPreparedTransactionOffset(long long preparedTransactionOffset)
{
    m_preparedTransactionOffset = preparedTransactionOffset;
}

std::string MessageExt::toString() const
{
    std::stringstream ss;
    ss << "{msgId=" << m_msgId
       << ",queueId=" << m_queueId
       << ",storeSize=" << m_storeSize
       << ",sysFlag=" << m_sysFlag
       << ",queueOffset=" << m_queueOffset
       << ",commitLogOffset=" << m_commitLogOffset
       << ",preparedTransactionOffset=" << m_preparedTransactionOffset
       << ",bornTimestamp=" << m_bornTimestamp
       << ",bornHost=" << socketAddress2String(m_bornHost)
       << ",storeHost=" << socketAddress2String(m_storeHost)
       << ",storeTimestamp=" << m_storeTimestamp
       << ",reconsumeTimes=" << m_reconsumeTimes
       << ",bodyCRC=" << m_bodyCRC
       << ",Message=" << Message::toString()
       << "}";
    return ss.str();
}

}
