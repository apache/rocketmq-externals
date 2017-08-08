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
#ifndef __MQCLIENTMANAGER_H__
#define __MQCLIENTMANAGER_H__

#include <map>
#include <string>
#include "Logging.h"
#include "MQClientFactory.h"

namespace rocketmq {
//<!***************************************************************************
class MQClientManager {
 public:
  virtual ~MQClientManager();
  MQClientFactory* getMQClientFactory(const string& clientId, int pullThreadNum,
                                      uint64_t tcpConnectTimeout,
                                      uint64_t tcpTransportTryLockTimeout,
                                      string unitName);
  void removeClientFactory(const string& clientId);

  static MQClientManager* getInstance();

 private:
  MQClientManager();

 private:
  typedef map<string, MQClientFactory*> FTMAP;
  FTMAP m_factoryTable;
};

//<!***************************************************************************
}  //<!end namespace;

#endif
