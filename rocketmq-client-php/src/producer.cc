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

#include "producer.h"
#include "message_queue.h"
#include "session_credentials.h"
#include "send_result.h"

void Producer::__construct(Php::Parameters &param){
    std::string groupName = param[0];
    this->producer = new rocketmq::DefaultMQProducer(groupName);
}

void Producer::setInstanceName(Php::Parameters &param){
    std::string instanceName = param[0];
    this->producer->setInstanceName(instanceName);
}

Php::Value Producer::getInstanceName(){
    return this->producer->getInstanceName();
}


void Producer::setGroupName(Php::Parameters &param){
    std::string groupName = param[0];
    this->producer->setGroupName(groupName);
}

Php::Value Producer::getGroupName(){
    return this->producer->getGroupName();
}

void Producer::setNamesrvAddr(Php::Parameters &param){
    std::string nameserver =  param[0];
    this->producer->setNamesrvAddr(nameserver);
}

void Producer::start(){
    this->producer->start();
}

// SendResult send(MQMessage& msg, const MQMessageQueue& mq);
Php::Value Producer::send(Php::Parameters &params){
    Php::Value pvMessage = params[0];
    Message *message = (Message *)pvMessage.implementation();
    if (params.size() == 1){
        rocketmq::SendResult sr = this->producer->send(message->getMQMessage());
        Php::Value pv(Php::Object(SEND_RESULT_CLASS_NAME, new SendResult(sr)));
        return pv;
    }

    Php::Value pvMessageQueue = params[1];
    MessageQueue* messageQueue = (MessageQueue*)pvMessageQueue.implementation();
    rocketmq::SendResult sr = this->producer->send(message->getMQMessage(), messageQueue->getInstance());
    Php::Value pv(Php::Object(SEND_RESULT_CLASS_NAME, new SendResult(sr)));
    return pv;
}

Php::Value Producer::getMQClientId(){
    return this->producer->getMQClientId();
}

Php::Value Producer::getNamesrvAddr(){
    return this->producer->getNamesrvAddr();
}

Php::Value Producer::getTopicMessageQueueInfo(Php::Parameters &params){
    std::string topic = params[0];
    Php::Array result;

    std::vector<rocketmq::MQMessageQueue> mqs = this->producer->getTopicMessageQueueInfo(topic);
    std::vector<rocketmq::MQMessageQueue>::iterator iter = mqs.begin();
    int idx = 0;

    for (; iter != mqs.end(); ++iter) {
        rocketmq::MQMessageQueue mq = (*iter);
        result[idx++] = Php::Object(MESSAGE_QUEUE_CLASS_NAME , new MessageQueue(mq)); 
    }

    return result;
}

void Producer::setSessionCredentials(Php::Parameters &param){
    std::string accessKey = param[0];
    std::string secretKey = param[1];
    std::string authChannel = param[2];

    this->producer->setSessionCredentials(accessKey, secretKey, authChannel);
}

Php::Value Producer::getSessionCredentials(){
    rocketmq::SessionCredentials sc = this->producer->getSessionCredentials();
    SessionCredentials *sessionCredentials = new SessionCredentials(&sc);
    Php::Value pv(Php::Object(SESSION_CREDENTIALS_CLASS_NAME , sessionCredentials));
    return pv;
}

void Producer::setNamesrvDomain(Php::Parameters &param){
    std::string domain = param[0];
    this->producer->setNamesrvDomain(domain);
}

Php::Value Producer::getNamesrvDomain(){
    return this->producer->getNamesrvDomain();
}

// int getRetryTimes() const;
Php::Value Producer::getRetryTimes(){
    return this->producer->getRetryTimes();
}

// void setRetryTimes(int times);
void Producer::setRetryTimes(Php::Parameters &param){
    this->producer->setRetryTimes(param[0]);
}


//int getSendMsgTimeout() const;
Php::Value Producer::getSendMsgTimeout(){
    return this->producer->getSendMsgTimeout();
}

