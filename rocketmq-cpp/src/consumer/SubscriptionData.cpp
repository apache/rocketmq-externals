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
#include "SubscriptionData.h"
#include <algorithm>
#include <sstream>
#include <vector>
#include "UtilAll.h"
#include "Logging.h"
namespace rocketmq {
//<!************************************************************************
SubscriptionData::SubscriptionData() {
  m_subVersion = UtilAll::currentTimeMillis();
}

SubscriptionData::SubscriptionData(const string& topic, const string& subString)
    : m_topic(topic), m_subString(subString) {
  m_subVersion = UtilAll::currentTimeMillis();
}

SubscriptionData::SubscriptionData(const SubscriptionData& other) {
  m_subString = other.m_subString;
  m_subVersion = other.m_subVersion;
  m_tagSet = other.m_tagSet;
  m_topic = other.m_topic;
  m_codeSet = other.m_codeSet;
}

const string& SubscriptionData::getTopic() const { return m_topic; }

const string& SubscriptionData::getSubString() const { return m_subString; }

void SubscriptionData::setSubString(const string& sub) { m_subString = sub; }

int64 SubscriptionData::getSubVersion() const { return m_subVersion; }

void SubscriptionData::putTagsSet(const string& tag) {
  m_tagSet.push_back(tag);
}

bool SubscriptionData::containTag(const string& tag) {
  return std::find(m_tagSet.begin(), m_tagSet.end(), tag) != m_tagSet.end();
}

vector<string>& SubscriptionData::getTagsSet() { return m_tagSet; }

bool SubscriptionData::operator==(const SubscriptionData& other) const {
  if (!m_subString.compare(other.m_subString)) {
    return false;
  }
  if (m_subVersion != other.m_subVersion) {
    return false;
  }
  if (m_tagSet.size() != other.m_tagSet.size()) {
    return false;
  }
  if (!m_topic.compare(other.m_topic)) {
    return false;
  }
  return true;
}

bool SubscriptionData::operator<(const SubscriptionData& other) const {
  int ret = m_topic.compare(other.m_topic);
  if (ret < 0) {
    return true;
  } else if (ret == 0) {
    ret = m_subString.compare(other.m_subString);
    if (ret < 0) {
      return true;
    } else {
      return false;
    }
  } else {
    return false;
  }
}

void SubscriptionData::putCodeSet(const string& tag) {
  int value = atoi(tag.c_str());
  m_codeSet.push_back(value);
}

Json::Value SubscriptionData::toJson() const {
  Json::Value outJson;
  outJson["subString"] = m_subString;
  outJson["subVersion"] = UtilAll::to_string(m_subVersion);
  outJson["topic"] = m_topic;

  {
    vector<string>::const_iterator it = m_tagSet.begin();
    for (; it != m_tagSet.end(); it++) {
      outJson["tagsSet"].append(*it);
    }
  }

  {
    vector<int>::const_iterator it = m_codeSet.begin();
    for (; it != m_codeSet.end(); it++) {
      outJson["codeSet"].append(*it);
    }
  }
  return outJson;
}

//<!***************************************************************************
}  //<!end namespace;
