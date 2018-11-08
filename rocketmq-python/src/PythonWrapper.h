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

#include "CCommon.h"
#include "CMessage.h"
#include "CMessageExt.h"
#include "CSendResult.h"
#include "CProducer.h"
#include "CPushConsumer.h"
#include <boost/python.hpp>

using namespace boost::python;

typedef struct _PySendResult_ {
    CSendStatus sendStatus;
    char msgId[MAX_MESSAGE_ID_LENGTH];
    long long offset;

    const char *GetMsgId() {
        return (const char *) msgId;
    }
} PySendResult;

typedef struct _PyMessageExt_ {
    CMessageExt *pMessageExt;
} PyMessageExt;

#define PYTHON_CLIENT_VERSION "1.0.0"
#define PYCLI_BUILD_DATE "16-10-2018"

#ifdef __cplusplus
extern "C" {
#endif

//message
void *PyCreateMessage(const char *topic);
int PyDestroyMessage(void *msg);
int PySetMessageTopic(void *msg, const char *topic);
int PySetMessageTags(void *msg, const char *tags);
int PySetMessageKeys(void *msg, const char *keys);
int PySetMessageBody(void *msg, const char *body);
int PySetByteMessageBody(void *msg, const char *body, int len);
int PySetMessageProperty(void *msg, const char *key, const char *value);
int PySetMessageDelayTimeLevel(void *msg, int level);

//messageExt
const char *PyGetMessageTopic(PyMessageExt msgExt);
const char *PyGetMessageTags(PyMessageExt msgExt);
const char *PyGetMessageKeys(PyMessageExt msgExt);
const char *PyGetMessageBody(PyMessageExt msgExt);
const char *PyGetMessageProperty(PyMessageExt msgExt, const char *key);
const char *PyGetMessageId(PyMessageExt msgExt);

//producer
void *PyCreateProducer(const char *groupId);
int PyDestroyProducer(void *producer);
int PyStartProducer(void *producer);
int PyShutdownProducer(void *producer);
int PySetProducerNameServerAddress(void *producer, const char *namesrv);
int PySetProducerInstanceName(void *producer, const char *instanceName);
int PySetProducerSessionCredentials(void *producer, const char *accessKey, const char *secretKey, const char *channel);
PySendResult PySendMessageSync(void *producer, void *msg);

//sendResult
const char *PyGetSendResultMsgID(CSendResult &sendResult);

//consumer
void *PyCreatePushConsumer(const char *groupId);
int PyDestroyPushConsumer(void *consumer);
int PyStartPushConsumer(void *consumer);
int PyShutdownPushConsumer(void *consumer);
int PySetPushConsumerNameServerAddress(void *consumer, const char *namesrv);
int PySubscribe(void *consumer, const char *topic, const char *expression);
int PyRegisterMessageCallback(void *consumer, PyObject *pCallback);
int PythonMessageCallBackInner(CPushConsumer *consumer, CMessageExt *msg);
int PySetPushConsumerThreadCount(void *consumer, int threadCount);
int PySetPushConsumerMessageBatchMaxSize(void *consumer, int batchSize);
int PySetPushConsumerInstanceName(void *consumer, const char *instanceName);
int PySetPushConsumerSessionCredentials(void *consumer, const char *accessKey, const char *secretKey,
                                     const char *channel);
#ifdef __cplusplus
};
#endif

