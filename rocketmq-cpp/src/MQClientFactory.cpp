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
#include "MQClientFactory.h"
#include "ConsumerRunningInfo.h"
#include "Logging.h"
#include "MQClientManager.h"
#include "PullRequest.h"
#include "Rebalance.h"
#include "TopicPublishInfo.h"

#define MAX_BUFF_SIZE 8192
#define SAFE_BUFF_SIZE 7936  // 8192 - 256 = 7936
#define PROCESS_NAME_BUF_SIZE 256

namespace rocketmq {
//<!***************************************************************************
MQClientFactory::MQClientFactory(const string& clientID, int pullThreadNum,
                                 uint64_t tcpConnectTimeout,
                                 uint64_t tcpTransportTryLockTimeout,
                                 string unitName)
    : m_bFetchNSService(true) {
  m_clientId = clientID;
  // default Topic register;
  boost::shared_ptr<TopicPublishInfo> pDefaultTopicInfo(new TopicPublishInfo());
  m_topicPublishInfoTable[DEFAULT_TOPIC] = pDefaultTopicInfo;
  m_pClientRemotingProcessor.reset(new ClientRemotingProcessor(this));
  m_pClientAPIImpl.reset(new MQClientAPIImpl(
      m_clientId, m_pClientRemotingProcessor.get(), pullThreadNum,
      tcpConnectTimeout, tcpTransportTryLockTimeout, unitName));
  m_serviceState = CREATE_JUST;
  LOG_DEBUG("MQClientFactory construct");
}

MQClientFactory::~MQClientFactory() {
  LOG_INFO("MQClientFactory:%s destruct", m_clientId.c_str());

  for (TRDMAP::iterator itp = m_topicRouteTable.begin();
       itp != m_topicRouteTable.end(); ++itp) {
    delete itp->second;
  }

  m_producerTable.clear();
  m_consumerTable.clear();
  m_topicRouteTable.clear();
  m_brokerAddrTable.clear();
  m_topicPublishInfoTable.clear();

  m_pClientAPIImpl = NULL;
}

void MQClientFactory::start() {
  switch (m_serviceState) {
    case CREATE_JUST:
      LOG_INFO("MQClientFactory:%s start", m_clientId.c_str());
      m_serviceState = START_FAILED;
      //<!start time task;
      m_async_service_thread.reset(new boost::thread(boost::bind(
          &MQClientFactory::startScheduledTask, this, m_bFetchNSService)));
      m_serviceState = RUNNING;
      break;
    case RUNNING:
    case SHUTDOWN_ALREADY:
    case START_FAILED:
      LOG_INFO("The Factory object:%s start failed with fault state:%d",
               m_clientId.c_str(), m_serviceState);
      break;
    default:
      break;
  }
}

void MQClientFactory::updateTopicRouteInfo(boost::system::error_code& ec,
                                           boost::asio::deadline_timer* t) {
  if ((getConsumerTableSize() == 0) && (getProducerTableSize() == 0)) {
    return;
  }

  set<string> topicList;
  //<!Consumer;
  getTopicListFromConsumerSubscription(topicList);

  //<!Producer;
  getTopicListFromTopicPublishInfo(topicList);

  //<! update;
  {
    SessionCredentials session_credentials;
    getSessionCredentialsFromOneOfProducerOrConsumer(session_credentials);
    set<string>::iterator it = topicList.begin();
    for (; it != topicList.end(); ++it) {
      updateTopicRouteInfoFromNameServer(*it, session_credentials);
    }
  }

  boost::system::error_code e;
  t->expires_from_now(t->expires_from_now() + boost::posix_time::seconds(30),
                      e);
  t->async_wait(
      boost::bind(&MQClientFactory::updateTopicRouteInfo, this, ec, t));
}

TopicRouteData* MQClientFactory::getTopicRouteData(const string& topic) {
  boost::lock_guard<boost::mutex> lock(m_topicRouteTableMutex);
  if (m_topicRouteTable.find(topic) != m_topicRouteTable.end()) {
    return m_topicRouteTable[topic];
  }
  return NULL;
}

void MQClientFactory::addTopicRouteData(const string& topic,
                                        TopicRouteData* pTopicRouteData) {
  boost::lock_guard<boost::mutex> lock(m_topicRouteTableMutex);
  if (m_topicRouteTable.find(topic) != m_topicRouteTable.end()) {
    delete m_topicRouteTable[topic];
    m_topicRouteTable.erase(topic);
  }
  m_topicRouteTable[topic] = pTopicRouteData;
}

boost::shared_ptr<TopicPublishInfo> MQClientFactory::tryToFindTopicPublishInfo(
    const string& topic, const SessionCredentials& session_credentials) {
  boost::lock_guard<boost::mutex> lock(
      m_topicPublishInfoLock);  // add topicPublishInfoLock to avoid con-current
                                // excuting updateTopicRouteInfoFromNameServer
                                // when producer send msg  before topicRouteInfo
                                // was got;
  if (!isTopicInfoValidInTable(topic)) {
    updateTopicRouteInfoFromNameServer(topic, session_credentials);
  }
  //<!if not exsit ,update dafult topic;
  if (!isTopicInfoValidInTable(topic)) {
    LOG_INFO("updateTopicRouteInfoFromNameServer with default");
    updateTopicRouteInfoFromNameServer(topic, session_credentials, true);
  }

  if (!isTopicInfoValidInTable(topic)) {
    LOG_WARN("tryToFindTopicPublishInfo null:%s", topic.c_str());
    boost::shared_ptr<TopicPublishInfo> pTopicPublishInfo;
    return pTopicPublishInfo;
  }

  return getTopicPublishInfoFromTable(topic);
}

bool MQClientFactory::updateTopicRouteInfoFromNameServer(
    const string& topic, const SessionCredentials& session_credentials,
    bool isDefault /* = false */) {
  boost::lock_guard<boost::mutex> lock(m_factoryLock);
  unique_ptr<TopicRouteData> pTopicRouteData;
  LOG_INFO("updateTopicRouteInfoFromNameServer start:%s", topic.c_str());

  if (isDefault) {
    pTopicRouteData.reset(m_pClientAPIImpl->getTopicRouteInfoFromNameServer(
        DEFAULT_TOPIC, 1000 * 5, session_credentials));
    if (pTopicRouteData != NULL) {
      vector<QueueData>& queueDatas = pTopicRouteData->getQueueDatas();
      vector<QueueData>::iterator it = queueDatas.begin();
      for (; it != queueDatas.end(); ++it) {
        // ¶ÁÐ´·ÖÇø¸öÊýÊÇÒ»ÖÂ£¬¹ÊÖ»×öÒ»´ÎÅÐ¶Ï;
        int queueNums = std::min(4, it->readQueueNums);
        it->readQueueNums = queueNums;
        it->writeQueueNums = queueNums;
      }
    }
  } else {
    pTopicRouteData.reset(m_pClientAPIImpl->getTopicRouteInfoFromNameServer(
        topic, 1000 * 5, session_credentials));
  }

  if (pTopicRouteData != NULL) {
    LOG_INFO("updateTopicRouteInfoFromNameServer has data");
    TopicRouteData* pTemp = getTopicRouteData(topic);
    bool changed = true;
    if (pTemp != NULL) {
      changed = !(*pTemp == *pTopicRouteData);
    }

    if (getConsumerTableSize() > 0) {
      vector<MQMessageQueue> mqs;
      topicRouteData2TopicSubscribeInfo(topic, pTopicRouteData.get(), mqs);
      updateConsumerSubscribeTopicInfo(topic, mqs);
    }

    if (changed) {
      //<!update Broker addr
      LOG_INFO("updateTopicRouteInfoFromNameServer changed:%s", topic.c_str());
      vector<BrokerData> brokerList = pTopicRouteData->getBrokerDatas();
      vector<BrokerData>::iterator it = brokerList.begin();
      for (; it != brokerList.end(); ++it) {
        LOG_INFO(
            "updateTopicRouteInfoFromNameServer changed with broker name:%s",
            (*it).brokerName.c_str());
        addBrokerToAddrMap((*it).brokerName, (*it).brokerAddrs);
      }

      //<! update publish info;
      {
        boost::shared_ptr<TopicPublishInfo> publishInfo(
            topicRouteData2TopicPublishInfo(topic, pTopicRouteData.get()));
        addTopicInfoToTable(topic, publishInfo);  // erase first, then add
      }

      //<! update subscribe info
      addTopicRouteData(topic, pTopicRouteData.release());
    }
    LOG_DEBUG("updateTopicRouteInfoFromNameServer end:%s", topic.c_str());
    return true;
  }
  LOG_DEBUG("updateTopicRouteInfoFromNameServer end null:%s", topic.c_str());
  return false;
}

boost::shared_ptr<TopicPublishInfo>
MQClientFactory::topicRouteData2TopicPublishInfo(const string& topic,
                                                 TopicRouteData* pRoute) {
  boost::shared_ptr<TopicPublishInfo> info(new TopicPublishInfo());
  string OrderTopicConf = pRoute->getOrderTopicConf();
  //<! order msg
  if (!OrderTopicConf.empty()) {
    // "broker-a:8";"broker-b:8"
    vector<string> brokers;
    UtilAll::Split(brokers, OrderTopicConf, ';');
    for (size_t i = 0; i < brokers.size(); i++) {
      vector<string> item;
      UtilAll::Split(item, brokers[i], ':');
      int nums = atoi(item[1].c_str());
      for (int i = 0; i < nums; i++) {
        MQMessageQueue mq(topic, item[0], i);
        info->updateMessageQueueList(mq);
      }
    }
  }
  //<!no order msg
  else {
    vector<QueueData>& queueDatas = pRoute->getQueueDatas();
    vector<QueueData>::iterator it = queueDatas.begin();
    for (; it != queueDatas.end(); ++it) {
      QueueData& qd = (*it);
      if (PermName::isWriteable(qd.perm)) {
        string addr = findBrokerAddressInPublish(qd.brokerName);
        if (addr.empty()) {
          continue;
        }
        for (int i = 0; i < qd.writeQueueNums; i++) {
          MQMessageQueue mq(topic, qd.brokerName, i);
          info->updateMessageQueueList(mq);
        }
      }
    }
  }
  return info;
}

void MQClientFactory::topicRouteData2TopicSubscribeInfo(
    const string& topic, TopicRouteData* pRoute, vector<MQMessageQueue>& mqs) {
  mqs.clear();
  vector<QueueData>& queueDatas = pRoute->getQueueDatas();
  vector<QueueData>::iterator it = queueDatas.begin();
  for (; it != queueDatas.end(); ++it) {
    QueueData& qd = (*it);
    if (PermName::isReadable(qd.perm)) {
      for (int i = 0; i < qd.readQueueNums; i++) {
        MQMessageQueue mq(topic, qd.brokerName, i);
        mqs.push_back(mq);
      }
    }
  }
}

void MQClientFactory::shutdown() {
  if (getConsumerTableSize() != 0) return;

  if (getProducerTableSize() != 0) return;

  switch (m_serviceState) {
    case RUNNING: {
      //<! stop;
      if (m_consumer_async_service_thread) {
        m_consumer_async_ioService.stop();
        m_consumer_async_service_thread->interrupt();
        m_consumer_async_service_thread->join();
      }
      m_async_ioService.stop();
      m_async_service_thread->interrupt();
      m_async_service_thread->join();
      m_pClientAPIImpl->stopAllTcpTransportThread();  // Note: stop all
                                                      // TcpTransport Threads
                                                      // and release all
                                                      // responseFuture
                                                      // conditions
      m_serviceState = SHUTDOWN_ALREADY;
      LOG_INFO("MQClientFactory:%s shutdown", m_clientId.c_str());
      break;
    }
    case SHUTDOWN_ALREADY:
    case CREATE_JUST:
      break;
    default:
      break;
  }

  //<!É¾³ý×Ô¼º;
  MQClientManager::getInstance()->removeClientFactory(m_clientId);
}

bool MQClientFactory::registerProducer(MQProducer* pProducer) {
  string groupName = pProducer->getGroupName();
  string namesrvaddr = pProducer->getNamesrvAddr();
  if (groupName.empty()) {
    return false;
  }

  if (!addProducerToTable(groupName, pProducer)) {
    return false;
  }

  LOG_DEBUG("registerProducer success:%s", groupName.c_str());
  //<!set nameserver;
  if (namesrvaddr.empty()) {
    string nameSrvDomain(pProducer->getNamesrvDomain());
    if (!nameSrvDomain.empty()) m_nameSrvDomain = nameSrvDomain;
    pProducer->setNamesrvAddr(
        m_pClientAPIImpl->fetchNameServerAddr(m_nameSrvDomain));
  } else {
    m_bFetchNSService = false;
    m_pClientAPIImpl->updateNameServerAddr(namesrvaddr);
    LOG_INFO("user specfied name server address: %s", namesrvaddr.c_str());
  }
  return true;
}

void MQClientFactory::unregisterProducer(MQProducer* pProducer) {
  string groupName = pProducer->getGroupName();
  unregisterClient(groupName, "", pProducer->getSessionCredentials());

  eraseProducerFromTable(groupName);
}

bool MQClientFactory::registerConsumer(MQConsumer* pConsumer) {
  string groupName = pConsumer->getGroupName();
  string namesrvaddr = pConsumer->getNamesrvAddr();
  if (groupName.empty()) {
    return false;
  }

  if (!addConsumerToTable(groupName, pConsumer)) {
    return false;
  }
  LOG_DEBUG("registerConsumer success:%s", groupName.c_str());
  //<!set nameserver;
  if (namesrvaddr.empty()) {
    string nameSrvDomain(pConsumer->getNamesrvDomain());
    if (!nameSrvDomain.empty()) m_nameSrvDomain = nameSrvDomain;
    pConsumer->setNamesrvAddr(
        m_pClientAPIImpl->fetchNameServerAddr(m_nameSrvDomain));
  } else {
    m_bFetchNSService = false;
    m_pClientAPIImpl->updateNameServerAddr(namesrvaddr);
    LOG_INFO("user specfied name server address: %s", namesrvaddr.c_str());
  }

  return true;
}

void MQClientFactory::unregisterConsumer(MQConsumer* pConsumer) {
  string groupName = pConsumer->getGroupName();
  unregisterClient("", groupName, pConsumer->getSessionCredentials());

  eraseConsumerFromTable(groupName);
}

MQProducer* MQClientFactory::selectProducer(const string& producerName) {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  if (m_producerTable.find(producerName) != m_producerTable.end()) {
    return m_producerTable[producerName];
  }
  return NULL;
}

bool MQClientFactory::getSessionCredentialFromProducerTable(
    SessionCredentials& sessionCredentials) {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  for (MQPMAP::iterator it = m_producerTable.begin();
       it != m_producerTable.end(); ++it) {
    if (it->second) sessionCredentials = it->second->getSessionCredentials();
  }

  if (sessionCredentials.isValid()) return true;

  return false;
}

bool MQClientFactory::addProducerToTable(const string& producerName,
                                         MQProducer* pMQProducer) {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  if (m_producerTable.find(producerName) != m_producerTable.end()) return false;
  m_producerTable[producerName] = pMQProducer;
  return true;
}

void MQClientFactory::eraseProducerFromTable(const string& producerName) {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  if (m_producerTable.find(producerName) != m_producerTable.end())
    m_producerTable.erase(producerName);
}

int MQClientFactory::getProducerTableSize() {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  return m_producerTable.size();
}

void MQClientFactory::insertProducerInfoToHeartBeatData(
    HeartbeatData* pHeartbeatData) {
  boost::lock_guard<boost::mutex> lock(m_producerTableMutex);
  for (MQPMAP::iterator it = m_producerTable.begin();
       it != m_producerTable.end(); ++it) {
    ProducerData producerData;
    producerData.groupName = it->first;
    pHeartbeatData->insertDataToProducerDataSet(producerData);
  }
}

MQConsumer* MQClientFactory::selectConsumer(const string& group) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  if (m_consumerTable.find(group) != m_consumerTable.end()) {
    return m_consumerTable[group];
  }
  return NULL;
}

