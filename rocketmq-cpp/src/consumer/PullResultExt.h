/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "PullResult.h"
#include "UtilAll.h"
#include "dataBlock.h"

namespace metaq {
/**
 * 只在内部使用，不对外公开
 */
//<!***************************************************************************
class PullResultExt : public PullResult {
 public:
  PullResultExt(PullStatus pullStatus, int64 nextBeginOffset, int64 minOffset,
                int64 maxOffset, int suggestWhichBrokerId,
                const MemoryBlock& messageBinary)
      : PullResult(pullStatus, nextBeginOffset, minOffset, maxOffset),
        suggestWhichBrokerId(suggestWhichBrokerId),
        msgMemBlock(messageBinary) {}
  PullResultExt(PullStatus pullStatus, int64 nextBeginOffset, int64 minOffset,
                int64 maxOffset, int suggestWhichBrokerId)
      : PullResult(pullStatus, nextBeginOffset, minOffset, maxOffset),
        suggestWhichBrokerId(suggestWhichBrokerId) {}
  virtual ~PullResultExt() {}

 public:
  int suggestWhichBrokerId;
  MemoryBlock msgMemBlock;
};

}  //<!end namespace;
