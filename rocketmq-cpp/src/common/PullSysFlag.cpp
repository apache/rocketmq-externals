/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include "PullSysFlag.h"

namespace rocketmq {
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
