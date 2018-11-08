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
#ifndef ROCKETMQ_CLIENT4CPP_EXAMPLE_COMMON_H_
#define ROCKETMQ_CLIENT4CPP_EXAMPLE_COMMON_H_

#include <boost/atomic.hpp>
#include <boost/chrono.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/thread.hpp>
#include <iostream>
#include <string>
#include <thread>
#include <vector>

#include "Arg_helper.h"
#include "DefaultMQProducer.h"
#include "DefaultMQPullConsumer.h"
#include "DefaultMQPushConsumer.h"
using namespace std;

boost::atomic<int> g_msgCount(1);

class RocketmqSendAndConsumerArgs {
 public:
  RocketmqSendAndConsumerArgs()
      : body("msgbody for test"),
        thread_count(boost::thread::hardware_concurrency()),
        broadcasting(false),
        syncpush(false),
        SelectUnactiveBroker(false),
        IsAutoDeleteSendCallback(false),
        retrytimes(5),
        PrintMoreInfo(false) {}

 public:
  std::string namesrv;
  std::string namesrv_domain;
  std::string groupname;
  std::string topic;
  std::string body;
  int thread_count;
  bool broadcasting;
  bool syncpush;
  bool SelectUnactiveBroker;  // default select active broker
  bool IsAutoDeleteSendCallback;
  int retrytimes;  // default retry 5 times;
  bool PrintMoreInfo;
};

class TpsReportService {
 public:
  TpsReportService() : tps_interval_(1), quit_flag_(false), tps_count_(0) {}
  void start() {
    tps_thread_.reset(
        new boost::thread(boost::bind(&TpsReportService::TpsReport, this)));
  }

  ~TpsReportService() {
    quit_flag_.store(true);
    if (tps_thread_->joinable()) tps_thread_->join();
  }

  void Increment() { ++tps_count_; }

  void TpsReport() {
    while (!quit_flag_.load()) {
      boost::this_thread::sleep_for(tps_interval_);
      std::cout << "tps: " << tps_count_.load() << std::endl;
      tps_count_.store(0);
    }
  }

 private:
  boost::chrono::seconds tps_interval_;
  boost::shared_ptr<boost::thread> tps_thread_;
  boost::atomic<bool> quit_flag_;
  boost::atomic<long> tps_count_;
};

static void PrintResult(rocketmq::SendResult* result) {
  std::cout << "sendresult = " << result->getSendStatus()
            << ", msgid = " << result->getMsgId()
            << ", queueOffset = " << result->getQueueOffset() << ","
            << result->getMessageQueue().toString() << endl;
}

void PrintPullResult(rocketmq::PullResult* result) {
  std::cout << result->toString() << std::endl;
  if (result->pullStatus == rocketmq::FOUND) {
    std::cout << result->toString() << endl;
    std::vector<rocketmq::MQMessageExt>::iterator it =
        result->msgFoundList.begin();
    for (; it != result->msgFoundList.end(); ++it) {
      cout << "=======================================================" << endl
           << (*it).toString() << endl;
    }
  }
}

static void PrintRocketmqSendAndConsumerArgs(
    const RocketmqSendAndConsumerArgs& info) {
  std::cout << "nameserver: " << info.namesrv << endl
            << "topic: " << info.topic << endl
            << "groupname: " << info.groupname << endl
            << "produce content: " << info.body << endl
            << "msg  count: " << g_msgCount.load() << endl
            << "thread count: " << info.thread_count << endl;
}

static void help() {
  std::cout
      << "need option,like follow: \n"
      << "-n nameserver addr, if not set -n and -i ,no nameSrv will be got \n"
         "-i nameserver domain name,  if not set -n and -i ,no nameSrv will be "
         "got \n"
         "-g groupname \n"
         "-t  msg topic \n"
         "-m messagecout(default value: 1) \n"
         "-c content(default value: only test ) \n"
         "-b (BROADCASTING model, default value: CLUSTER) \n"
         "-s sync push(default is async push)\n"
         "-r setup retry times(default value: 5 times)\n"
         "-u select active broker to send msg(default value: false)\n"
         "-d use AutoDeleteSendcallback by cpp client(defalut value: false) \n"
         "-T thread count of send msg or consume msg(defalut value: system cpu "
         "core number) \n"
         "-v print more details information \n";
}

static bool ParseArgs(int argc, char* argv[],
                      RocketmqSendAndConsumerArgs* info) {
#ifndef WIN32
  int ch;
  while ((ch = getopt(argc, argv, "n:i:g:t:m:c:b:s:h:r:T:bu")) != -1) {
    switch (ch) {
      case 'n':
        info->namesrv.insert(0, optarg);
        break;
      case 'i':
        info->namesrv_domain.insert(0, optarg);
        break;
      case 'g':
        info->groupname.insert(0, optarg);
        break;
      case 't':
        info->topic.insert(0, optarg);
        break;
      case 'm':
        g_msgCount.store(atoi(optarg));
        break;
      case 'c':
        info->body.insert(0, optarg);
        break;
      case 'b':
        info->broadcasting = true;
        break;
      case 's':
        info->syncpush = true;
        break;
      case 'r':
        info->retrytimes = atoi(optarg);
        break;
      case 'u':
        info->SelectUnactiveBroker = true;
        break;
      case 'T':
        info->thread_count = atoi(optarg);
        break;
      case 'v':
        info->PrintMoreInfo = true;
        break;
      case 'h':
        help();
        return false;
      default:
        help();
        return false;
    }
  }
#else
  rocketmq::Arg_helper arg_help(argc, argv);
  info->namesrv = arg_help.get_option_value("-n");
  info->namesrv_domain = arg_help.get_option_value("-i");
  info->groupname = arg_help.get_option_value("-g");
  info->topic = arg_help.get_option_value("-t");
  info->broadcasting = atoi(arg_help.get_option_value("-b").c_str());
  string msgContent(arg_help.get_option_value("-c"));
  if (!msgContent.empty()) info->body = msgContent;
  info->syncpush = atoi(arg_help.get_option_value("-s").c_str());
  int retrytimes = atoi(arg_help.get_option_value("-r").c_str());
  if (retrytimes > 0) info->retrytimes = retrytimes;
  info->SelectUnactiveBroker = atoi(arg_help.get_option_value("-u").c_str());
  int thread_count = atoi(arg_help.get_option_value("-T").c_str());
  if (thread_count > 0) info->thread_count = thread_count;
  info->PrintMoreInfo = atoi(arg_help.get_option_value("-v").c_str());
  g_msgCount = atoi(arg_help.get_option_value("-m").c_str());
#endif
  if (info->groupname.empty() || info->topic.empty() ||
      (info->namesrv_domain.empty() && info->namesrv.empty())) {
    std::cout << "please use -g to setup groupname and -t setup topic \n";
    help();
    return false;
  }
  return true;
}
#endif  // ROCKETMQ_CLIENT4CPP_EXAMPLE_COMMON_H_
