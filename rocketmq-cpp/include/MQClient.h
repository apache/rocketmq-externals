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

#ifndef __MQADMIN_H__
#define __MQADMIN_H__
#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include "MQMessageExt.h"
#include "MQMessageQueue.h"
#include "QueryResult.h"
#include "RocketMQClient.h"
#include "SessionCredentials.h"

namespace rocketmq {
class MQClientFactory;
//<!***************************************************************************

enum elogLevel {
  eLOG_LEVEL_FATAL = 1,
  eLOG_LEVEL_ERROR = 2,
  eLOG_LEVEL_WARN = 3,
  eLOG_LEVEL_INFO = 4,
  eLOG_LEVEL_DEBUG = 5,
  eLOG_LEVEL_TRACE = 6,
  eLOG_LEVEL_LEVEL_NUM = 7
};

class ROCKETMQCLIENT_API MQClient {
 public:
  MQClient();
  virtual ~MQClient();

 public:
  // clientid=processId-ipAddr@instanceName;
  std::string getMQClientId() const;
  const std::string& getNamesrvAddr() const;
  void setNamesrvAddr(const std::string& namesrvAddr);
  const std::string& getNamesrvDomain() const;
  void setNamesrvDomain(const std::string& namesrvDomain);
  const std::string& getInstanceName() const;
  void setInstanceName(const std::string& instanceName);
  //<!groupName;
  const std::string& getGroupName() const;
  void setGroupName(const std::string& groupname);
  
  /**
  * no realization
  */
  void createTopic(const std::string& key, const std::string& newTopic, int queueNum);
  /**
  * search earliest msg store time for specified queue
  *
  * @param mq
  *            message queue
  * @return earliest store time, ms
  */
  int64 earliestMsgStoreTime(const MQMessageQueue& mq);
  /**
  * search maxOffset of queue
  *
  * @param mq
  *            message queue
  * @return minOffset of queue
  */
  int64 minOffset(const MQMessageQueue& mq);
  /**
  * search maxOffset of queue
  * Note: maxOffset-1 is max offset that could get msg
  * @param mq
  *            message queue
  * @return maxOffset of queue
  */
  int64 maxOffset(const MQMessageQueue& mq);
  /**
  * get queue offset by timestamp
  *
  * @param mq
  *            mq queue
  * @param timestamp
  *            timestamp with ms unit
  * @return queue offset according to timestamp
  */
  int64 searchOffset(const MQMessageQueue& mq, uint64_t timestamp);
  /**
  * get whole msg info from broker by msgId
  *
  * @param msgId
  * @return MQMessageExt
  */
  MQMessageExt* viewMessage(const std::string& msgId);
  /**
  * query message by topic and key
  *
  * @param topic
  *            topic name
  * @param key
  *            topic key
  * @param maxNum
  *            query num
  * @param begin
  *            begin timestamp
  * @param end
  *            end timestamp
  * @return
  *            according to QueryResult
  */
  QueryResult queryMessage(const std::string& topic, const std::string& key, int maxNum,
                           int64 begin, int64 end);

  std::vector<MQMessageQueue> getTopicMessageQueueInfo(const std::string& topic);

  // log configuration interface, default LOG_LEVEL is LOG_LEVEL_INFO, default
  // log file num is 3, each log size is 100M
  void setLogLevel(elogLevel inputLevel);
  elogLevel getLogLevel();
  void setLogFileSizeAndNum(int fileNum,
                                 long perFileSize);  // perFileSize is MB unit

  /** set TcpTransport pull thread num, which dermine the num of threads to
 distribute network data,
     1. its default value is CPU num, it must be setted before producer/consumer
 start, minimum value is CPU num;
     2. this pullThread num must be tested on your environment to find the best
 value for RT of sendMsg or delay time of consume msg before you change it;
     3. producer and consumer need different pullThread num, if set this num,
 producer and consumer must set different instanceName.
     4. configuration suggestion:
         1>. minimum RT of sendMsg:
                 pullThreadNum = brokerNum*2
 **/
  void setTcpTransportPullThreadNum(int num);
  const int getTcpTransportPullThreadNum() const;

  /** timeout of tcp connect, it is same meaning for both producer and consumer;
      1. default value is 3000ms
      2. input parameter could only be milliSecond, suggestion value is
  1000-3000ms;
  **/
  void setTcpTransportConnectTimeout(uint64_t timeout);  // ms
  const uint64_t getTcpTransportConnectTimeout() const;

  /** timeout of tryLock tcpTransport before sendMsg/pullMsg, if timeout,
  returns NULL
      1. paremeter unit is ms, default value is 3000ms, the minimun value is
  1000ms
          suggestion value is 3000ms;
      2. if configured with value smaller than 1000ms, the tryLockTimeout value
  will be setted to 1000ms
  **/
  void setTcpTransportTryLockTimeout(uint64_t timeout);  // ms
  const uint64_t getTcpTransportTryLockTimeout() const;

  void setUnitName(std::string unitName);
  const std::string& getUnitName();

  void setSessionCredentials(const std::string& input_accessKey,
                             const std::string& input_secretKey,
                             const std::string& input_onsChannel);
  const SessionCredentials& getSessionCredentials() const;

 protected:
  virtual void start();
  virtual void shutdown();
  MQClientFactory* getFactory() const;
  virtual bool isServiceStateOk();

 protected:
  std::string m_namesrvAddr;
  std::string m_namesrvDomain;
  std::string m_instanceName;
  //<!  the name is globle only
  std::string m_GroupName;
  //<!factory;
  MQClientFactory* m_clientFactory;
  int m_serviceState;
  int m_pullThreadNum;
  uint64_t m_tcpConnectTimeout;           // ms
  uint64_t m_tcpTransportTryLockTimeout;  // s

  std::string m_unitName;
  SessionCredentials m_SessionCredentials;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
