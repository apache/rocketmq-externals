/********************************************************************
author: qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __CLIENTRPCHOOK_H__
#define __CLIENTRPCHOOK_H__

#include "RemotingCommand.h"
#include "RocketMQClient.h"
#include "SessionCredentials.h"
namespace metaq {
class RPCHook {
 public:
  RPCHook() {}
  virtual ~RPCHook() {}
  virtual void doBeforeRequest(const string& remoteAddr,
                               RemotingCommand& request) = 0;
  virtual void doAfterResponse(RemotingCommand& request,
                               RemotingCommand& response) = 0;
};

class ClientRPCHook : public RPCHook {
 private:
  SessionCredentials sessionCredentials;

 public:
  ClientRPCHook(const SessionCredentials& session_credentials)
      : sessionCredentials(session_credentials) {}
  virtual ~ClientRPCHook() {}

  virtual void doBeforeRequest(const string& remoteAddr,
                               RemotingCommand& request);

  virtual void doAfterResponse(RemotingCommand& request,
                               RemotingCommand& response) {}
};
}
#endif
