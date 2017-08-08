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
#ifndef __MESSAGELISTENER_H__
#define __MESSAGELISTENER_H__

#include <limits.h>
#include "MQMessageExt.h"
#include "MQMessageQueue.h"

namespace rocketmq {
//<!***************************************************************************
enum ConsumeStatus {
  //consume success, msg will be cleard from memory
  CONSUME_SUCCESS,
  //consume fail, but will be re-consume by call messageLisenter again
  RECONSUME_LATER
};

/*enum ConsumeOrderlyStatus
{*/
/**
 * Success consumption
 */
// SUCCESS,
/**
 * Rollback consumption(only for binlog consumption)
 */
// ROLLBACK,
/**
 * Commit offset(only for binlog consumption)
 */
// COMMIT,
/**
 * Suspend current queue a moment
 */
// SUSPEND_CURRENT_QUEUE_A_MOMENT
/*};*/

enum MessageListenerType {
  messageListenerDefaultly = 0,
  messageListenerOrderly = 1,
  messageListenerConcurrently = 2
};

//<!***************************************************************************
class ROCKETMQCLIENT_API MQMessageListener {
 public:
  virtual ~MQMessageListener() {}
  virtual ConsumeStatus consumeMessage(const std::vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerDefaultly;
  }
};

class ROCKETMQCLIENT_API MessageListenerOrderly : public MQMessageListener {
 public:
  virtual ~MessageListenerOrderly() {}
  virtual ConsumeStatus consumeMessage(const std::vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerOrderly;
  }
};

class ROCKETMQCLIENT_API MessageListenerConcurrently
    : public MQMessageListener {
 public:
  virtual ~MessageListenerConcurrently() {}
  virtual ConsumeStatus consumeMessage(const std::vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerConcurrently;
  }
};

//<!***************************************************************************
}  //<!end namespace;
#endif
