/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MQMESSAGEQUEUE_H__
#define __MQMESSAGEQUEUE_H__

#include <iomanip>
#include <sstream>
#include <string>
#include "RocketMQClient.h"
#include "UtilAll.h"

namespace metaq {
//<!************************************************************************/
//<!* MQ(T,B,ID);
//<!************************************************************************/
class ROCKETMQCLIENT_API MQMessageQueue {
 public:
  MQMessageQueue();
  MQMessageQueue(const string& topic, const string& brokerName, int queueId);
  MQMessageQueue(const MQMessageQueue& other);
  MQMessageQueue& operator=(const MQMessageQueue& other);

  string getTopic() const;
  void setTopic(const string& topic);

  string getBrokerName() const;
  void setBrokerName(const string& brokerName);

  int getQueueId() const;
  void setQueueId(int queueId);

  bool operator==(const MQMessageQueue& mq) const;
  bool operator<(const MQMessageQueue& mq) const;
  int compareTo(const MQMessageQueue& mq) const;

  const string toString() const {
    stringstream ss;
    ss << "MessageQueue [topic=" << m_topic << ", brokerName=" << m_brokerName
       << ", queueId=" << m_queueId << "]";

    return ss.str();
  }

 private:
  string m_topic;
  string m_brokerName;
  int m_queueId;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
