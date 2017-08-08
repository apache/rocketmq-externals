/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "LockBatchBody.h"
#include "Logging.h"
namespace metaq {  //<!end namespace;

string LockBatchRequestBody::getConsumerGroup() { return consumerGroup; }
void LockBatchRequestBody::setConsumerGroup(string in_consumerGroup) {
  consumerGroup = in_consumerGroup;
}
string LockBatchRequestBody::getClientId() { return clientId; }
void LockBatchRequestBody::setClientId(string in_clientId) {
  clientId = in_clientId;
}
vector<MQMessageQueue> LockBatchRequestBody::getMqSet() { return mqSet; }
void LockBatchRequestBody::setMqSet(vector<MQMessageQueue> in_mqSet) {
  mqSet.swap(in_mqSet);
}
void LockBatchRequestBody::Encode(string& outData) {
  MetaqJson::Value root;
  root["consumerGroup"] = consumerGroup;
  root["clientId"] = clientId;

  vector<MQMessageQueue>::const_iterator it = mqSet.begin();
  for (; it != mqSet.end(); it++) {
    root["mqSet"].append(toJson(*it));
  }

  MetaqJson::FastWriter fastwrite;
  outData = fastwrite.write(root);
}

MetaqJson::Value LockBatchRequestBody::toJson(const MQMessageQueue& mq) const {
  MetaqJson::Value outJson;
  outJson["topic"] = mq.getTopic();
  outJson["brokerName"] = mq.getBrokerName();
  outJson["queueId"] = mq.getQueueId();
  return outJson;
}

vector<MQMessageQueue> LockBatchResponseBody::getLockOKMQSet() {
  return lockOKMQSet;
}
void LockBatchResponseBody::setLockOKMQSet(
    vector<MQMessageQueue> in_lockOKMQSet) {
  lockOKMQSet.swap(in_lockOKMQSet);
}

void LockBatchResponseBody::Decode(const MemoryBlock* mem,
                                   vector<MQMessageQueue>& messageQueues) {
  messageQueues.clear();
  //<! decode;
  const char* const pData = static_cast<const char*>(mem->getData());

  MetaqJson::Reader reader;
  MetaqJson::Value root;
  if (!reader.parse(pData, root)) {
    LOG_WARN("decode LockBatchResponseBody error");
    return;
  }

  MetaqJson::Value mqs = root["lockOKMQSet"];
  LOG_DEBUG("LockBatchResponseBody mqs size:%d", mqs.size());
  for (size_t i = 0; i < mqs.size(); i++) {
    MQMessageQueue mq;
    MetaqJson::Value qd = mqs[i];
    mq.setTopic(qd["topic"].asString());
    mq.setBrokerName(qd["brokerName"].asString());
    mq.setQueueId(qd["queueId"].asInt());
    LOG_INFO("LockBatchResponseBody MQ:%s", mq.toString().c_str());
    messageQueues.push_back(mq);
  }
}

string UnlockBatchRequestBody::getConsumerGroup() { return consumerGroup; }
void UnlockBatchRequestBody::setConsumerGroup(string in_consumerGroup) {
  consumerGroup = in_consumerGroup;
}
string UnlockBatchRequestBody::getClientId() { return clientId; }
void UnlockBatchRequestBody::setClientId(string in_clientId) {
  clientId = in_clientId;
}
vector<MQMessageQueue> UnlockBatchRequestBody::getMqSet() { return mqSet; }
void UnlockBatchRequestBody::setMqSet(vector<MQMessageQueue> in_mqSet) {
  mqSet.swap(in_mqSet);
}
void UnlockBatchRequestBody::Encode(string& outData) {
  MetaqJson::Value root;
  root["consumerGroup"] = consumerGroup;
  root["clientId"] = clientId;

  vector<MQMessageQueue>::const_iterator it = mqSet.begin();
  for (; it != mqSet.end(); it++) {
    root["mqSet"].append(toJson(*it));
  }

  MetaqJson::FastWriter fastwrite;
  outData = fastwrite.write(root);
}

MetaqJson::Value UnlockBatchRequestBody::toJson(
    const MQMessageQueue& mq) const {
  MetaqJson::Value outJson;
  outJson["topic"] = mq.getTopic();
  outJson["brokerName"] = mq.getBrokerName();
  outJson["queueId"] = mq.getQueueId();
  return outJson;
}
}
