/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
