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
#include "RemotingCommand.h"
#include "Logging.h"
#include "MQProtos.h"
#include "MQVersion.h"
#include "SessionCredentials.h"

namespace rocketmq {
boost::atomic<int> RemotingCommand::s_seqNumber;
boost::mutex RemotingCommand::m_clock;
//<!************************************************************************
RemotingCommand::RemotingCommand(int code,
                                 CommandHeader* pExtHeader /* = NULL */)
    : m_code(code),
      m_language("CPP"),
      m_version(MQVersion::s_CurrentVersion),
      m_flag(0),
      m_remark(""),
      m_pExtHeader(pExtHeader) {
  boost::lock_guard<boost::mutex> lock(m_clock);
  m_opaque = (s_seqNumber.load(boost::memory_order_acquire)) %
             (numeric_limits<int>::max());
  s_seqNumber.store(m_opaque, boost::memory_order_release);
  ++s_seqNumber;
}

RemotingCommand::RemotingCommand(int code, string language, int version,
                                 int opaque, int flag, string remark,
                                 CommandHeader* pExtHeader)
    : m_code(code),
      m_language(language),
      m_version(version),
      m_opaque(opaque),
      m_flag(flag),
      m_remark(remark),
      m_pExtHeader(pExtHeader) {}

RemotingCommand::~RemotingCommand() { m_pExtHeader = NULL; }

void RemotingCommand::Encode() {
  Json::Value root;
  root["code"] = m_code;
  root["language"] = "CPP";
  root["version"] = m_version;
  root["opaque"] = m_opaque;
  root["flag"] = m_flag;
  root["remark"] = m_remark;

  if (m_pExtHeader) {
    Json::Value extJson;
    m_pExtHeader->Encode(extJson);

    extJson[SessionCredentials::Signature] =
        m_extFields[SessionCredentials::Signature];
    extJson[SessionCredentials::AccessKey] =
        m_extFields[SessionCredentials::AccessKey];
    extJson[SessionCredentials::ONSChannelKey] =
        m_extFields[SessionCredentials::ONSChannelKey];

    root["extFields"] = extJson;
  } else {  // for heartbeat
    Json::Value extJson;
    extJson[SessionCredentials::Signature] =
        m_extFields[SessionCredentials::Signature];
    extJson[SessionCredentials::AccessKey] =
        m_extFields[SessionCredentials::AccessKey];
    extJson[SessionCredentials::ONSChannelKey] =
        m_extFields[SessionCredentials::ONSChannelKey];
    root["extFields"] = extJson;
  }

  Json::FastWriter fastwrite;
  string data = fastwrite.write(root);

  uint32 headLen = data.size();
  uint32 totalLen = 4 + headLen + m_body.getSize();

  uint32 messageHeader[2];
  messageHeader[0] = htonl(totalLen);
  messageHeader[1] = htonl(headLen);

  //<!include self 4 bytes, see : doc/protocol.txt;
  m_head.setSize(4 + 4 + headLen);
  m_head.copyFrom(messageHeader, 0, sizeof(messageHeader));
  m_head.copyFrom(data.c_str(), sizeof(messageHeader), headLen);
}

const MemoryBlock* RemotingCommand::GetHead() const { return &m_head; }

const MemoryBlock* RemotingCommand::GetBody() const { return &m_body; }

void RemotingCommand::SetBody(const char* pData, int len) {
  m_body.reset();
  m_body.setSize(len);
  m_body.copyFrom(pData, 0, len);
}

RemotingCommand* RemotingCommand::Decode(const MemoryBlock& mem) {
  //<!decode 1 bytes,4+head+body
  uint32 messageHeader[1];
  mem.copyTo(messageHeader, 0, sizeof(messageHeader));
  int totalLen = mem.getSize();
  int headLen = ntohl(messageHeader[0]);
  int bodyLen = totalLen - 4 - headLen;

  //<!decode header;
  const char* const pData = static_cast<const char*>(mem.getData());
  Json::Reader reader;
  Json::Value object;
  const char* begin = pData + 4;
  const char* end = pData + 4 + headLen;

  if (!reader.parse(begin, end, object)) {
    THROW_MQEXCEPTION(MQClientException, "conn't parse json", -1);
  }

  int code = object["code"].asInt();

  string language = object["language"].asString();
  int version = object["version"].asInt();
  int opaque = object["opaque"].asInt();
  int flag = object["flag"].asInt();
  Json::Value v = object["remark"];
  string remark = "";
  if (!v.isNull()) {
    remark = object["remark"].asString();
  }
  LOG_DEBUG(
      "code:%d, remark:%s, version:%d, opaque:%d, flag:%d, remark:%s, "
      "headLen:%d, bodyLen:%d ",
      code, language.c_str(), version, opaque, flag, remark.c_str(), headLen,
      bodyLen);
  RemotingCommand* cmd =
      new RemotingCommand(code, language, version, opaque, flag, remark, NULL);
  cmd->setParsedJson(object);
  if (bodyLen > 0) {
    cmd->SetBody(pData + 4 + headLen, bodyLen);
  }
  return cmd;
}

void RemotingCommand::markResponseType() {
  int bits = 1 << RPC_TYPE;
  m_flag |= bits;
}

bool RemotingCommand::isResponseType() {
  int bits = 1 << RPC_TYPE;
  return (m_flag & bits) == bits;
}

void RemotingCommand::markOnewayRPC() {
  int bits = 1 << RPC_ONEWAY;
  m_flag |= bits;
}

bool RemotingCommand::isOnewayRPC() {
  int bits = 1 << RPC_ONEWAY;
  return (m_flag & bits) == bits;
}

void RemotingCommand::setOpaque(const int opa) { m_opaque = opa; }

void RemotingCommand::SetExtHeader(int code) {
  try {
    Json::Value ext = m_parsedJson["extFields"];
    if (!ext.isNull()) {
      m_pExtHeader = NULL;
      switch (code) {
        case SEND_MESSAGE:
          m_pExtHeader.reset(SendMessageResponseHeader::Decode(ext));
          break;
        case PULL_MESSAGE:
          m_pExtHeader.reset(PullMessageResponseHeader::Decode(ext));
          break;
        case GET_MIN_OFFSET:
          m_pExtHeader.reset(GetMinOffsetResponseHeader::Decode(ext));
          break;
        case GET_MAX_OFFSET:
          m_pExtHeader.reset(GetMaxOffsetResponseHeader::Decode(ext));
          break;
        case SEARCH_OFFSET_BY_TIMESTAMP:
          m_pExtHeader.reset(SearchOffsetResponseHeader::Decode(ext));
          break;
        case GET_EARLIEST_MSG_STORETIME:
          m_pExtHeader.reset(
              GetEarliestMsgStoretimeResponseHeader::Decode(ext));
          break;
        case QUERY_CONSUMER_OFFSET:
          m_pExtHeader.reset(QueryConsumerOffsetResponseHeader::Decode(ext));
          break;
        case RESET_CONSUMER_CLIENT_OFFSET:
          m_pExtHeader.reset(ResetOffsetRequestHeader::Decode(ext));
          break;
        case GET_CONSUMER_RUNNING_INFO:
          m_pExtHeader.reset(GetConsumerRunningInfoRequestHeader::Decode(ext));
          break;
        case NOTIFY_CONSUMER_IDS_CHANGED:
          m_pExtHeader.reset(
              NotifyConsumerIdsChangedRequestHeader::Decode(ext));
        default:
          break;
      }
    }
  } catch (MQException& e) {
    LOG_ERROR("set response head error");
  }
}

void RemotingCommand::setCode(int code) { m_code = code; }

int RemotingCommand::getCode() const { return m_code; }

int RemotingCommand::getOpaque() const { return m_opaque; }

string RemotingCommand::getRemark() const { return m_remark; }

void RemotingCommand::setRemark(string mark) { m_remark = mark; }

CommandHeader* RemotingCommand::getCommandHeader() const {
  return m_pExtHeader.get();
}

void RemotingCommand::setParsedJson(Json::Value json) { m_parsedJson = json; }

const int RemotingCommand::getFlag() const { return m_flag; }

const int RemotingCommand::getVersion() const { return m_version; }

void RemotingCommand::setMsgBody(const string& body) { m_msgBody = body; }

string RemotingCommand::getMsgBody() const { return m_msgBody; }

void RemotingCommand::addExtField(const string& key, const string& value) {
  m_extFields[key] = value;
}

}  //<!end namespace;
