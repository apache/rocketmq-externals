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

#include "push_consumer.h"
#include "msg_listener.h"
#include "session_credentials.h"

void PushConsumer::doRebalance(){
    this->consumer->doRebalance();
}

void PushConsumer::persistConsumerOffset(){
    this->consumer->persistConsumerOffset();
}
void PushConsumer::persistConsumerOffsetByResetOffset(){
    this->consumer->persistConsumerOffsetByResetOffset();
}

void PushConsumer::setNamesrvDomain(Php::Parameters &param){
    std::string nameserver =  param[0];
    this->consumer->setNamesrvDomain(nameserver);
}

Php::Value PushConsumer::getNamesrvDomain(){
    return this->consumer->getNamesrvDomain();
}

void PushConsumer::setNamesrvAddr(Php::Parameters &param){
    std::string namesrvAddr = param[0];
    this->consumer->setNamesrvAddr(namesrvAddr);
}

Php::Value PushConsumer::getNamesrvAddr(){
    return this->consumer->getNamesrvAddr();
}


void PushConsumer::setInstanceName(Php::Parameters &param){
    std::string groupName= param[0];
    this->consumer->setInstanceName(this->groupName);
}  

void PushConsumer::setTryLockTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportTryLockTimeout((int64_t)param[0]);
}

void PushConsumer::setConnectTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportConnectTimeout((int64_t)param[0]);
}

void PushConsumer::setThreadCount(Php::Parameters &param){
    this->consumer->setConsumeThreadCount((int64_t)param[0]);
}

Php::Value PushConsumer::getConsumeType(){
    return (int) this->consumer->getConsumeType();
}

Php::Value PushConsumer::getConsumeFromWhere(){
    return (int) this->consumer->getConsumeFromWhere();
}

void PushConsumer::setConsumeFromWhere(Php::Parameters &param){
    int consumeFromWhere = (int) param[0];
    this->consumer->setConsumeFromWhere(rocketmq::ConsumeFromWhere(consumeFromWhere));
}

void PushConsumer::setListenerType(Php::Parameters &param){
    this->msgListenerType = param[0];
}

void PushConsumer::subscribe(Php::Parameters &param){
    std::string topic = param[0];
    std::string tag = param[1];
    this->consumer->subscribe(topic, tag);
}
void PushConsumer::setCallback(Php::Parameters &param){
    if (!param[0].isCallable())
        throw Php::Exception("Not a callable type.");
    this->callback = param[0];
}

void PushConsumer::setMaxRequestTime(Php::Parameters &param){
    this->maxRequestTime = (int)param[0];
}

void PushConsumer::start(){
    MsgListenerOrderly* msgListenerOrderly;
    MsgListenerConcurrently* msgListenerConcurrently;
    MsgListener* msgListener;
    switch(this->msgListenerType){
        case rocketmq::messageListenerOrderly:
            msgListenerOrderly = new MsgListenerOrderly();
            msgListenerOrderly->setCallback(this->callback);
            this->consumer->registerMessageListener(msgListenerOrderly);
            break;
        case rocketmq::messageListenerConcurrently:
            msgListenerConcurrently = new MsgListenerConcurrently();
            msgListenerConcurrently->setCallback(this->callback);
            this->consumer->registerMessageListener(msgListenerConcurrently);
            break;
        default :
            msgListener = new MsgListener();
            msgListener->setCallback(this->callback);
            this->consumer->registerMessageListener(msgListener);
            break;
    }

    try {
        this->consumer->start();
    } catch (rocketmq::MQClientException &e) {
        std::cout << e << std::endl;
    }
    sleep(this->maxRequestTime);
}
void PushConsumer::shutdown(){
    this->consumer->shutdown();
}

void PushConsumer::__construct(Php::Parameters &params){
    std::string groupName = params[0];
    this->consumer = new rocketmq::DefaultMQPushConsumer(groupName);
}

void PushConsumer::setSessionCredentials(Php::Parameters &param){
    std::string accessKey = param[0];
    std::string secretKey = param[1];
    std::string authChannel = param[2];

    this->consumer->setSessionCredentials(accessKey, secretKey, authChannel);
}

Php::Value PushConsumer::getSessionCredentials(){
    rocketmq::SessionCredentials sc = this->consumer->getSessionCredentials();
    SessionCredentials *sessionCredentials = new SessionCredentials(&sc);
    Php::Value pv(Php::Object(SESSION_CREDENTIALS_CLASS_NAME , sessionCredentials));
    return pv;
}

Php::Value PushConsumer::getMessageModel(){
    return this->consumer->getMessageModel();
}
void PushConsumer::setMessageModel(Php::Parameters &params){
    this->consumer->setMessageModel(rocketmq::MessageModel((int)params[0]));
}

// void setTcpTransportPullThreadNum(int num);
void PushConsumer::setTcpTransportPullThreadNum(Php::Parameters &param){
    this->consumer->setTcpTransportPullThreadNum((int64_t)param[0]);
}

// const int getTcpTransportPullThreadNum() const;
Php::Value PushConsumer::getTcpTransportPullThreadNum(){
    return (int64_t)this->consumer->getTcpTransportPullThreadNum();
}

// void setTcpTransportConnectTimeout(uint64_t timeout);  // ms
void PushConsumer::setTcpTransportConnectTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportConnectTimeout((int64_t)param[0]);
}
// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value PushConsumer::getTcpTransportConnectTimeout(){
    return (int64_t)this->consumer->getTcpTransportConnectTimeout();
}

