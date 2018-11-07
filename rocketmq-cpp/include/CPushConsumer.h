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

#ifndef __C_PUSH_CONSUMER_H__
#define __C_PUSH_CONSUMER_H__

#include "CMessageExt.h"

#ifdef __cplusplus
extern "C" {
#endif

//typedef struct _CConsumer_ _CConsumer;
struct CPushConsumer;

typedef enum {
    E_CONSUME_SUCCESS = 0,
    E_RECONSUME_LATER = 1
} CConsumeStatus;

typedef int(*MessageCallBack)(CPushConsumer *, CMessageExt *);


CPushConsumer *CreatePushConsumer(const char *groupId);
int DestroyPushConsumer(CPushConsumer *consumer);
int StartPushConsumer(CPushConsumer *consumer);
int ShutdownPushConsumer(CPushConsumer *consumer);
int SetPushConsumerGroupID(CPushConsumer *consumer, const char *groupId);
const char *GetPushConsumerGroupID(CPushConsumer *consumer);
int SetPushConsumerNameServerAddress(CPushConsumer *consumer, const char *namesrv);
int Subscribe(CPushConsumer *consumer, const char *topic, const char *expression);
int RegisterMessageCallback(CPushConsumer *consumer, MessageCallBack pCallback);
int SetPushConsumeThreadCount(CPushConsumer *consumer, int threadCount);
int SetPushConsumeMessageBatchMaxSize(CPushConsumer *consumer, int batchSize);

#ifdef __cplusplus
};
#endif
#endif //__C_PUSH_CONSUMER_H__