bool MQClientFactory::getSessionCredentialFromConsumerTable(
    SessionCredentials& sessionCredentials) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  for (MQCMAP::iterator it = m_consumerTable.begin();
       it != m_consumerTable.end(); ++it) {
    if (it->second) sessionCredentials = it->second->getSessionCredentials();
  }

  if (sessionCredentials.isValid()) return true;

  return false;
}

bool MQClientFactory::getSessionCredentialFromConsumer(
    const string& consumerGroup, SessionCredentials& sessionCredentials) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  if (m_consumerTable.find(consumerGroup) != m_consumerTable.end()) {
    sessionCredentials =
        m_consumerTable[consumerGroup]->getSessionCredentials();
  }

  if (sessionCredentials.isValid()) return true;

  return false;
}

bool MQClientFactory::addConsumerToTable(const string& consumerName,
                                         MQConsumer* pMQConsumer) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  if (m_consumerTable.find(consumerName) != m_consumerTable.end()) return false;
  m_consumerTable[consumerName] = pMQConsumer;
  return true;
}

void MQClientFactory::eraseConsumerFromTable(const string& consumerName) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  if (m_consumerTable.find(consumerName) != m_consumerTable.end())
    m_consumerTable.erase(consumerName);  // do not need freee pConsumer, as it
                                          // was allocated by user
  else
    LOG_WARN("could not find consumer:%s from table", consumerName.c_str());
}

