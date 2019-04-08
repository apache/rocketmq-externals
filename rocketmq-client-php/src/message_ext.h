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

#ifndef ROCKETMQ_CLIENT_PHP_MESSAGE_EXT_H_
#define ROCKETMQ_CLIENT_PHP_MESSAGE_EXT_H_

#include "common.h"
#include "message.h"
#include <phpcpp.h>
#include <iostream>
#include <rocketmq/MQMessageExt.h>

#define MESSAGE_EXT_CLASS_NAME NAMESPACE_NAME"\\MessageExt"

class MessageExt: public Php::Base {
    private:
        rocketmq::MQMessageExt messageExt;

    public:
        MessageExt(rocketmq::MQMessageExt message){
            this->messageExt = message;
        }

        // static int parseTopicFilterType(int sysFlag);
        Php::Value parseTopicFilterType(Php::Parameters &params){
            return this->messageExt.parseTopicFilterType(params[0]);
        }

        // int getQueueId() const;
        Php::Value getQueueId(){
            return this->messageExt.getQueueId();
        }

        // void setQueueId(int queueId);
        void setQueueId(Php::Parameters &params){
            this->messageExt.setQueueId(params[0]);
        }

        //int64 getBornTimestamp() const;
        Php::Value getBornTimestamp(){
            return (int64_t)this->messageExt.getBornTimestamp();
        }

        //void setBornTimestamp(int64 bornTimestamp);
        void setBornTimestamp(Php::Parameters &param){
            this->messageExt.setBornTimestamp((int64_t)param[0]);
        }

        // sockaddr getStoreHost() const;
        // std::string getStoreHostString() const;
        Php::Value getStoreHostString(){
            return this->messageExt.getStoreHostString();
        }

        //void setStoreHost(const sockaddr& storeHost);
        // TODO
        //void setStoreHost(Php::Parameters &params){ }

        //const std::string& getMsgId() const;
        Php::Value getMsgId(){
            return this->messageExt.getMsgId();
        }

        //void setMsgId(const std::string& msgId);
        void setMsgId(Php::Parameters &params){
            std::string msgId = params[0];
            return this->messageExt.setMsgId(msgId);
        }

        //const std::string& getOffsetMsgId() const;
        Php::Value getOffsetMsgId(){
            return this->messageExt.getOffsetMsgId();
        }

        //void setOffsetMsgId(const std::string& offsetMsgId);
        void setOffsetMsgId(Php::Parameters &params){
            std::string offsetMsgId =params[0];
            this->messageExt.setOffsetMsgId(offsetMsgId);
        }

        //int getBodyCRC() const;
        Php::Value getBodyCRC(){
            return this->messageExt.getBodyCRC();
        }

        //void setBodyCRC(int bodyCRC);
        void setBodyCRC(Php::Parameters &params){
            this->messageExt.setBodyCRC(params[0]);
        }

        //int64 getQueueOffset() const;
        Php::Value getQueueOffset(){
            return (int64_t)this->messageExt.getQueueOffset();
        }

        //void setQueueOffset(int64 queueOffset);
        void setQueueOffset(Php::Parameters &params){
            this->messageExt.setQueueOffset((int64_t)params[0]);
        }

        //int64 getCommitLogOffset() const;
        Php::Value getCommitLogOffset(){
            return (int64_t)this->messageExt.getCommitLogOffset();
        }

        //void setCommitLogOffset(int64 physicOffset);
        void setCommitLogOffset(Php::Parameters &params){
            this->messageExt.setCommitLogOffset((int64_t)params[0]);
        }

        //int getStoreSize() const;
        Php::Value getStoreSize(){
            return this->messageExt.getStoreSize();
        }

        //void setStoreSize(int storeSize);
        void setStoreSize(Php::Parameters &params){
            this->messageExt.setStoreSize(params[0]);
        }

        //int getReconsumeTimes() const;
        Php::Value getReconsumeTimes(){
            return this->messageExt.getReconsumeTimes();
        }

        //void setReconsumeTimes(int reconsumeTimes);
        void setReconsumeTimes(Php::Parameters &params){
            this->messageExt.setReconsumeTimes(params[0]);
        }

        //int64 getPreparedTransactionOffset() const;
        Php::Value getPreparedTransactionOffset(){
            return (int64_t)this->messageExt.getPreparedTransactionOffset();
        }

        //void setPreparedTransactionOffset(int64 preparedTransactionOffset);
        void setPreparedTransactionOffset(Php::Parameters &params){
            this->messageExt.setPreparedTransactionOffset((int64_t)params[0]);
        }

        //		std::string toString();
        Php::Value toString(){
            return this->messageExt.toString();
        }

        Php::Value getMessage(){
            rocketmq::MQMessage msg = (rocketmq::MQMessage)this->messageExt;
            Php::Value message(Php::Object(MESSAGE_CLASS_NAME, new Message(msg)));
            return message;
        }
};

void registerMessageExt(Php::Namespace &rocketMQNamespace);
#endif
