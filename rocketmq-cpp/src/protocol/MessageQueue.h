/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGEQUEUE_H__
#define __MESSAGEQUEUE_H__

#include <string>
#include "json/json.h"

namespace metaq {
//<!************************************************************************/
//<!* MQ(T,B,ID);
//<!************************************************************************/
class MessageQueue {
 public:
  MessageQueue();
  MessageQueue(const std::string& topic, const std::string& brokerName,
               int queueId);
  MessageQueue(const MessageQueue& other);
  MessageQueue& operator=(const MessageQueue& other);

  std::string getTopic() const;
  void setTopic(const std::string& topic);

  std::string getBrokerName() const;
  void setBrokerName(const std::string& brokerName);

  int getQueueId() const;
  void setQueueId(int queueId);

  bool operator==(const MessageQueue& mq) const;
  bool operator<(const MessageQueue& mq) const;
  int compareTo(const MessageQueue& mq) const;
  MetaqJson::Value toJson() const;

 private:
  std::string m_topic;
  std::string m_brokerName;
  int m_queueId;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
