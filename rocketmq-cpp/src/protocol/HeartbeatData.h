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

#ifndef __HEARTBEATDATA_H__
#define __HEARTBEATDATA_H__
#include <boost/thread/thread.hpp>
#include <cstdlib>
#include <string>
#include <vector>
#include "ConsumeType.h"
#include "SubscriptionData.h"

namespace rocketmq {
//<!***************************************************************************
class ProducerData {
 public:
  ProducerData(){};
  bool operator<(const ProducerData& pd) const {
    return groupName < pd.groupName;
  }
  Json::Value toJson() const {
    Json::Value outJson;
    outJson["groupName"] = groupName;
    return outJson;
  }

 public:
  string groupName;
};

//<!***************************************************************************
class ConsumerData {
 public:
  ConsumerData(){};
  virtual ~ConsumerData() { subscriptionDataSet.clear(); }
  bool operator<(const ConsumerData& cd) const {
    return groupName < cd.groupName;
  }

  Json::Value toJson() const {
    Json::Value outJson;
    outJson["groupName"] = groupName;
    outJson["consumeFromWhere"] = consumeFromWhere;
    outJson["consumeType"] = consumeType;
    outJson["messageModel"] = messageModel;

    vector<SubscriptionData>::const_iterator it = subscriptionDataSet.begin();
    for (; it != subscriptionDataSet.end(); it++) {
      outJson["subscriptionDataSet"].append((*it).toJson());
    }

    return outJson;
  }

 public:
  string groupName;
  ConsumeType consumeType;
  MessageModel messageModel;
  ConsumeFromWhere consumeFromWhere;
  vector<SubscriptionData> subscriptionDataSet;
};

//<!***************************************************************************
class HeartbeatData {
 public:
  virtual ~HeartbeatData() {
    m_producerDataSet.clear();
    m_consumerDataSet.clear();
  }
  void Encode(string& outData) {
    Json::Value root;

    //<!id;
    root["clientID"] = m_clientID;

    //<!consumer;
    {
      boost::lock_guard<boost::mutex> lock(m_consumerDataMutex);
      vector<ConsumerData>::iterator itc = m_consumerDataSet.begin();
      for (; itc != m_consumerDataSet.end(); itc++) {
        root["consumerDataSet"].append((*itc).toJson());
      }
    }

    //<!producer;
    {
      boost::lock_guard<boost::mutex> lock(m_producerDataMutex);
      vector<ProducerData>::iterator itp = m_producerDataSet.begin();
      for (; itp != m_producerDataSet.end(); itp++) {
        root["producerDataSet"].append((*itp).toJson());
      }
    }
    //<!output;
    Json::FastWriter fastwrite;
    outData = fastwrite.write(root);
  }

  void setClientID(const string& clientID) { m_clientID = clientID; }

  bool isProducerDataSetEmpty() {
    boost::lock_guard<boost::mutex> lock(m_producerDataMutex);
    return m_producerDataSet.empty();
  }

  void insertDataToProducerDataSet(ProducerData& producerData) {
    boost::lock_guard<boost::mutex> lock(m_producerDataMutex);
    m_producerDataSet.push_back(producerData);
  }

  bool isConsumerDataSetEmpty() {
    boost::lock_guard<boost::mutex> lock(m_consumerDataMutex);
    return m_consumerDataSet.empty();
  }

  void insertDataToConsumerDataSet(ConsumerData& consumerData) {
    boost::lock_guard<boost::mutex> lock(m_consumerDataMutex);
    m_consumerDataSet.push_back(consumerData);
  }

 private:
  string m_clientID;
  vector<ProducerData> m_producerDataSet;
  vector<ConsumerData> m_consumerDataSet;
  boost::mutex m_producerDataMutex;
  boost::mutex m_consumerDataMutex;
};
}  //<!end namespace;

#endif