int MQClientFactory::getConsumerTableSize() {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  return m_consumerTable.size();
}

void MQClientFactory::getTopicListFromConsumerSubscription(
    set<string>& topicList) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  for (MQCMAP::iterator it = m_consumerTable.begin();
       it != m_consumerTable.end(); ++it) {
    vector<SubscriptionData> result;
    it->second->getSubscriptions(result);

    vector<SubscriptionData>::iterator iter = result.begin();
    for (; iter != result.end(); ++iter) {
      topicList.insert((*iter).getTopic());
    }
  }
}

void MQClientFactory::updateConsumerSubscribeTopicInfo(
    const string& topic, vector<MQMessageQueue> mqs) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  for (MQCMAP::iterator it = m_consumerTable.begin();
       it != m_consumerTable.end(); ++it) {
    it->second->updateTopicSubscribeInfo(topic, mqs);
  }
}

void MQClientFactory::insertConsumerInfoToHeartBeatData(
    HeartbeatData* pHeartbeatData) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  for (MQCMAP::iterator it = m_consumerTable.begin();
       it != m_consumerTable.end(); ++it) {
    MQConsumer* pConsumer = it->second;
    ConsumerData consumerData;
    consumerData.groupName = pConsumer->getGroupName();
    consumerData.consumeType = pConsumer->getConsumeType();
    consumerData.messageModel = pConsumer->getMessageModel();
    consumerData.consumeFromWhere = pConsumer->getConsumeFromWhere();

    //<!fill data;
    vector<SubscriptionData> result;
    pConsumer->getSubscriptions(result);
    consumerData.subscriptionDataSet.swap(result);

    pHeartbeatData->insertDataToConsumerDataSet(consumerData);
  }
}

