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

#include "DefaultMQProducer.h"
#include "CProducer.h"
#include "CCommon.h"
#include <string.h>


#ifdef __cplusplus
extern "C" {
#endif
using namespace rocketmq;
CProducer *CreateProducer(const char *groupId) {
    if (groupId == NULL) {
        return NULL;
    }
    DefaultMQProducer *defaultMQProducer = new DefaultMQProducer(groupId);
    return (CProducer *) defaultMQProducer;
}
int DestroyProducer(CProducer *pProducer) {
    if (pProducer == NULL) {
        return NULL_POINTER;
    }
    delete reinterpret_cast<DefaultMQProducer * >(pProducer);
    return OK;
}
int StartProducer( CProducer *producer) {
    if (producer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQProducer *) producer)->start();
    return OK;
}
int ShutdownProducer( CProducer *producer) {
    if (producer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQProducer *) producer)->shutdown();
    return OK;
}
int SetProducerNameServerAddress( CProducer *producer, const char *namesrv) {
    if (producer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQProducer *) producer)->setNamesrvAddr(namesrv);
    return OK;
}

int SendMessageSync( CProducer *producer, CMessage *msg, CSendResult *result) {
    //CSendResult sendResult;
    if (producer == NULL || msg == NULL || result == NULL) {
        return NULL_POINTER;
    }
    DefaultMQProducer *defaultMQProducer = (DefaultMQProducer *) producer;
    MQMessage *message = (MQMessage *) msg;
    SendResult sendResult = defaultMQProducer->send(*message);
    switch (sendResult.getSendStatus()) {
        case SEND_OK:
            result->sendStatus = E_SEND_OK;
            break;
        case SEND_FLUSH_DISK_TIMEOUT:
            result->sendStatus = E_SEND_FLUSH_DISK_TIMEOUT;
            break;
        case SEND_FLUSH_SLAVE_TIMEOUT:
            result->sendStatus = E_SEND_FLUSH_SLAVE_TIMEOUT;
            break;
        case SEND_SLAVE_NOT_AVAILABLE:
            result->sendStatus = E_SEND_SLAVE_NOT_AVAILABLE;
            break;
        default:
            result->sendStatus = E_SEND_OK;
            break;
    }
    result->offset = sendResult.getQueueOffset();
    //strcpy(result->msgId, sendResult.getMsgId().c_str());
    strncpy(result->msgId, sendResult.getMsgId().c_str(), MAX_MESSAGE_ID_LENGTH - 1);
    result->msgId[MAX_MESSAGE_ID_LENGTH - 1] = 0;
    return OK;
}

#ifdef __cplusplus
};
#endif
