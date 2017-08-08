/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "MQClientManager.h"
#include "Logging.h"

namespace metaq {
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
