/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <condition_variable>
#include <iomanip>
#include <iomanip>
#include <iostream>
#include <iostream>
#include <mutex>
#include <thread>

#include "common.h"

using namespace rocketmq;

int main(int argc, char* argv[]) {
  RocketmqSendAndConsumerArgs info;
  if (!ParseArgs(argc, argv, &info)) {
    exit(-1);
  }
  PrintRocketmqSendAndConsumerArgs(info);
  DefaultMQProducer producer("please_rename_unique_group_name");
  producer.setNamesrvAddr(info.namesrv);
  producer.setNamesrvDomain(info.namesrv_domain);
  producer.setGroupName(info.groupname);
  producer.setInstanceName(info.groupname);

  producer.setSendMsgTimeout(500);
  producer.setTcpTransportTryLockTimeout(1000);
  producer.setTcpTransportConnectTimeout(400);

  producer.start();

  MQMessage msg(info.topic,  // topic
                "*",         // tag
                info.body);  // body

  // messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h
  // 2h
  msg.setDelayTimeLevel(5);  // 1m
  try {
    SendResult sendResult = producer.send(msg, info.SelectUnactiveBroker);
  } catch (const MQException& e) {
    std::cout << "send failed: " << std::endl;
  }

  producer.shutdown();
  return 0;
}
