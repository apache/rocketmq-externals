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

#include "message.h"

Message::Message(rocketmq::MQMessage &message){
    this->message = message;
}

void Message::__construct(Php::Parameters &params){
    std::string topic = params[0];
    std::string tags = params[1];
    if (params.size() == 2){
        this->message = rocketmq::MQMessage(topic, tags);
    }else if (params.size() == 3){
        std::string body = params[2];
        this->message = rocketmq::MQMessage(topic, tags, body);
    }else if (params.size() == 4){
        std::string body = params[2];
        std::string keys = params[3];
        this->message = rocketmq::MQMessage(topic, tags, keys, body);
    }/*else if (params.size() == 6){
       std::string keys = params[2];
       std::string body = params[4];
       this->message = Message(topic, tags, keys, (int)params[3], body, params[5]);
       }*/
}


rocketmq::MQMessage& Message::getMQMessage(){
    return this->message;
}

//void setProperty(const std::string& name, const std::string& value) ;
void Message::setProperty(Php::Parameters &params){
    std::string name = params[0];
    std::string value = params[1];

    this->message.setProperty(name, value);
}

// const std::string &getTopic() const;
Php::Value Message::getTopic(){
    return this->message.getTopic();
}

// void setTopic(const std::string& topic);
void Message::setTopic(Php::Parameters &params){
    std::string topic = params[0];
    this->message.setTopic(topic);
}


//const std::string &getTags() const;
Php::Value Message::getTags(){
    return this->message.getTags();
}

void Message::setTags(Php::Parameters &params){
    std::string tags = params[0];
    return this->message.setTags(tags);
}

//const std::string &getKeys() const;
Php::Value Message::getKeys(){
    return this->message.getKeys();
}

// void setKeys(const std::string& keys);
void Message::setKeys(Php::Parameters &params){
    std::string keys = params[0];
    this->message.setKeys(keys);
}

Php::Value Message::getDelayTimeLevel(){
    return this->message.getDelayTimeLevel();
}

void Message::setDelayTimeLevel(Php::Parameters &params){
    this->message.setDelayTimeLevel(params[0]);
}

Php::Value Message::isWaitStoreMsgOK(){
    return this->message.isWaitStoreMsgOK();
}

// void setWaitStoreMsgOK(bool waitStoreMsgOK);
void Message::setWaitStoreMsgOK(Php::Parameters &params){
    this->message.setWaitStoreMsgOK(params[0]);
}

//int getFlag() const;
Php::Value Message::getFlag(){
    return this->message.getFlag();
}

//void setFlag(int flag);
void Message::setFlag(Php::Parameters &params){
    this->message.setFlag(params[0]);
}

//int getSysFlag() const;
Php::Value Message::getSysFlag(){
    return this->message.getSysFlag();
}

//void setSysFlag(int sysFlag);
void Message::setSysFlag(Php::Parameters &params){
    this->message.setSysFlag(params[0]);
}

//const std::string &getBody() const;
Php::Value Message::getBody(){
    return this->message.getBody();
}

//void setBody(const char* body, int len);
//void setBody(const std::string& body);
void Message::setBody(Php::Parameters &params){
    std::string body = params[0];
    this->message.setBody(body);
}


// std::map<std::string, std::string> getProperties() const;
Php::Value Message::getProperties(){
    Php::Array result;
    std::map<std::string, std::string>::iterator iter = this->message.getProperties().begin();

    for (; iter != this->message.getProperties().end(); iter ++){
        result[iter->first] = result[iter->second];
    }

    return result;
}

//void setProperties(std::map<std::string, std::string>& properties);
void Message::setProperties(Php::Parameters &params){
    std::map<std::string, std::string> properties = params[0];
    this->message.setProperties(properties);
}

// const std::string toString() const
Php::Value Message::toString(){
    return this->message.toString();
}

//const std::string & getProperty(const std::string& name) const;
Php::Value Message::getProperty(Php::Parameters &params){
    std::string name = params[0];
    return this->message.getProperty(name);
}


void registerMessage(Php::Namespace &rocketMQNamespace){
    Php::Class<Message> messageClass("Message");

    messageClass.method<&Message::__construct>("__construct", {
            Php::ByVal("topic", Php::Type::String),
            Php::ByVal("tags", Php::Type::String),
            Php::ByVal("body", Php::Type::String, false),
            Php::ByVal("keys", Php::Type::String, false),
            });

    messageClass.method<&Message::setProperty>("setProperty", {
            Php::ByVal("name", Php::Type::String),
            Php::ByVal("value", Php::Type::String),
            });
    messageClass.method<&Message::getProperty>("getProperty", { Php::ByVal("name", Php::Type::String), });

    messageClass.method<&Message::getTopic>("getTopic");
    messageClass.method<&Message::setTopic>("setTopic", { Php::ByVal("topic", Php::Type::String), });

    messageClass.method<&Message::getTags>("getTags");
    messageClass.method<&Message::setTags>("setTags", { Php::ByVal("tags", Php::Type::String), });

    messageClass.method<&Message::getKeys>("getKeys"); 
    messageClass.method<&Message::setKeys>("setKeys", { Php::ByVal("keys", Php::Type::String), });

    messageClass.method<&Message::getDelayTimeLevel>("getDelayTimeLevel");
    messageClass.method<&Message::setDelayTimeLevel>("setDelayTimeLevel", { Php::ByVal("delayTimeLevel", Php::Type::Numeric), });

    messageClass.method<&Message::isWaitStoreMsgOK>("isWaitStoreMsgOK");
    messageClass.method<&Message::setWaitStoreMsgOK>("setWaitStoreMsgOK", { Php::ByVal("waitStoreMsgOK", Php::Type::Bool), });

    messageClass.method<&Message::getFlag>("getFlag");
    messageClass.method<&Message::setFlag>("setFlag", { Php::ByVal("flag", Php::Type::Numeric), });

    messageClass.method<&Message::getSysFlag>("getSysFlag");
    messageClass.method<&Message::setSysFlag>("setSysFlag", { Php::ByVal("sysFlag", Php::Type::Numeric), });

    messageClass.method<&Message::getBody>("getBody");
    messageClass.method<&Message::setBody>("setBody", { Php::ByVal("body", Php::Type::String), });

    messageClass.method<&Message::getProperties>("getProperties");
    messageClass.method<&Message::setProperties>("setProperties", { Php::ByVal("properties", Php::Type::Array), });

    messageClass.method<&Message::toString>("toString");


    rocketMQNamespace.add(messageClass);
}
