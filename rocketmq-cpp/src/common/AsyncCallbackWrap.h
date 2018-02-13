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
#ifndef __ASYNCCALLBACKWRAP_H__
#define __ASYNCCALLBACKWRAP_H__

#include "AsyncArg.h"
#include "AsyncCallback.h"
#include "MQMessage.h"
#include "UtilAll.h"

namespace rocketmq {

class ResponseFuture;
class MQClientAPIImpl;
//<!***************************************************************************
enum asyncCallBackType {
  asyncCallbackWrap = 0,
  sendCallbackWrap = 1,
  pullCallbackWarp = 2
};

struct AsyncCallbackWrap {
 public:
  AsyncCallbackWrap(AsyncCallback* pAsyncCallback, MQClientAPIImpl* pclientAPI);
  virtual ~AsyncCallbackWrap();
  virtual void operationComplete(ResponseFuture* pResponseFuture,
                                 bool bProducePullRequest) = 0;
  virtual void onException() = 0;
  virtual asyncCallBackType getCallbackType() = 0;

 protected:
  AsyncCallback* m_pAsyncCallBack;
  MQClientAPIImpl* m_pClientAPI;
};

//<!************************************************************************
class SendCallbackWrap : public AsyncCallbackWrap {
 public:
  SendCallbackWrap(const string& brokerName, const MQMessage& msg,
                   AsyncCallback* pAsyncCallback, MQClientAPIImpl* pclientAPI);

  virtual ~SendCallbackWrap(){};
  virtual void operationComplete(ResponseFuture* pResponseFuture,
                                 bool bProducePullRequest);
  virtual void onException();
  virtual asyncCallBackType getCallbackType() { return sendCallbackWrap; }

 private:
  MQMessage m_msg;
  string m_brokerName;
};

//<!***************************************************************************
class PullCallbackWarp : public AsyncCallbackWrap {
 public:
  PullCallbackWarp(AsyncCallback* pAsyncCallback, MQClientAPIImpl* pclientAPI,
                   void* pArg);
  virtual ~PullCallbackWarp();
  virtual void operationComplete(ResponseFuture* pResponseFuture,
                                 bool bProducePullRequest);
  virtual void onException();
  virtual asyncCallBackType getCallbackType() { return pullCallbackWarp; }

 private:
  AsyncArg m_pArg;
};

//<!***************************************************************************
}  //<!end namespace;
#endif  //<! _AsyncCallbackWrap_H_