void MQClientFactory::addTopicInfoToTable(
    const string& topic,
    boost::shared_ptr<TopicPublishInfo> pTopicPublishInfo) {
  boost::lock_guard<boost::mutex> lock(m_topicPublishInfoTableMutex);
  if (m_topicPublishInfoTable.find(topic) != m_topicPublishInfoTable.end()) {
    m_topicPublishInfoTable.erase(topic);
  }
  m_topicPublishInfoTable[topic] = pTopicPublishInfo;
}

void MQClientFactory::eraseTopicInfoFromTable(const string& topic) {
  boost::lock_guard<boost::mutex> lock(m_topicPublishInfoTableMutex);
  if (m_topicPublishInfoTable.find(topic) != m_topicPublishInfoTable.end()) {
    m_topicPublishInfoTable.erase(topic);
  }
}

bool MQClientFactory::isTopicInfoValidInTable(const string& topic) {
  boost::lock_guard<boost::mutex> lock(m_topicPublishInfoTableMutex);
  if (m_topicPublishInfoTable.find(topic) != m_topicPublishInfoTable.end()) {
    if (m_topicPublishInfoTable[topic]->ok()) return true;
  }
  return false;
}

boost::shared_ptr<TopicPublishInfo>
MQClientFactory::getTopicPublishInfoFromTable(const string& topic) {
  boost::lock_guard<boost::mutex> lock(m_topicPublishInfoTableMutex);
  if (m_topicPublishInfoTable.find(topic) != m_topicPublishInfoTable.end()) {
    return m_topicPublishInfoTable[topic];
  }
  boost::shared_ptr<TopicPublishInfo> pTopicPublishInfo;
  return pTopicPublishInfo;
}

void MQClientFactory::getTopicListFromTopicPublishInfo(set<string>& topicList) {
  boost::lock_guard<boost::mutex> lock(m_topicPublishInfoTableMutex);
  for (TPMap::iterator itp = m_topicPublishInfoTable.begin();
       itp != m_topicPublishInfoTable.end(); ++itp) {
    topicList.insert(itp->first);
  }
}

void MQClientFactory::clearBrokerAddrMap() {
  boost::lock_guard<boost::mutex> lock(m_brokerAddrlock);
  m_brokerAddrTable.clear();
}

