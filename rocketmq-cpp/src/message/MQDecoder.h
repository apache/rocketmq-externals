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
#ifndef __MESSAGEDECODER_H__
#define __MESSAGEDECODER_H__

#include "MQClientException.h"
#include "MQMessageExt.h"
#include "MQMessageId.h"
#include "MemoryInputStream.h"
#include "SocketUtil.h"

namespace rocketmq {
//<!***************************************************************************
class MQDecoder {
 public:
  static string createMessageId(sockaddr addr, int64 offset);
  static MQMessageId decodeMessageId(const string& msgId);

  static void decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec);

  static void decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec,
                      bool readBody);

  static string messageProperties2String(const map<string, string>& properties);
  static void string2messageProperties(const string& propertiesString,
                                       map<string, string>& properties);

 private:
  static MQMessageExt* decode(MemoryInputStream& byteBuffer);
  static MQMessageExt* decode(MemoryInputStream& byteBuffer, bool readBody);

 public:
  static const char NAME_VALUE_SEPARATOR;
  static const char PROPERTY_SEPARATOR;
  static const int MSG_ID_LENGTH;
  static int MessageMagicCodePostion;
  static int MessageFlagPostion;
  static int MessagePhysicOffsetPostion;
  static int MessageStoreTimestampPostion;
};
}  //<!end namespace;

#endif
