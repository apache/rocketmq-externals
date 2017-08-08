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
#include "MQDecoder.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sstream>
#include "Logging.h"
#include "MemoryOutputStream.h"
#include "MessageSysFlag.h"
#include "UtilAll.h"
namespace rocketmq {
//<!***************************************************************************
const int MQDecoder::MSG_ID_LENGTH = 8 + 8;

const char MQDecoder::NAME_VALUE_SEPARATOR = 1;
const char MQDecoder::PROPERTY_SEPARATOR = 2;

int MQDecoder::MessageMagicCodePostion = 4;
int MQDecoder::MessageFlagPostion = 16;
int MQDecoder::MessagePhysicOffsetPostion = 28;
int MQDecoder::MessageStoreTimestampPostion = 56;
//<!***************************************************************************
string MQDecoder::createMessageId(sockaddr addr, int64 offset) {
  int host, port;
  socketAddress2IPPort(addr, host, port);

  MemoryOutputStream outputmen(MSG_ID_LENGTH);
  outputmen.writeIntBigEndian(host);
  outputmen.writeIntBigEndian(port);
  outputmen.writeInt64BigEndian(offset);

  const char* bytes = static_cast<const char*>(outputmen.getData());
  int len = outputmen.getDataSize();

  return UtilAll::bytes2string(bytes, len);
}

MQMessageId MQDecoder::decodeMessageId(const string& msgId) {

  string ipstr = msgId.substr(0, 8);
  string portstr = msgId.substr(8, 8);
  string offsetstr = msgId.substr(16);

  char* end;
  int ipint = strtoul(ipstr.c_str(), &end, 16);
  int portint = strtoul(portstr.c_str(), &end, 16);

  int64 offset = UtilAll::hexstr2ull(offsetstr.c_str());

  offset = n2hll(offset);

  portint = ntohl(portint);
  short port = portint;

  struct sockaddr_in sa;
  sa.sin_family = AF_INET;
  sa.sin_port = htons(port);
  sa.sin_addr.s_addr = ipint;

  sockaddr addr;
  memcpy(&addr, &sa, sizeof(sockaddr));

  MQMessageId id(addr, offset);

  return id;
}

MQMessageExt* MQDecoder::decode(MemoryInputStream& byteBuffer) {
  return decode(byteBuffer, true);
}

MQMessageExt* MQDecoder::decode(MemoryInputStream& byteBuffer, bool readBody) {
  MQMessageExt* msgExt = new MQMessageExt();

  // 1 TOTALSIZE
  int storeSize = byteBuffer.readIntBigEndian();
  msgExt->setStoreSize(storeSize);

  // 2 MAGICCODE sizeof(int)
  byteBuffer.skipNextBytes(sizeof(int));

  // 3 BODYCRC
  int bodyCRC = byteBuffer.readIntBigEndian();
  msgExt->setBodyCRC(bodyCRC);

  // 4 QUEUEID
  int queueId = byteBuffer.readIntBigEndian();
  msgExt->setQueueId(queueId);

  // 5 FLAG
  int flag = byteBuffer.readIntBigEndian();
  msgExt->setFlag(flag);

  // 6 QUEUEOFFSET
  int64 queueOffset = byteBuffer.readInt64BigEndian();
  msgExt->setQueueOffset(queueOffset);

  // 7 PHYSICALOFFSET
  int64 physicOffset = byteBuffer.readInt64BigEndian();
  msgExt->setCommitLogOffset(physicOffset);

  // 8 SYSFLAG
  int sysFlag = byteBuffer.readIntBigEndian();
  msgExt->setSysFlag(sysFlag);

  // 9 BORNTIMESTAMP
  int64 bornTimeStamp = byteBuffer.readInt64BigEndian();
  msgExt->setBornTimestamp(bornTimeStamp);

  // 10 BORNHOST
  int bornHost = byteBuffer.readIntBigEndian();
  int port = byteBuffer.readIntBigEndian();
  sockaddr bornAddr = IPPort2socketAddress(bornHost, port);
  msgExt->setBornHost(bornAddr);

  // 11 STORETIMESTAMP
  int64 storeTimestamp = byteBuffer.readInt64BigEndian();
  msgExt->setStoreTimestamp(storeTimestamp);

  // // 12 STOREHOST
  int storeHost = byteBuffer.readIntBigEndian();
  port = byteBuffer.readIntBigEndian();
  sockaddr storeAddr = IPPort2socketAddress(storeHost, port);
  msgExt->setStoreHost(storeAddr);

  // 13 RECONSUMETIMES
  int reconsumeTimes = byteBuffer.readIntBigEndian();
  msgExt->setReconsumeTimes(reconsumeTimes);

  // 14 Prepared Transaction Offset
  int64 preparedTransactionOffset = byteBuffer.readInt64BigEndian();
  msgExt->setPreparedTransactionOffset(preparedTransactionOffset);

  // 15 BODY
  int bodyLen = byteBuffer.readIntBigEndian();
  if (bodyLen > 0) {
    if (readBody) {
      MemoryBlock block;
      byteBuffer.readIntoMemoryBlock(block, bodyLen);

      const char* const pBody = static_cast<const char*>(block.getData());
      int len = block.getSize();
      string msgbody(pBody, len);

      // decompress body
      if ((sysFlag & MessageSysFlag::CompressedFlag) ==
          MessageSysFlag::CompressedFlag) {
        string outbody;
        if (UtilAll::inflate(msgbody, outbody)) {
          msgExt->setBody(outbody);
        }
      } else {
        msgExt->setBody(msgbody);
      }
    } else {
      byteBuffer.skipNextBytes(bodyLen);
    }
  }

  // 16 TOPIC
  int topicLen = (int)byteBuffer.readByte();
  MemoryBlock block;
  byteBuffer.readIntoMemoryBlock(block, topicLen);
  const char* const pTopic = static_cast<const char*>(block.getData());
  topicLen = block.getSize();
  msgExt->setTopic(pTopic, topicLen);

  // 17 properties
  short propertiesLen = byteBuffer.readShortBigEndian();
  if (propertiesLen > 0) {
    MemoryBlock block;
    byteBuffer.readIntoMemoryBlock(block, propertiesLen);
    const char* const pProperty = static_cast<const char*>(block.getData());
    int len = block.getSize();
    string propertiesString(pProperty, len);

    map<string, string> propertiesMap;
    string2messageProperties(propertiesString, propertiesMap);
    msgExt->setProperties(propertiesMap);
    propertiesMap.clear();
  }

  // 18 msg ID
  string msgId = createMessageId(msgExt->getStoreHost(),
                                 (int64)msgExt->getCommitLogOffset());
  msgExt->setMsgId(msgId);

  // LOG_INFO("get msgExt from remote server, its contents
  // are:%s",msgExt->toString().c_str());
  return msgExt;
}

void MQDecoder::decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec) {
  mqvec.clear();
  decodes(mem, mqvec, true);
}

