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

#ifndef ROCKETMQ_CLIENT_PHP_CONSUME_STATUS_H_
#define ROCKETMQ_CLIENT_PHP_CONSUME_STATUS_H_

#include "common.h"

class ConsumeStatus: public Php::Base
{
    //consume success, msg will be cleard from memory
    // CONSUME_SUCCESS
    //consume fail, but will be re-consume by call messageLisenter again
    // RECONSUME_LATER
};

void registerConsumeStatus(Php::Namespace &rocketMQNamespace){
    Php::Class<ConsumeStatus> consumeStatusClass("ConsumeStatus");
    consumeStatusClass.constant("CONSUME_SUCCESS", (int) rocketmq::CONSUME_SUCCESS);
    consumeStatusClass.constant("RECONSUME_LATER", (int) rocketmq::RECONSUME_LATER);
    rocketMQNamespace.add(consumeStatusClass);
}

#endif
