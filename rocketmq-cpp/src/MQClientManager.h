/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MQCLIENTMANAGER_H__
#define __MQCLIENTMANAGER_H__

#include <map>
#include <string>
#include "Logging.h"
#include "MQClientFactory.h"

namespace metaq {
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
