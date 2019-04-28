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

#ifndef ROCKETMQ_CLIENT_PHP_PULL_STATUS_H_
#define ROCKETMQ_CLIENT_PHP_PULL_STATUS_H_

#include "common.h"

class PullStatus : public Php::Base
{
    //  FOUND,
    //  NO_NEW_MSG,
    //  NO_MATCHED_MSG,
    //  OFFSET_ILLEGAL,
    //  BROKER_TIMEOUT  // indicate pull request timeout or received NULL response
};

void registerPullStatus(Php::Namespace &rocketMQNamespace){
    // class PullStatus
    Php::Class<PullStatus> pullStatusClass("PullStatus");
    pullStatusClass.constant("FOUND", (int)rocketmq::FOUND);
    pullStatusClass.constant("NO_MATCHED_MSG", (int)rocketmq::NO_MATCHED_MSG);
    pullStatusClass.constant("OFFSET_ILLEGAL", (int)rocketmq::OFFSET_ILLEGAL);
    pullStatusClass.constant("BROKER_TIMEOUT", (int)rocketmq::BROKER_TIMEOUT);
    pullStatusClass.constant("NO_NEW_MSG", (int)rocketmq::NO_NEW_MSG);

    rocketMQNamespace.add(pullStatusClass);
}
#endif
