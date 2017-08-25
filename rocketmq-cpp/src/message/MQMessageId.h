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
#ifndef __MESSAGEID_H__
#define __MESSAGEID_H__

#include "SocketUtil.h"
#include "UtilAll.h"

namespace rocketmq {
//<!***************************************************************************
class MQMessageId {
 public:
  MQMessageId(sockaddr address, int64 offset)
      : m_address(address), m_offset(offset) {}

  sockaddr getAddress() const { return m_address; }

  void setAddress(sockaddr address) { m_address = address; }

  int64 getOffset() const { return m_offset; }

  void setOffset(int64 offset) { m_offset = offset; }

 private:
  sockaddr m_address;
  int64 m_offset;
};

}  //<!end namespace;

#endif