//void setSendMsgTimeout(int sendMsgTimeout);
void Producer::setSendMsgTimeout(Php::Parameters &param){
    this->producer->setSendMsgTimeout(param[0]);
}

//int getCompressMsgBodyOverHowmuch() const;
Php::Value Producer::getCompressMsgBodyOverHowmuch(){
    return this->producer->getCompressMsgBodyOverHowmuch();
}

//void setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch);
void Producer::setCompressMsgBodyOverHowmuch(Php::Parameters &param){
    this->producer->setCompressMsgBodyOverHowmuch(param[0]);
}

//int getCompressLevel() const;
Php::Value Producer::getCompressLevel(){
    return this->producer->getCompressLevel();
}

//void setCompressLevel(int compressLevel);
void Producer::setCompressLevel(Php::Parameters &param){
    this->producer->setCompressLevel(param[0]);
}

//int getMaxMessageSize() const;
Php::Value Producer::getMaxMessageSize(){
    return this->producer->getMaxMessageSize();
}

//void setMaxMessageSize(int maxMessageSize);
void Producer::setMaxMessageSize(Php::Parameters &param){
    this->producer->setMaxMessageSize(param[0]);
}

// void setTcpTransportPullThreadNum(int num);
void Producer::setTcpTransportPullThreadNum(Php::Parameters &param){
    this->producer->setTcpTransportPullThreadNum((int64_t)param[0]);
}

// const int getTcpTransportPullThreadNum() const;
Php::Value Producer::getTcpTransportPullThreadNum(){
    return (int64_t)this->producer->getTcpTransportPullThreadNum();
}

// void setTcpTransportConnectTimeout(uint64_t timeout);  // ms
void Producer::setTcpTransportConnectTimeout(Php::Parameters &param){
    this->producer->setTcpTransportConnectTimeout((int64_t)param[0]);
}
// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value Producer::getTcpTransportConnectTimeout(){
    return (int64_t)this->producer->getTcpTransportConnectTimeout();
}

// void setTcpTransportTryLockTimeout(uint64_t timeout);  // ms
void Producer::setTcpTransportTryLockTimeout(Php::Parameters &param){
    this->producer->setTcpTransportTryLockTimeout((int64_t)param[0]);
}

// const uint64_t getTcpTransportConnectTimeout() const;
Php::Value Producer::getTcpTransportTryLockTimeout(){
    return (int64_t)this->producer->getTcpTransportTryLockTimeout();
}

//void setUnitName(std::string unitName);
void Producer::setUnitName(Php::Parameters &param){
    this->producer->setUnitName(param[0]);
}
//const std::string& getUnitName();
Php::Value Producer::getUnitName(){
    return this->producer->getUnitName();
}

void Producer::setLogLevel(Php::Parameters &param){
    this->producer->setLogLevel(rocketmq::elogLevel((int)param[0]));
}

Php::Value Producer::getLogLevel(){
    return this->producer->getLogLevel();
}

void Producer::setLogFileSizeAndNum(Php::Parameters &param){
    this->producer->setLogFileSizeAndNum(param[0], param[1]);
}

