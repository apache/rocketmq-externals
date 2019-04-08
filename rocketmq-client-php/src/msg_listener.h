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

#ifndef ROCKETMQ_CLIENT_PHP_MESSAGE_LISTENER_TYPE_H_
#define ROCKETMQ_CLIENT_PHP_MESSAGE_LISTENER_TYPE_H_

#include "rocketmq/MQMessageListener.h"
#include "common.h"

class MessageListenerType : public Php::Base{
    //  messageListenerDefaultly = 0,
    //  messageListenerOrderly = 1,
    //  messageListenerConcurrently = 2
};

void registerMessageListenerType(Php::Namespace &rocketMQNamespace);

class MsgListenerConcurrently : public rocketmq::MessageListenerConcurrently {
    private:
        Php::Value callback;
    public:
        void setCallback(Php::Value callback){
            this->callback = callback;
        }
        MsgListenerConcurrently() {}
        ~MsgListenerConcurrently() {}

        rocketmq::ConsumeStatus consumeMessage(const std::vector<rocketmq::MQMessageExt> &msgs);
};

class MsgListenerOrderly: public rocketmq::MessageListenerOrderly {
    private:
        Php::Value callback;
    public:
        void setCallback(Php::Value callback){
            this->callback = callback;
        }
        MsgListenerOrderly() {}
        ~MsgListenerOrderly() {}

        rocketmq::ConsumeStatus consumeMessage(const std::vector<rocketmq::MQMessageExt> &msgs);
};

class MsgListener : public rocketmq::MQMessageListener {
    private:
        Php::Value callback;
    public:
        void setCallback(Php::Value callback){
            this->callback = callback;
        }
        MsgListener() {}
        ~MsgListener() {}

        rocketmq::ConsumeStatus consumeMessage(const std::vector<rocketmq::MQMessageExt> &msgs);
};

#endif
