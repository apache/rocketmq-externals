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
#include "TcpTransport.h"
#ifndef WIN32
#include <arpa/inet.h>  // for sockaddr_in and inet_ntoa...
#include <netinet/tcp.h>
#include <sys/socket.h>  // for socket(), bind(), and connect()...
#endif
#include "Logging.h"
#include "TcpRemotingClient.h"
#include "UtilAll.h"

namespace rocketmq {

//<!************************************************************************
TcpTransport::TcpTransport(TcpRemotingClient *pTcpRemointClient,
                           READ_CALLBACK handle /* = NULL */)
    : m_tcpConnectStatus(e_connectInit),
      m_ReadDatathread(NULL),
      m_readcallback(handle),
      m_tcpRemotingClient(pTcpRemointClient),
      m_event_base_status(false),
      m_event_base_mtx(),
      m_event_base_cv() {
  m_startTime = UtilAll::currentTimeMillis();
#ifdef WIN32
  evthread_use_windows_threads();
#else
  evthread_use_pthreads();
#endif
  m_eventBase = NULL;
  m_bufferEvent = NULL;
}
TcpTransport::~TcpTransport() {
  m_readcallback = NULL;
  m_bufferEvent = NULL;
  m_eventBase = NULL;
}

tcpConnectStatus TcpTransport::connect(const string &strServerURL,
                                       int timeOutMillisecs /* = 3000 */) {
  string hostName;
  short portNumber;
  if (!UtilAll::SplitURL(strServerURL, hostName, portNumber)) {
    return e_connectFail;
  }

  boost::lock_guard<boost::mutex> lock(m_socketLock);

  struct sockaddr_in sin;
  memset(&sin, 0, sizeof(sin));
  sin.sin_family = AF_INET;
  sin.sin_addr.s_addr = inet_addr(hostName.c_str());
  sin.sin_port = htons(portNumber);

  m_eventBase = event_base_new();
  m_bufferEvent = bufferevent_socket_new(
      m_eventBase, -1, BEV_OPT_CLOSE_ON_FREE | BEV_OPT_THREADSAFE);
  bufferevent_setcb(m_bufferEvent, readNextMessageIntCallback, NULL, eventcb,
                    this);
  bufferevent_enable(m_bufferEvent, EV_READ | EV_WRITE);
  bufferevent_setwatermark(m_bufferEvent, EV_READ, 4, 0);

  setTcpConnectStatus(e_connectWaitResponse);
  if (bufferevent_socket_connect(m_bufferEvent, (struct sockaddr *)&sin,
                                 sizeof(sin)) < 0) {
    LOG_INFO("connect to fd:%d failed", bufferevent_getfd(m_bufferEvent));
    setTcpConnectStatus(e_connectFail);
    freeBufferEvent();
    return e_connectFail;
  } else {
    int fd = bufferevent_getfd(m_bufferEvent);
    LOG_INFO("try to connect to fd:%d, addr:%s", fd, (hostName.c_str()));

    evthread_make_base_notifiable(m_eventBase);
    
    m_ReadDatathread = new boost::thread(boost::bind(&TcpTransport::runThread, this));
    
    while(!m_event_base_status) {
      LOG_INFO("Wait till event base is looping");
      boost::system_time const timeout=boost::get_system_time()+ boost::posix_time::milliseconds(1000);
      boost::unique_lock<boost::mutex> lock(m_event_base_mtx);
      m_event_base_cv.timed_wait(lock, timeout);
    }

    return e_connectWaitResponse;
  }
}

void TcpTransport::setTcpConnectStatus(tcpConnectStatus connectStatus) {
  m_tcpConnectStatus = connectStatus;
}

tcpConnectStatus TcpTransport::getTcpConnectStatus() {
  return m_tcpConnectStatus;
}

tcpConnectStatus TcpTransport::waitTcpConnectEvent(int timeoutMillisecs) {
  boost::unique_lock<boost::mutex> lk(m_connectEventLock);
  if (!m_connectEvent.timed_wait(
          lk, boost::posix_time::milliseconds(timeoutMillisecs))) {
    LOG_INFO("connect timeout");
  }
  return getTcpConnectStatus();
}

void TcpTransport::setTcpConnectEvent(tcpConnectStatus connectStatus) {
  tcpConnectStatus baseStatus(getTcpConnectStatus());
  setTcpConnectStatus(connectStatus);
  if (baseStatus == e_connectWaitResponse) {
    LOG_INFO("received libevent callback event");
    m_connectEvent.notify_all();
  }
}

void TcpTransport::disconnect(const string &addr) {
  boost::lock_guard<boost::mutex> lock(m_socketLock);
  if (getTcpConnectStatus() != e_connectInit) {
    clearBufferEventCallback();
    LOG_INFO("disconnect:%s start", addr.c_str());
    m_connectEvent.notify_all();
    setTcpConnectStatus(e_connectInit);
    if (m_ReadDatathread) {
      m_ReadDatathread->interrupt();
      exitBaseDispatch();
      while (m_ReadDatathread->timed_join(boost::posix_time::seconds(1)) ==
             false) {
        LOG_WARN("join readDataThread fail, retry");
        m_ReadDatathread->interrupt();
        exitBaseDispatch();
      }
      delete m_ReadDatathread;
      m_ReadDatathread = NULL;
    }
    freeBufferEvent();
    LOG_INFO("disconnect:%s completely", addr.c_str());
  }
}

void TcpTransport::clearBufferEventCallback() {
  if (m_bufferEvent) {
    // Bufferevents are internally reference-counted, so if the bufferevent has
    // pending deferred callbacks when you free it, it won't be deleted until
    // the callbacks are done.
    // so just empty callback to avoid future callback by libevent
    bufferevent_setcb(m_bufferEvent, NULL, NULL, NULL, NULL);
  }
}

void TcpTransport::freeBufferEvent() {
  if (m_bufferEvent) {
    bufferevent_free(m_bufferEvent);
    m_bufferEvent = NULL;
  }
  if (m_eventBase) {
    event_base_free(m_eventBase);
    m_eventBase = NULL;
  }
}
void TcpTransport::exitBaseDispatch() {
  if (m_eventBase) {
    event_base_loopbreak(m_eventBase);
    // event_base_loopexit(m_eventBase, NULL);  //Note: memory leak will be
    // occured when timer callback was not done;
  }
}

void TcpTransport::runThread() {
  while (m_ReadDatathread) {
    if (m_eventBase != NULL) {
      
      if (!m_event_base_status) {
        boost::mutex::scoped_lock lock(m_event_base_mtx);
        m_event_base_status.store(true);
        m_event_base_cv.notify_all();
        LOG_INFO("Notify on event_base_dispatch");
      }
      event_base_dispatch(m_eventBase);
      // event_base_loop(m_eventBase, EVLOOP_ONCE);//EVLOOP_NONBLOCK should not
      // be used, as could not callback event immediatly
    }
    LOG_INFO("event_base_dispatch exit once");
    boost::this_thread::sleep(boost::posix_time::milliseconds(1));
    if (getTcpConnectStatus() != e_connectSuccess) return;
  }
}

void TcpTransport::timeoutcb(evutil_socket_t fd, short what, void *arg) {
  LOG_INFO("timeoutcb: received  event:%d on fd:%d", what, fd);
  TcpTransport *tcpTrans = (TcpTransport *)arg;
  if (tcpTrans->getTcpConnectStatus() != e_connectSuccess) {
    LOG_INFO("timeoutcb: after connect time, tcp was not established on fd:%d",
             fd);
    tcpTrans->setTcpConnectStatus(e_connectFail);
  } else {
    LOG_INFO("timeoutcb: after connect time, tcp was established on fd:%d", fd);
  }
}

void TcpTransport::eventcb(struct bufferevent *bev, short what, void *ctx) {
  evutil_socket_t fd = bufferevent_getfd(bev);
  TcpTransport *tcpTrans = (TcpTransport *)ctx;
  LOG_INFO("eventcb: received event:%x on fd:%d", what, fd);
  if (what & BEV_EVENT_CONNECTED) {
    int val = 1;
    setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, (char *)&val, sizeof(val));
    LOG_INFO("eventcb:connect to fd:%d successfully", fd);
    tcpTrans->setTcpConnectEvent(e_connectSuccess);
  } else if (what & (BEV_EVENT_ERROR | BEV_EVENT_EOF | BEV_EVENT_READING |
                     BEV_EVENT_WRITING)) {
    LOG_INFO("eventcb:rcv error event cb:%x on fd:%d", what, fd);
    tcpTrans->setTcpConnectEvent(e_connectFail);
    bufferevent_setcb(bev, NULL, NULL, NULL, NULL);
    // bufferevent_disable(bev, EV_READ|EV_WRITE);
    // bufferevent_free(bev);
  } else {
    LOG_ERROR("eventcb: received error event:%d on fd:%d", what, fd);
  }
}

