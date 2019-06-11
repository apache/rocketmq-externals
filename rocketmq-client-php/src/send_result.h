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

#ifndef ROCKETMQ_CLIENT_PHP_SEND_RESULT_H_
#define ROCKETMQ_CLIENT_PHP_SEND_RESULT_H_

#include "common.h"
#include "message_queue.h"
#include <rocketmq/SendResult.h>

#define SEND_RESULT_CLASS_NAME NAMESPACE_NAME"\\SendResult"

class SendResult: public Php::Base
{
    private:
        rocketmq::SendResult sendResult;

    public:
        SendResult(rocketmq::SendResult& sendResult);

        Php::Value getMsgId();

        Php::Value getOffsetMsgId();

        Php::Value getSendStatus();

        Php::Value getMessageQueue();

        Php::Value getQueueOffset();
};

void registerSendResult(Php::Namespace &rocketMQNamespace);

#endif

