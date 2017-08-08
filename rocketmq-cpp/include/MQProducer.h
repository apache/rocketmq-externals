/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MQPRODUCER_H__
#define __MQPRODUCER_H__

#include "AsyncCallback.h"
#include "MQClient.h"
#include "MQMessageQueue.h"
#include "MQSelector.h"
#include "RocketMQClient.h"
#include "SendResult.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MQProducer : public MQClient {
 public:
  MQProducer() {}
  virtual ~MQProducer() {}
  // if setted bActiveBroker, will search brokers with best service state
  // firstly, then search brokers that had been sent failed by last time;
  virtual SendResult send(MQMessage& msg, bool bSelectActiveBroker = false) = 0;
  virtual SendResult send(MQMessage& msg, const MQMessageQueue& mq) = 0;
  // strict order msg, if send failed on seleted MessageQueue, throw exception
  // to up layer
  virtual SendResult send(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg) = 0;
  // non-strict order msg, if send failed on seleted MessageQueue, will auto
  // retry others Broker queues with autoRetryTimes;
  // if setted bActiveBroker, if send failed on seleted MessageQueue, , and then
  // search brokers with best service state, lastly will search brokers that had
  // been sent failed by last time;
  virtual SendResult send(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg, int autoRetryTimes,
                          bool bActiveBroker = false) = 0;
  virtual void send(MQMessage& msg, SendCallback* sendCallback,
                    bool bSelectActiveBroker = false) = 0;
  virtual void send(MQMessage& msg, const MQMessageQueue& mq,
                    SendCallback* sendCallback) = 0;
  virtual void send(MQMessage& msg, MessageQueueSelector* selector, void* arg,
                    SendCallback* sendCallback) = 0;
  virtual void sendOneway(MQMessage& msg, bool bSelectActiveBroker = false) = 0;
  virtual void sendOneway(MQMessage& msg, const MQMessageQueue& mq) = 0;
  virtual void sendOneway(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
