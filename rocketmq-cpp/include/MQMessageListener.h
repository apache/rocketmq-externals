/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGELISTENER_H__
#define __MESSAGELISTENER_H__

#include <limits.h>
#include "MQMessageExt.h"
#include "MQMessageQueue.h"

namespace metaq {
//<!***************************************************************************
enum ConsumeStatus {
  //consume success, msg will be cleard from memory
  CONSUME_SUCCESS,
  //consume fail, but will be re-consume by call messageLisenter again
  RECONSUME_LATER
};

/*enum ConsumeOrderlyStatus
{*/
/**
 * Success consumption
 */
// SUCCESS,
/**
 * Rollback consumption(only for binlog consumption)
 */
// ROLLBACK,
/**
 * Commit offset(only for binlog consumption)
 */
// COMMIT,
/**
 * Suspend current queue a moment
 */
// SUSPEND_CURRENT_QUEUE_A_MOMENT
/*};*/

enum MessageListenerType {
  messageListenerDefaultly = 0,
  messageListenerOrderly = 1,
  messageListenerConcurrently = 2
};

//<!***************************************************************************
class ROCKETMQCLIENT_API MQMessageListener {
 public:
  virtual ~MQMessageListener() {}
  virtual ConsumeStatus consumeMessage(const vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerDefaultly;
  }
};

class ROCKETMQCLIENT_API MessageListenerOrderly : public MQMessageListener {
 public:
  virtual ~MessageListenerOrderly() {}
  virtual ConsumeStatus consumeMessage(const vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerOrderly;
  }
};

class ROCKETMQCLIENT_API MessageListenerConcurrently
    : public MQMessageListener {
 public:
  virtual ~MessageListenerConcurrently() {}
  virtual ConsumeStatus consumeMessage(const vector<MQMessageExt>& msgs) = 0;
  virtual MessageListenerType getMessageListenerType() {
    return messageListenerConcurrently;
  }
};

//<!***************************************************************************
}  //<!end namespace;
#endif
