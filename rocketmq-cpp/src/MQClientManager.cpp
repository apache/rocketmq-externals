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
#include "MQClientManager.h"
#include "Logging.h"

namespace rocketmq {
//<!************************************************************************
MQClientManager::MQClientManager() {}

MQClientManager::~MQClientManager() { m_factoryTable.clear(); }

MQClientManager* MQClientManager::getInstance() {
  static MQClientManager instance;
  return &instance;
}

MQClientFactory* MQClientManager::getMQClientFactory(
    const string& clientId, int pullThreadNum, uint64_t tcpConnectTimeout,
    uint64_t tcpTransportTryLockTimeout, string unitName) {
  FTMAP::iterator it = m_factoryTable.find(clientId);
  if (it != m_factoryTable.end()) {
    return it->second;
  } else {
    MQClientFactory* factory =
        new MQClientFactory(clientId, pullThreadNum, tcpConnectTimeout,
                            tcpTransportTryLockTimeout, unitName);
    m_factoryTable[clientId] = factory;
    return factory;
  }
}

void MQClientManager::removeClientFactory(const string& clientId) {
  FTMAP::iterator it = m_factoryTable.find(clientId);
  if (it != m_factoryTable.end()) {
    deleteAndZero(it->second);
    m_factoryTable.erase(it);
  }
}
//<!************************************************************************
}  //<!end namespace;
