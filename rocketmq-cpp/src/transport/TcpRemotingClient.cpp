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
#include "TcpRemotingClient.h"
#include <stddef.h>
#ifndef WIN32
#include <sys/prctl.h>
#endif
#include "Logging.h"
#include "MemoryOutputStream.h"
#include "TopAddressing.h"
#include "UtilAll.h"

namespace rocketmq {

//<!************************************************************************
TcpRemotingClient::TcpRemotingClient(int pullThreadNum,
                                     uint64_t tcpConnectTimeout,
                                     uint64_t tcpTransportTryLockTimeout)
    : m_pullThreadNum(pullThreadNum),
      m_tcpConnectTimeout(tcpConnectTimeout),
      m_tcpTransportTryLockTimeout(tcpTransportTryLockTimeout),
      m_namesrvIndex(0),
      m_ioServiceWork(m_ioService) {
#ifndef WIN32
  string taskName = UtilAll::getProcessName();
  prctl(PR_SET_NAME, "networkTP", 0, 0, 0);
#endif
  for (int i = 0; i != pullThreadNum; ++i) {
    m_threadpool.create_thread(
        boost::bind(&boost::asio::io_service::run, &m_ioService));
  }
#ifndef WIN32
  prctl(PR_SET_NAME, taskName.c_str(), 0, 0, 0);
#endif
  LOG_INFO(
      "m_tcpConnectTimeout:%ju, m_tcpTransportTryLockTimeout:%ju, "
      "m_pullThreadNum:%d",
      m_tcpConnectTimeout, m_tcpTransportTryLockTimeout, m_pullThreadNum);
  m_async_service_thread.reset(new boost::thread(
      boost::bind(&TcpRemotingClient::boost_asio_work, this)));
}

void TcpRemotingClient::boost_asio_work() {
  LOG_INFO("TcpRemotingClient::boost asio async service runing");
  boost::asio::io_service::work work(m_async_ioService);  // avoid async io
                                                          // service stops after
                                                          // first timer timeout
                                                          // callback
  m_async_ioService.run();
}

TcpRemotingClient::~TcpRemotingClient() {
  m_tcpTable.clear();
  m_futureTable.clear();
  m_asyncFutureTable.clear();
  m_namesrvAddrList.clear();
  removeAllTimerCallback();
}

void TcpRemotingClient::stopAllTcpTransportThread() {
  LOG_DEBUG("TcpRemotingClient::stopAllTcpTransportThread Begin");
  m_async_ioService.stop();
  m_async_service_thread->interrupt();
  m_async_service_thread->join();
  removeAllTimerCallback();

  {
    TcpMap::iterator it = m_tcpTable.begin();
    for (; it != m_tcpTable.end(); ++it) {
      it->second->disconnect(it->first);
    }
    m_tcpTable.clear();
  }

  m_ioService.stop();
  m_threadpool.join_all();

  {
    boost::lock_guard<boost::mutex> lock(m_futureTableMutex);
    for (ResMap::iterator it = m_futureTable.begin(); it != m_futureTable.end();
         ++it) {
      if (it->second) it->second->releaseThreadCondition();
    }
  }
  LOG_DEBUG("TcpRemotingClient::stopAllTcpTransportThread End");
}

void TcpRemotingClient::updateNameServerAddressList(const string& addrs) {
  if (!addrs.empty()) {
    boost::unique_lock<boost::timed_mutex> lock(m_namesrvlock,
                                                boost::try_to_lock);
    if (!lock.owns_lock()) {
      if (!lock.timed_lock(boost::get_system_time() +
                           boost::posix_time::seconds(10))) {
        LOG_ERROR("updateNameServerAddressList get timed_mutex timeout");
        return;
      }
    }
    // clear first;
    m_namesrvAddrList.clear();

    vector<string> out;
    UtilAll::Split(out, addrs, ";");
    for (size_t i = 0; i < out.size(); i++) {
      string addr = out[i];
      UtilAll::Trim(addr);

      string hostName;
      short portNumber;
      if (UtilAll::SplitURL(addr, hostName, portNumber)) {
        LOG_INFO("update Namesrv:%s", addr.c_str());
        m_namesrvAddrList.push_back(addr);
      }
    }
    out.clear();
  }
}

bool TcpRemotingClient::invokeHeartBeat(const string& addr,
                                        RemotingCommand& request) {
  boost::shared_ptr<TcpTransport> pTcp = GetTransport(addr, true);
  if (pTcp != NULL) {
    int code = request.getCode();
    int opaque = request.getOpaque();
    boost::shared_ptr<ResponseFuture> responseFuture(
        new ResponseFuture(code, opaque, this, 3000, false, NULL));
    addResponseFuture(opaque, responseFuture);
    // LOG_INFO("invokeHeartbeat success, addr:%s, code:%d, opaque:%d,
    // timeoutms:%d", addr.c_str(), code, opaque, 3000);

    if (SendCommand(pTcp, request)) {
      responseFuture->setSendRequestOK(true);
      unique_ptr<RemotingCommand> pRsp(responseFuture->waitResponse(3000));
      if (pRsp == NULL) {
        LOG_ERROR(
            "wait response timeout of heartbeat, so closeTransport of addr:%s",
            addr.c_str());
        CloseTransport(addr, pTcp);
        return false;
      } else if (pRsp->getCode() == SUCCESS_VALUE) {
        return true;
      } else {
        LOG_WARN("get error response:%d of heartbeat to addr:%s",
                 pRsp->getCode(), addr.c_str());
        return false;
      }
    } else {
      CloseTransport(addr, pTcp);
    }
  }
  return false;
}

RemotingCommand* TcpRemotingClient::invokeSync(const string& addr,
                                               RemotingCommand& request,
                                               int timeoutMillis /* = 3000 */) {
  boost::shared_ptr<TcpTransport> pTcp = GetTransport(addr, true);
  if (pTcp != NULL) {
    int code = request.getCode();
    int opaque = request.getOpaque();
    boost::shared_ptr<ResponseFuture> responseFuture(
        new ResponseFuture(code, opaque, this, timeoutMillis, false, NULL));
    addResponseFuture(opaque, responseFuture);

    if (SendCommand(pTcp, request)) {
      // LOG_INFO("invokeSync success, addr:%s, code:%d, opaque:%d,
      // timeoutms:%d", addr.c_str(), code, opaque, timeoutMillis);
      responseFuture->setSendRequestOK(true);
      RemotingCommand* pRsp = responseFuture->waitResponse(timeoutMillis);
      if (pRsp == NULL) {
        if (code != GET_CONSUMER_LIST_BY_GROUP) {
          LOG_WARN(
              "wait response timeout or get NULL response of code:%d, so "
              "closeTransport of addr:%s",
              code, addr.c_str());
          CloseTransport(addr, pTcp);
        }
        // avoid responseFuture leak;
        findAndDeleteResponseFuture(opaque);
        return NULL;
      } else {
        return pRsp;
      }
    } else {
      // avoid responseFuture leak;
      findAndDeleteResponseFuture(opaque);
      CloseTransport(addr, pTcp);
    }
  }
  return NULL;
}

bool TcpRemotingClient::invokeAsync(const string& addr,
                                    RemotingCommand& request,
                                    AsyncCallbackWrap* cbw,
                                    int64 timeoutMilliseconds) {
  boost::shared_ptr<TcpTransport> pTcp = GetTransport(addr, true);
  if (pTcp != NULL) {
    //<!not delete, for callback to delete;
    int code = request.getCode();
    int opaque = request.getOpaque();
    boost::shared_ptr<ResponseFuture> responseFuture(
        new ResponseFuture(code, opaque, this, timeoutMilliseconds, true, cbw));
    addAsyncResponseFuture(opaque, responseFuture);
    if (cbw) {
      boost::asio::deadline_timer* t = new boost::asio::deadline_timer(
          m_async_ioService,
          boost::posix_time::milliseconds(timeoutMilliseconds));
      addTimerCallback(t, opaque);
      boost::system::error_code e;
      t->async_wait(
          boost::bind(&TcpRemotingClient::handleAsyncPullForResponseTimeout,
                      this, e, opaque));
    }

    if (SendCommand(pTcp, request))  // Even if send failed, asyncTimerThread
                                     // will trigger next pull request or report
                                     // send msg failed
    {
      LOG_DEBUG("invokeAsync success, addr:%s, code:%d, opaque:%d",
                addr.c_str(), code, opaque);
      responseFuture->setSendRequestOK(true);
    }
    return true;
  }
  LOG_ERROR("invokeAsync failed of addr:%s", addr.c_str());
  return false;
}

void TcpRemotingClient::invokeOneway(const string& addr,
                                     RemotingCommand& request) {
  //<!not need callback;
  boost::shared_ptr<TcpTransport> pTcp = GetTransport(addr, true);
  if (pTcp != NULL) {
    request.markOnewayRPC();
    LOG_DEBUG("invokeOneway success, addr:%s, code:%d", addr.c_str(),
              request.getCode());
    SendCommand(pTcp, request);
  }
}

boost::shared_ptr<TcpTransport> TcpRemotingClient::GetTransport(
    const string& addr, bool needRespons) {
  if (addr.empty()) return CreateNameserverTransport(needRespons);

  return CreateTransport(addr, needRespons);
}

boost::shared_ptr<TcpTransport> TcpRemotingClient::CreateTransport(
    const string& addr, bool needRespons) {
  boost::shared_ptr<TcpTransport> tts;
  {
    // try get m_tcpLock util m_tcpTransportTryLockTimeout to avoid blocking
    // long
    // time, if could not get m_tcpLock, return NULL
    bool bGetMutex = false;
    boost::unique_lock<boost::timed_mutex> lock(m_tcpLock, boost::try_to_lock);
    if (!lock.owns_lock()) {
      if (!lock.timed_lock(
              boost::get_system_time() +
              boost::posix_time::seconds(m_tcpTransportTryLockTimeout))) {
        LOG_ERROR("GetTransport of:%s get timed_mutex timeout", addr.c_str());
        boost::shared_ptr<TcpTransport> pTcp;
        return pTcp;
      } else {
        bGetMutex = true;
      }
    } else {
      bGetMutex = true;
    }
    if (bGetMutex) {
      if (m_tcpTable.find(addr) != m_tcpTable.end()) {
        boost::weak_ptr<TcpTransport> weakPtcp(m_tcpTable[addr]);
        boost::shared_ptr<TcpTransport> tcp = weakPtcp.lock();
        if (tcp) {
          tcpConnectStatus connectStatus = tcp->getTcpConnectStatus();
          if (connectStatus == e_connectWaitResponse) {
            boost::shared_ptr<TcpTransport> pTcp;
            return pTcp;
          } else if (connectStatus == e_connectFail) {
            LOG_ERROR("tcpTransport with server disconnected, erase server:%s",
                      addr.c_str());
            tcp->disconnect(
                addr);  // avoid coredump when connection with broker was broken
            m_tcpTable.erase(addr);
          } else if (connectStatus == e_connectSuccess) {
            return tcp;
          } else {
            LOG_ERROR(
                "go to fault state, erase:%s from tcpMap, and reconnect "
                "it",
                addr.c_str());
            m_tcpTable.erase(addr);
          }
        }
      }

      //<!callback;
      READ_CALLBACK callback =
          needRespons ? &TcpRemotingClient::static_messageReceived : NULL;

      tts.reset(new TcpTransport(this, callback));
      tcpConnectStatus connectStatus = tts->connect(addr, m_tcpConnectTimeout);
      if (connectStatus != e_connectWaitResponse) {
        LOG_WARN("can not connect to :%s", addr.c_str());
        tts->disconnect(addr);
        boost::shared_ptr<TcpTransport> pTcp;
        return pTcp;
      } else {
        m_tcpTable[addr] = tts;  // even if connecting failed finally, this
                                 // server transport will be erased by next
                                 // CreateTransport
      }
    } else {
      LOG_WARN("get tcpTransport mutex failed :%s", addr.c_str());
      boost::shared_ptr<TcpTransport> pTcp;
      return pTcp;
    }
  }

  tcpConnectStatus connectStatus =
      tts->waitTcpConnectEvent(m_tcpConnectTimeout);
  if (connectStatus != e_connectSuccess) {
    LOG_WARN("can not connect to server:%s", addr.c_str());
    tts->disconnect(addr);
    boost::shared_ptr<TcpTransport> pTcp;
    return pTcp;
  } else {
    LOG_INFO("connect server with addr:%s success", addr.c_str());
    return tts;
  }
}

boost::shared_ptr<TcpTransport> TcpRemotingClient::CreateNameserverTransport(
    bool needRespons) {
  // m_namesrvLock was added to avoid operation of nameServer was blocked by
  // m_tcpLock, it was used by single Thread mostly, so no performance impact
  // try get m_tcpLock util m_tcpTransportTryLockTimeout to avoid blocking long
  // time, if could not get m_namesrvlock, return NULL
  bool bGetMutex = false;
  boost::unique_lock<boost::timed_mutex> lock(m_namesrvlock,
                                              boost::try_to_lock);
  if (!lock.owns_lock()) {
    if (!lock.timed_lock(
            boost::get_system_time() +
            boost::posix_time::seconds(m_tcpTransportTryLockTimeout))) {
      LOG_ERROR("CreateNameserverTransport get timed_mutex timeout");
      boost::shared_ptr<TcpTransport> pTcp;
      return pTcp;
    } else {
      bGetMutex = true;
    }
  } else {
    bGetMutex = true;
  }

  if (bGetMutex) {
    if (!m_namesrvAddrChoosed.empty()) {
      boost::shared_ptr<TcpTransport> pTcp =
          GetTransport(m_namesrvAddrChoosed, true);
      if (pTcp)
        return pTcp;
      else
        m_namesrvAddrChoosed.clear();
    }

    vector<string>::iterator itp = m_namesrvAddrList.begin();
    for (; itp != m_namesrvAddrList.end(); ++itp) {
      unsigned int index = m_namesrvIndex % m_namesrvAddrList.size();
      if (m_namesrvIndex == numeric_limits<unsigned int>::max())
        m_namesrvIndex = 0;
      m_namesrvIndex++;
      LOG_INFO("namesrvIndex is:%d, index:%d, namesrvaddrlist size:" SIZET_FMT
               "",
               m_namesrvIndex, index, m_namesrvAddrList.size());
      boost::shared_ptr<TcpTransport> pTcp =
          GetTransport(m_namesrvAddrList[index], true);
      if (pTcp) {
        m_namesrvAddrChoosed = m_namesrvAddrList[index];
        return pTcp;
      }
    }
    boost::shared_ptr<TcpTransport> pTcp;
    return pTcp;
  } else {
    LOG_WARN("get nameServer tcpTransport mutex failed");
    boost::shared_ptr<TcpTransport> pTcp;
    return pTcp;
  }
}

void TcpRemotingClient::CloseTransport(const string& addr,
                                       boost::shared_ptr<TcpTransport> pTcp) {
  if (addr.empty()) {
    return CloseNameServerTransport(pTcp);
  }

  bool bGetMutex = false;
  boost::unique_lock<boost::timed_mutex> lock(m_tcpLock, boost::try_to_lock);
  if (!lock.owns_lock()) {
    if (!lock.timed_lock(
            boost::get_system_time() +
            boost::posix_time::seconds(m_tcpTransportTryLockTimeout))) {
      LOG_ERROR("CloseTransport of:%s get timed_mutex timeout", addr.c_str());
      return;
    } else {
      bGetMutex = true;
    }
  } else {
    bGetMutex = true;
  }
  LOG_ERROR("CloseTransport of:%s", addr.c_str());
  if (bGetMutex) {
    bool removeItemFromTable = true;
    if (m_tcpTable.find(addr) != m_tcpTable.end()) {
      if (m_tcpTable[addr]->getStartTime() != pTcp->getStartTime()) {
        LOG_INFO(
            "tcpTransport with addr:%s has been closed before, and has been "
            "created again, nothing to do",
            addr.c_str());
        removeItemFromTable = false;
      }
    } else {
      LOG_INFO(
          "tcpTransport with addr:%s had been removed from tcpTable before",
          addr.c_str());
      removeItemFromTable = false;
    }

    if (removeItemFromTable == true) {
      LOG_WARN("closeTransport: disconnect broker:%s with state:%d",
               addr.c_str(), m_tcpTable[addr]->getTcpConnectStatus());
      if (m_tcpTable[addr]->getTcpConnectStatus() == e_connectSuccess)
        m_tcpTable[addr]->disconnect(
            addr);  // avoid coredump when connection with server was broken
      LOG_WARN("closeTransport: erase broker: %s", addr.c_str());
      m_tcpTable.erase(addr);
    }
  } else {
    LOG_WARN("CloseTransport::get tcpTransport mutex failed:%s", addr.c_str());
    return;
  }
  LOG_ERROR("CloseTransport of:%s end", addr.c_str());
}

void TcpRemotingClient::CloseNameServerTransport(
    boost::shared_ptr<TcpTransport> pTcp) {
  bool bGetMutex = false;
  boost::unique_lock<boost::timed_mutex> lock(m_namesrvlock,
                                              boost::try_to_lock);
  if (!lock.owns_lock()) {
    if (!lock.timed_lock(
            boost::get_system_time() +
            boost::posix_time::seconds(m_tcpTransportTryLockTimeout))) {
      LOG_ERROR("CreateNameserverTransport get timed_mutex timeout");
      return;
    } else {
      bGetMutex = true;
    }
  } else {
    bGetMutex = true;
  }
  if (bGetMutex) {
    string addr = m_namesrvAddrChoosed;
    bool removeItemFromTable = true;
    if (m_tcpTable.find(addr) != m_tcpTable.end()) {
      if (m_tcpTable[addr]->getStartTime() != pTcp->getStartTime()) {
        LOG_INFO(
            "tcpTransport with addr:%s has been closed before, and has been "
            "created again, nothing to do",
            addr.c_str());
        removeItemFromTable = false;
      }
    } else {
      LOG_INFO(
          "tcpTransport with addr:%s had been removed from tcpTable before",
          addr.c_str());
      removeItemFromTable = false;
    }

    if (removeItemFromTable == true) {
      m_tcpTable[addr]->disconnect(
          addr);  // avoid coredump when connection with server was broken
      LOG_WARN("closeTransport: erase broker: %s", addr.c_str());
      m_tcpTable.erase(addr);
      m_namesrvAddrChoosed.clear();
    }
  } else {
    LOG_WARN("CloseNameServerTransport::get tcpTransport mutex failed:%s",
             m_namesrvAddrChoosed.c_str());
    return;
  }
}

bool TcpRemotingClient::SendCommand(boost::shared_ptr<TcpTransport> pTts,
                                    RemotingCommand& msg) {
  const MemoryBlock* phead = msg.GetHead();
  const MemoryBlock* pbody = msg.GetBody();

  unique_ptr<MemoryOutputStream> result(new MemoryOutputStream(1024));
  if (phead->getData()) {
    result->write(phead->getData(), phead->getSize());
  }
  if (pbody->getData()) {
    result->write(pbody->getData(), pbody->getSize());
  }
  const char* pData = static_cast<const char*>(result->getData());
  int len = result->getDataSize();
  return pTts->sendMessage(pData, len);
}

void TcpRemotingClient::static_messageReceived(void* context,
                                               const MemoryBlock& mem,
                                               const string& addr) {
  TcpRemotingClient* pTcpRemotingClient = (TcpRemotingClient*)context;
  if (pTcpRemotingClient) pTcpRemotingClient->messageReceived(mem, addr);
}

void TcpRemotingClient::messageReceived(const MemoryBlock& mem,
                                        const string& addr) {
  m_ioService.post(
      boost::bind(&TcpRemotingClient::ProcessData, this, mem, addr));
}

void TcpRemotingClient::ProcessData(const MemoryBlock& mem,
                                    const string& addr) {
  RemotingCommand* pRespondCmd = NULL;
  try {
    pRespondCmd = RemotingCommand::Decode(mem);
  } catch (...) {
    LOG_ERROR("processData_error");
    return;
  }

  int opaque = pRespondCmd->getOpaque();

  //<!process self;
  if (pRespondCmd->isResponseType()) {
    boost::shared_ptr<ResponseFuture> pFuture(
        findAndDeleteAsyncResponseFuture(opaque));
    if (!pFuture) {
      pFuture = findAndDeleteResponseFuture(opaque);
      if (pFuture) {
        if (pFuture->getSyncResponseFlag()) {
          LOG_WARN("waitResponse already timeout of opaque:%d", opaque);
          deleteAndZero(pRespondCmd);
          return;
        }
        LOG_DEBUG("find_response opaque:%d", opaque);
      } else {
        LOG_DEBUG("responseFuture was deleted by timeout of opaque:%d", opaque);
        deleteAndZero(pRespondCmd);
        return;
      }
    }
    processResponseCommand(pRespondCmd, pFuture);
  } else {
    processRequestCommand(pRespondCmd, addr);
  }
}

void TcpRemotingClient::processResponseCommand(
    RemotingCommand* pCmd, boost::shared_ptr<ResponseFuture> pfuture) {
  int code = pfuture->getRequestCode();
  int opaque = pCmd->getOpaque();
  LOG_DEBUG("processResponseCommand, code:%d,opaque:%d", code, opaque);
  pCmd->SetExtHeader(code);  // set head , for response use

  pfuture->setResponse(pCmd);

  if (pfuture->getASyncFlag()) {
    if (!pfuture->getAsyncResponseFlag()) {
      pfuture->setAsyncResponseFlag();
      pfuture->setAsyncCallBackStatus(asyncCallBackStatus_response);
      pfuture->executeInvokeCallback();
      cancelTimerCallback(opaque);
    }
  }
}

void TcpRemotingClient::processRequestCommand(RemotingCommand* pCmd,
                                              const string& addr) {
  unique_ptr<RemotingCommand> pRequestCommand(pCmd);
  int requestCode = pRequestCommand->getCode();
  if (m_requestTable.find(requestCode) == m_requestTable.end()) {
    LOG_ERROR("can_not_find request:%d processor", requestCode);
  } else {
    unique_ptr<RemotingCommand> pResponse(
        m_requestTable[requestCode]->processRequest(addr,
                                                    pRequestCommand.get()));
    if (!pRequestCommand->isOnewayRPC()) {
      if (pResponse) {
        pResponse->setOpaque(pRequestCommand->getOpaque());
        pResponse->markResponseType();
        pResponse->Encode();

        invokeOneway(addr, *pResponse);
      }
    }
  }
}

void TcpRemotingClient::addResponseFuture(
    int opaque, boost::shared_ptr<ResponseFuture> pfuture) {
  boost::lock_guard<boost::mutex> lock(m_futureTableMutex);
  m_futureTable[opaque] = pfuture;
}

// Note: after call this function, shared_ptr of m_syncFutureTable[opaque] will
// be erased, so caller must ensure the life cycle of returned shared_ptr;
boost::shared_ptr<ResponseFuture>
TcpRemotingClient::findAndDeleteResponseFuture(int opaque) {
  boost::lock_guard<boost::mutex> lock(m_futureTableMutex);
  boost::shared_ptr<ResponseFuture> pResponseFuture;
  if (m_futureTable.find(opaque) != m_futureTable.end()) {
    pResponseFuture = m_futureTable[opaque];
    m_futureTable.erase(opaque);
  }
  return pResponseFuture;
}

void TcpRemotingClient::handleAsyncPullForResponseTimeout(
    const boost::system::error_code& e, int opaque) {
  if (e == boost::asio::error::operation_aborted) {
    return;
  }

  boost::shared_ptr<ResponseFuture> pFuture(
      findAndDeleteAsyncResponseFuture(opaque));
  if (pFuture && pFuture->getASyncFlag() && (pFuture->getAsyncCallbackWrap())) {
    if ((pFuture->getAsyncResponseFlag() !=
         true))  // if no response received, then check timeout or not
    {
      LOG_ERROR("no response got for opaque:%d", opaque);
      pFuture->setAsyncCallBackStatus(asyncCallBackStatus_timeout);
      pFuture->executeInvokeCallbackException();
    }
  }

  eraseTimerCallback(opaque);
}

void TcpRemotingClient::addAsyncResponseFuture(
    int opaque, boost::shared_ptr<ResponseFuture> pfuture) {
  boost::lock_guard<boost::mutex> lock(m_asyncFutureLock);
  m_asyncFutureTable[opaque] = pfuture;
}

// Note: after call this function, shared_ptr of m_asyncFutureTable[opaque] will
// be erased, so caller must ensure the life cycle of returned shared_ptr;
boost::shared_ptr<ResponseFuture>
TcpRemotingClient::findAndDeleteAsyncResponseFuture(int opaque) {
  boost::lock_guard<boost::mutex> lock(m_asyncFutureLock);
  boost::shared_ptr<ResponseFuture> pResponseFuture;
  if (m_asyncFutureTable.find(opaque) != m_asyncFutureTable.end()) {
    pResponseFuture = m_asyncFutureTable[opaque];
    m_asyncFutureTable.erase(opaque);
  }

  return pResponseFuture;
}

void TcpRemotingClient::registerProcessor(
    MQRequestCode requestCode,
    ClientRemotingProcessor* clientRemotingProcessor) {
  if (m_requestTable.find(requestCode) != m_requestTable.end())
    m_requestTable.erase(requestCode);
  m_requestTable[requestCode] = clientRemotingProcessor;
}

void TcpRemotingClient::addTimerCallback(boost::asio::deadline_timer* t,
                                         int opaque) {
  boost::lock_guard<boost::mutex> lock(m_timerMapMutex);
  if (m_async_timer_map.find(opaque) != m_async_timer_map.end()) {
    // AGENT_INFO("addTimerCallback:erase timerCallback opaque:%lld", opaque);
    boost::asio::deadline_timer* old_t = m_async_timer_map[opaque];
    old_t->cancel();
    delete old_t;
    old_t = NULL;
    m_async_timer_map.erase(opaque);
  }
  m_async_timer_map[opaque] = t;
}

void TcpRemotingClient::eraseTimerCallback(int opaque) {
  boost::lock_guard<boost::mutex> lock(m_timerMapMutex);
  if (m_async_timer_map.find(opaque) != m_async_timer_map.end()) {
    boost::asio::deadline_timer* t = m_async_timer_map[opaque];
    delete t;
    t = NULL;
    m_async_timer_map.erase(opaque);
  }
}

void TcpRemotingClient::cancelTimerCallback(int opaque) {
  boost::lock_guard<boost::mutex> lock(m_timerMapMutex);
  if (m_async_timer_map.find(opaque) != m_async_timer_map.end()) {
    // AGENT_INFO("cancel timerCallback opaque:%lld", opaque);
    boost::asio::deadline_timer* t = m_async_timer_map[opaque];
    t->cancel();
    delete t;
    t = NULL;
    m_async_timer_map.erase(opaque);
  }
}

void TcpRemotingClient::removeAllTimerCallback() {
  boost::lock_guard<boost::mutex> lock(m_timerMapMutex);
  for (asyncTimerMap::iterator it = m_async_timer_map.begin();
       it != m_async_timer_map.end(); ++it) {
    boost::asio::deadline_timer* t = it->second;
    t->cancel();
    delete t;
    t = NULL;
  }
  m_async_timer_map.clear();
}

//<!************************************************************************
}  //<!end namespace;
