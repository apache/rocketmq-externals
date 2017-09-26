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
#ifndef __MQCLIENTFACTORY_H__
#define __MQCLIENTFACTORY_H__
#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/thread/thread.hpp>
#include "FindBrokerResult.h"
#include "MQClientAPIImpl.h"
#include "MQClientException.h"
#include "MQConsumer.h"
#include "MQDecoder.h"
#include "MQMessageQueue.h"
#include "MQProducer.h"
#include "PermName.h"
#include "QueryResult.h"
#include "ServiceState.h"
#include "SocketUtil.h"
#include "TopicConfig.h"
#include "TopicRouteData.h"

namespace rocketmq {
//<!************************************************************************
class TopicPublishInfo;
class MQClientFactory {
 public:
  MQClientFactory(const string& clientID, int pullThreadNum,
                  uint64_t tcpConnectTimeout,
                  uint64_t tcpTransportTryLockTimeout, string unitName);
  virtual ~MQClientFactory();

  void start();
  void shutdown();
  bool registerProducer(MQProducer* pProducer);
  void unregisterProducer(MQProducer* pProducer);
  bool registerConsumer(MQConsumer* pConsumer);
  void unregisterConsumer(MQConsumer* pConsumer);

  void createTopic(const string& key, const string& newTopic, int queueNum,
                   const SessionCredentials& session_credentials);
  int64 minOffset(const MQMessageQueue& mq,
                  const SessionCredentials& session_credentials);
  int64 maxOffset(const MQMessageQueue& mq,
                  const SessionCredentials& session_credentials);
  int64 searchOffset(const MQMessageQueue& mq, int64 timestamp,
                     const SessionCredentials& session_credentials);
  int64 earliestMsgStoreTime(const MQMessageQueue& mq,
                             const SessionCredentials& session_credentials);
  MQMessageExt* viewMessage(const string& msgId,
                            const SessionCredentials& session_credentials);
  QueryResult queryMessage(const string& topic, const string& key, int maxNum,
                           int64 begin, int64 end,
                           const SessionCredentials& session_credentials);

  MQClientAPIImpl* getMQClientAPIImpl() const;
  MQProducer* selectProducer(const string& group);
  MQConsumer* selectConsumer(const string& group);

  boost::shared_ptr<TopicPublishInfo> topicRouteData2TopicPublishInfo(
      const string& topic, TopicRouteData* pRoute);

  void topicRouteData2TopicSubscribeInfo(const string& topic,
                                         TopicRouteData* pRoute,
                                         vector<MQMessageQueue>& mqs);

  FindBrokerResult* findBrokerAddressInSubscribe(const string& brokerName,
                                                 int brokerId,
                                                 bool onlyThisBroker);

  FindBrokerResult* findBrokerAddressInAdmin(const string& brokerName);

  string findBrokerAddressInPublish(const string& brokerName);

  boost::shared_ptr<TopicPublishInfo> tryToFindTopicPublishInfo(
      const string& topic, const SessionCredentials& session_credentials);

  void fetchSubscribeMessageQueues(
      const string& topic, vector<MQMessageQueue>& mqs,
      const SessionCredentials& session_credentials);

  bool updateTopicRouteInfoFromNameServer(
      const string& topic, const SessionCredentials& session_credentials,
      bool isDefault = false);
  void rebalanceImmediately();
  void doRebalanceByConsumerGroup(const string& consumerGroup);
  void sendHeartbeatToAllBroker();

  void findConsumerIds(const string& topic, const string& group,
                       vector<string>& cids,
                       const SessionCredentials& session_credentials);
  void resetOffset(const string& group, const string& topic,
                   const map<MQMessageQueue, int64>& offsetTable);
  ConsumerRunningInfo* consumerRunningInfo(const string& consumerGroup);
  bool getSessionCredentialFromConsumer(const string& consumerGroup,
                                        SessionCredentials& sessionCredentials);
  void addBrokerToAddrMap(const string& brokerName,
                          map<int, string>& brokerAddrs);
  map<string, map<int, string>> getBrokerAddrMap();
  void clearBrokerAddrMap();

