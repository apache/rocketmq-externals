/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __LOCKBATCHBODY_H__
#define __LOCKBATCHBODY_H__
#include <set>
#include <string>
#include "MQMessageQueue.h"
#include "RemotingSerializable.h"
#include "dataBlock.h"
#include "json/json.h"

namespace metaq {
//<!***************************************************************************

class LockBatchRequestBody {
 public:
  virtual ~LockBatchRequestBody() { mqSet.clear(); }
  string getConsumerGroup();
  void setConsumerGroup(string consumerGroup);
  string getClientId();
  void setClientId(string clientId);
  vector<MQMessageQueue> getMqSet();
  void setMqSet(vector<MQMessageQueue> mqSet);
  void Encode(string& outData);
  MetaqJson::Value toJson(const MQMessageQueue& mq) const;

 private:
  string consumerGroup;
  string clientId;
  vector<MQMessageQueue> mqSet;
};

class LockBatchResponseBody {
 public:
  virtual ~LockBatchResponseBody() { lockOKMQSet.clear(); }
  vector<MQMessageQueue> getLockOKMQSet();
  void setLockOKMQSet(vector<MQMessageQueue> lockOKMQSet);
  static void Decode(const MemoryBlock* mem,
                     vector<MQMessageQueue>& messageQueues);

 private:
  vector<MQMessageQueue> lockOKMQSet;
};

class UnlockBatchRequestBody {
 public:
  virtual ~UnlockBatchRequestBody() { mqSet.clear(); }
  string getConsumerGroup();
  void setConsumerGroup(string consumerGroup);
  string getClientId();
  void setClientId(string clientId);
  vector<MQMessageQueue> getMqSet();
  void setMqSet(vector<MQMessageQueue> mqSet);
  void Encode(string& outData);
  MetaqJson::Value toJson(const MQMessageQueue& mq) const;

 private:
  string consumerGroup;
  string clientId;
  vector<MQMessageQueue> mqSet;
};

}  //<!end namespace;
#endif
