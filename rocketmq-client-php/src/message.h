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

#ifndef ROCKETMQ_CLIENT_PHP_MESSAGE_H_
#define ROCKETMQ_CLIENT_PHP_MESSAGE_H_
#include "common.h"
#include <phpcpp.h>
#include <iostream>
#include <rocketmq/MQMessage.h>

#define MESSAGE_CLASS_NAME   NAMESPACE_NAME"\\Message"

class Message: public Php::Base {
    private:
        rocketmq::MQMessage message;
    public:
        Message(){}
        Message(rocketmq::MQMessage &message);

        // MQMessage constructor.
        virtual void __construct(Php::Parameters &params);

        rocketmq::MQMessage& getMQMessage();

        //void setProperty(const std::string& name, const std::string& value) ;
        void setProperty(Php::Parameters &params);

        //const std::string & getProperty(const std::string& name) const;
        Php::Value getProperty(Php::Parameters &params);

        // const std::string &getTopic() const;
        Php::Value getTopic();
        // void setTopic(const std::string& topic);
        void setTopic(Php::Parameters &params);

        //const std::string &getTags() const;
        Php::Value getTags();
        //void setTags(const std::string& tags);
        void setTags(Php::Parameters &params);

        //const std::string &getKeys() const;
        Php::Value getKeys();
        // void setKeys(const std::string& keys);
        void setKeys(Php::Parameters &params);

        // TODO
        //void setKeys(const std::vector<std::string>& keys);

        //int getDelayTimeLevel() const;
        Php::Value getDelayTimeLevel();

        //void setDelayTimeLevel(int level);
        void setDelayTimeLevel(Php::Parameters &params);

        //bool isWaitStoreMsgOK();
        Php::Value isWaitStoreMsgOK();

        // void setWaitStoreMsgOK(bool waitStoreMsgOK);
        void setWaitStoreMsgOK(Php::Parameters &params);

        //int getFlag() const;
        Php::Value getFlag();

        //void setFlag(int flag);
        void setFlag(Php::Parameters &params);

        //int getSysFlag() const;
        Php::Value getSysFlag();

        //void setSysFlag(int sysFlag);
        void setSysFlag(Php::Parameters &params);

        //const std::string &getBody() const;
        Php::Value getBody();

        //void setBody(const char* body, int len);
        //void setBody(const std::string& body);
        void setBody(Php::Parameters &params);

        // std::map<std::string, std::string> getProperties() const;
        Php::Value getProperties();

        //void setProperties(std::map<std::string, std::string>& properties);
        void setProperties(Php::Parameters &params);

        // const std::string toString() const
        Php::Value toString();
};
void registerMessage(Php::Namespace &rocketMQNamespace);

#endif

