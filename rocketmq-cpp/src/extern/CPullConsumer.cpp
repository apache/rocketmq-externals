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

#include "DefaultMQPullConsumer.h"
#include "CMessageExt.h"
#include "CPullConsumer.h"
#include "CCommon.h"
#include <map>

using namespace rocketmq;

#ifdef __cplusplus
extern "C" {
#endif


CPullConsumer *CreatePullConsumer(const char *groupId) {
    if (groupId == NULL) {
        return NULL;
    }
    DefaultMQPullConsumer *defaultMQPullConsumer = new DefaultMQPullConsumer(groupId);
    return (CPullConsumer *) defaultMQPullConsumer;
}
int DestroyPullConsumer(CPullConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    delete reinterpret_cast<DefaultMQPullConsumer * >(consumer);
    return OK;
}
int StartPullConsumer(CPullConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPullConsumer *) consumer)->start();
    return OK;
}
int ShutdownPullConsumer(CPullConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPullConsumer *) consumer)->shutdown();
    return OK;
}
int SetPullConsumerGroupID(CPullConsumer *consumer,const char *groupId){
    if (consumer == NULL || groupId == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPullConsumer *) consumer)->setGroupName(groupId);
    return OK;
}
const char * GetPullConsumerGroupID(CPullConsumer *consumer){
    if (consumer == NULL) {
        return NULL;
    }
    return ((DefaultMQPullConsumer *) consumer)->getGroupName().c_str();
}
int SetPullConsumerNameServerAddress(CPullConsumer *consumer, const char *namesrv) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPullConsumer *) consumer)->setNamesrvAddr(namesrv);
    return OK;
}

#ifdef __cplusplus
};
#endif
