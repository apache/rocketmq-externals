/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGEQUEUELISTENER_H__
#define __MESSAGEQUEUELISTENER_H__

#include <vector>
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
class ROCKETMQCLIENT_API MQueueListener {
 public:
  virtual ~MQueueListener() {}
  virtual void messageQueueChanged(const string& topic,
                                   vector<MQMessageQueue>& mqAll,
                                   vector<MQMessageQueue>& mqDivided) = 0;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
