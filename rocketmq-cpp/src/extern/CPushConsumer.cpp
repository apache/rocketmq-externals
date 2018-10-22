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

#include "DefaultMQPushConsumer.h"
#include "CMessageExt.h"
#include "CPushConsumer.h"
#include "CCommon.h"
#include <map>

using namespace rocketmq;
using namespace std;
class MessageListenerInner : public MessageListenerConcurrently {
public:
    MessageListenerInner() {}

    MessageListenerInner(CPushConsumer *consumer, MessageCallBack pCallback) {
        m_pconsumer = consumer;
        m_pMsgReceiveCallback = pCallback;
    }

    ~MessageListenerInner() {}

    ConsumeStatus consumeMessage(const std::vector<MQMessageExt> &msgs) {
        //to do user call back
        if (m_pMsgReceiveCallback == NULL) {
            return RECONSUME_LATER;
        }
        for (size_t i = 0; i < msgs.size(); ++i) {
            MQMessageExt *msg = const_cast<MQMessageExt *>(&msgs[i]);
            CMessageExt *message = (CMessageExt *) (msg);
            if (m_pMsgReceiveCallback(m_pconsumer, message) != E_CONSUME_SUCCESS)
                return RECONSUME_LATER;
        }
        return CONSUME_SUCCESS;
    }

private:
    MessageCallBack m_pMsgReceiveCallback;
    CPushConsumer *m_pconsumer;
};

map<CPushConsumer *, MessageListenerInner *> g_ListenerMap;

#ifdef __cplusplus
extern "C" {
#endif


CPushConsumer *CreatePushConsumer(const char *groupId) {
    if (groupId == NULL) {
        return NULL;
    }
    DefaultMQPushConsumer *defaultMQPushConsumer = new DefaultMQPushConsumer(groupId);
    defaultMQPushConsumer->setConsumeFromWhere(CONSUME_FROM_LAST_OFFSET);
    return (CPushConsumer *) defaultMQPushConsumer;
}
int DestroyPushConsumer(CPushConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    delete reinterpret_cast<DefaultMQPushConsumer * >(consumer);
    return OK;
}
int StartPushConsumer(CPushConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->start();
    return OK;
}
int ShutdownPushConsumer(CPushConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->shutdown();
    return OK;
}
int SetPushConsumerGroupID(CPushConsumer *consumer, const char *groupId) {
    if (consumer == NULL || groupId == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->setGroupName(groupId);
    return OK;
}
const char *GetPushConsumerGroupID(CPushConsumer *consumer) {
    if (consumer == NULL) {
        return NULL;
    }
    return ((DefaultMQPushConsumer *) consumer)->getGroupName().c_str();
}
int SetPushConsumerNameServerAddress(CPushConsumer *consumer, const char *namesrv) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->setNamesrvAddr(namesrv);
    return OK;
}
int Subscribe(CPushConsumer *consumer, const char *topic, const char *expression) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->subscribe(topic, expression);
    return OK;
}
int RegisterMessageCallback(CPushConsumer *consumer, MessageCallBack pCallback) {
    if (consumer == NULL || pCallback == NULL) {
        return NULL_POINTER;
    }
    MessageListenerInner *listenerInner = new MessageListenerInner(consumer, pCallback);
    ((DefaultMQPushConsumer *) consumer)->registerMessageListener(listenerInner);
    g_ListenerMap[consumer] = listenerInner;
    return OK;
}
int UnregisterMessageCallback(CPushConsumer *consumer) {
    if (consumer == NULL) {
        return NULL_POINTER;
    }
    map<CPushConsumer *, MessageListenerInner *>::iterator iter;
    iter = g_ListenerMap.find(consumer);

    if (iter != g_ListenerMap.end()) {
        MessageListenerInner *listenerInner = iter->second;
        if (listenerInner != NULL) {
            delete listenerInner;
        }
        g_ListenerMap.erase(iter);
    }
    return OK;
}
int SetPushConsumeThreadCount(CPushConsumer *consumer, int threadCount) {
    if (consumer == NULL || threadCount == 0) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->setConsumeThreadCount(threadCount);
    return OK;
}
int SetPushConsumeMessageBatchMaxSize(CPushConsumer *consumer, int batchSize) {
    if (consumer == NULL || batchSize == 0) {
        return NULL_POINTER;
    }
    ((DefaultMQPushConsumer *) consumer)->setConsumeMessageBatchMaxSize(batchSize);
    return OK;
}
#ifdef __cplusplus
};
#endif
