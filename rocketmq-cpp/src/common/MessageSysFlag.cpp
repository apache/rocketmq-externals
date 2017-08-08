/********************************************************************
author:  qiwei.wqw@alibaba-inc.com
*********************************************************************/
#include "MessageSysFlag.h"

namespace metaq {
int MessageSysFlag::CompressedFlag = (0x1 << 0);
int MessageSysFlag::MultiTagsFlag = (0x1 << 1);

int MessageSysFlag::TransactionNotType = (0x0 << 2);
int MessageSysFlag::TransactionPreparedType = (0x1 << 2);
int MessageSysFlag::TransactionCommitType = (0x2 << 2);
int MessageSysFlag::TransactionRollbackType = (0x3 << 2);

int MessageSysFlag::getTransactionValue(int flag) {
  return flag & TransactionRollbackType;
}

int MessageSysFlag::resetTransactionValue(int flag, int type) {
  return (flag & (~TransactionRollbackType)) | type;
}

//<!***************************************************************************
}  //<!end namespace;