 private:
  void unregisterClient(const string& producerGroup,
                        const string& consumerGroup,
                        const SessionCredentials& session_credentials);
  TopicRouteData* getTopicRouteData(const string& topic);
  void addTopicRouteData(const string& topic, TopicRouteData* pTopicRouteData);
  HeartbeatData* prepareHeartbeatData();

  void startScheduledTask(bool startFetchNSService = true);
  //<!timer async callback
  void fetchNameServerAddr(boost::system::error_code& ec,
                           boost::asio::deadline_timer* t);
  void updateTopicRouteInfo(boost::system::error_code& ec,
                            boost::asio::deadline_timer* t);
  void timerCB_sendHeartbeatToAllBroker(boost::system::error_code& ec,
                                        boost::asio::deadline_timer* t);

  // consumer related operation
  void consumer_timerOperation();
  void persistAllConsumerOffset(boost::system::error_code& ec,
                                boost::asio::deadline_timer* t);
  void doRebalance();
  void timerCB_doRebalance(boost::system::error_code& ec,
                           boost::asio::deadline_timer* t);
  bool getSessionCredentialFromConsumerTable(
      SessionCredentials& sessionCredentials);
  bool addConsumerToTable(const string& consumerName, MQConsumer* pMQConsumer);
  void eraseConsumerFromTable(const string& consumerName);
  int getConsumerTableSize();
  void getTopicListFromConsumerSubscription(set<string>& topicList);
  void updateConsumerSubscribeTopicInfo(const string& topic,
                                        vector<MQMessageQueue> mqs);
  void insertConsumerInfoToHeartBeatData(HeartbeatData* pHeartbeatData);

  // producer related operation
  bool getSessionCredentialFromProducerTable(
      SessionCredentials& sessionCredentials);
  bool addProducerToTable(const string& producerName, MQProducer* pMQProducer);
  void eraseProducerFromTable(const string& producerName);
  int getProducerTableSize();
  void insertProducerInfoToHeartBeatData(HeartbeatData* pHeartbeatData);

  // topicPublishInfo related operation
  void addTopicInfoToTable(
      const string& topic,
      boost::shared_ptr<TopicPublishInfo> pTopicPublishInfo);
  void eraseTopicInfoFromTable(const string& topic);
  bool isTopicInfoValidInTable(const string& topic);
  boost::shared_ptr<TopicPublishInfo> getTopicPublishInfoFromTable(
      const string& topic);
  void getTopicListFromTopicPublishInfo(set<string>& topicList);

  void getSessionCredentialsFromOneOfProducerOrConsumer(
      SessionCredentials& session_credentials);

 private:
  string m_clientId;
  string m_nameSrvDomain;  // per clientId
  ServiceState m_serviceState;
  bool m_bFetchNSService;

  //<! group --> MQProducer;
  typedef map<string, MQProducer*> MQPMAP;
  boost::mutex m_producerTableMutex;
  MQPMAP m_producerTable;

  //<! group --> MQConsumer;
  typedef map<string, MQConsumer*> MQCMAP;
  boost::mutex m_consumerTableMutex;
  MQCMAP m_consumerTable;

  //<! Topic---> TopicRouteData
  typedef map<string, TopicRouteData*> TRDMAP;
  boost::mutex m_topicRouteTableMutex;
  TRDMAP m_topicRouteTable;

  //<!-----brokerName
  //<!     ------brokerid;
  //<!     ------add;
  boost::mutex m_brokerAddrlock;
  typedef map<string, map<int, string>> BrokerAddrMAP;
  BrokerAddrMAP m_brokerAddrTable;

  //<!topic ---->TopicPublishInfo> ;
  typedef map<string, boost::shared_ptr<TopicPublishInfo>> TPMap;
  boost::mutex m_topicPublishInfoTableMutex;
  TPMap m_topicPublishInfoTable;
  boost::mutex m_factoryLock;
  boost::mutex m_topicPublishInfoLock;

  //<!clientapi;
  unique_ptr<MQClientAPIImpl> m_pClientAPIImpl;
  unique_ptr<ClientRemotingProcessor> m_pClientRemotingProcessor;

  boost::asio::io_service m_async_ioService;
  unique_ptr<boost::thread> m_async_service_thread;

  boost::asio::io_service m_consumer_async_ioService;
  unique_ptr<boost::thread> m_consumer_async_service_thread;
};

}  //<!end namespace;

#endif
