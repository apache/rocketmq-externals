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

#include "send_result.h"

SendResult::SendResult(rocketmq::SendResult& sendResult){
    this->sendResult = sendResult;
}
Php::Value SendResult::getMsgId(){
    return this->sendResult.getMsgId();
}
Php::Value SendResult::getOffsetMsgId(){
    return this->sendResult.getOffsetMsgId();
}
Php::Value SendResult::getSendStatus(){
    return (int64_t)this->sendResult.getSendStatus();
}

Php::Value SendResult::getMessageQueue(){
    rocketmq::MQMessageQueue mq = this->sendResult.getMessageQueue();
    return Php::Object(MESSAGE_QUEUE_CLASS_NAME, new MessageQueue(mq));
}

Php::Value SendResult::getQueueOffset(){
    return (int64_t) this->sendResult.getQueueOffset();
}

void registerSendResult(Php::Namespace &rocketMQNamespace){
    Php::Class<SendResult> sendResultClass("SendResult");

    sendResultClass.method<&SendResult::getMsgId>("getMsgId");
    sendResultClass.method<&SendResult::getOffsetMsgId>("getOffsetMsgId");
    sendResultClass.method<&SendResult::getSendStatus>("getSendStatus");
    sendResultClass.method<&SendResult::getMessageQueue>("getMessageQueue");
    sendResultClass.method<&SendResult::getQueueOffset>("getQueueOffset");

    rocketMQNamespace.add(sendResultClass);
}

