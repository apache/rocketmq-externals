/**
* Copyright (C) 2013 kangliqiang, kangliq@163.com
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
#include "Validators.h"

#include <stdlib.h>
#include <stdio.h>
#include "MQClientException.h"
#include "UtilAll.h"
#include "MixAll.h"
#include "Message.h"
#include "MQProtos.h"
#include "DefaultMQProducer.h"

namespace rmq
{

const std::string Validators::validPatternStr = "^[a-zA-Z0-9_-]+$";
const size_t Validators::CHARACTER_MAX_LENGTH = 255;

bool Validators::regularExpressionMatcher(const std::string& origin, const std::string& patternStr)
{
    if (UtilAll::isBlank(origin))
    {
        return false;
    }

    if (UtilAll::isBlank(patternStr))
    {
        return true;
    }

    //Pattern pattern = Pattern.compile(patternStr);
    //Matcher matcher = pattern.matcher(origin);

    //return matcher.matches();
    return true;
}

std::string Validators::getGroupWithRegularExpression(const std::string& origin, const std::string& patternStr)
{
    /*Pattern pattern = Pattern.compile(patternStr);
    Matcher matcher = pattern.matcher(origin);
    while (matcher.find()) {
    return matcher.group(0);
    }*/
    return "";
}

void Validators::checkTopic(const std::string& topic)
{
    if (UtilAll::isBlank(topic))
    {
        THROW_MQEXCEPTION(MQClientException, "the specified topic is blank", -1);
    }

    if (topic.length() > CHARACTER_MAX_LENGTH)
    {
        THROW_MQEXCEPTION(MQClientException, "the specified topic is longer than topic max length 255.", -1);
    }

    // TopicÃû×ÖÊÇ·ñÓë±£Áô×Ö¶Î³åÍ»
    if (topic == MixAll::DEFAULT_TOPIC)
    {
        THROW_MQEXCEPTION(MQClientException, "the topic[" + topic + "] is conflict with default topic.", -1);
    }

    if (!regularExpressionMatcher(topic, validPatternStr))
    {
        std::string str;
        str = "the specified topic[" + topic + "] contains illegal characters, allowing only" + validPatternStr;

        THROW_MQEXCEPTION(MQClientException, str.c_str(), -1);
    }
}

void Validators::checkGroup(const std::string& group)
{
    if (UtilAll::isBlank(group))
    {
        THROW_MQEXCEPTION(MQClientException, "the specified group is blank", -1);
    }

    if (!regularExpressionMatcher(group, validPatternStr))
    {
        std::string str;
        str = "the specified group[" + group + "] contains illegal characters, allowing only" + validPatternStr;

        THROW_MQEXCEPTION(MQClientException, str.c_str(), -1);
    }
    if (group.length() > CHARACTER_MAX_LENGTH)
    {
        THROW_MQEXCEPTION(MQClientException, "the specified group is longer than group max length 255.", -1);
    }
}

void Validators::checkMessage(const Message& msg, DefaultMQProducer* pDefaultMQProducer)
{
    checkTopic(msg.getTopic());

    //// body
    if (msg.getBody() == NULL)
    {
        THROW_MQEXCEPTION(MQClientException, "the message body is null", MESSAGE_ILLEGAL_VALUE);
    }

    if (msg.getBodyLen() == 0)
    {
        THROW_MQEXCEPTION(MQClientException, "the message body length is zero", MESSAGE_ILLEGAL_VALUE);
    }

    if (msg.getBodyLen() > pDefaultMQProducer->getMaxMessageSize())
    {
        char info[256];
        snprintf(info, sizeof(info), "the message body size over max value, MAX: %d", pDefaultMQProducer->getMaxMessageSize());
        THROW_MQEXCEPTION(MQClientException, info, MESSAGE_ILLEGAL_VALUE);
    }
}

}
