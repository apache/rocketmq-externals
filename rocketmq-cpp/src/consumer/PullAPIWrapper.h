/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef _PULLAPIWRAPPER_H_
#define _PULLAPIWRAPPER_H_

#include <boost/thread/mutex.hpp>
#include <boost/thread/thread.hpp>
#include "AsyncCallback.h"
#include "MQMessageQueue.h"
#include "SessionCredentials.h"
#include "SubscriptionData.h"

namespace metaq {
class MQClientFactory;
//<!***************************************************************************
class PullAPIWrapper {
 public:
  PullAPIWrapper(MQClientFactory* mQClientFactory, const string& consumerGroup);
  ~PullAPIWrapper();

  PullResult processPullResult(const MQMessageQueue& mq, PullResult* pullResult,
                               SubscriptionData* subscriptionData);

  PullResult* pullKernelImpl(const MQMessageQueue& mq,        // 1
                             string subExpression,            // 2
                             int64 subVersion,                // 3
                             int64 offset,                    // 4
                             int maxNums,                     // 5
                             int sysFlag,                     // 6
                             int64 commitOffset,              // 7
                             int brokerSuspendMaxTimeMillis,  // 8
                             int timeoutMillis,               // 9
                             int communicationMode,           // 10
                             PullCallback* pullCallback,
                             const SessionCredentials& session_credentials,
                             void* pArg = NULL);

 private:
  void updatePullFromWhichNode(const MQMessageQueue& mq, int brokerId);

  int recalculatePullFromWhichNode(const MQMessageQueue& mq);

 private:
  MQClientFactory* m_MQClientFactory;
  string m_consumerGroup;
  boost::mutex m_lock;
  map<MQMessageQueue, int /* brokerId */> m_pullFromWhichNodeTable;
};

//<!***************************************************************************
}  //<!end namespace;

#endif  //<! _PULLAPIWRAPPER_H_
