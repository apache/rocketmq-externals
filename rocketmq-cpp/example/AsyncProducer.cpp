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

#include <chrono>
#include <condition_variable>
#include <iomanip>
#include <iostream>
#include <mutex>
#include <thread>

#include "common.h"

using namespace rocketmq;

boost::atomic<bool> g_quit;
std::mutex g_mtx;
std::condition_variable g_finished;
SendCallback* g_callback = NULL;
TpsReportService g_tps;

class MySendCallback : public SendCallback {
  virtual void onSuccess(SendResult& sendResult) {
    g_msgCount--;
    g_tps.Increment();
    if (g_msgCount.load() <= 0) {
      std::unique_lock<std::mutex> lck(g_mtx);
      g_finished.notify_one();
    }
  }
  virtual void onException(MQException& e) { cout << "send Exception\n"; }
};

class MyAutoDeleteSendCallback : public AutoDeleteSendCallBack {
 public:
  virtual ~MyAutoDeleteSendCallback() {}
  virtual void onSuccess(SendResult& sendResult) {
    g_msgCount--;
    if (g_msgCount.load() <= 0) {
      std::unique_lock<std::mutex> lck(g_mtx);
      g_finished.notify_one();
    }
  }
  virtual void onException(MQException& e) {
    std::cout << "send Exception" << e << "\n";
  }
};

void AsyncProducerWorker(RocketmqSendAndConsumerArgs* info,
                         DefaultMQProducer* producer) {
  while (!g_quit.load()) {
    if (g_msgCount.load() <= 0) {
      std::unique_lock<std::mutex> lck(g_mtx);
      g_finished.notify_one();
    }
    MQMessage msg(info->topic,  // topic
                  "*",          // tag
                  info->body);  // body

    if (info->IsAutoDeleteSendCallback) {
      g_callback = new MyAutoDeleteSendCallback();  // auto delete
    }

    try {
      producer->send(msg, g_callback);
    } catch (MQException& e) {
      std::cout << e << endl;  // if catch excepiton , need re-send this msg by
                               // service
    }
  }
}

int main(int argc, char* argv[]) {
  RocketmqSendAndConsumerArgs info;
  if (!ParseArgs(argc, argv, &info)) {
    exit(-1);
  }

  DefaultMQProducer producer("please_rename_unique_group_name");
  if (!info.IsAutoDeleteSendCallback) {
    g_callback = new MySendCallback();
  }

  PrintRocketmqSendAndConsumerArgs(info);

  if (!info.namesrv.empty()) producer.setNamesrvAddr(info.namesrv);

  producer.setGroupName(info.groupname);
  producer.setInstanceName(info.groupname);
  producer.setNamesrvDomain(info.namesrv_domain);
  producer.start();
  g_tps.start();
  std::vector<std::shared_ptr<std::thread>> work_pool;
  auto start = std::chrono::system_clock::now();
  int msgcount = g_msgCount.load();
  for (int j = 0; j < info.thread_count; j++) {
    std::shared_ptr<std::thread> th =
        std::make_shared<std::thread>(AsyncProducerWorker, &info, &producer);
    work_pool.push_back(th);
  }

  {
    std::unique_lock<std::mutex> lck(g_mtx);
    g_finished.wait(lck);
    g_quit.store(true);
  }

  auto end = std::chrono::system_clock::now();
  auto duration =
      std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

  std::cout
      << "per msg time: " << duration.count() / (double)msgcount << "ms \n"
      << "========================finished==============================\n";

  producer.shutdown();
  for (size_t th = 0; th != work_pool.size(); ++th) {
    work_pool[th]->join();
  }
  if (!info.IsAutoDeleteSendCallback) {
    delete g_callback;
  }
  return 0;
}
