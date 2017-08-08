/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SENDMESSAGEHOOK_H__
#define __SENDMESSAGEHOOK_H__

#include "MQClientException.h"
#include "MQMessage.h"
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API SendMessageContext {
 public:
  string producerGroup;
  MQMessage msg;
  MQMessageQueue mq;
  string brokerAddr;
  int communicationMode;
  SendResult sendResult;
  MQException* pException;
  void* pArg;
};

class ROCKETMQCLIENT_API SendMessageHook {
 public:
  virtual ~SendMessageHook() {}
  virtual string hookName() = 0;
  virtual void sendMessageBefore(const SendMessageContext& context) = 0;
  virtual void sendMessageAfter(const SendMessageContext& context) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
