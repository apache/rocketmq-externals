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
#ifndef __SENDMESSAGEHOOK_H__
#define __SENDMESSAGEHOOK_H__

#include "MQClientException.h"
#include "MQMessage.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!***************************************************************************
class ROCKETMQCLIENT_API SendMessageContext {
 public:
  string producerGroup;
  MQMessage msg;
  MQMessageQueue mq;
  string brokerAddr;
  int communicationMode;
  SendResult sendResult;
  MQException* pException;
  void* pArg;
};

class ROCKETMQCLIENT_API SendMessageHook {
 public:
  virtual ~SendMessageHook() {}
  virtual string hookName() = 0;
  virtual void sendMessageBefore(const SendMessageContext& context) = 0;
  virtual void sendMessageAfter(const SendMessageContext& context) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