void MQClientFactory::addBrokerToAddrMap(const string& brokerName,
                                         map<int, string>& brokerAddrs) {
  boost::lock_guard<boost::mutex> lock(m_brokerAddrlock);
  if (m_brokerAddrTable.find(brokerName) != m_brokerAddrTable.end()) {
    m_brokerAddrTable.erase(brokerName);
  }
  m_brokerAddrTable[brokerName] = brokerAddrs;
}

MQClientFactory::BrokerAddrMAP MQClientFactory::getBrokerAddrMap() {
  boost::lock_guard<boost::mutex> lock(m_brokerAddrlock);
  return m_brokerAddrTable;
}

string MQClientFactory::findBrokerAddressInPublish(const string& brokerName) {
  /*reslove the concurrent access m_brokerAddrTable by
  findBrokerAddressInPublish(called by sendKernlImpl) And
  sendHeartbeatToAllBroker, which leads hign RT of sendMsg
  1. change m_brokerAddrTable from hashMap to map;
  2. do not add m_factoryLock here, but copy m_brokerAddrTable,
      this is used to avoid con-current access m_factoryLock by
  findBrokerAddressInPublish(called by sendKernlImpl) And
  updateTopicRouteInfoFromNameServer

   Note: after copying m_brokerAddrTable, updateTopicRouteInfoFromNameServer
  modify m_brokerAddrTable imediatly,
           after 1st send fail, producer will get topicPushlibshInfo again
  before next try, so 2nd try will get correct broker to send ms;
   */
  BrokerAddrMAP brokerTable(getBrokerAddrMap());
  string brokerAddr;
  bool found = false;

  if (brokerTable.find(brokerName) != brokerTable.end()) {
    map<int, string> brokerMap(brokerTable[brokerName]);
    map<int, string>::iterator it1 = brokerMap.find(MASTER_ID);
    if (it1 != brokerMap.end()) {
      brokerAddr = it1->second;
      found = true;
    }
  }

  brokerTable.clear();
  if (found) return brokerAddr;

  return "";
}

FindBrokerResult* MQClientFactory::findBrokerAddressInSubscribe(
    const string& brokerName, int brokerId, bool onlyThisBroker) {
  string brokerAddr;
  bool slave = false;
  bool found = false;
  BrokerAddrMAP brokerTable(getBrokerAddrMap());

  if (brokerTable.find(brokerName) != brokerTable.end()) {
    map<int, string> brokerMap(brokerTable[brokerName]);
    map<int, string>::iterator it1 = brokerMap.find(brokerId);
    if (it1 != brokerMap.end()) {
      brokerAddr = it1->second;
      slave = (brokerId != MASTER_ID);
      found = true;
    } else  // from master
    {
      it1 = brokerMap.find(MASTER_ID);
      if (it1 != brokerMap.end()) {
        brokerAddr = it1->second;
        slave = false;
        found = true;
      }
    }
  }

  brokerTable.clear();
  if (found) return new FindBrokerResult(brokerAddr, slave);

  return NULL;
}

FindBrokerResult* MQClientFactory::findBrokerAddressInAdmin(
    const string& brokerName) {
  BrokerAddrMAP brokerTable(getBrokerAddrMap());
  bool found = false;
  bool slave = false;
  string brokerAddr;

  if (brokerTable.find(brokerName) != brokerTable.end()) {
    map<int, string> brokerMap(brokerTable[brokerName]);
    map<int, string>::iterator it1 = brokerMap.begin();
    if (it1 != brokerMap.end()) {
      slave = (it1->first != MASTER_ID);
      found = true;
      brokerAddr = it1->second;
    }
  }

  brokerTable.clear();
  if (found) return new FindBrokerResult(brokerAddr, slave);

  return NULL;
}

MQClientAPIImpl* MQClientFactory::getMQClientAPIImpl() const {
  return m_pClientAPIImpl.get();
}

void MQClientFactory::sendHeartbeatToAllBroker() {
  BrokerAddrMAP brokerTable(getBrokerAddrMap());
  if (brokerTable.size() == 0) {
    LOG_WARN("sendheartbeat brokeradd is empty");
    return;
  }

  unique_ptr<HeartbeatData> heartbeatData(prepareHeartbeatData());
  bool producerEmpty = heartbeatData->isProducerDataSetEmpty();
  bool consumerEmpty = heartbeatData->isConsumerDataSetEmpty();
  if (producerEmpty && consumerEmpty) {
    LOG_WARN("sendheartbeat heartbeatData empty");
    brokerTable.clear();
    return;
  }

  SessionCredentials session_credentials;
  getSessionCredentialsFromOneOfProducerOrConsumer(session_credentials);
  for (BrokerAddrMAP::iterator it = brokerTable.begin();
       it != brokerTable.end(); ++it) {
    map<int, string> brokerMap(it->second);
    map<int, string>::iterator it1 = brokerMap.begin();
    for (; it1 != brokerMap.end(); ++it1) {
      string& addr = it1->second;
      if (consumerEmpty && it1->first != MASTER_ID) continue;

      try {
        m_pClientAPIImpl->sendHearbeat(addr, heartbeatData.get(),
                                       session_credentials);
      } catch (MQException& e) {
        LOG_ERROR(e.what());
      }
    }
  }
  brokerTable.clear();
}

