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
#ifndef __TOPICROUTEDATA_H__
#define __TOPICROUTEDATA_H__
#include <algorithm>
#include "Logging.h"
#include "UtilAll.h"
#include "dataBlock.h"
#include "json/json.h"

namespace rocketmq {
//<!***************************************************************************
struct QueueData {
  string brokerName;
  int readQueueNums;
  int writeQueueNums;
  int perm;

  bool operator<(const QueueData& other) const {
    return brokerName < other.brokerName;
  }

  bool operator==(const QueueData& other) const {
    if (brokerName == other.brokerName &&
        readQueueNums == other.readQueueNums &&
        writeQueueNums == other.writeQueueNums && perm == other.perm) {
      return true;
    }
    return false;
  }
};

//<!***************************************************************************
struct BrokerData {
  string brokerName;
  map<int, string> brokerAddrs;  //<!0:master,1,2.. slave

  bool operator<(const BrokerData& other) const {
    return brokerName < other.brokerName;
  }

  bool operator==(const BrokerData& other) const {
    if (brokerName == other.brokerName && brokerAddrs == other.brokerAddrs) {
      return true;
    }
    return false;
  }
};

//<!************************************************************************/
class TopicRouteData {
 public:
  virtual ~TopicRouteData() {
    m_brokerDatas.clear();
    m_queueDatas.clear();
  }

  static TopicRouteData* Decode(const MemoryBlock* mem) {
    //<!see doc/TopicRouteData.json;
    const char* const pData = static_cast<const char*>(mem->getData());
    string data(pData, mem->getSize());
    
    Json::Value root;
    Json::CharReaderBuilder charReaderBuilder;
    charReaderBuilder.settings_["allowNumericKeys"] = true;
    unique_ptr<Json::CharReader> pCharReaderPtr(charReaderBuilder.newCharReader());
    const char* begin = pData;
    const char* end = pData + mem->getSize(); 
    string errs;
    if (!pCharReaderPtr->parse(begin, end, &root, &errs)) {
      LOG_ERROR("parse json error:%s, value isArray:%d, isObject:%d", errs.c_str(), root.isArray(), root.isObject());
      return NULL;
    }

    TopicRouteData* trd = new TopicRouteData();
    trd->setOrderTopicConf(root["orderTopicConf"].asString());

    Json::Value qds = root["queueDatas"];
    for (unsigned int i = 0; i < qds.size(); i++) {
      QueueData d;
      Json::Value qd = qds[i];
      d.brokerName = qd["brokerName"].asString();
      d.readQueueNums = qd["readQueueNums"].asInt();
      d.writeQueueNums = qd["writeQueueNums"].asInt();
      d.perm = qd["perm"].asInt();

      trd->getQueueDatas().push_back(d);
    }

    sort(trd->getQueueDatas().begin(), trd->getQueueDatas().end());

    Json::Value bds = root["brokerDatas"];
    for (unsigned int i = 0; i < bds.size(); i++) {
      BrokerData d;
      Json::Value bd = bds[i];
      d.brokerName = bd["brokerName"].asString();

      LOG_DEBUG("brokerName:%s", d.brokerName.c_str());

      Json::Value bas = bd["brokerAddrs"];
      Json::Value::Members mbs = bas.getMemberNames();
      for (size_t i = 0; i < mbs.size(); i++) {
        string key = mbs.at(i);
        LOG_DEBUG("brokerid:%s,brokerAddr:%s", key.c_str(),
                  bas[key].asString().c_str());
        d.brokerAddrs[atoi(key.c_str())] = bas[key].asString();
      }

      trd->getBrokerDatas().push_back(d);
    }

    sort(trd->getBrokerDatas().begin(), trd->getBrokerDatas().end());

    return trd;
  }

  string selectBrokerAddr() {
    vector<BrokerData>::iterator it = m_brokerDatas.begin();
    for (; it != m_brokerDatas.end(); ++it) {
      map<int, string>::iterator it1 = (*it).brokerAddrs.find(MASTER_ID);
      if (it1 != (*it).brokerAddrs.end()) {
        return it1->second;
      }
    }
    return "";
  }


  vector<QueueData>& getQueueDatas() { return m_queueDatas; }

  vector<BrokerData>& getBrokerDatas() { return m_brokerDatas; }

  const string& getOrderTopicConf() const { return m_orderTopicConf; }

  void setOrderTopicConf(const string& orderTopicConf) {
    m_orderTopicConf = orderTopicConf;
  }

  bool operator==(const TopicRouteData& other) const {
    if (m_brokerDatas != other.m_brokerDatas) {
      return false;
    }

    if (m_orderTopicConf != other.m_orderTopicConf) {
      return false;
    }

    if (m_queueDatas != other.m_queueDatas) {
      return false;
    }
    return true;
  }

 public:
 private:
  string m_orderTopicConf;
  vector<QueueData> m_queueDatas;
  vector<BrokerData> m_brokerDatas;
};

}  //<!end namespace;

#endif
