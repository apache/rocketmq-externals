/********************************************************************
author: qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SENDCALLBACK_H__
#define __SENDCALLBACK_H__

#include "MQClientException.h"
#include "PullResult.h"
#include "RocketMQClient.h"
#include "SendResult.h"

namespace metaq {
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

//async SendCallback will be deleted automatically by metaq cpp after invoke callback interface
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
