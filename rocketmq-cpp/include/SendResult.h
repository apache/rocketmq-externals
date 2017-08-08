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
#ifndef __SENDRESULT_H__
#define __SENDRESULT_H__

#include "MQMessageQueue.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!***************************************************************************
//<!all to Master;
enum SendStatus {
  SEND_OK,
  SEND_FLUSH_DISK_TIMEOUT,
  SEND_FLUSH_SLAVE_TIMEOUT,
  SEND_SLAVE_NOT_AVAILABLE
};

//<!***************************************************************************
class ROCKETMQCLIENT_API SendResult {
 public:
  SendResult();
  SendResult(const SendStatus& sendStatus, const std::string& msgId,
             const MQMessageQueue& messageQueue, int64 queueOffset);

  virtual ~SendResult();
  SendResult(const SendResult& other);
  SendResult& operator=(const SendResult& other);

  const std::string& getMsgId() const;
  SendStatus getSendStatus() const;
  MQMessageQueue getMessageQueue() const;
  int64 getQueueOffset() const;

 private:
  SendStatus m_sendStatus;
  std::string m_msgId;
  MQMessageQueue m_messageQueue;
  int64 m_queueOffset;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
