/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MQCONSUMER_H__
#define __MQCONSUMER_H__

#include <string>
#include "AsyncCallback.h"
#include "ConsumeType.h"
#include "MQClient.h"
#include "RocketMQClient.h"

namespace metaq {
class SubscriptionData;
class PullRequest;
class Rebalance;
class ConsumerRunningInfo;
//<!************************************************************************
class ROCKETMQCLIENT_API MQConsumer : public MQClient {
 public:
  virtual ~MQConsumer() {}
  virtual void sendMessageBack(MQMessageExt& msg, int delayLevel) = 0;
  virtual void fetchSubscribeMessageQueues(const string& topic,
                                           vector<MQMessageQueue>& mqs) = 0;
  virtual void doRebalance() = 0;
  virtual void persistConsumerOffset() = 0;
  virtual void persistConsumerOffsetByResetOffset() = 0;
  virtual void updateTopicSubscribeInfo(const string& topic,
                                        vector<MQMessageQueue>& info) = 0;
  virtual void updateConsumeOffset(const MQMessageQueue& mq,
                                   int64 offset) = 0;
  virtual void removeConsumeOffset(const MQMessageQueue& mq) = 0;
  virtual ConsumeType getConsumeType() = 0;
  virtual ConsumeFromWhere getConsumeFromWhere() = 0;
  virtual void getSubscriptions(vector<SubscriptionData>&) = 0;
  virtual void producePullMsgTask(PullRequest*) = 0;
  virtual Rebalance* getRebalance() const = 0;
  virtual PullResult pull(const MQMessageQueue& mq, const string& subExpression,
                          int64 offset, int maxNums) = 0;
  virtual void pull(const MQMessageQueue& mq, const string& subExpression,
                    int64 offset, int maxNums,
                    PullCallback* pPullCallback) = 0;
  virtual ConsumerRunningInfo* getConsumerRunningInfo() = 0;

 public:
  MessageModel getMessageModel() const { return m_messageModel; }
  void setMessageModel(MessageModel messageModel) {
    m_messageModel = messageModel;
  }

 protected:
  MessageModel m_messageModel;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