void registerProducer(Php::Namespace &rocketMQNamespace){
    Php::Class<Producer> producerClass("Producer");

    producerClass.method<&Producer::getMQClientId>("getMQClientId");

    producerClass.method<&Producer::__construct>("__construct", { Php::ByVal("groupName", Php::Type::String), });
    producerClass.method<&Producer::__destruct>("__desatruct");

    producerClass.method<&Producer::getInstanceName>("getInstanceName");
    producerClass.method<&Producer::setInstanceName>("setInstanceName", { Php::ByVal("groupName", Php::Type::String), });

    producerClass.method<&Producer::getNamesrvAddr>("getNamesrvAddr");
    producerClass.method<&Producer::setNamesrvAddr>("setNamesrvAddr", { Php::ByVal("nameserver", Php::Type::String), });

    producerClass.method<&Producer::setNamesrvDomain>("setNamesrvDomain", { Php::ByVal("domain", Php::Type::String), });
    producerClass.method<&Producer::getNamesrvDomain>("getNamesrvDomain");

    producerClass.method<&Producer::getGroupName>("getGroupName");
    producerClass.method<&Producer::setGroupName>("setGroupName", { Php::ByVal("groupName", Php::Type::String), });

    producerClass.method<&Producer::start>("start");
    producerClass.method<&Producer::send>("send", { Php::ByVal("message", MESSAGE_CLASS_NAME), });

    producerClass.method<&Producer::getSessionCredentials>("getSessionCredentials");
    producerClass.method<&Producer::setSessionCredentials>("setSessionCredentials", {
            Php::ByVal("accessKey", Php::Type::String),
            Php::ByVal("secretKey", Php::Type::String),
            Php::ByVal("authChannel", Php::Type::String),
            });

    producerClass.method<&Producer::getTopicMessageQueueInfo>("getTopicMessageQueueInfo", { Php::ByVal("topic", Php::Type::String), });


    producerClass.method<&Producer::setRetryTimes>("setRetryTimes", { Php::ByVal("retryTimes", Php::Type::Numeric), });
    producerClass.method<&Producer::getRetryTimes>("getRetryTimes");

    producerClass.method<&Producer::getSendMsgTimeout>("getSendMsgTimeout");
    producerClass.method<&Producer::setSendMsgTimeout>("setSendMsgTimeout", {Php::ByVal("sendMsgTimeout", Php::Type::Numeric),});

    producerClass.method<&Producer::getCompressMsgBodyOverHowmuch>("getCompressMsgBodyOverHowmuch");
    producerClass.method<&Producer::setCompressMsgBodyOverHowmuch>("setCompressMsgBodyOverHowmuch", {Php::ByVal("compressMsgBodyOverHowmuch", Php::Type::Numeric),});

    producerClass.method<&Producer::getCompressLevel>("getCompressLevel");
    producerClass.method<&Producer::setCompressLevel>("setCompressLevel", {Php::ByVal("compressLevel", Php::Type::Numeric),});

    producerClass.method<&Producer::getMaxMessageSize>("getMaxMessageSize");
    producerClass.method<&Producer::setMaxMessageSize>("setMaxMessageSize", {Php::ByVal("maxMessageSize", Php::Type::Numeric),});

    producerClass.method<&Producer::getTcpTransportTryLockTimeout>("getTcpTransportTryLockTimeout");
    producerClass.method<&Producer::setTcpTransportTryLockTimeout>("setTcpTransportTryLockTimeout",{ Php::ByVal("timeout", Php::Type::Numeric), });

    producerClass.method<&Producer::getTcpTransportConnectTimeout>("getTcpTransportConnectTimeout");
    producerClass.method<&Producer::setTcpTransportConnectTimeout>("setTcpTransportConnectTimeout", {Php::ByVal("timeout", Php::Type::Numeric), });

    producerClass.method<&Producer::getTcpTransportPullThreadNum>("getTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });
    producerClass.method<&Producer::setTcpTransportPullThreadNum>("setTcpTransportPullThreadNum", {Php::ByVal("threadNum", Php::Type::Numeric), });

    producerClass.method<&Producer::getUnitName>("getUnitName");
    producerClass.method<&Producer::setUnitName>("setUnitName", {Php::ByVal("unitName", Php::Type::String),});

    producerClass.method<&Producer::setLogLevel>("setLogLevel", {Php::ByVal("inputLevel", Php::Type::Numeric),});
    producerClass.method<&Producer::getLogLevel>("getLogLevel");
    producerClass.method<&Producer::setLogFileSizeAndNum>("setLogFileSizeAndNum", {Php::ByVal("fileNum", Php::Type::Numeric),Php::ByVal("perFileSize", Php::Type::Numeric),});

    rocketMQNamespace.add(producerClass);
}