void TcpTransport::readNextMessageIntCallback(struct bufferevent *bev,
                                              void *ctx) {
  /* This callback is invoked when there is data to read on bev. */

  // protocol:  <length> <header length> <header data> <body data>
  //                    1                   2                       3 4
  // rocketmq protocol contains 4 parts as following:
  //     1, big endian 4 bytes int, its length is sum of 2,3 and 4
  //     2, big endian 4 bytes int, its length is 3
  //     3, use json to serialization data
  //     4, application could self-defination binary data

  struct evbuffer *input = bufferevent_get_input(bev);
  while (1) {
    struct evbuffer_iovec v[4];
    int n = evbuffer_peek(input, 4, NULL, v, sizeof(v) / sizeof(v[0]));

    int idx = 0;
    char hdr[4];
    char *p = hdr;
    unsigned int needed = 4;

    for (idx = 0; idx < n; idx++) {
      if (needed) {
        unsigned int tmp = needed < v[idx].iov_len ? needed : v[idx].iov_len;
        memcpy(p, v[idx].iov_base, tmp);
        p += tmp;
        needed -= tmp;
      } else {
        break;
      }
    }

    if (needed) {
      LOG_DEBUG(" too little data received with sum = %d ", 4 - needed);
      return;
    }
    uint32 totalLenOfOneMsg =
        *(uint32 *)hdr;  // first 4 bytes, which indicates 1st part of protocol
    uint32 bytesInMessage = ntohl(totalLenOfOneMsg);
    LOG_DEBUG("fd:%d, totalLen:" SIZET_FMT ", bytesInMessage:%d",
              bufferevent_getfd(bev), v[0].iov_len, bytesInMessage);

    uint32 len = evbuffer_get_length(input);
    if (len >= bytesInMessage + 4) {
      LOG_DEBUG("had received all data with len:%d from fd:%d", len,
                bufferevent_getfd(bev));
    } else {
      LOG_DEBUG(
          "didn't received whole bytesInMessage:%d, from fd:%d, totalLen:%d",
          bytesInMessage, bufferevent_getfd(bev), len);
      return;  // consider large data which was not received completely by now
    }

    if (bytesInMessage > 0) {
      MemoryBlock messageData(bytesInMessage, true);
      uint32 bytesRead = 0;
      char *data = messageData.getData() + bytesRead;
      bufferevent_read(bev, data, 4);
      bytesRead = bufferevent_read(bev, data, bytesInMessage);

      TcpTransport *tcpTrans = (TcpTransport *)ctx;
      tcpTrans->messageReceived(messageData);
    }
  }
}

