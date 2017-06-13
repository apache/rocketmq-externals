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
#include <stdlib.h>
#include <sstream>

#include "TopicConfig.h"
#include "PermName.h"

namespace rmq
{

int TopicConfig::DefaultReadQueueNums = 16;
int TopicConfig::DefaultWriteQueueNums = 16;
std::string TopicConfig::SEPARATOR = " ";

TopicConfig::TopicConfig()
    : m_topicName(""),
      m_readQueueNums(DefaultReadQueueNums),
      m_writeQueueNums(DefaultWriteQueueNums),
      m_perm(PermName::PERM_READ | PermName::PERM_WRITE),
      m_topicFilterType(SINGLE_TAG),
      m_topicSysFlag(0),
      m_order(false)
{

}

TopicConfig::TopicConfig(const std::string& topicName)
    : m_topicName(topicName),
      m_readQueueNums(DefaultReadQueueNums),
      m_writeQueueNums(DefaultWriteQueueNums),
      m_perm(PermName::PERM_READ | PermName::PERM_WRITE),
      m_topicFilterType(SINGLE_TAG),
      m_topicSysFlag(0),
      m_order(false)
{

}

TopicConfig::TopicConfig(const std::string& topicName, int readQueueNums, int writeQueueNums, int perm)
    : m_topicName(topicName),
      m_readQueueNums(readQueueNums),
      m_writeQueueNums(writeQueueNums),
      m_perm(perm),
      m_topicFilterType(SINGLE_TAG),
      m_topicSysFlag(0),
      m_order(false)
{
}

TopicConfig::~TopicConfig()
{
}

std::string  TopicConfig::encode()
{
    std::stringstream ss;
    ss << m_topicName << SEPARATOR
       << m_readQueueNums << SEPARATOR
       << m_writeQueueNums << SEPARATOR
       << m_perm << SEPARATOR
       << m_topicFilterType;

    return ss.str();
}

bool  TopicConfig::decode(const std::string& in)
{
    std::stringstream ss(in);

    ss >> m_topicName;
    ss >> m_readQueueNums;
    ss >> m_writeQueueNums;
    ss >> m_perm;

    int type;
    ss >> type;
    m_topicFilterType = (TopicFilterType)type;

    return true;
}

const std::string&  TopicConfig::getTopicName()
{
    return m_topicName;
}

void  TopicConfig::setTopicName(const std::string& topicName)
{
    m_topicName = topicName;
}

int  TopicConfig::getReadQueueNums()
{
    return m_readQueueNums;
}

void  TopicConfig::setReadQueueNums(int readQueueNums)
{
    m_readQueueNums = readQueueNums;
}

int  TopicConfig::getWriteQueueNums()
{
    return m_writeQueueNums;
}

void  TopicConfig::setWriteQueueNums(int writeQueueNums)
{
    m_writeQueueNums = writeQueueNums;
}

int  TopicConfig::getPerm()
{
    return m_perm;
}

void  TopicConfig::setPerm(int perm)
{
    m_perm = perm;
}

TopicFilterType  TopicConfig::getTopicFilterType()
{
    return m_topicFilterType;
}

void  TopicConfig::setTopicFilterType(TopicFilterType topicFilterType)
{
    m_topicFilterType = topicFilterType;
}

int  TopicConfig::getTopicSysFlag()
{
    return m_topicSysFlag;
}

void  TopicConfig::setTopicSysFlag(int perm)
{
    m_topicSysFlag = perm;
}

bool  TopicConfig::isOrder()
{
    return m_order;
}

void  TopicConfig::setOrder(bool order)
{
    m_order = order;
}


}
