/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __TCPTRANSPORT_H__
#define __TCPTRANSPORT_H__

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

namespace metaq {
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
  void freeBufferEvent();
  void exitBaseDispatch();
  void setTcpConnectEvent(tcpConnectStatus connectStatus);

 private:
  uint64_t m_startTime;
  boost::mutex m_socketLock;
  struct event_base *m_eventBase;
  struct bufferevent *m_bufferEvent;
  boost::mutex m_tcpConnectStatusMutex;
  tcpConnectStatus m_tcpConnectStatus;
  boost::mutex m_connectEventLock;
  boost::condition_variable_any m_connectEvent;
  //<!read data thread
  boost::thread *m_ReadDatathread;

  //<! read data callback
  READ_CALLBACK m_readcallback;
  TcpRemotingClient *m_tcpRemotingClient;
};

//<!************************************************************************
}  //<!end namespace;

#endif
