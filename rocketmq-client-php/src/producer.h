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

#ifndef ROCKETMQ_CLIENT_PHP_PRODUCER_H_
#define ROCKETMQ_CLIENT_PHP_PRODUCER_H_
#include "common.h"
#include <rocketmq/DefaultMQProducer.h>

class Producer : public Php::Base
{
    private:
        rocketmq::DefaultMQProducer *producer;

    public:
        Producer() { }

        virtual ~Producer() { }

        void __construct(Php::Parameters &param);

        void setInstanceName(Php::Parameters &param);
        Php::Value getInstanceName();

        void setNamesrvAddr(Php::Parameters &param);
        Php::Value getNamesrvAddr();

        Php::Value getNamesrvDomain();
        void setNamesrvDomain(Php::Parameters &param);

        Php::Value getTopicMessageQueueInfo(Php::Parameters &param);

        Php::Value getMQClientId();

        void setGroupName(Php::Parameters &param);
        Php::Value getGroupName();

        void start();

        Php::Value send(Php::Parameters &params);

        void setSessionCredentials(Php::Parameters &param);
        Php::Value getSessionCredentials();

        Php::Value getRetryTimes();
        void setRetryTimes(Php::Parameters &param);

        //int getSendMsgTimeout() const;
        Php::Value getSendMsgTimeout();

        //void setSendMsgTimeout(int sendMsgTimeout);
        void setSendMsgTimeout(Php::Parameters &param);

        //int getCompressMsgBodyOverHowmuch() const;
        Php::Value getCompressMsgBodyOverHowmuch();
        //void setCompressMsgBodyOverHowmuch(int compressMsgBodyOverHowmuch);
        void setCompressMsgBodyOverHowmuch(Php::Parameters &param);

        //int getCompressLevel() const;
        Php::Value getCompressLevel();
        //void setCompressLevel(int compressLevel);
        void setCompressLevel(Php::Parameters &param);

        //int getMaxMessageSize() const;
        Php::Value getMaxMessageSize();
        //void setMaxMessageSize(int maxMessageSize);
        void setMaxMessageSize(Php::Parameters &param);

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
        
        virtual void __destruct(){
            if (this->producer != nullptr){
                producer->shutdown();
                delete(this->producer);
            }
        }
};

void registerProducer(Php::Namespace &rocketMQNamespace);

#endif