bool TcpTransport::sendMessage(const char *pData, int len) {
  boost::lock_guard<boost::mutex> lock(m_socketLock);
  if (getTcpConnectStatus() != e_connectSuccess) {
    return false;
  }

  int bytes_left = len;
  int bytes_written = 0;
  const char *ptr = pData;

  /*NOTE:
      1. do not need to consider large data which could not send by once, as
     bufferevent could handle this case;
  */
  if (m_bufferEvent) {
    bytes_written = bufferevent_write(m_bufferEvent, ptr, bytes_left);
    if (bytes_written == 0)
      return true;
    else
      return false;
  }
  return false;
}

void TcpTransport::messageReceived(const MemoryBlock &mem) {
  if (m_readcallback) {
    m_readcallback(m_tcpRemotingClient, mem, getPeerAddrAndPort());
  }
}

const string TcpTransport::getPeerAddrAndPort() {
  struct sockaddr_in broker;
  socklen_t cLen = sizeof(broker);

  // getsockname(m_socket->getRawSocketHandle(), (struct sockaddr*) &s, &sLen);
  // // ! use connectSock here.
  getpeername(bufferevent_getfd(m_bufferEvent), (struct sockaddr *)&broker,
              &cLen);  // ! use connectSock here.
  LOG_DEBUG("broker addr: %s, broker port: %d", inet_ntoa(broker.sin_addr),
            ntohs(broker.sin_port));
  string brokerAddr(inet_ntoa(broker.sin_addr));
  brokerAddr.append(":");
  string brokerPort(UtilAll::to_string(ntohs(broker.sin_port)));
  brokerAddr.append(brokerPort);
  LOG_DEBUG("brokerAddr:%s", brokerAddr.c_str());
  return brokerAddr;
}

const uint64_t TcpTransport::getStartTime() const { return m_startTime; }

}  //<!end namespace;
