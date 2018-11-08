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

#ifndef __C_COMMON_H__
#define __C_COMMON_H__

#ifdef __cplusplus
extern "C" {
#endif

#define  MAX_MESSAGE_ID_LENGTH 256
#define  MAX_TOPIC_LENGTH 512
#define  MAX_BROKER_NAME_ID_LENGTH 256
typedef enum {
    // Success
    OK = 0,
    // Failed, null pointer value
    NULL_POINTER = 1,
} CStatus;
typedef enum {
    E_LOG_LEVEL_FATAL = 1,
    E_LOG_LEVEL_ERROR = 2,
    E_LOG_LEVEL_WARN = 3,
    E_LOG_LEVEL_INFO = 4,
    E_LOG_LEVEL_DEBUG = 5,
    E_LOG_LEVEL_TRACE = 6,
    E_LOG_LEVEL_LEVEL_NUM = 7
} CLogLevel;
#ifdef __cplusplus
};
#endif
#endif //__C_COMMON_H__
