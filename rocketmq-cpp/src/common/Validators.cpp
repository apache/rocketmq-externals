/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
#include <stdio.h>
#include <stdlib.h>
namespace rocketmq {

const string Validators::validPatternStr = "^[a-zA-Z0-9_-]+$";
const int Validators::CHARACTER_MAX_LENGTH = 255;
//<!***************************************************************************
bool Validators::regularExpressionMatcher(const string& origin,
                                          const string& patternStr) {
  if (UtilAll::isBlank(origin)) {
    return false;
  }

  if (UtilAll::isBlank(patternStr)) {
    return true;
  }

  // Pattern pattern = Pattern.compile(patternStr);
  // Matcher matcher = pattern.matcher(origin);

  // return matcher.matches();
  return true;
}

string Validators::getGroupWithRegularExpression(const string& origin,
                                                 const string& patternStr) {
  /*Pattern pattern = Pattern.compile(patternStr);
  Matcher matcher = pattern.matcher(origin);
  while (matcher.find()) {
  return matcher.group(0);
  }*/
  return "";
}

void Validators::checkTopic(const string& topic) {
  if (UtilAll::isBlank(topic)) {
    THROW_MQEXCEPTION(MQClientException, "the specified topic is blank", -1);
  }

  if ((int)topic.length() > CHARACTER_MAX_LENGTH) {
    THROW_MQEXCEPTION(
        MQClientException,
        "the specified topic is longer than topic max length 255.", -1);
  }

  if (topic == DEFAULT_TOPIC) {
    THROW_MQEXCEPTION(
        MQClientException,
        "the topic[" + topic + "] is conflict with default topic.", -1);
  }

  if (!regularExpressionMatcher(topic, validPatternStr)) {
    string str;
    str = "the specified topic[" + topic +
          "] contains illegal characters, allowing only" + validPatternStr;

    THROW_MQEXCEPTION(MQClientException, str.c_str(), -1);
  }
}

void Validators::checkGroup(const string& group) {
  if (UtilAll::isBlank(group)) {
    THROW_MQEXCEPTION(MQClientException, "the specified group is blank", -1);
  }

  if (!regularExpressionMatcher(group, validPatternStr)) {
    string str;
    str = "the specified group[" + group +
          "] contains illegal characters, allowing only" + validPatternStr;

    THROW_MQEXCEPTION(MQClientException, str.c_str(), -1);
  }
  if ((int)group.length() > CHARACTER_MAX_LENGTH) {
    THROW_MQEXCEPTION(
        MQClientException,
        "the specified group is longer than group max length 255.", -1);
  }
}

void Validators::checkMessage(const MQMessage& msg, int maxMessageSize) {
  checkTopic(msg.getTopic());

  string body = msg.getBody();
  //<!body;
  if (body.empty()) {
    THROW_MQEXCEPTION(MQClientException, "the message body is empty", -1);
  }

  if ((int)body.length() > maxMessageSize) {
    char info[256];
    sprintf(info, "the message body size over max value, MAX: %d",
            maxMessageSize);
    THROW_MQEXCEPTION(MQClientException, info, -1);
  }
}

//<!***************************************************************************
}  //<!end namespace;
