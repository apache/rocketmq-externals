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

#ifndef __MQCLIENTAPIIMPL_H__
#define __MQCLIENTAPIIMPL_H__

#include <string>
#include <map>
#include <list>
#include <set>

#include "ClientConfig.h"
#include "RemoteClientConfig.h"
#include "SubscriptionGroupConfig.h"
#include "TopicConfig.h"
#include "ConsumeStats.h"
#include "TopicStatsTable.h"
#include "KVTable.h"
#include "TopicRouteData.h"
#include "SendResult.h"
#include "PullResult.h"
#include "MessageExt.h"
#include "CommunicationMode.h"
#include "TopAddressing.h"
#include "HeartbeatData.h"
#include "LockBatchBody.h"

namespace rmq
{
class ClientConfig;
class TcpRemotingClient;
class QueryConsumerOffsetRequestHeader;
class UpdateConsumerOffsetRequestHeader;
class EndTransactionRequestHeader;
class SendMessageRequestHeader;
class PullMessageRequestHeader;
class QueryMessageRequestHeader;
class ProducerConnection;
class ConsumerConnection;
class ClusterInfo;
class TopicList;
class InvokeCallback;
class RemotingCommand;
class PullCallback;
class SendCallback;
class ClientRemotingProcessor;

class MQClientAPIImpl
{
  public:
      MQClientAPIImpl(ClientConfig& clientConfig,
	  				  const RemoteClientConfig& remoteClientConfig,
                      ClientRemotingProcessor* pClientRemotingProcessor);
      ~MQClientAPIImpl();

      void start();
      void shutdown();

      std::string getProjectGroupPrefix();
      std::vector<std::string> getNameServerAddressList();
      void updateNameServerAddressList(const std::string& addrs);
      std::string fetchNameServerAddr();

      void createSubscriptionGroup(const std::string& addr,
                                   SubscriptionGroupConfig config,
                                   int timeoutMillis);

      void createTopic(const std::string& addr,
                       const std::string& defaultTopic,
                       TopicConfig topicConfig,
                       int timeoutMillis);

      SendResult sendMessage(const std::string& addr,
                             const std::string& brokerName,
                             Message& msg,
                             SendMessageRequestHeader* pRequestHeader,
                             int timeoutMillis,
                             CommunicationMode communicationMode,
                             SendCallback* pSendCallback);

      PullResult* pullMessage(const std::string& addr,
                              PullMessageRequestHeader* pRequestHeader,
                              int timeoutMillis,
                              CommunicationMode communicationMode,
                              PullCallback* pPullCallback);

      MessageExt* viewMessage(const std::string& addr, long long phyoffset, int timeoutMillis);


      long long searchOffset(const std::string& addr,
                             const std::string& topic,
                             int queueId,
                             long long timestamp,
                             int timeoutMillis);

      long long getMaxOffset(const std::string& addr,
                             const std::string& topic,
                             int queueId,
                             int timeoutMillis);

      std::list<std::string> getConsumerIdListByGroup(const std::string& addr,
              const std::string& consumerGroup,
              int timeoutMillis);

      long long getMinOffset(const std::string& addr,
                             const std::string& topic,
                             int queueId,
                             int timeoutMillis);

      long long getEarliestMsgStoretime(const std::string& addr,
                                        const std::string& topic,
                                        int queueId,
                                        int timeoutMillis);

      long long queryConsumerOffset(const std::string& addr,
                                    QueryConsumerOffsetRequestHeader* pRequestHeader,
                                    int timeoutMillis);

      void updateConsumerOffset(const std::string& addr,
                                UpdateConsumerOffsetRequestHeader* pRequestHeader,
                                int timeoutMillis);

      void updateConsumerOffsetOneway(const std::string& addr,
                                      UpdateConsumerOffsetRequestHeader* pRequestHeader,
                                      int timeoutMillis);

      void sendHearbeat(const std::string& addr, HeartbeatData* pHeartbeatData, int timeoutMillis);

      void unregisterClient(const std::string& addr,
                            const std::string& clientID,
                            const std::string& producerGroup,
                            const std::string& consumerGroup,
                            int timeoutMillis);

      void endTransactionOneway(const std::string& addr,
                                EndTransactionRequestHeader* pRequestHeader,
                                const std::string& remark,
                                int timeoutMillis);

      void queryMessage(const std::string& addr,
                        QueryMessageRequestHeader* pRequestHeader,
                        int timeoutMillis,
                        InvokeCallback* pInvokeCallback);

