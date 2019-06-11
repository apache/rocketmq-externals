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

#include "pull_consumer.h"
#include "session_credentials.h"

void PullConsumer::__construct(Php::Parameters &params){
    std::string groupName = params[0];
    this->consumer = new rocketmq::DefaultMQPullConsumer(groupName);
}

void PullConsumer::setGroup(Php::Parameters &params){
    std::string groupName = params[0];
    this->consumer->setGroupName(groupName);
}

void PullConsumer::start(){
    this->consumer->start();
}

Php::Value PullConsumer::getQueues(){
    Php::Array result;
    int idx = 0;
    this->consumer->fetchSubscribeMessageQueues(this->topicName, this->mqs);
    std::vector<rocketmq::MQMessageQueue>::iterator iter = mqs.begin();
    for (; iter != mqs.end(); ++iter) {
        rocketmq::MQMessageQueue mq = (*iter);
        result[idx++] = Php::Object(MESSAGE_QUEUE_CLASS_NAME , new MessageQueue(mq)); 
    }
    return result;
}

Php::Value PullConsumer::getNamesrvDomain(){
    return this->consumer->getNamesrvDomain();
}

void PullConsumer::setNamesrvDomain(Php::Parameters &param){
    std::string namesrv_domain = param[0];
    this->consumer->setNamesrvDomain(namesrv_domain);
}

Php::Value PullConsumer::getNamesrvAddr(){
    return this->consumer->getNamesrvAddr();
}

void PullConsumer::setNamesrvAddr(Php::Parameters &param){
    std::string namesrv_addr = param[0];
    this->consumer->setNamesrvAddr(namesrv_addr);
}

void PullConsumer::setInstanceName(Php::Parameters &param){
    std::string instanceName = param[0];
    this->consumer->setInstanceName(instanceName);
}

void PullConsumer::setTopic(Php::Parameters &param){
    std::string topic = param[0];
    this->topicName= topic;
}

// pull( MessageQueue mq, string subExpression, int offset, int maxNums)
Php::Value PullConsumer::pull(Php::Parameters &param){
    Php::Value mq = param[0];
    std::string subExpression = param[1];
    int64_t offset = param[2];
    int64_t maxNums = param[3];
    MessageQueue* messageQueue = (MessageQueue*)mq.implementation();
    rocketmq::PullResult result = this->consumer->pull(messageQueue->getInstance(), subExpression, offset, maxNums);
    PullResult *pullResult = new PullResult(result);
    Php::Value pv(Php::Object(PULL_RESULT_CLASS_NAME, pullResult));
    return pv;
}

/*
   void PullConsumer::persistConsumerOffset(){
   this->consumer->persistConsumerOffset();
   }

   void PullConsumer::persistConsumerOffsetByResetOffset(){
   this->consumer->persistConsumerOffsetByResetOffset();
   }
   */

Php::Value PullConsumer::pullBlockIfNotFound(Php::Parameters &param){
    Php::Value mq = param[0];
    std::string subExpression = param[1];
    int64_t offset = param[2];
    int64_t maxNums = param[3];
    MessageQueue* messageQueue = (MessageQueue*)mq.implementation();
    rocketmq::PullResult result = this->consumer->pullBlockIfNotFound(messageQueue->getInstance(), subExpression, offset, maxNums);
    PullResult *pullResult = new PullResult(result);
    Php::Value pv(Php::Object(PULL_RESULT_CLASS_NAME, pullResult));
    return pv;
}

void PullConsumer::setSessionCredentials(Php::Parameters &param){
    std::string accessKey = param[0];
    std::string secretKey = param[1];
    std::string authChannel = param[2];

    this->consumer->setSessionCredentials(accessKey, secretKey, authChannel);
}

Php::Value PullConsumer::getSessionCredentials(){
    rocketmq::SessionCredentials sc = this->consumer->getSessionCredentials();
    SessionCredentials *sessionCredentials = new SessionCredentials(&sc);
    Php::Value pv(Php::Object(SESSION_CREDENTIALS_CLASS_NAME , sessionCredentials));
    return pv;
}


