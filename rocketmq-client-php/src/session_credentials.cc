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

#include "session_credentials.h"

void SessionCredentials::__construct(Php::Parameters &params){
    std::string accessKey = params[0];
    std::string secretKey = params[1];
    std::string authChannel = params[2];

    if (sc != nullptr){
        this->sc = new rocketmq::SessionCredentials(accessKey, secretKey, authChannel);
    }else{
        this->sc->setAccessKey(accessKey);
        this->sc->setSecretKey(secretKey);
        this->sc->setAuthChannel(authChannel);
    }
}
void SessionCredentials::__destruct(){
    delete(this->sc);
}

Php::Value SessionCredentials::getAccessKey(){
    return this->sc->getAccessKey();
}
void SessionCredentials::setAccessKey(Php::Parameters &params){
    std::string accessKey = params[0];

    return this->sc->setAccessKey(accessKey);
}

Php::Value SessionCredentials::getSecretKey(){
    return this->sc->getAccessKey();
}
void SessionCredentials::setSecretKey(Php::Parameters &params){
    std::string secretKey = params[0];
    this->sc->setSecretKey(secretKey);
}

Php::Value SessionCredentials::getSignature(){
    return this->sc->getSignature();
}
void SessionCredentials::setSignature(Php::Parameters &params){
    std::string signature = params[0];
    this->sc->setSignature(signature);
}

Php::Value SessionCredentials::getSignatureMethod(){
    return this->sc->getSignatureMethod();
}
void SessionCredentials::setSignatureMethod(Php::Parameters &params){
    std::string signatureMethod = params[0];
    this->sc->setSignatureMethod(signatureMethod);
}

Php::Value SessionCredentials::getAuthChannel(){
    return this->sc->getAuthChannel();
}
void SessionCredentials::setAuthChannel(Php::Parameters &params){
    std::string authChannel = params[0];
    this->sc->setAuthChannel(authChannel);
}

Php::Value SessionCredentials::isValid(){
    return this->sc->isValid();
}

void registerSessionCredentials(Php::Namespace &rocketMQNamespace){
    Php::Class<SessionCredentials> scClass("SessionCredentials");

    scClass.method<&SessionCredentials::__construct>("__construct", {
            Php::ByVal("accessKey", Php::Type::String, false),
            Php::ByVal("secretKey", Php::Type::String, false),
            Php::ByVal("authChannel", Php::Type::String, false),
            });
    scClass.method<&SessionCredentials::__destruct>("__destruct");

    scClass.method<&SessionCredentials::getAccessKey>("getAccessKey");
    scClass.method<&SessionCredentials::setAccessKey>("setAccessKey",{
            Php::ByVal("accessKey", Php::Type::String),
            });

    scClass.method<&SessionCredentials::getSecretKey>("getSecretKey");
    scClass.method<&SessionCredentials::setSecretKey>("setSecretKey", {
            Php::ByVal("secretKey", Php::Type::String),
            });

    scClass.method<&SessionCredentials::getSignature>("getSignature");
    scClass.method<&SessionCredentials::setSignature>("setSignature", {
            Php::ByVal("signature", Php::Type::String),
            });

    scClass.method<&SessionCredentials::getSignatureMethod>("getSignatureMethod");
    scClass.method<&SessionCredentials::setSignatureMethod>("setSignatureMethod",{
            Php::ByVal("signatureMethod", Php::Type::String),
            });

    scClass.method<&SessionCredentials::getAuthChannel>("getAuthChannel");
    scClass.method<&SessionCredentials::setAuthChannel>("setAuthChannel", {
            Php::ByVal("authChannel", Php::Type::String),
            });

    scClass.method<&SessionCredentials::isValid>("isValid");

    rocketMQNamespace.add(scClass);
}
