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
#ifndef __TCPREMOTINGCLIENT_H__
#define __TCPREMOTINGCLIENT_H__

#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/weak_ptr.hpp>
#include <map>
#include "ClientRemotingProcessor.h"
#include "RemotingCommand.h"
#include "ResponseFuture.h"
#include "SocketUtil.h"
#include "TcpTransport.h"

namespace rocketmq {
//<!************************************************************************

class TcpRemotingClient {
 public:
  TcpRemotingClient(int pullThreadNum, uint64_t tcpConnectTimeout,
                    uint64_t tcpTransportTryLockTimeout);
  virtual ~TcpRemotingClient();
  void stopAllTcpTransportThread();
  void updateNameServerAddressList(const string& addrs);

  //<!delete outsite;
  RemotingCommand* invokeSync(const string& addr, RemotingCommand& request,
                              int timeoutMillis = 3000);

  bool invokeHeartBeat(const string& addr, RemotingCommand& request);

  bool invokeAsync(const string& addr, RemotingCommand& request,
                   AsyncCallbackWrap* cbw, int64 timeoutMilliseconds);

  void invokeOneway(const string& addr, RemotingCommand& request);

  void ProcessData(const MemoryBlock& mem, const string& addr);

  void registerProcessor(MQRequestCode requestCode,
                         ClientRemotingProcessor* clientRemotingProcessor);

  void boost_asio_work();
  void handleAsyncPullForResponseTimeout(const boost::system::error_code& e,
                                         int opaque);

 private:
  static void static_messageReceived(void* context, const MemoryBlock& mem,
                                     const string& addr);
  void messageReceived(const MemoryBlock& mem, const string& addr);
  boost::shared_ptr<TcpTransport> GetTransport(const string& addr,
                                               bool needRespons);
  boost::shared_ptr<TcpTransport> CreateTransport(const string& addr,
                                                  bool needRespons);
  boost::shared_ptr<TcpTransport> CreateNameserverTransport(bool needRespons);
  void CloseTransport(const string& addr, boost::shared_ptr<TcpTransport> pTcp);
  void CloseNameServerTransport(boost::shared_ptr<TcpTransport> pTcp);
  bool SendCommand(boost::shared_ptr<TcpTransport> pTts, RemotingCommand& msg);
  void processRequestCommand(RemotingCommand* pCmd, const string& addr);
  void processResponseCommand(RemotingCommand* pCmd,
                              boost::shared_ptr<ResponseFuture> pfuture);

  void addResponseFuture(int opaque, boost::shared_ptr<ResponseFuture> pfuture);
  boost::shared_ptr<ResponseFuture> findAndDeleteResponseFuture(int opaque);

  void addAsyncResponseFuture(int opaque,
                              boost::shared_ptr<ResponseFuture> pfuture);
  boost::shared_ptr<ResponseFuture> findAndDeleteAsyncResponseFuture(
      int opaque);

  void addTimerCallback(boost::asio::deadline_timer* t, int opaque);
  void eraseTimerCallback(int opaque);
  void cancelTimerCallback(int opaque);
  void removeAllTimerCallback();

 private:
  typedef map<string, boost::shared_ptr<TcpTransport>> TcpMap;
  typedef map<int, boost::shared_ptr<ResponseFuture>> ResMap;

  typedef map<int, ClientRemotingProcessor*> RequestMap;
  RequestMap m_requestTable;

  boost::mutex m_futureTableMutex;
  ResMap m_futureTable;  //<! id->future;

  ResMap m_asyncFutureTable;
  boost::mutex m_asyncFutureLock;

  TcpMap m_tcpTable;  //<! ip->tcp;
  boost::timed_mutex m_tcpLock;

  // ThreadPool        m_threadpool;
  int m_pullThreadNum;
  uint64_t m_tcpConnectTimeout;           // ms
  uint64_t m_tcpTransportTryLockTimeout;  // s

  //<! Nameserver
  boost::timed_mutex m_namesrvlock;
  vector<string> m_namesrvAddrList;
  string m_namesrvAddrChoosed;
  unsigned int m_namesrvIndex;
  boost::asio::io_service m_ioService;
  boost::thread_group m_threadpool;
  boost::asio::io_service::work m_ioServiceWork;

  boost::asio::io_service m_async_ioService;
  unique_ptr<boost::thread> m_async_service_thread;

  typedef map<int, boost::asio::deadline_timer*> asyncTimerMap;
  boost::mutex m_timerMapMutex;
  asyncTimerMap m_async_timer_map;
};

//<!************************************************************************
}  //<!end namespace;

#endif