void MQClientFactory::persistAllConsumerOffset(boost::system::error_code& ec,
                                               boost::asio::deadline_timer* t) {
  {
    boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
    if (m_consumerTable.size() > 0) {
      for (MQCMAP::iterator it = m_consumerTable.begin();
           it != m_consumerTable.end(); ++it) {
        LOG_DEBUG("Client factory start persistAllConsumerOffset");
        it->second->persistConsumerOffset();
      }
    }
  }

  boost::system::error_code e;
  t->expires_from_now(t->expires_from_now() + boost::posix_time::seconds(5), e);
  t->async_wait(
      boost::bind(&MQClientFactory::persistAllConsumerOffset, this, ec, t));
}

HeartbeatData* MQClientFactory::prepareHeartbeatData() {
  HeartbeatData* pHeartbeatData = new HeartbeatData();
  // clientID
  pHeartbeatData->setClientID(m_clientId);

  // Consumer
  insertConsumerInfoToHeartBeatData(pHeartbeatData);

  // Producer
  insertProducerInfoToHeartBeatData(pHeartbeatData);

  return pHeartbeatData;
}

void MQClientFactory::timerCB_sendHeartbeatToAllBroker(
    boost::system::error_code& ec, boost::asio::deadline_timer* t) {
  sendHeartbeatToAllBroker();

  boost::system::error_code e;
  t->expires_from_now(t->expires_from_now() + boost::posix_time::seconds(30),
                      e);
  t->async_wait(boost::bind(&MQClientFactory::timerCB_sendHeartbeatToAllBroker,
                            this, ec, t));
}

void MQClientFactory::fetchNameServerAddr(boost::system::error_code& ec,
                                          boost::asio::deadline_timer* t) {
  m_pClientAPIImpl->fetchNameServerAddr(m_nameSrvDomain);

  boost::system::error_code e;
  t->expires_from_now(
      t->expires_from_now() + boost::posix_time::seconds(60 * 2), e);
  t->async_wait(
      boost::bind(&MQClientFactory::fetchNameServerAddr, this, ec, t));
}

void MQClientFactory::startScheduledTask(bool startFetchNSService) {
  boost::asio::io_service::work work(m_async_ioService);  // avoid async io
                                                          // service stops after
                                                          // first timer timeout
                                                          // callback

  boost::system::error_code ec1;
  boost::asio::deadline_timer t1(m_async_ioService,
                                 boost::posix_time::seconds(3));
  t1.async_wait(
      boost::bind(&MQClientFactory::updateTopicRouteInfo, this, ec1, &t1));

  boost::system::error_code ec2;
  boost::asio::deadline_timer t2(m_async_ioService,
                                 boost::posix_time::milliseconds(10));
  t2.async_wait(boost::bind(&MQClientFactory::timerCB_sendHeartbeatToAllBroker,
                            this, ec2, &t2));

  if (startFetchNSService) {
    boost::system::error_code ec5;
    boost::asio::deadline_timer* t5 = new boost::asio::deadline_timer(
        m_async_ioService, boost::posix_time::seconds(60 * 2));
    t5->async_wait(
        boost::bind(&MQClientFactory::fetchNameServerAddr, this, ec5, t5));
  }

  LOG_INFO("start scheduled task:%s", m_clientId.c_str());
  boost::system::error_code ec;
  m_async_ioService.run(ec);
}

void MQClientFactory::rebalanceImmediately() {
  // m_consumer_async_service_thread will be only started once for all consumer
  if (m_consumer_async_service_thread == NULL) {
    doRebalance();
    m_consumer_async_service_thread.reset(new boost::thread(
        boost::bind(&MQClientFactory::consumer_timerOperation, this)));
  }
}

void MQClientFactory::consumer_timerOperation() {
  LOG_INFO("clientFactory:%s start consumer_timerOperation",
           m_clientId.c_str());
  boost::asio::io_service::work work(
      m_consumer_async_ioService);  // avoid async io
                                    // service stops after
                                    // first timer timeout
                                    // callback

  boost::system::error_code ec1;
  boost::asio::deadline_timer t(m_consumer_async_ioService,
                                boost::posix_time::seconds(10));
  t.async_wait(
      boost::bind(&MQClientFactory::timerCB_doRebalance, this, ec1, &t));

  boost::system::error_code ec2;
  boost::asio::deadline_timer t2(m_consumer_async_ioService,
                                 boost::posix_time::seconds(5));
  t2.async_wait(
      boost::bind(&MQClientFactory::persistAllConsumerOffset, this, ec2, &t2));

  boost::system::error_code ec;
  m_consumer_async_ioService.run(ec);
  LOG_INFO("clientFactory:%s stop consumer_timerOperation", m_clientId.c_str());
}

void MQClientFactory::timerCB_doRebalance(boost::system::error_code& ec,
                                          boost::asio::deadline_timer* t) {
  doRebalance();

  boost::system::error_code e;
  t->expires_from_now(t->expires_from_now() + boost::posix_time::seconds(10),
                      e);
  t->async_wait(
      boost::bind(&MQClientFactory::timerCB_doRebalance, this, ec, t));
}

void MQClientFactory::doRebalance() {
  LOG_INFO("Client factory:%s start dorebalance", m_clientId.c_str());
  if (getConsumerTableSize() > 0) {
    boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
    for (MQCMAP::iterator it = m_consumerTable.begin();
         it != m_consumerTable.end(); ++it) {
      it->second->doRebalance();
    }
  }
  LOG_INFO("Client factory:%s finish dorebalance", m_clientId.c_str());
}

