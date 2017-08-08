/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#ifndef __MESSAGESYSFLAG_H__
#define __MESSAGESYSFLAG_H__

namespace metaq {
//<!************************************************************************
class MessageSysFlag {
 public:
  static int getTransactionValue(int flag);
  static int resetTransactionValue(int flag, int type);

 public:
  static int CompressedFlag;
  static int MultiTagsFlag;
  static int TransactionNotType;
  static int TransactionPreparedType;
  static int TransactionCommitType;
  static int TransactionRollbackType;
};

//<!***************************************************************************
}  //<!end namespace;
#endif
