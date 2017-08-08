/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SENDRESULT_H__
#define __SENDRESULT_H__

#include "MQMessageQueue.h"
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
//<!all to Master;
enum SendStatus {
  SEND_OK,
  SEND_FLUSH_DISK_TIMEOUT,
  SEND_FLUSH_SLAVE_TIMEOUT,
  SEND_SLAVE_NOT_AVAILABLE
};

//<!***************************************************************************
class ROCKETMQCLIENT_API SendResult {
 public:
  SendResult();
  SendResult(const SendStatus& sendStatus, const string& msgId,
             const MQMessageQueue& messageQueue, int64 queueOffset);

  virtual ~SendResult();
  SendResult(const SendResult& other);
  SendResult& operator=(const SendResult& other);

  const string& getMsgId() const;
  SendStatus getSendStatus() const;
  MQMessageQueue getMessageQueue() const;
  int64 getQueueOffset() const;

 private:
  SendStatus m_sendStatus;
  string m_msgId;
  MQMessageQueue m_messageQueue;
  int64 m_queueOffset;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
