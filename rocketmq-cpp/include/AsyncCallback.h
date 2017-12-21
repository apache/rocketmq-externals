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

#ifndef __SENDCALLBACK_H__
#define __SENDCALLBACK_H__

#include "MQClientException.h"
#include "PullResult.h"
#include "RocketMQClient.h"
#include "SendResult.h"

namespace rocketmq {
//<!***************************************************************************
struct AsyncCallback {};
//<!***************************************************************************
typedef enum sendCallbackType {
  noAutoDeleteSendCallback = 0,
  autoDeleteSendCallback = 1
} sendCallbackType;

class ROCKETMQCLIENT_API SendCallback : public AsyncCallback {
 public:
  virtual ~SendCallback() {}
  virtual void onSuccess(SendResult& sendResult) = 0;
  virtual void onException(MQException& e) = 0;
  virtual sendCallbackType getSendCallbackType() {
    return noAutoDeleteSendCallback;
  }
};

//async SendCallback will be deleted automatically by rocketmq cpp after invoke callback interface
class ROCKETMQCLIENT_API AutoDeleteSendCallBack : public SendCallback {
 public:
  virtual ~AutoDeleteSendCallBack() {}
  virtual void onSuccess(SendResult& sendResult) = 0;
  virtual void onException(MQException& e) = 0;
  virtual sendCallbackType getSendCallbackType() {
    return autoDeleteSendCallback;
  }
};

//<!************************************************************************
class ROCKETMQCLIENT_API PullCallback : public AsyncCallback {
 public:
  virtual ~PullCallback() {}
  virtual void onSuccess(MQMessageQueue& mq, PullResult& result,
                         bool bProducePullRequest) = 0;
  virtual void onException(MQException& e) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
