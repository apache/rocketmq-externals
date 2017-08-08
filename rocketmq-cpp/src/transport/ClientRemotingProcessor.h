#ifndef __CLIENTREMOTINGPROCESSOR_H__
#define __CLIENTREMOTINGPROCESSOR_H__

#include "MQMessageQueue.h"
#include "MQProtos.h"
#include "RemotingCommand.h"

namespace rocketmq {

class MQClientFactory;
class ClientRemotingProcessor {
 public:
  ClientRemotingProcessor(MQClientFactory* mqClientFactory);
  virtual ~ClientRemotingProcessor();

  RemotingCommand* processRequest(const string& addr, RemotingCommand* request);
  RemotingCommand* resetOffset(RemotingCommand* request);
  RemotingCommand* getConsumerRunningInfo(const string& addr,
                                          RemotingCommand* request);
  RemotingCommand* notifyConsumerIdsChanged(RemotingCommand* request);

 private:
  MQClientFactory* m_mqClientFactory;
};

class ResetOffsetBody {
 public:
  ResetOffsetBody() {}
  virtual ~ResetOffsetBody() { m_offsetTable.clear(); }
  void setOffsetTable(MQMessageQueue mq, int64 offset);
  std::map<MQMessageQueue, int64> getOffsetTable();
  static ResetOffsetBody* Decode(const MemoryBlock* mem);

 private:
  std::map<MQMessageQueue, int64> m_offsetTable;
};
}

#endif
