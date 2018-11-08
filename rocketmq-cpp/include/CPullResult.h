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

#ifndef __C_PULL_RESULT_H__
#define __C_PULL_RESULT_H__

#include "CCommon.h"
#include "CMessageExt.h"

#ifdef __cplusplus
extern "C" {
#endif
typedef enum E_CPullStatus
{
    E_FOUND,
    E_NO_NEW_MSG,
    E_NO_MATCHED_MSG,
    E_OFFSET_ILLEGAL,
    E_BROKER_TIMEOUT   //indicate pull request timeout or received NULL response
} CPullStatus;

typedef struct _CPullResult_ {
    CPullStatus pullStatus;
    long long  nextBeginOffset;
    long long  minOffset;
    long long  maxOffset;
    CMessageExt* msgFoundList;
    int size;
} CPullResult;

#ifdef __cplusplus
};
#endif
#endif //__C_PULL_RESULT_H__
