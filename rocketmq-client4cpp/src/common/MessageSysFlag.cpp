/**
* Copyright (C) 2013 kangliqiang ,kangliq@163.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

#include "MessageSysFlag.h"

namespace rmq
{

/**
* SysFlag
*/
int MessageSysFlag::CompressedFlag = (0x1 << 0);
int MessageSysFlag::MultiTagsFlag = (0x1 << 1);

/**
* 7 6 5 4 3 2 1 0<br>
* SysFlag for transaction
*/
int MessageSysFlag::TransactionNotType = (0x0 << 2);
int MessageSysFlag::TransactionPreparedType = (0x1 << 2);
int MessageSysFlag::TransactionCommitType = (0x2 << 2);
int MessageSysFlag::TransactionRollbackType = (0x3 << 2);

int MessageSysFlag::getTransactionValue(int flag)
{
    return flag & TransactionRollbackType;
}

int MessageSysFlag::resetTransactionValue(int flag, int type)
{
    return (flag & (~TransactionRollbackType)) | type;
}

}
