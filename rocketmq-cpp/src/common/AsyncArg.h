/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef _AsyncArg_H_
#define _AsyncArg_H_

#include "MQMessageQueue.h"
#include "PullAPIWrapper.h"
#include "SubscriptionData.h"

namespace metaq {
//<!***************************************************************************

struct AsyncArg {
  MQMessageQueue mq;
  SubscriptionData subData;
  PullAPIWrapper* pPullWrapper;
};

//<!***************************************************************************
}
#endif  //<! _AsyncArg_H_
