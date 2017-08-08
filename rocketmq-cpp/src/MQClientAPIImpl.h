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
#ifndef __MQCLIENTAPIIMPL_H__
#define __MQCLIENTAPIIMPL_H__
#include "AsyncCallback.h"
#include "ClientRPCHook.h"
#include "ClientRemotingProcessor.h"
#include "CommandHeader.h"
#include "HeartbeatData.h"
#include "KVTable.h"
#include "LockBatchBody.h"
#include "MQClientException.h"
#include "MQMessageExt.h"
#include "MQProtos.h"
#include "SendResult.h"
#include "SocketUtil.h"
#include "TcpRemotingClient.h"
#include "TopAddressing.h"
#include "TopicConfig.h"
#include "TopicList.h"
#include "TopicRouteData.h"
#include "UtilAll.h"
#include "VirtualEnvUtil.h"

namespace rocketmq {
//<!wrap all API to net ;
//<!************************************************************************
class MQClientAPIImpl {
 public:
  MQClientAPIImpl(const string& mqClientId, ClientRemotingProcessor* clientRemotingProcessor,
                  int pullThreadNum, uint64_t tcpConnectTimeout,
                  uint64_t tcpTransportTryLockTimeout, string unitName);
  virtual ~MQClientAPIImpl();
  void stopAllTcpTransportThread();
  bool writeDataToFile(string filename, string data, bool isSync);
  string fetchNameServerAddr(const string& NSDomain);
  void updateNameServerAddr(const string& addrs);

  void callSignatureBeforeRequest(
      const string& addr, RemotingCommand& request,
      const SessionCredentials& session_credentials);
  void createTopic(const string& addr, const string& defaultTopic,
                   TopicConfig topicConfig,
                   const SessionCredentials& sessionCredentials);

  SendResult sendMessage(const string& addr, const string& brokerName,
                         const MQMessage& msg,
                         SendMessageRequestHeader* pRequestHeader,
                         int timeoutMillis, int communicationMode,
                         SendCallback* pSendCallback,
                         const SessionCredentials& sessionCredentials);

  PullResult* pullMessage(const string& addr,
                          PullMessageRequestHeader* pRequestHeader,
                          int timeoutMillis, int communicationMode,
                          PullCallback* pullCallback, void* pArg,
                          const SessionCredentials& sessionCredentials);

  void sendHearbeat(const string& addr, HeartbeatData* pHeartbeatData,
                    const SessionCredentials& sessionCredentials);

  void unregisterClient(const string& addr, const string& clientID,
                        const string& producerGroup,
                        const string& consumerGroup,
                        const SessionCredentials& sessionCredentials);

  TopicRouteData* getTopicRouteInfoFromNameServer(
      const string& topic, int timeoutMillis,
      const SessionCredentials& sessionCredentials);

  TopicList* getTopicListFromNameServer(
      const SessionCredentials& sessionCredentials);

  int wipeWritePermOfBroker(const string& namesrvAddr, const string& brokerName,
                            int timeoutMillis);

  void deleteTopicInBroker(const string& addr, const string& topic,
                           int timeoutMillis);

  void deleteTopicInNameServer(const string& addr, const string& topic,
                               int timeoutMillis);

  void deleteSubscriptionGroup(const string& addr, const string& groupName,
                               int timeoutMillis);

  string getKVConfigByValue(const string& projectNamespace,
                            const string& projectGroup, int timeoutMillis);

  KVTable getKVListByNamespace(const string& projectNamespace,
                               int timeoutMillis);

  void deleteKVConfigByValue(const string& projectNamespace,
                             const string& projectGroup, int timeoutMillis);

  SendResult processSendResponse(const string& brokerName, const MQMessage& msg,
                                 RemotingCommand* pResponse);

  PullResult* processPullResponse(RemotingCommand* pResponse);

  int64 getMinOffset(const string& addr, const string& topic, int queueId,
                     int timeoutMillis,
                     const SessionCredentials& sessionCredentials);

  int64 getMaxOffset(const string& addr, const string& topic, int queueId,
                     int timeoutMillis,
                     const SessionCredentials& sessionCredentials);

  int64 searchOffset(const string& addr, const string& topic, int queueId,
                     uint64_t timestamp, int timeoutMillis,
                     const SessionCredentials& sessionCredentials);

  MQMessageExt* viewMessage(const string& addr, int64 phyoffset,
                            int timeoutMillis,
                            const SessionCredentials& sessionCredentials);

  int64 getEarliestMsgStoretime(const string& addr, const string& topic,
                                int queueId, int timeoutMillis,
                                const SessionCredentials& sessionCredentials);

  void getConsumerIdListByGroup(const string& addr, const string& consumerGroup,
                                vector<string>& cids, int timeoutMillis,
                                const SessionCredentials& sessionCredentials);

  int64 queryConsumerOffset(const string& addr,
                            QueryConsumerOffsetRequestHeader* pRequestHeader,
                            int timeoutMillis,
                            const SessionCredentials& sessionCredentials);

  void updateConsumerOffset(const string& addr,
                            UpdateConsumerOffsetRequestHeader* pRequestHeader,
                            int timeoutMillis,
                            const SessionCredentials& sessionCredentials);

  void updateConsumerOffsetOneway(
      const string& addr, UpdateConsumerOffsetRequestHeader* pRequestHeader,
      int timeoutMillis, const SessionCredentials& sessionCredentials);

  void consumerSendMessageBack(MQMessageExt& msg, const string& consumerGroup,
                               int delayLevel, int timeoutMillis,
                               const SessionCredentials& sessionCredentials);

  void lockBatchMQ(const string& addr, LockBatchRequestBody* requestBody,
                   vector<MQMessageQueue>& mqs, int timeoutMillis,
                   const SessionCredentials& sessionCredentials);

  void unlockBatchMQ(const string& addr, UnlockBatchRequestBody* requestBody,
                     int timeoutMillis,
                     const SessionCredentials& sessionCredentials);

 private:
  SendResult sendMessageSync(const string& addr, const string& brokerName,
                             const MQMessage& msg, RemotingCommand& request,
                             int timeoutMillis);

  void sendMessageAsync(const string& addr, const string& brokerName,
                        const MQMessage& msg, RemotingCommand& request,
                        SendCallback* pSendCallback, int64 timeoutMilliseconds);

  PullResult* pullMessageSync(const string& addr, RemotingCommand& request,
                              int timeoutMillis);

  void pullMessageAsync(const string& addr, RemotingCommand& request,
                        int timeoutMillis, PullCallback* pullCallback,
                        void* pArg);

 private:
  unique_ptr<TcpRemotingClient> m_pRemotingClient;
  unique_ptr<TopAddressing> m_topAddressing;
  string m_nameSrvAddr;
  bool m_firstFetchNameSrv;
  string m_mqClientId;
};
}  //<!end namespace;
//<!***************************************************************************
#endif
