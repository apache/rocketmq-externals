<?php
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
namespace RocketMQ\Common\Message;

class MessageConst
{
    const PROPERTY_KEYS = "KEYS";
    const PROPERTY_TAGS = "TAGS";
    const PROPERTY_WAIT_STORE_MSG_OK = "WAIT";
    const PROPERTY_DELAY_TIME_LEVEL = "DELAY";
    const PROPERTY_RETRY_TOPIC = "RETRY_TOPIC";
    const PROPERTY_REAL_TOPIC = "REAL_TOPIC";
    const PROPERTY_REAL_QUEUE_ID = "REAL_QID";
    const PROPERTY_TRANSACTION_PREPARED = "TRAN_MSG";
    const PROPERTY_PRODUCER_GROUP = "PGROUP";
    const PROPERTY_MIN_OFFSET = "MIN_OFFSET";
    const PROPERTY_MAX_OFFSET = "MAX_OFFSET";
    const PROPERTY_BUYER_ID = "BUYER_ID";
    const PROPERTY_ORIGIN_MESSAGE_ID = "ORIGIN_MESSAGE_ID";
    const PROPERTY_TRANSFER_FLAG = "TRANSFER_FLAG";
    const PROPERTY_CORRECTION_FLAG = "CORRECTION_FLAG";
    const PROPERTY_MQ2_FLAG = "MQ2_FLAG";
    const PROPERTY_RECONSUME_TIME = "RECONSUME_TIME";
    const PROPERTY_MSG_REGION = "MSG_REGION";
    const PROPERTY_TRACE_SWITCH = "TRACE_ON";
    const PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX = "UNIQ_KEY";
    const PROPERTY_MAX_RECONSUME_TIMES = "MAX_RECONSUME_TIMES";
    const PROPERTY_CONSUME_START_TIMESTAMP = "CONSUME_START_TIME";

    const KEY_SEPARATOR = " ";

    const STRING_HASH_SET = [
        PROPERTY_TRACE_SWITCH,
        PROPERTY_MSG_REGION,
        PROPERTY_KEYS,
        PROPERTY_TAGS,
        PROPERTY_WAIT_STORE_MSG_OK,
        PROPERTY_DELAY_TIME_LEVEL,
        PROPERTY_RETRY_TOPIC,
        PROPERTY_REAL_TOPIC,
        PROPERTY_REAL_QUEUE_ID,
        PROPERTY_TRANSACTION_PREPARED,
        PROPERTY_PRODUCER_GROUP,
        PROPERTY_MIN_OFFSET,
        PROPERTY_MAX_OFFSET,
        PROPERTY_BUYER_ID,
        PROPERTY_ORIGIN_MESSAGE_ID,
        PROPERTY_TRANSFER_FLAG,
        PROPERTY_CORRECTION_FLAG,
        PROPERTY_MQ2_FLAG,
        PROPERTY_RECONSUME_TIME,
        PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX,
        PROPERTY_MAX_RECONSUME_TIMES,
        PROPERTY_CONSUME_START_TIMESTAMP
        ];
}
