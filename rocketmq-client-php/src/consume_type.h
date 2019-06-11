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

#ifndef ROCKETMQ_CLIENT_PHP_CONSUME_TYPE_H_
#define ROCKETMQ_CLIENT_PHP_CONSUME_TYPE_H_

#include "common.h"


class ConsumeType : public Php::Base
{
    // CONSUME_ACTIVELY,
    // CONSUME_PASSIVELY,
};


class ConsumeFromWhere : public Php::Base {
    /**
     *new consumer will consume from end offset of queue, 
     * and then consume from last consumed offset of queue follow-up 
     */
    //CONSUME_FROM_LAST_OFFSET,

    // @Deprecated
    //CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST,
    // @Deprecated
    //CONSUME_FROM_MIN_OFFSET,
    // @Deprecated
    //CONSUME_FROM_MAX_OFFSET,
    /**
     *new consumer will consume from first offset of queue, 
     * and then consume from last consumed offset of queue follow-up 
     */
    //CONSUME_FROM_FIRST_OFFSET,
    /**
     *new consumer will consume from the queue offset specified by timestamp, 
     * and then consume from last consumed offset of queue follow-up 
     */
    //CONSUME_FROM_TIMESTAMP,

};

class MessageModel : public Php::Base
{
    //  BROADCASTING,
    //	CLUSTERING,
};


void registerConsumeType(Php::Namespace &rocketMQNamespace)
{
    // class ConsumeType
    Php::Class<ConsumeType> consumeTypeClass("ConsumeType");
    consumeTypeClass.constant("CONSUME_ACTIVELY", rocketmq::CONSUME_ACTIVELY);
    consumeTypeClass.constant("CONSUME_PASSIVELY", rocketmq::CONSUME_PASSIVELY);
    rocketMQNamespace.add(consumeTypeClass);

    // class ConsumeFromWhere
    Php::Class<ConsumeFromWhere> consumeFromWhereClass("ConsumeFromWhere");
    consumeFromWhereClass.constant("CONSUME_FROM_LAST_OFFSET", rocketmq::CONSUME_FROM_LAST_OFFSET);
    consumeFromWhereClass.constant("CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST", rocketmq::CONSUME_FROM_LAST_OFFSET_AND_FROM_MIN_WHEN_BOOT_FIRST);
    consumeFromWhereClass.constant("CONSUME_FROM_MIN_OFFSET", rocketmq::CONSUME_FROM_MIN_OFFSET);
    consumeFromWhereClass.constant("CONSUME_FROM_MAX_OFFSET", rocketmq::CONSUME_FROM_MAX_OFFSET);
    consumeFromWhereClass.constant("CONSUME_FROM_FIRST_OFFSET", rocketmq::CONSUME_FROM_FIRST_OFFSET);
    consumeFromWhereClass.constant("CONSUME_FROM_TIMESTAMP", rocketmq::CONSUME_FROM_TIMESTAMP);
    rocketMQNamespace.add(consumeFromWhereClass);

    // class MessageModel
    Php::Class<MessageModel> messageModelClass("MessageModel");
    messageModelClass.constant("BROADCASTING", rocketmq::BROADCASTING);
    messageModelClass.constant("CLUSTERING", rocketmq::CLUSTERING);
    rocketMQNamespace.add(messageModelClass);
}

#endif

