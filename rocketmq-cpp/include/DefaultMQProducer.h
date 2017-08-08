/********************************************************************
author: qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __DEFAULTMQPRODUCER_H__
#define __DEFAULTMQPRODUCER_H__

#include "MQMessageQueue.h"
#include "MQProducer.h"
#include "RocketMQClient.h"
#include "SendResult.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API DefaultMQProducer : public MQProducer {
 public:
  DefaultMQProducer(const string& groupname);
  virtual ~DefaultMQProducer();

  //<!begin mqadmin;
  virtual void start();
  virtual void shutdown();
  //<!end mqadmin;

  //<! begin MQProducer;
  virtual SendResult send(MQMessage& msg, bool bSelectActiveBroker = false);
  virtual SendResult send(MQMessage& msg, const MQMessageQueue& mq);
  virtual SendResult send(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg);
  virtual SendResult send(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg, int autoRetryTimes,
                          bool bActiveBroker = false);
  virtual void send(MQMessage& msg, SendCallback* pSendCallback,
                    bool bSelectActiveBroker = false);
  virtual void send(MQMessage& msg, const MQMessageQueue& mq,
                    SendCallback* pSendCallback);
  virtual void send(MQMessage& msg, MessageQueueSelector* selector, void* arg,
                    SendCallback* pSendCallback);
  virtual void sendOneway(MQMessage& msg, bool bSelectActiveBroker = false);
  virtual void sendOneway(MQMessage& msg, const MQMessageQueue& mq);
  virtual void sendOneway(MQMessage& msg, MessageQueueSelector* selector,
                          void* arg);
  //<! end MQProducer;

  //set and get timeout of per msg
  int getSendMsgTimeout() const;
  void setSendMsgTimeout(int sendMsgTimeout);

  /*
  *  if msgBody size is large than m_compressMsgBodyOverHowmuch
      metaq cpp will compress msgBody according to compressLevel
  */
  int getCompressMsgBodyOverHowmuch() const;
  void setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch);
  int getCompressLevel() const;
  void setCompressLevel(int compressLevel);
  
  //if msgbody size larger than maxMsgBodySize, exception will be throwed
  int getMaxMessageSize() const;
  void setMaxMessageSize(int maxMessageSize);

  //set msg max retry times, default retry times is 5
  int getRetryTimes() const;
  void setRetryTimes(int times);

 protected:
  SendResult sendAutoRetrySelectImpl(MQMessage& msg,
                                     MessageQueueSelector* pSelector,
                                     void* pArg, int communicationMode,
                                     SendCallback* pSendCallback,
                                     int retryTimes,
                                     bool bActiveBroker = false);
  SendResult sendSelectImpl(MQMessage& msg, MessageQueueSelector* pSelector,
                            void* pArg, int communicationMode,
                            SendCallback* sendCallback);
  SendResult sendDefaultImpl(MQMessage& msg, int communicationMode,
                             SendCallback* pSendCallback,
                             bool bActiveBroker = false);
  SendResult sendKernelImpl(MQMessage& msg, const MQMessageQueue& mq,
                            int communicationMode, SendCallback* pSendCallback);
  bool tryToCompressMessage(MQMessage& msg);

 private:
  int m_sendMsgTimeout;
  int m_compressMsgBodyOverHowmuch;
  int m_maxMessageSize;  //<! default:128K;
  bool m_retryAnotherBrokerWhenNotStoreOK;
  int m_compressLevel;
  int m_retryTimes;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