void MQDecoder::decodes(const MemoryBlock* mem, vector<MQMessageExt>& mqvec,
                        bool readBody) {
  MemoryInputStream rawInput(*mem, true);

  while (rawInput.getNumBytesRemaining() > 0) {
    unique_ptr<MQMessageExt> msg(decode(rawInput, readBody));
    mqvec.push_back(*msg);
  }
}

string MQDecoder::messageProperties2String(
    const map<string, string>& properties) {
  string os;
  map<string, string>::const_iterator it = properties.begin();

  for (; it != properties.end(); ++it) {
    // os << it->first << NAME_VALUE_SEPARATOR << it->second <<
    // PROPERTY_SEPARATOR;
    os.append(it->first);
    os += NAME_VALUE_SEPARATOR;
    os.append(it->second);
    os += PROPERTY_SEPARATOR;
  }

  return os;
}

void MQDecoder::string2messageProperties(const string& propertiesString,
                                         map<string, string>& properties) {
  vector<string> out;
  UtilAll::Split(out, propertiesString, PROPERTY_SEPARATOR);

  for (size_t i = 0; i < out.size(); i++) {
    vector<string> outValue;
    UtilAll::Split(outValue, out[i], NAME_VALUE_SEPARATOR);

    if (outValue.size() == 2) {
      properties[outValue[0]] = outValue[1];
    }
  }
}
}  //<!end namespace;
