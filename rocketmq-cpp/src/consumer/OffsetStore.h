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
#ifndef __OFFSETSTORE_H__
#define __OFFSETSTORE_H__

#include <boost/asio.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <map>
#include "MQMessageQueue.h"
#include "RocketMQClient.h"
#include "SessionCredentials.h"

namespace rocketmq {
class MQClientFactory;
//<!***************************************************************************
enum ReadOffsetType {
  //read offset from memory
  READ_FROM_MEMORY,
  //read offset from remoting
  READ_FROM_STORE,
  //read offset from memory firstly, then from remoting
  MEMORY_FIRST_THEN_STORE,
};

//<!***************************************************************************
class OffsetStore {
 public:
  OffsetStore(const std::string& groupName, MQClientFactory*);
  virtual ~OffsetStore();

  virtual void load() = 0;
  virtual void updateOffset(const MQMessageQueue& mq, int64 offset) = 0;
  virtual int64 readOffset(const MQMessageQueue& mq, ReadOffsetType type,
                           const SessionCredentials& session_credentials) = 0;
  virtual void persist(const MQMessageQueue& mq,
                       const SessionCredentials& session_credentials) = 0;
  virtual void persistAll(const std::vector<MQMessageQueue>& mq) = 0;
  virtual void removeOffset(const MQMessageQueue& mq) = 0;

 protected:
  std::string m_groupName;
  typedef std::map<MQMessageQueue, int64> MQ2OFFSET;
  MQ2OFFSET m_offsetTable;
  MQClientFactory* m_pClientFactory;
  boost::mutex m_lock;
};

//<!***************************************************************************
class LocalFileOffsetStore : public OffsetStore {
 public:
  LocalFileOffsetStore(const std::string& groupName, MQClientFactory*);
  virtual ~LocalFileOffsetStore();

  virtual void load();
  virtual void updateOffset(const MQMessageQueue& mq, int64 offset);
  virtual int64 readOffset(const MQMessageQueue& mq, ReadOffsetType type,
                           const SessionCredentials& session_credentials);
  virtual void persist(const MQMessageQueue& mq,
                       const SessionCredentials& session_credentials);
  virtual void persistAll(const std::vector<MQMessageQueue>& mq);
  virtual void removeOffset(const MQMessageQueue& mq);

 private:
  std::string m_storePath;
  std::string m_storeFile;
};

//<!***************************************************************************
class RemoteBrokerOffsetStore : public OffsetStore {
 public:
  RemoteBrokerOffsetStore(const std::string& groupName, MQClientFactory*);
  virtual ~RemoteBrokerOffsetStore();

  virtual void load();
  virtual void updateOffset(const MQMessageQueue& mq, int64 offset);
  virtual int64 readOffset(const MQMessageQueue& mq, ReadOffsetType type,
                           const SessionCredentials& session_credentials);
  virtual void persist(const MQMessageQueue& mq,
                       const SessionCredentials& session_credentials);
  virtual void persistAll(const std::vector<MQMessageQueue>& mq);
  virtual void removeOffset(const MQMessageQueue& mq);

 private:
  void updateConsumeOffsetToBroker(
      const MQMessageQueue& mq, int64 offset,
      const SessionCredentials& session_credentials);
  int64 fetchConsumeOffsetFromBroker(
      const MQMessageQueue& mq, const SessionCredentials& session_credentials);
};
//<!***************************************************************************
}  //<!end namespace;

#endif
