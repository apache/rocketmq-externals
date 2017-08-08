/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef _AsyncCallbackWrap_H_
#define _AsyncCallbackWrap_H_

#include "AsyncArg.h"
#include "AsyncCallback.h"
#include "MQMessage.h"
#include "UtilAll.h"

namespace metaq {

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
