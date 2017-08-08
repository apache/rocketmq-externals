/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef _MQSELECTOR_H_
#define _MQSELECTOR_H_
#include "MQMessage.h"
#include "MQMessageQueue.h"
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MessageQueueSelector {
 public:
  virtual ~MessageQueueSelector() {}
  virtual MQMessageQueue select(const vector<MQMessageQueue>& mqs,
                                const MQMessage& msg, void* arg) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif  //<! _MQSELECTOR_H_
