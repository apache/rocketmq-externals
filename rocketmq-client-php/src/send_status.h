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

#ifndef ROCKETMQ_CLIENT_PHP_SEND_STATUS_H_
#define ROCKETMQ_CLIENT_PHP_SEND_STATUS_H_

#include <rocketmq/SendResult.h>
#include "common.h"

class SendStatus : public Php::Base
{
    //SEND_OK,
    //SEND_FLUSH_DISK_TIMEOUT,
    //SEND_FLUSH_SLAVE_TIMEOUT,
    //SEND_SLAVE_NOT_AVAILABLE
};

void registerSendStatus(Php::Namespace &rocketMQNamespace)
{
    Php::Class<SendStatus> sendStatusClass("SendStatus");
    sendStatusClass.constant("SEND_OK", rocketmq::SEND_OK);
    sendStatusClass.constant("SEND_FLUSH_DISK_TIMEOUT", rocketmq::SEND_FLUSH_DISK_TIMEOUT);
    sendStatusClass.constant("SEND_FLUSH_SLAVE_TIMEOUT", rocketmq::SEND_FLUSH_SLAVE_TIMEOUT);
    sendStatusClass.constant("SEND_SLAVE_NOT_AVAILABLE", rocketmq::SEND_SLAVE_NOT_AVAILABLE);
    rocketMQNamespace.add(sendStatusClass);
}

#endif
