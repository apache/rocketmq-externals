/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "PullSysFlag.h"

namespace metaq {
//<!************************************************************************
int PullSysFlag::FLAG_COMMIT_OFFSET = 0x1 << 0;
int PullSysFlag::FLAG_SUSPEND = 0x1 << 1;
int PullSysFlag::FLAG_SUBSCRIPTION = 0x1 << 2;
int PullSysFlag::FLAG_CLASS_FILTER = 0x1 << 3;

int PullSysFlag::buildSysFlag(bool commitOffset, bool suspend,
                              bool subscription, bool classFilter) {
  int flag = 0;

  if (commitOffset) {
    flag |= FLAG_COMMIT_OFFSET;
  }

  if (suspend) {
    flag |= FLAG_SUSPEND;
  }

  if (subscription) {
    flag |= FLAG_SUBSCRIPTION;
  }

  if (classFilter) {
    flag |= FLAG_CLASS_FILTER;
  }

  return flag;
}

int PullSysFlag::clearCommitOffsetFlag(int sysFlag) {
  return sysFlag & (~FLAG_COMMIT_OFFSET);
}

bool PullSysFlag::hasCommitOffsetFlag(int sysFlag) {
  return (sysFlag & FLAG_COMMIT_OFFSET) == FLAG_COMMIT_OFFSET;
}

bool PullSysFlag::hasSuspendFlag(int sysFlag) {
  return (sysFlag & FLAG_SUSPEND) == FLAG_SUSPEND;
}

bool PullSysFlag::hasSubscriptionFlag(int sysFlag) {
  return (sysFlag & FLAG_SUBSCRIPTION) == FLAG_SUBSCRIPTION;
}

bool PullSysFlag::hasClassFilterFlag(int sysFlag) {
  return (sysFlag & FLAG_CLASS_FILTER) == FLAG_CLASS_FILTER;
}

//<!***************************************************************************
}  //<!end namespace;
