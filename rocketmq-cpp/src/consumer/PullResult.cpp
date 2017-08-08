/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "PullResult.h"
#include "UtilAll.h"

namespace metaq {
//<!************************************************************************
PullResult::PullResult()
    : pullStatus(NO_MATCHED_MSG),
      nextBeginOffset(0),
      minOffset(0),
      maxOffset(0) {}

PullResult::PullResult(PullStatus status)
    : pullStatus(status), nextBeginOffset(0), minOffset(0), maxOffset(0) {}

PullResult::PullResult(PullStatus pullStatus, int64 nextBeginOffset,
                       int64 minOffset, int64 maxOffset)
    : pullStatus(pullStatus),
      nextBeginOffset(nextBeginOffset),
      minOffset(minOffset),
      maxOffset(maxOffset) {}

PullResult::PullResult(PullStatus pullStatus, int64 nextBeginOffset,
                       int64 minOffset, int64 maxOffset,
                       const vector<MQMessageExt>& src)
    : pullStatus(pullStatus),
      nextBeginOffset(nextBeginOffset),
      minOffset(minOffset),
      maxOffset(maxOffset) {
  msgFoundList.reserve(src.size());
  for (size_t i = 0; i < src.size(); i++) {
    msgFoundList.push_back(src[i]);
  }
}

PullResult::~PullResult() { msgFoundList.clear(); }

//<!***************************************************************************
}  //<!end namespace;
