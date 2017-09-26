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

boost::atomic<bool> g_quit;
std::mutex g_mtx;
std::condition_variable g_finished;
TpsReportService g_tps;

void SyncProducerWorker(RocketmqSendAndConsumerArgs* info,
                        DefaultMQProducer* producer) {
  while (!g_quit.load()) {
    if (g_msgCount.load() <= 0) {
      std::unique_lock<std::mutex> lck(g_mtx);
      g_finished.notify_one();
    }
    MQMessage msg(info->topic,  // topic
                  "*",          // tag
                  info->body);  // body
    try {
      auto start = std::chrono::system_clock::now();
      SendResult sendResult = producer->send(msg, info->SelectUnactiveBroker);
      g_tps.Increment();
      --g_msgCount;
      auto end = std::chrono::system_clock::now();
      auto duration =
          std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
      if (duration.count() >= 500) {
        std::cout << "send RT more than: " << duration.count()
                  << " ms with msgid: " << sendResult.getMsgId() << endl;
      }
    } catch (const MQException& e) {
      std::cout << "send failed: " << std::endl;
    }
  }
}

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
  producer.setSessionCredentials("mq acesskey", "mq secretkey", "ALIYUN");
  producer.setSendMsgTimeout(500);
  producer.setTcpTransportTryLockTimeout(1000);
  producer.setTcpTransportConnectTimeout(400);

  producer.start();
  std::vector<std::shared_ptr<std::thread>> work_pool;
  auto start = std::chrono::system_clock::now();
  int msgcount = g_msgCount.load();
  g_tps.start();

  int threadCount = info.thread_count;
  for (int j = 0; j < threadCount; j++) {
    std::shared_ptr<std::thread> th =
        std::make_shared<std::thread>(SyncProducerWorker, &info, &producer);
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

  for (size_t th = 0; th != work_pool.size(); ++th) {
    work_pool[th]->join();
  }

  producer.shutdown();

  return 0;
}
