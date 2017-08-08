#ifndef __CONSUMERRUNNINGINFO_H__
#define __CONSUMERRUNNINGINFO_H__

#include "MessageQueue.h"
#include "ProcessQueueInfo.h"
#include "SubscriptionData.h"

namespace rocketmq {

class ConsumerRunningInfo {
 public:
  ConsumerRunningInfo() {}
  virtual ~ConsumerRunningInfo() {
    properties.clear();
    mqTable.clear();
    subscriptionSet.clear();
  }

 public:
  static const string PROP_NAMESERVER_ADDR;
  static const string PROP_THREADPOOL_CORE_SIZE;
  static const string PROP_CONSUME_ORDERLY;
  static const string PROP_CONSUME_TYPE;
  static const string PROP_CLIENT_VERSION;
  static const string PROP_CONSUMER_START_TIMESTAMP;

 public:
  const map<string, string> getProperties() const;
  void setProperties(const map<string, string>& input_properties);
  void setProperty(const string& key, const string& value);
  const map<MessageQueue, ProcessQueueInfo> getMqTable() const;
  void setMqTable(MessageQueue queue, ProcessQueueInfo queueInfo);
  // const map<string, ConsumeStatus> getStatusTable() const;
  // void setStatusTable(const map<string, ConsumeStatus>& input_statusTable) ;
  const vector<SubscriptionData> getSubscriptionSet() const;
  void setSubscriptionSet(
      const vector<SubscriptionData>& input_subscriptionSet);
  const string getJstack() const;
  void setJstack(const string& input_jstack);
  string encode();

 private:
  map<string, string> properties;
  vector<SubscriptionData> subscriptionSet;
  map<MessageQueue, ProcessQueueInfo> mqTable;
  // map<string, ConsumeStatus> statusTable;
  string jstack;
};
}
#endif