void PullConsumer::updateConsumeOffset(Php::Parameters &params){
    Php::Value mq = params[0];
    int64_t offset = params[1];

    MessageQueue* messageQueue = (MessageQueue*)mq.implementation();
    this->consumer->updateConsumeOffset(messageQueue->getInstance(), offset);
}

void PullConsumer::removeConsumeOffset(Php::Parameters &params){
    Php::Value mq = params[0];
    MessageQueue* messageQueue = (MessageQueue*)mq.implementation();
    this->consumer->removeConsumeOffset(messageQueue->getInstance());
}

Php::Value PullConsumer::fetchConsumeOffset(Php::Parameters &params){
    Php::Value mq = params[0];
    bool fromStore = params[1];
    MessageQueue* messageQueue = (MessageQueue*)mq.implementation();
    return (int64_t)this->consumer->fetchConsumeOffset(messageQueue->getInstance(), fromStore);
}

Php::Value PullConsumer::getMessageModel(){
    return this->consumer->getMessageModel();
}
void PullConsumer::setMessageModel(Php::Parameters &params){
    this->consumer->setMessageModel(rocketmq::MessageModel((int)params[0]));
}

// void setTcpTransportPullThreadNum(int num);
void PullConsumer::setTcpTransportPullThreadNum(Php::Parameters &param){
    this->consumer->setTcpTransportPullThreadNum((int64_t)param[0]);
}

// const int getTcpTransportPullThreadNum() const;
Php::Value PullConsumer::getTcpTransportPullThreadNum(){
    return (int64_t)this->consumer->getTcpTransportPullThreadNum();
}

// void setTcpTransportConnectTimeout(uint64_t timeout);  // ms
void PullConsumer::setTcpTransportConnectTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportConnectTimeout((int64_t)param[0]);
}
// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value PullConsumer::getTcpTransportConnectTimeout(){
    return (int64_t)this->consumer->getTcpTransportConnectTimeout();
}

// void setTcpTransportTryLockTimeout(uint64_t timeout);  // ms
void PullConsumer::setTcpTransportTryLockTimeout(Php::Parameters &param){
    this->consumer->setTcpTransportTryLockTimeout((int64_t)param[0]);
}

// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value PullConsumer::getTcpTransportTryLockTimeout(){
    return (int64_t)this->consumer->getTcpTransportTryLockTimeout();
}

//void setUnitName(std::string unitName);
void PullConsumer::setUnitName(Php::Parameters &param){
    this->consumer->setUnitName(param[0]);
}
//const std::string& getUnitName();
Php::Value PullConsumer::getUnitName(){
    return this->consumer->getUnitName();
}

void PullConsumer::setLogLevel(Php::Parameters &param){
    this->consumer->setLogLevel(rocketmq::elogLevel((int)param[0]));
}

Php::Value PullConsumer::getLogLevel(){
    return this->consumer->getLogLevel();
}

void PullConsumer::setLogFileSizeAndNum(Php::Parameters &param){
    this->consumer->setLogFileSizeAndNum(param[0], param[1]);
}



