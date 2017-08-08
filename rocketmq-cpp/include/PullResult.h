/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __PULLRESULT_H__
#define __PULLRESULT_H__

#include <sstream>
#include "MQMessageExt.h"
#include "RocketMQClient.h"

namespace metaq {
//<!***************************************************************************
enum PullStatus {
  FOUND,
  NO_NEW_MSG,
  NO_MATCHED_MSG,
  OFFSET_ILLEGAL,
  BROKER_TIMEOUT  // indicate pull request timeout or received NULL response
};

static const char* EnumStrings[] = {"FOUND", "NO_NEW_MSG", "NO_MATCHED_MSG",
                                    "OFFSET_ILLEGAL", "BROKER_TIMEOUT"};

//<!***************************************************************************
class ROCKETMQCLIENT_API PullResult {
 public:
  PullResult();
  PullResult(PullStatus status);
  PullResult(PullStatus pullStatus, int64 nextBeginOffset,
             int64 minOffset, int64 maxOffset);

  PullResult(PullStatus pullStatus, int64 nextBeginOffset,
             int64 minOffset, int64 maxOffset,
             const vector<MQMessageExt>& src);

  virtual ~PullResult();

  string toString() {
    stringstream ss;
    ss << "PullResult [ pullStatus=" << EnumStrings[pullStatus]
       << ", nextBeginOffset=" << nextBeginOffset << ", minOffset=" << minOffset
       << ", maxOffset=" << maxOffset
       << ", msgFoundList=" << msgFoundList.size() << " ]";
    return ss.str();
  }

 public:
  PullStatus pullStatus;
  int64 nextBeginOffset;
  int64 minOffset;
  int64 maxOffset;
  vector<MQMessageExt> msgFoundList;
};
//<!***************************************************************************
}  //<!end namespace;
#endif