// void setTcpTransportTryLockTimeout(uint64_t timeout);  // ms
void PushConsumer::setTcpTransportTryLockTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportTryLockTimeout((int64_t)param[0]);
}

// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value PushConsumer::getTcpTransportTryLockTimeout(){
    return (int64_t)this->consumer->getTcpTransportTryLockTimeout();
}

//void setUnitName(std::string unitName);
void PushConsumer::setUnitName(Php::Parameters &param){
    this->consumer->setUnitName(param[0]);
}
//const std::string& getUnitName();
Php::Value PushConsumer::getUnitName(){
    return this->consumer->getUnitName();
}

void PushConsumer::setLogLevel(Php::Parameters &param){
    this->consumer->setLogLevel(rocketmq::elogLevel((int)param[0]));
}

Php::Value PushConsumer::getLogLevel(){
    return this->consumer->getLogLevel();
}

void PushConsumer::setLogFileSizeAndNum(Php::Parameters &param){
    this->consumer->setLogFileSizeAndNum(param[0], param[1]);
}



void registerPushConsumer(Php::Namespace &rocketMQNamespace){
    Php::Class<PushConsumer> pushConsumer("PushConsumer");
    pushConsumer.method<&PushConsumer::doRebalance>("doRebalance");
    pushConsumer.method<&PushConsumer::persistConsumerOffset>("persistConsumerOffset");
    pushConsumer.method<&PushConsumer::persistConsumerOffsetByResetOffset>("persistConsumerOffsetByResetOffset");
    pushConsumer.method<&PushConsumer::__construct>("__construct", 				{ Php::ByVal("groupName", Php::Type::String), });
    pushConsumer.method<&PushConsumer::setNamesrvDomain>("setNamesrvDomain", 	{ Php::ByVal("nameserver", Php::Type::String), });
    pushConsumer.method<&PushConsumer::setNamesrvAddr>("setNamesrvAddr",     	{ Php::ByVal("namesrvAddr", Php::Type::String), });
    pushConsumer.method<&PushConsumer::setInstanceName>("setInstanceName", 		{ Php::ByVal("groupName", Php::Type::String), });
    pushConsumer.method<&PushConsumer::setTryLockTimeout>("setTryLockTimeout", 	{Php::ByVal("tryLockTimeout", Php::Type::Numeric),});
    pushConsumer.method<&PushConsumer::setConnectTimeout>("setConnectTimeout", 	{Php::ByVal("connectTimeout", Php::Type::Numeric),});
    pushConsumer.method<&PushConsumer::setThreadCount>("setThreadCount", 		{Php::ByVal("threadCount", Php::Type::Numeric),});
    pushConsumer.method<&PushConsumer::setListenerType>("setListenerType", 		{Php::ByVal("listenerType", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::getConsumeType>("getConsumeType");
    pushConsumer.method<&PushConsumer::getConsumeFromWhere>("getConsumeFromWhere");
    pushConsumer.method<&PushConsumer::setConsumeFromWhere>("setConsumeFromWhere", { Php::ByVal("consumeFromWhere", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::subscribe>("subscribe", { 
            Php::ByVal("topic", Php::Type::String), 
            Php::ByVal("tag", Php::Type::String), 
            });
    pushConsumer.method<&PushConsumer::start>("start");
    pushConsumer.method<&PushConsumer::shutdown>("shutdown");
    pushConsumer.method<&PushConsumer::setMaxRequestTime>("setMaxRequestTime", { Php::ByVal("maxRequestTime", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::setCallback>("setCallback", { Php::ByVal("callback", Php::Type::Callable), });

    pushConsumer.method<&PushConsumer::setSessionCredentials>("setSessionCredentials", {
            Php::ByVal("accessKey", Php::Type::String),
            Php::ByVal("secretKey", Php::Type::String),
            Php::ByVal("authChannel", Php::Type::String),
            });
    pushConsumer.method<&PushConsumer::getSessionCredentials>("getSessionCredentials");
    pushConsumer.method<&PushConsumer::getMessageModel>("getMessageModel");
    pushConsumer.method<&PushConsumer::setMessageModel>("setMessageModel", {
            Php::ByVal("messageModel", Php::Type::Numeric),
            });

    pushConsumer.method<&PushConsumer::getTcpTransportTryLockTimeout>("getTcpTransportTryLockTimeout");
    pushConsumer.method<&PushConsumer::setTcpTransportTryLockTimeout>("setTcpTransportTryLockTimeout",{ Php::ByVal("timeout", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::getTcpTransportConnectTimeout>("getTcpTransportConnectTimeout");
    pushConsumer.method<&PushConsumer::setTcpTransportConnectTimeout>("setTcpTransportConnectTimeout", {Php::ByVal("timeout", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::getTcpTransportPullThreadNum>("getTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::setTcpTransportPullThreadNum>("setTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });
    pushConsumer.method<&PushConsumer::getUnitName>("getUnitName");
    pushConsumer.method<&PushConsumer::setUnitName>("setUnitName", {Php::ByVal("unitName", Php::Type::String),});

    pushConsumer.method<&PushConsumer::setLogLevel>("setLogLevel", {Php::ByVal("inputLevel", Php::Type::Numeric),});
    pushConsumer.method<&PushConsumer::getLogLevel>("getLogLevel");
    pushConsumer.method<&PushConsumer::setLogFileSizeAndNum>("setLogFileSizeAndNum", {Php::ByVal("fileNum", Php::Type::Numeric),Php::ByVal("perFileSize", Php::Type::Numeric),});


    rocketMQNamespace.add(pushConsumer);
}


