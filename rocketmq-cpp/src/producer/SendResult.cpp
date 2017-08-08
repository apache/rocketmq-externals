/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "SendResult.h"
#include "UtilAll.h"
#include "VirtualEnvUtil.h"

namespace metaq {
//<!***************************************************************************
SendResult::SendResult() : m_sendStatus(SEND_OK), m_queueOffset(0) {}

SendResult::SendResult(const SendStatus& sendStatus, const string& msgId,
                       const MQMessageQueue& messageQueue, int64 queueOffset)
    : m_sendStatus(sendStatus),
      m_msgId(msgId),
      m_messageQueue(messageQueue),
      m_queueOffset(queueOffset) {}

SendResult::SendResult(const SendResult& other) {
  m_sendStatus = other.m_sendStatus;
  m_msgId = other.m_msgId;
  m_messageQueue = other.m_messageQueue;
  m_queueOffset = other.m_queueOffset;
}

SendResult& SendResult::operator=(const SendResult& other) {
  if (this != &other) {
    m_sendStatus = other.m_sendStatus;
    m_msgId = other.m_msgId;
    m_messageQueue = other.m_messageQueue;
    m_queueOffset = other.m_queueOffset;
  }
  return *this;
}

SendResult::~SendResult() {}

const string& SendResult::getMsgId() const { return m_msgId; }

SendStatus SendResult::getSendStatus() const { return m_sendStatus; }

MQMessageQueue SendResult::getMessageQueue() const { return m_messageQueue; }

int64 SendResult::getQueueOffset() const { return m_queueOffset; }

//<!************************************************************************
}  //<!end namespace;
