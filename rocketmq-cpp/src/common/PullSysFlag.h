/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __PULLSYSFLAG_H__
#define __PULLSYSFLAG_H__
namespace metaq {
//<!************************************************************************
class PullSysFlag {
 public:
  static int buildSysFlag(bool commitOffset, bool suspend, bool subscription,
                          bool classFilter);

  static int clearCommitOffsetFlag(int sysFlag);
  static bool hasCommitOffsetFlag(int sysFlag);
  static bool hasSuspendFlag(int sysFlag);
  static bool hasSubscriptionFlag(int sysFlag);
  static bool hasClassFilterFlag(int sysFlag);

 private:
  static int FLAG_COMMIT_OFFSET;
  static int FLAG_SUSPEND;
  static int FLAG_SUBSCRIPTION;
  static int FLAG_CLASS_FILTER;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
