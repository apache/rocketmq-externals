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

#ifndef ROCKETMQ_CLIENT_PHP_SESSION_CREDENTIALS_H_
#define ROCKETMQ_CLIENT_PHP_SESSION_CREDENTIALS_H_
#include "common.h"
#include <rocketmq/SessionCredentials.h>

#define SESSION_CREDENTIALS_CLASS_NAME NAMESPACE_NAME"\\SessionCredentials"

class SessionCredentials : public Php::Base {
    rocketmq::SessionCredentials* sc;

    public:

    SessionCredentials(rocketmq::SessionCredentials* sc){
        this->sc = sc;
    }

    void __construct(Php::Parameters &params);
    void __destruct();

    Php::Value getAccessKey();
    void setAccessKey(Php::Parameters &params);

    Php::Value getSecretKey();
    void setSecretKey(Php::Parameters &params);

    Php::Value getSignature();
    void setSignature(Php::Parameters &params);

    Php::Value getSignatureMethod();
    void setSignatureMethod(Php::Parameters &params);

    Php::Value getAuthChannel();
    void setAuthChannel(Php::Parameters &params);

    Php::Value isValid();
};

void registerSessionCredentials(Php::Namespace &rocketMQNamespace);
#endif
