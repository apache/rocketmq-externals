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

#ifndef __C_PRODUCER_H__
#define __C_PRODUCER_H__

#include "CMessage.h"
#include "CSendResult.h"

#ifdef __cplusplus
extern "C" {
#endif

//typedef struct _CProducer_ _CProducer;
struct CProducer;


CProducer *CreateProducer(const char *groupId);
int DestroyProducer(CProducer *producer);
int StartProducer(CProducer *producer);
int ShutdownProducer(CProducer *producer);

int SetProducerNameServerAddress(CProducer *producer, const char *namesrv);
int SetProducerGroupName(CProducer *producer, const char *groupName);
int SetProducerInstanceName(CProducer *producer, const char *instanceName);
int SetProducerSessionCredentials(CProducer *producer, const char *accessKey, const char *secretKey,
                                  const char *onsChannel);
int SetProducerLogPath(CProducer *producer, const char *logPath);
int SetProducerLogFileNumAndSize(CProducer *producer, int fileNum, long fileSize);
int SetProducerLogLevel(CProducer *producer, CLogLevel level);
int SetProducerSendMsgTimeout(CProducer *producer, int timeout);
int SetProducerCompressLevel(CProducer *producer, int level);
int SetProducerMaxMessageSize(CProducer *producer, int size);

int SendMessageSync(CProducer *producer, CMessage *msg, CSendResult *result);

#ifdef __cplusplus
};
#endif
#endif //__C_PRODUCER_H__
