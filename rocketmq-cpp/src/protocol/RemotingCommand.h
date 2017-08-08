/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __REMOTINGCOMMAND_H__
#define __REMOTINGCOMMAND_H__
#include <boost/atomic.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include <memory>
#include <sstream>
#include "CommandHeader.h"
#include "dataBlock.h"

namespace metaq {
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
  void setParsedJson(MetaqJson::Value json);

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
  MetaqJson::Value m_parsedJson;
  static boost::atomic<int> s_seqNumber;
  unique_ptr<CommandHeader> m_pExtHeader;
};

}  //<!end namespace;

#endif
