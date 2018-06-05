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
#ifndef __TCPTRANSPORT_H__
#define __TCPTRANSPORT_H__

#include <boost/atomic.hpp>
#include <boost/thread/condition_variable.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include "dataBlock.h"

extern "C" {
#include "event2/buffer.h"
#include "event2/bufferevent.h"
#include "event2/event.h"
#include "event2/thread.h"
}

namespace rocketmq {
//<!***************************************************************************
typedef enum {
  e_connectInit = 0,
  e_connectWaitResponse = 1,
  e_connectSuccess = 2,
  e_connectFail = 3
} tcpConnectStatus;

typedef void (*READ_CALLBACK)(void *context, const MemoryBlock &,
                              const std::string &);
class TcpRemotingClient;
class TcpTransport {
 public:
  TcpTransport(TcpRemotingClient *pTcpRemointClient,
               READ_CALLBACK handle = NULL);
  virtual ~TcpTransport();

  tcpConnectStatus connect(const std::string &strServerURL,
                           int timeOutMillisecs = 3000);
  void disconnect(const std::string &addr);
  tcpConnectStatus waitTcpConnectEvent(int timeoutMillisecs = 3000);
  void setTcpConnectStatus(tcpConnectStatus connectStatus);
  tcpConnectStatus getTcpConnectStatus();
  bool sendMessage(const char *pData, int len);
  const std::string getPeerAddrAndPort();
  const uint64_t getStartTime() const;

 private:
  void messageReceived(const MemoryBlock &mem);
  static void readNextMessageIntCallback(struct bufferevent *bev, void *ctx);
  static void eventcb(struct bufferevent *bev, short what, void *ctx);
  static void timeoutcb(evutil_socket_t fd, short what, void *arg);
  void runThread();
  void clearBufferEventCallback();
  void freeBufferEvent();
  void exitBaseDispatch();
  void setTcpConnectEvent(tcpConnectStatus connectStatus);

 private:
  uint64_t m_startTime;
  boost::mutex m_socketLock;
  struct event_base *m_eventBase;
  struct bufferevent *m_bufferEvent;
  boost::atomic<tcpConnectStatus> m_tcpConnectStatus;
  boost::mutex m_connectEventLock;
  boost::condition_variable_any m_connectEvent;

  boost::atomic<bool> m_event_base_status;
  boost::mutex        m_event_base_mtx;
  boost::condition_variable_any m_event_base_cv;

  //<!read data thread
  boost::thread *m_ReadDatathread;

  //<! read data callback
  READ_CALLBACK m_readcallback;
  TcpRemotingClient *m_tcpRemotingClient;
};

//<!************************************************************************
}  //<!end namespace;

#endif
