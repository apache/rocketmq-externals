/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include "common.h"
#include "producer.h"
#include "push_consumer.h"
#include "pull_consumer.h"
#include "message.h"
#include "message_ext.h"
#include "pull_status.h"
#include "pull_result.h"
#include "message_queue.h"
#include "consume_type.h"
#include "consume_status.h"
#include "msg_listener.h"
#include "session_credentials.h"
#include "send_status.h"
#include "send_result.h"

// symbols are exported according to the "C" language
extern "C"
{
    // export the "get_module" function that will be called by the Zend engine
    PHPCPP_EXPORT void *get_module() 
    { 
        // all class in RocketMQ namespace.
        Php::Namespace rocketMQNamespace(NAMESPACE_NAME);

        // class Producer
        registerProducer(rocketMQNamespace);

        // class PullStatus
        registerPullStatus(rocketMQNamespace);

        // class ConsumeType, ConsumeFromWhere, MessageModel
        registerConsumeType(rocketMQNamespace);

        // class PushConsumer
        registerPushConsumer(rocketMQNamespace);

        // class PullResult
        registerPullResult(rocketMQNamespace);

        // class PullConsumer 
        registerPullConsumer(rocketMQNamespace);

        // class MessageQueue
        registerMessageQueue(rocketMQNamespace);

        // class Message 
        registerMessage(rocketMQNamespace);

        // class MessageExt
        registerMessageExt(rocketMQNamespace);

        // class ConsumeStatus
        registerConsumeStatus(rocketMQNamespace);

        // class MessageListenerType
        registerMessageListenerType(rocketMQNamespace);

        // class SessionCredentials
        registerSessionCredentials(rocketMQNamespace);

        // class SendStatus
        registerSendStatus(rocketMQNamespace);

        // class SendResult
        registerSendResult(rocketMQNamespace);

        //registerClient(rocketMQNamespace);
        // create extension
        static Php::Extension extension("rocketmq", "1.0");
        extension.add(std::move(rocketMQNamespace));

        // return the module entry
        return extension.module();
    }
}

