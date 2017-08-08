/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __RESPONSEFUTURE_H__
#define __RESPONSEFUTURE_H__
#include <boost/atomic.hpp>
#include <boost/thread/condition_variable.hpp>
#include "AsyncCallbackWrap.h"
#include "RemotingCommand.h"
#include "UtilAll.h"

namespace metaq {

typedef enum asyncCallBackStatus {
  asyncCallBackStatus_init = 0,
  asyncCallBackStatus_response = 1,
  asyncCallBackStatus_timeout = 2
} asyncCallBackStatus;

class TcpRemotingClient;
//<!***************************************************************************
class ResponseFuture {
 public:
  ResponseFuture(int requestCode, int opaque, TcpRemotingClient* powner,
                 int64 timeoutMilliseconds, bool bAsync = false,
                 AsyncCallbackWrap* pcall = NULL);
  virtual ~ResponseFuture();
  void releaseThreadCondition();
  RemotingCommand* waitResponse(int timeoutMillis);
  RemotingCommand* getCommand() const;

  void setResponse(RemotingCommand* pResponseCommand);
  bool isSendRequestOK();
  void setSendRequestOK(bool sendRequestOK);
  int getRequestCode() const;
  int getOpaque() const;

  //<!callback;
  void executeInvokeCallback();
  void executeInvokeCallbackException();
  bool isTimeOut() const;
  // bool    isTimeOutMoreThan30s() const;
  const bool getASyncFlag();
  void setAsyncResponseFlag();
  const bool getAsyncResponseFlag();
  const bool getSyncResponseFlag();
  AsyncCallbackWrap* getAsyncCallbackWrap();
  void setAsyncCallBackStatus(asyncCallBackStatus asyncCallbackStatus);

 private:
  int m_requestCode;
  int m_opaque;
  bool m_sendRequestOK;
  boost::mutex m_defaultEventLock;
  boost::condition_variable_any m_defaultEvent;
  int64 m_beginTimestamp;
  int64 m_timeout;  // ms
  boost::atomic<bool> m_bAsync;
  RemotingCommand* m_pResponseCommand;  //<!delete outside;
  AsyncCallbackWrap* m_pCallbackWrap;
  boost::mutex m_asyncCallbackLock;
  asyncCallBackStatus m_asyncCallbackStatus;
  boost::atomic<bool> m_asyncResponse;
  boost::atomic<bool> m_syncResponse;
  // TcpRemotingClient*    m_tcpRemoteClient;
};
//<!************************************************************************
}  //<!end namespace;

#endif
