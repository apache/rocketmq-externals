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
#ifndef __MESSAGEEXT_H__
#define __MESSAGEEXT_H__

#ifdef WIN32
#include <Winsock2.h>
#include <Windows.h>
#else
#include <sys/socket.h>
#endif

#include "MQMessage.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!message extend class, which was generated on broker;
//<!***************************************************************************
class ROCKETMQCLIENT_API MQMessageExt : public MQMessage {
 public:
  MQMessageExt();
  MQMessageExt(int queueId, int64 bornTimestamp, sockaddr bornHost,
               int64 storeTimestamp, sockaddr storeHost, std::string msgId);

  virtual ~MQMessageExt();

  static int parseTopicFilterType(int sysFlag);

  int getQueueId() const;
  void setQueueId(int queueId);

  int64 getBornTimestamp() const;
  void setBornTimestamp(int64 bornTimestamp);

  sockaddr getBornHost() const;
  std::string getBornHostString() const;
  std::string getBornHostNameString() const;
  void setBornHost(const sockaddr& bornHost);

  int64 getStoreTimestamp() const;
  void setStoreTimestamp(int64 storeTimestamp);

  sockaddr getStoreHost() const;
  std::string getStoreHostString() const;
  void setStoreHost(const sockaddr& storeHost);

  const std::string& getMsgId() const;
  void setMsgId(const std::string& msgId);

  int getSysFlag() const;
  void setSysFlag(int sysFlag);

  int getBodyCRC() const;
  void setBodyCRC(int bodyCRC);

  int64 getQueueOffset() const;
  void setQueueOffset(int64 queueOffset);

  int64 getCommitLogOffset() const;
  void setCommitLogOffset(int64 physicOffset);

  int getStoreSize() const;
  void setStoreSize(int storeSize);

  int getReconsumeTimes() const;
  void setReconsumeTimes(int reconsumeTimes);

  int64 getPreparedTransactionOffset() const;
  void setPreparedTransactionOffset(int64 preparedTransactionOffset);

  std::string toString() const {
    std::stringstream ss;
    ss << "MessageExt [queueId=" << m_queueId << ", storeSize=" << m_storeSize
       << ", queueOffset=" << m_queueOffset << ", sysFlag=" << m_sysFlag
       << ", bornTimestamp=" << m_bornTimestamp
       << ", bornHost=" << getBornHostString()
       << ", storeTimestamp=" << m_storeTimestamp
       << ", storeHost=" << getStoreHostString() << ", msgId=" << m_msgId
       << ", commitLogOffset=" << m_commitLogOffset << ", bodyCRC=" << m_bodyCRC
       << ", reconsumeTimes=" << m_reconsumeTimes
       << ", preparedTransactionOffset=" << m_preparedTransactionOffset << ",  "
       << MQMessage::toString() << "]";
    return ss.str();
  }

 private:
  int64 m_queueOffset;
  int64 m_commitLogOffset;
  int64 m_bornTimestamp;
  int64 m_storeTimestamp;
  int64 m_preparedTransactionOffset;
  int m_queueId;
  int m_storeSize;
  int m_sysFlag;
  int m_bodyCRC;
  int m_reconsumeTimes;
  sockaddr m_bornHost;
  sockaddr m_storeHost;
  std::string m_msgId;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
