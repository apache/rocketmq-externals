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

#include "pull_result.h"		
#include "pull_result_iterator.h"

Php::Value PullResult::getMessage(Php::Parameters &param){
    Php::Value idx_val = param[0];
    int idx = idx_val;

    if (idx < (int)this->result.msgFoundList.size()){
        Php::Value msg(Php::Object(MESSAGE_EXT_CLASS_NAME, new MessageExt(this->result.msgFoundList[idx])));
        return msg;
    }
    return nullptr;
}

PullResult::PullResult(rocketmq::PullResult result){
    this->result = result;
}

Php::Value PullResult::getPullStatus(){
    return this->result.pullStatus;
}

Php::Value PullResult::getNextBeginOffset(){
    return (int64_t)this->result.nextBeginOffset;
}

Php::Value PullResult::getMinOffset(){
    return (int64_t)this->result.minOffset;
}

Php::Value PullResult::getMaxOffset(){
    return (int64_t)this->result.maxOffset;
}

long PullResult::count() { 
    return this->result.msgFoundList.size();
}

Php::Iterator* PullResult::getIterator(){
    return new PullResultIterator(this, this->result.msgFoundList);
}

void registerPullResult(Php::Namespace &rocketMQNamespace){
    Php::Class<PullResult> pullResultClass("PullResult");

    pullResultClass.method<&PullResult::getMessage>("getMessage", { Php::ByVal("index", Php::Type::Numeric), });
    pullResultClass.method<&PullResult::getPullStatus>("getPullStatus");
    pullResultClass.method<&PullResult::getNextBeginOffset>("getNextBeginOffset");
    pullResultClass.method<&PullResult::getMinOffset>("getMinOffset");
    pullResultClass.method<&PullResult::getMaxOffset>("getMaxOffset");

    rocketMQNamespace.add(pullResultClass);
}