void registerPullConsumer(Php::Namespace &rocketMQNamespace){
    Php::Class<PullConsumer> pullConsumer("PullConsumer");
    pullConsumer.method<&PullConsumer::__construct>("__construct", { Php::ByVal("groupName", Php::Type::String), });
    pullConsumer.method<&PullConsumer::setInstanceName>("setInstanceName", { Php::ByVal("instance", Php::Type::String), });
    pullConsumer.method<&PullConsumer::setTopic>("setTopic", { Php::ByVal("topic", Php::Type::String), });
    pullConsumer.method<&PullConsumer::start>("start");
    pullConsumer.method<&PullConsumer::getQueues>("getQueues");

    pullConsumer.method<&PullConsumer::setNamesrvAddr>("setNamesrvAddr", { Php::ByVal("namesrvAddr", Php::Type::String), });
    pullConsumer.method<&PullConsumer::getNamesrvAddr>("getNamesrvAddr");

    pullConsumer.method<&PullConsumer::setNamesrvDomain>("setNamesrvDomain", { Php::ByVal("nameserver", Php::Type::String), });
    pullConsumer.method<&PullConsumer::getNamesrvDomain>("getNamesrvDomain");

    pullConsumer.method<&PullConsumer::setGroup>("setGroup", { Php::ByVal("group", Php::Type::String), });
    pullConsumer.method<&PullConsumer::pull>("pull", {
            Php::ByVal("mq", MESSAGE_QUEUE_CLASS_NAME),
            Php::ByVal("subExpression", Php::Type::String),
            Php::ByVal("offset", Php::Type::Numeric),
            Php::ByVal("maxNums", Php::Type::Numeric),
            });

    pullConsumer.method<&PullConsumer::pullBlockIfNotFound>("pullBlockIfNotFound", {
            Php::ByVal("mq", MESSAGE_QUEUE_CLASS_NAME),
            Php::ByVal("subExpression", Php::Type::String),
            Php::ByVal("offset", Php::Type::Numeric),
            Php::ByVal("maxNums", Php::Type::Numeric),
            });

    pullConsumer.method<&PullConsumer::setSessionCredentials>("setSessionCredentials", {
            Php::ByVal("accessKey", Php::Type::String),
            Php::ByVal("secretKey", Php::Type::String),
            Php::ByVal("authChannel", Php::Type::String),
            });
    pullConsumer.method<&PullConsumer::getSessionCredentials>("getSessionCredentials");
    pullConsumer.method<&PullConsumer::updateConsumeOffset>("updateConsumeOffset", {
            Php::ByVal("mq", MESSAGE_QUEUE_CLASS_NAME),
            Php::ByVal("offset", Php::Type::Numeric),
            });
    pullConsumer.method<&PullConsumer::removeConsumeOffset>("removeConsumeOffset", { Php::ByVal("mq", MESSAGE_QUEUE_CLASS_NAME), });
    pullConsumer.method<&PullConsumer::fetchConsumeOffset>("fetchConsumeOffset", {
            Php::ByVal("mq", MESSAGE_QUEUE_CLASS_NAME),
            Php::ByVal("fromStore", Php::Type::Bool),
            });
    pullConsumer.method<&PullConsumer::getMessageModel>("getMessageModel");
    pullConsumer.method<&PullConsumer::setMessageModel>("setMessageModel", { Php::ByVal("messageModel", Php::Type::Numeric), });

    pullConsumer.method<&PullConsumer::getTcpTransportTryLockTimeout>("getTcpTransportTryLockTimeout");
    pullConsumer.method<&PullConsumer::setTcpTransportTryLockTimeout>("setTcpTransportTryLockTimeout",{ Php::ByVal("timeout", Php::Type::Numeric), });

    pullConsumer.method<&PullConsumer::getTcpTransportConnectTimeout>("getTcpTransportConnectTimeout");
    pullConsumer.method<&PullConsumer::setTcpTransportConnectTimeout>("setTcpTransportConnectTimeout", {Php::ByVal("timeout", Php::Type::Numeric), });

    pullConsumer.method<&PullConsumer::getTcpTransportPullThreadNum>("getTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });
    pullConsumer.method<&PullConsumer::setTcpTransportPullThreadNum>("setTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });

    pullConsumer.method<&PullConsumer::getUnitName>("getUnitName");
    pullConsumer.method<&PullConsumer::setUnitName>("setUnitName", {Php::ByVal("unitName", Php::Type::String),});

    pullConsumer.method<&PullConsumer::setLogLevel>("setLogLevel", {Php::ByVal("inputLevel", Php::Type::Numeric),});
    pullConsumer.method<&PullConsumer::getLogLevel>("getLogLevel");
    pullConsumer.method<&PullConsumer::setLogFileSizeAndNum>("setLogFileSizeAndNum", {Php::ByVal("fileNum", Php::Type::Numeric),Php::ByVal("perFileSize", Php::Type::Numeric),});



    rocketMQNamespace.add(pullConsumer);
}
