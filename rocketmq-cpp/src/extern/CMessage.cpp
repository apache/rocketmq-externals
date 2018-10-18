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

#include "CMessage.h"
#include "MQMessage.h"
#include "CCommon.h"

#ifdef __cplusplus
extern "C" {
#endif

using namespace rocketmq;

CMessage *CreateMessage(const char *topic) {

    MQMessage *mqMessage = new MQMessage();
    if (topic != NULL) {
        mqMessage->setTopic(topic);
    }
    return (CMessage *) mqMessage;
}
int DestroyMessage(CMessage *msg) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    delete (MQMessage *)msg;
    return OK;
}
int SetMessageTopic(CMessage *msg, const char *topic) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setTopic(topic);
    return OK;
}
int SetMessageTags(CMessage *msg, const char *tags) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setTags(tags);
    return OK;
}
int SetMessageKeys(CMessage *msg, const char *keys) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setKeys(keys);
    return OK;
}
int SetMessageBody(CMessage *msg, const char *body) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setBody(body);
    return OK;
}
int SetByteMessageBody(CMessage *msg, const char *body, int len) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setBody(body,len);
    return OK;
}
int SetMessageProperty(CMessage *msg, const char *key, const char *value) {
    if (msg == NULL) {
        return NULL_POINTER;
    }
    ((MQMessage *)msg)->setProperty(key,value);
    return OK;
}

#ifdef __cplusplus
};
#endif
