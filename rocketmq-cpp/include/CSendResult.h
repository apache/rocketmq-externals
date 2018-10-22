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

#ifndef __C_SEND_RESULT_H__
#define __C_SEND_RESULT_H__

#include "CCommon.h"
#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    E_SEND_OK = 0,
    E_SEND_FLUSH_DISK_TIMEOUT = 1,
    E_SEND_FLUSH_SLAVE_TIMEOUT = 2,
    E_SEND_SLAVE_NOT_AVAILABLE = 3
} CSendStatus;

typedef struct _SendResult_ {
    CSendStatus sendStatus;
    char        msgId[MAX_MESSAGE_ID_LENGTH];
    long long   offset;
} CSendResult;

#ifdef __cplusplus
};
#endif
#endif //__C_PRODUCER_H__
