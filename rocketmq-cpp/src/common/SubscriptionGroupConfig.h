/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __SUBSCRIPTIONGROUPCONFIG_H__
#define __SUBSCRIPTIONGROUPCONFIG_H__

#include <string>

namespace metaq {
//<!***************************************************************************
class SubscriptionGroupConfig {
 public:
  SubscriptionGroupConfig(const string& groupName) {
    this->groupName = groupName;
    consumeEnable = true;
    consumeFromMinEnable = true;
    consumeBroadcastEnable = true;
    retryQueueNums = 1;
    retryMaxTimes = 5;
    brokerId = MASTER_ID;
    whichBrokerWhenConsumeSlowly = 1;
  }

  string groupName;
  bool consumeEnable;
  bool consumeFromMinEnable;
  bool consumeBroadcastEnable;
  int retryQueueNums;
  int retryMaxTimes;
  int brokerId;
  int whichBrokerWhenConsumeSlowly;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
