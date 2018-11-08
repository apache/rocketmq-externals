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
#include <stdlib.h>
#include <string.h>

#include <chrono>
#include <condition_variable>
#include <iomanip>
#include <iostream>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#include "common.h"

std::mutex g_mtx;
std::condition_variable g_finished;
TpsReportService g_tps;

using namespace rocketmq;

class MyMsgListener : public MessageListenerConcurrently {
 public:
  MyMsgListener() {}
  virtual ~MyMsgListener() {}

  virtual ConsumeStatus consumeMessage(const std::vector<MQMessageExt> &msgs) {
    g_msgCount.store(g_msgCount.load() - msgs.size());
    for (size_t i = 0; i < msgs.size(); ++i) {
      g_tps.Increment();
      // cout << "msg body: "<<  msgs[i].getBody() << endl;
    }

    if (g_msgCount.load() <= 0) {
      std::unique_lock<std::mutex> lck(g_mtx);
      g_finished.notify_one();
    }
    return CONSUME_SUCCESS;
  }
};

int main(int argc, char *argv[]) {
  RocketmqSendAndConsumerArgs info;
  if (!ParseArgs(argc, argv, &info)) {
    exit(-1);
  }
  PrintRocketmqSendAndConsumerArgs(info);
  DefaultMQPushConsumer consumer("please_rename_unique_group_name");
  DefaultMQProducer producer("please_rename_unique_group_name");
  producer.setSessionCredentials("AccessKey",
                                 "SecretKey", "ALIYUN");
  producer.setTcpTransportTryLockTimeout(1000);
  producer.setTcpTransportConnectTimeout(400);
  producer.setNamesrvDomain(info.namesrv_domain);
  producer.setNamesrvAddr(info.namesrv);
  producer.setGroupName("msg-persist-group_producer_sandbox");
  producer.start();

  consumer.setNamesrvAddr(info.namesrv);
  consumer.setGroupName(info.groupname);
  consumer.setSessionCredentials("AccessKey",
                                 "SecretKey", "ALIYUN");
  consumer.setConsumeThreadCount(info.thread_count);
  consumer.setNamesrvDomain(info.namesrv_domain);
  consumer.setConsumeFromWhere(CONSUME_FROM_LAST_OFFSET);

  if (info.syncpush) consumer.setAsyncPull(false);  // set sync pull
  if (info.broadcasting) {
    consumer.setMessageModel(BROADCASTING);
  }

  consumer.setInstanceName(info.groupname);

  consumer.subscribe(info.topic, "*");
  consumer.setConsumeThreadCount(15);
  consumer.setTcpTransportTryLockTimeout(1000);
  consumer.setTcpTransportConnectTimeout(400);

  MyMsgListener msglistener;
  consumer.registerMessageListener(&msglistener);

  try {
    consumer.start();
  } catch (MQClientException &e) {
    cout << e << endl;
  }
  g_tps.start();

  int msgcount = g_msgCount.load();
  for (int i = 0; i < msgcount; ++i) {
    MQMessage msg(info.topic,  // topic
                  "*",         // tag
                  info.body);  // body

    //    std::this_thread::sleep_for(std::chrono::seconds(100000));
    try {
      producer.send(msg);
    } catch (MQException &e) {
      std::cout << e << endl;  // if catch excepiton , need re-send this msg by
                               // service
    }
  }

  {
    std::unique_lock<std::mutex> lck(g_mtx);
    g_finished.wait(lck);
  }
  producer.shutdown();
  consumer.shutdown();
  return 0;
}
