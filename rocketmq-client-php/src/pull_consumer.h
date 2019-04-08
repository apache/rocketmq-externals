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

#ifndef ROCKETMQ_CLIENT_PHP_PULL_CONSUMER_H_
#define ROCKETMQ_CLIENT_PHP_PULL_CONSUMER_H_

#include "common.h"
#include <rocketmq/DefaultMQPullConsumer.h>
#include "message.h"
#include "message_queue.h"


class PullConsumer:public Php::Base
{
    private:
        std::string topicName;
        rocketmq::DefaultMQPullConsumer *consumer;
        std::vector<rocketmq::MQMessageQueue> mqs;
    public:
        PullConsumer(){
            this->consumer = nullptr;
        }
        virtual ~PullConsumer(){
            if (nullptr != this->consumer){
                delete(this->consumer);
            }
        }
        virtual void __construct(Php::Parameters &params);

        void start();

        Php::Value getQueues();

        Php::Value getNamesrvDomain();
        void setNamesrvDomain(Php::Parameters &param);

        Php::Value getNamesrvAddr();
        void setNamesrvAddr(Php::Parameters &param);

        void setInstanceName(Php::Parameters &param);

        void setTopic(Php::Parameters &param);

        void setGroup(Php::Parameters &param);

        Php::Value pull(Php::Parameters &param);
        Php::Value pullBlockIfNotFound(Php::Parameters &param);

        void setSessionCredentials(Php::Parameters &param);
        Php::Value getSessionCredentials();

        void updateConsumeOffset(Php::Parameters &params);
        void removeConsumeOffset(Php::Parameters &params);

        Php::Value fetchConsumeOffset(Php::Parameters &params);

        Php::Value getMessageModel();
        void setMessageModel(Php::Parameters &params);

        // void setTcpTransportPullThreadNum(int num);
        void setTcpTransportPullThreadNum(Php::Parameters &param);

        // const int getTcpTransportPullThreadNum() const;
        Php::Value getTcpTransportPullThreadNum();

        // void setTcpTransportConnectTimeout(uint64_t timeout);  // ms
        void setTcpTransportConnectTimeout(Php::Parameters &param);
        // const uint64_t getTcpTransportConnectTimeout() const;
        Php::Value getTcpTransportConnectTimeout();

        // void setTcpTransportTryLockTimeout(uint64_t timeout);  // ms
        void setTcpTransportTryLockTimeout(Php::Parameters &param);
        // const uint64_t getTcpTransportConnectTimeout() const;
        Php::Value getTcpTransportTryLockTimeout();

        //void setUnitName(std::string unitName);
        void setUnitName(Php::Parameters &param);
        //const std::string& getUnitName();
        Php::Value getUnitName();

        //void setLogLevel(elogLevel inputLevel);
        void setLogLevel(Php::Parameters &param);
        //ELogLevel getLogLevel();
        Php::Value getLogLevel();
        //void setLogFileSizeAndNum(int fileNum, long perFileSize);  // perFileSize is MB unit
        void setLogFileSizeAndNum(Php::Parameters &param);
};

void registerPullConsumer(Php::Namespace &rocketMQNamespace);

#endif
