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
#ifndef __REMOTINGCOMMAND_H__
#define __REMOTINGCOMMAND_H__
#include <boost/atomic.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <memory>
#include <sstream>
#include "CommandHeader.h"
#include "dataBlock.h"

namespace rocketmq {
//<!***************************************************************************
const int RPC_TYPE = 0;    // 0, REQUEST_COMMAND // 1, RESPONSE_COMMAND;
const int RPC_ONEWAY = 1;  // 0, RPC // 1, Oneway;
//<!***************************************************************************
class RemotingCommand {
 public:
  RemotingCommand(int code, CommandHeader* pCustomHeader = NULL);
  RemotingCommand(int code, string language, int version, int opaque, int flag,
                  string remark, CommandHeader* pCustomHeader);
  virtual ~RemotingCommand();

  const MemoryBlock* GetHead() const;
  const MemoryBlock* GetBody() const;

  void SetBody(const char* pData, int len);
  void setOpaque(const int opa);
  void SetExtHeader(int code);

  void setCode(int code);
  int getCode() const;
  int getOpaque() const;
  void setRemark(string mark);
  string getRemark() const;
  void markResponseType();
  bool isResponseType();
  void markOnewayRPC();
  bool isOnewayRPC();
  void setParsedJson(Json::Value json);

  CommandHeader* getCommandHeader() const;
  const int getFlag() const;
  const int getVersion() const;

  void addExtField(const string& key, const string& value);
  string getMsgBody() const;
  void setMsgBody(const string& body);

 public:
  void Encode();
  static RemotingCommand* Decode(const MemoryBlock& mem);

 private:
  int m_code;
  string m_language;
  int m_version;
  int m_opaque;
  int m_flag;
  string m_remark;
  string m_msgBody;
  map<string, string> m_extFields;

  static boost::mutex m_clock;
  MemoryBlock m_head;
  MemoryBlock m_body;
  //<!save here
  Json::Value m_parsedJson;
  static boost::atomic<int> s_seqNumber;
  unique_ptr<CommandHeader> m_pExtHeader;
};

}  //<!end namespace;

#endif