void MQClientFactory::doRebalanceByConsumerGroup(const string& consumerGroup) {
  boost::lock_guard<boost::mutex> lock(m_consumerTableMutex);
  if (m_consumerTable.find(consumerGroup) != m_consumerTable.end()) {
    LOG_INFO("Client factory:%s start dorebalance for consumer:%s",
             m_clientId.c_str(), consumerGroup.c_str());
    MQConsumer* pMQConsumer = m_consumerTable[consumerGroup];
    pMQConsumer->doRebalance();
  }
}

void MQClientFactory::unregisterClient(
    const string& producerGroup, const string& consumerGroup,
    const SessionCredentials& sessionCredentials) {
  BrokerAddrMAP brokerTable(getBrokerAddrMap());
  for (BrokerAddrMAP::iterator it = brokerTable.begin();
       it != brokerTable.end(); ++it) {
    map<int, string> brokerMap(it->second);
    map<int, string>::iterator it1 = brokerMap.begin();
    for (; it1 != brokerMap.end(); ++it1) {
      string& addr = it1->second;
      m_pClientAPIImpl->unregisterClient(addr, m_clientId, producerGroup,
                                         consumerGroup, sessionCredentials);
    }
  }
}

//<!************************************************************************
void MQClientFactory::fetchSubscribeMessageQueues(
    const string& topic, vector<MQMessageQueue>& mqs,
    const SessionCredentials& sessionCredentials) {
  TopicRouteData* pTopicRouteData = getTopicRouteData(topic);
  if (pTopicRouteData == NULL) {
    updateTopicRouteInfoFromNameServer(topic, sessionCredentials);
    pTopicRouteData = getTopicRouteData(topic);
  }
  if (pTopicRouteData != NULL) {
    topicRouteData2TopicSubscribeInfo(topic, pTopicRouteData, mqs);
    if (mqs.empty()) {
      THROW_MQEXCEPTION(MQClientException, "Can not find Message Queue", -1);
    }
    return;
  }
  THROW_MQEXCEPTION(MQClientException, "Can not find Message Queue", -1);
}

//<!***************************************************************************
void MQClientFactory::createTopic(
    const string& key, const string& newTopic, int queueNum,
    const SessionCredentials& sessionCredentials) {}

int64 MQClientFactory::minOffset(const MQMessageQueue& mq,
                                 const SessionCredentials& sessionCredentials) {
  string brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  if (brokerAddr.empty()) {
    updateTopicRouteInfoFromNameServer(mq.getTopic(), sessionCredentials);
    brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  }

  if (!brokerAddr.empty()) {
    try {
      return m_pClientAPIImpl->getMinOffset(brokerAddr, mq.getTopic(),
                                            mq.getQueueId(), 1000 * 3,
                                            sessionCredentials);
    } catch (MQException& e) {
      LOG_ERROR(e.what());
    }
  }
  THROW_MQEXCEPTION(MQClientException, "The broker is not exist", -1);
}

int64 MQClientFactory::maxOffset(const MQMessageQueue& mq,
                                 const SessionCredentials& sessionCredentials) {
  string brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  if (brokerAddr.empty()) {
    updateTopicRouteInfoFromNameServer(mq.getTopic(), sessionCredentials);
    brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  }

  if (!brokerAddr.empty()) {
    try {
      return m_pClientAPIImpl->getMaxOffset(brokerAddr, mq.getTopic(),
                                            mq.getQueueId(), 1000 * 3,
                                            sessionCredentials);
    } catch (MQException& e) {
      THROW_MQEXCEPTION(MQClientException, "Invoke Broker exception", -1);
    }
  }
  THROW_MQEXCEPTION(MQClientException, "The broker is not exist", -1);
}

int64 MQClientFactory::searchOffset(
    const MQMessageQueue& mq, int64 timestamp,
    const SessionCredentials& sessionCredentials) {
  string brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  if (brokerAddr.empty()) {
    updateTopicRouteInfoFromNameServer(mq.getTopic(), sessionCredentials);
    brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  }

  if (!brokerAddr.empty()) {
    try {
      return m_pClientAPIImpl->searchOffset(brokerAddr, mq.getTopic(),
                                            mq.getQueueId(), timestamp,
                                            1000 * 3, sessionCredentials);
    } catch (MQException& e) {
      THROW_MQEXCEPTION(MQClientException, "Invoke Broker exception", -1);
    }
  }
  THROW_MQEXCEPTION(MQClientException, "The broker is not exist", -1);
}

MQMessageExt* MQClientFactory::viewMessage(
    const string& msgId, const SessionCredentials& sessionCredentials) {
  try {
    return NULL;
  } catch (MQException& e) {
    THROW_MQEXCEPTION(MQClientException, "message id illegal", -1);
  }
}

int64 MQClientFactory::earliestMsgStoreTime(
    const MQMessageQueue& mq, const SessionCredentials& sessionCredentials) {
  string brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  if (brokerAddr.empty()) {
    updateTopicRouteInfoFromNameServer(mq.getTopic(), sessionCredentials);
    brokerAddr = findBrokerAddressInPublish(mq.getBrokerName());
  }

  if (!brokerAddr.empty()) {
    try {
      return m_pClientAPIImpl->getEarliestMsgStoretime(
          brokerAddr, mq.getTopic(), mq.getQueueId(), 1000 * 3,
          sessionCredentials);
    } catch (MQException& e) {
      THROW_MQEXCEPTION(MQClientException, "Invoke Broker exception", -1);
    }
  }
  THROW_MQEXCEPTION(MQClientException, "The broker is not exist", -1);
}