      bool registerClient(const std::string& addr,
                          HeartbeatData& heartbeat,
                          int timeoutMillis);

      void consumerSendMessageBack(const std::string& addr,
      							   MessageExt& msg,
                                   const std::string& consumerGroup,
                                   int delayLevel,
                                   int timeoutMillis);

      std::set<MessageQueue> lockBatchMQ(const std::string& addr,
                                         LockBatchRequestBody* pRequestBody,
                                         int timeoutMillis);

      void unlockBatchMQ(const std::string& addr,
                         UnlockBatchRequestBody* pRequestBody,
                         int timeoutMillis,
                         bool oneway);

      TopicStatsTable getTopicStatsInfo(const std::string& addr,
                                        const std::string& topic,
                                        int timeoutMillis);

      ConsumeStats getConsumeStats(const std::string& addr,
                                   const std::string& consumerGroup,
                                   int timeoutMillis);

      ProducerConnection* getProducerConnectionList(const std::string& addr,
              const std::string& producerGroup,
              int timeoutMillis);

      ConsumerConnection* getConsumerConnectionList(const std::string& addr,
              const std::string& consumerGroup,
              int timeoutMillis);

      KVTable getBrokerRuntimeInfo(const std::string& addr,  int timeoutMillis);

      void updateBrokerConfig(const std::string& addr,
                              const std::map<std::string, std::string>& properties,
                              int timeoutMillis);

      ClusterInfo* getBrokerClusterInfo(int timeoutMillis);

      TopicRouteData* getDefaultTopicRouteInfoFromNameServer(const std::string& topic, int timeoutMillis);

      TopicRouteData* getTopicRouteInfoFromNameServer(const std::string& topic, int timeoutMillis);

      TopicList* getTopicListFromNameServer(int timeoutMillis);

      int wipeWritePermOfBroker(const std::string& namesrvAddr,
                                const std::string& brokerName,
                                int timeoutMillis);

      void deleteTopicInBroker(const std::string& addr, const std::string& topic, int timeoutMillis);
      void deleteTopicInNameServer(const std::string& addr, const std::string& topic, int timeoutMillis);
      void deleteSubscriptionGroup(const std::string& addr,
                                   const std::string& groupName,
                                   int timeoutMillis);

      std::string getKVConfigValue(const std::string& projectNamespace,
                                   const std::string& key,
                                   int timeoutMillis);

      void putKVConfigValue(const std::string& projectNamespace,
                            const std::string& key,
                            const std::string& value,
                            int timeoutMillis);

      void deleteKVConfigValue(const std::string& projectNamespace, const std::string& key, int timeoutMillis);

      std::string getProjectGroupByIp(const std::string& ip,  int timeoutMillis);

      std::string getKVConfigByValue(const std::string& projectNamespace,
                                     const std::string& projectGroup,
                                     int timeoutMillis);

      KVTable getKVListByNamespace(const std::string& projectNamespace,  int timeoutMillis);

      void deleteKVConfigByValue(const std::string& projectNamespace,
                                 const std::string& projectGroup,
                                 int timeoutMillis);

      TcpRemotingClient* getRemotingClient();

      SendResult* processSendResponse(const std::string& brokerName,
                                      const std::string& topic,
                                      RemotingCommand* pResponse);

      PullResult* processPullResponse(RemotingCommand* pResponse);

  private:
      SendResult* sendMessageSync(const std::string& addr,
                                  const std::string& brokerName,
                                  Message& msg,
                                  int timeoutMillis,
                                  RemotingCommand* request);

      void sendMessageAsync(const std::string& addr,
                            const std::string& brokerName,
                            Message& msg,
                            int timeoutMillis,
                            RemotingCommand* request,
                            SendCallback* pSendCallback);

      void pullMessageAsync(const std::string& addr,
                            RemotingCommand* pRequest,
                            int timeoutMillis,
                            PullCallback* pPullCallback);

      PullResult* pullMessageSync(const std::string& addr,
                                  RemotingCommand* pRequest,
                                  int timeoutMillis);

  private:
      TcpRemotingClient* m_pRemotingClient;
      TopAddressing m_topAddressing;
      ClientRemotingProcessor* m_pClientRemotingProcessor;
      std::string m_nameSrvAddr;
      std::string m_projectGroupPrefix;
  };
}

#endif
