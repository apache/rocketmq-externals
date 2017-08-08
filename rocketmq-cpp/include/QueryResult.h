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
#ifndef __QUERYRESULT_H__
#define __QUERYRESULT_H__

#include "MQMessageExt.h"
#include "RocketMQClient.h"

namespace rocketmq {
//<!************************************************************************
class ROCKETMQCLIENT_API QueryResult {
 public:
  QueryResult(uint64 indexLastUpdateTimestamp,
              const std::vector<MQMessageExt*>& messageList) {
    m_indexLastUpdateTimestamp = indexLastUpdateTimestamp;
    m_messageList = messageList;
  }

  uint64 getIndexLastUpdateTimestamp() { return m_indexLastUpdateTimestamp; }

  std::vector<MQMessageExt*>& getMessageList() { return m_messageList; }

 private:
  uint64 m_indexLastUpdateTimestamp;
  std::vector<MQMessageExt*> m_messageList;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