QueryResult MQClientFactory::queryMessage(
    const string& topic, const string& key, int maxNum, int64 begin, int64 end,
    const SessionCredentials& sessionCredentials) {
  THROW_MQEXCEPTION(MQClientException, "queryMessage", -1);
}

void MQClientFactory::findConsumerIds(
    const string& topic, const string& group, vector<string>& cids,
    const SessionCredentials& sessionCredentials) {
  string brokerAddr;
  TopicRouteData* pTopicRouteData = getTopicRouteData(topic);
  if (pTopicRouteData == NULL) {
    updateTopicRouteInfoFromNameServer(topic, sessionCredentials);
    pTopicRouteData = getTopicRouteData(topic);
  }
  if (pTopicRouteData != NULL) {
    brokerAddr = pTopicRouteData->selectBrokerAddr();
  }

  if (!brokerAddr.empty()) {
    try {
      LOG_INFO("getConsumerIdList from broker:%s", brokerAddr.c_str());
      return m_pClientAPIImpl->getConsumerIdListByGroup(
          brokerAddr, group, cids, 5000, sessionCredentials);
    } catch (MQException& e) {
      LOG_ERROR(e.what());
    }
  }
}

void MQClientFactory::resetOffset(
    const string& group, const string& topic,
    const map<MQMessageQueue, int64>& offsetTable) {
  MQConsumer* pConsumer = selectConsumer(group);
  if (pConsumer) {
    map<MQMessageQueue, int64>::const_iterator it = offsetTable.begin();

    for (; it != offsetTable.end(); ++it) {
      MQMessageQueue mq = it->first;
      PullRequest* pullreq = pConsumer->getRebalance()->getPullRequest(mq);
      if (pullreq) {
        pullreq->setDroped(true);
        pullreq->clearAllMsgs();
        pullreq->updateQueueMaxOffset(it->second);
      } else {
        LOG_ERROR("no corresponding pullRequest found for topic:%s",
                  topic.c_str());
      }
    }

    for (it = offsetTable.begin(); it != offsetTable.end(); ++it) {
      MQMessageQueue mq = it->first;
      if (topic == mq.getTopic()) {
        LOG_INFO("offset sets to:%lld", it->second);
        pConsumer->updateConsumeOffset(mq, it->second);
      }
    }
    pConsumer->persistConsumerOffsetByResetOffset();

    boost::this_thread::sleep_for(boost::chrono::milliseconds(10));

    for (it = offsetTable.begin(); it != offsetTable.end(); ++it) {
      MQMessageQueue mq = it->first;
      if (topic == mq.getTopic()) {
        LOG_DEBUG("resetOffset sets to:%lld for mq:%s", it->second,
                  mq.toString().c_str());
        pConsumer->updateConsumeOffset(mq, it->second);
      }
    }
    pConsumer->persistConsumerOffsetByResetOffset();

    for (it = offsetTable.begin(); it != offsetTable.end(); ++it) {
      MQMessageQueue mq = it->first;
      if (topic == mq.getTopic()) {
        pConsumer->removeConsumeOffset(mq);
      }
    }

    // do call pConsumer->doRebalance directly here, as it is conflict with
    // timerCB_doRebalance;
    doRebalanceByConsumerGroup(pConsumer->getGroupName());
  } else {
    LOG_ERROR("no corresponding consumer found for group:%s", group.c_str());
  }
}

ConsumerRunningInfo* MQClientFactory::consumerRunningInfo(
    const string& consumerGroup) {
  MQConsumer* pConsumer = selectConsumer(consumerGroup);
  if (pConsumer) {
    ConsumerRunningInfo* runningInfo = pConsumer->getConsumerRunningInfo();
    if (runningInfo) {
      runningInfo->setProperty(ConsumerRunningInfo::PROP_NAMESERVER_ADDR,
                               pConsumer->getNamesrvAddr());
      if (pConsumer->getConsumeType() == CONSUME_PASSIVELY) {
        runningInfo->setProperty(ConsumerRunningInfo::PROP_CONSUME_TYPE,
                                 "CONSUME_PASSIVELY");
      } else {
        runningInfo->setProperty(ConsumerRunningInfo::PROP_CONSUME_TYPE,
                                 "CONSUME_ACTIVELY");
      }
      runningInfo->setProperty(ConsumerRunningInfo::PROP_CLIENT_VERSION,
                               "V3_1_8");  // MQVersion::s_CurrentVersion ));

      return runningInfo;
    }
  }

  LOG_ERROR("no corresponding consumer found for group:%s",
            consumerGroup.c_str());
  return NULL;
}

void MQClientFactory::getSessionCredentialsFromOneOfProducerOrConsumer(
    SessionCredentials& session_credentials) {
  // Note: on the same MQClientFactory, all producers and consumers used the
  // same
  // sessionCredentials,
  // So only need get sessionCredentials from the first one producer or consumer
  // now.
  // this function was only used by updateTopicRouteInfo() and
  // sendHeartbeatToAllBrokers() now.
  // if this strategy was changed in future, need get sessionCredentials for
  // each
  // producer and consumer.
  getSessionCredentialFromProducerTable(session_credentials);
  if (!session_credentials.isValid())
    getSessionCredentialFromConsumerTable(session_credentials);

  if (!session_credentials.isValid()) {
    LOG_ERROR(
        "updateTopicRouteInfo: didn't get the session_credentials from any "
        "producers and consumers, please re-intialize it");
  }
}

//<!************************************************************************
}  //<!end namespace;
