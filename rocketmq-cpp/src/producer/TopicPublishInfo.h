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
#ifndef __TOPICPUBLISHINFO_H__
#define __TOPICPUBLISHINFO_H__

#include <boost/asio.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/atomic.hpp>
#include <boost/bind.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/thread.hpp>
#include "Logging.h"
#include "MQMessageQueue.h"

namespace rocketmq {
//<!************************************************************************/
class TopicPublishInfo {
 public:
  TopicPublishInfo() : m_sendWhichQueue(0) {
    m_async_service_thread.reset(new boost::thread(
        boost::bind(&TopicPublishInfo::boost_asio_work, this)));
  }

  void boost_asio_work() {
    boost::asio::io_service::work work(m_async_ioService);  // avoid async io
                                                            // service stops
                                                            // after first timer
                                                            // timeout callback
    boost::system::error_code e;
    boost::asio::deadline_timer t(m_async_ioService,
                                  boost::posix_time::seconds(60));
    t.async_wait(boost::bind(
        &TopicPublishInfo::op_resumeNonServiceMessageQueueList, this, e, &t));
    boost::system::error_code ec;
    m_async_ioService.run(ec);
  }

  virtual ~TopicPublishInfo() {
    m_async_ioService.stop();
    m_async_service_thread->interrupt();
    m_async_service_thread->join();

    m_nonSerivceQueues.clear();
    m_onSerivceQueues.clear();
    m_brokerTimerMap.clear();
    m_queues.clear();
  }

  bool ok() {
    boost::lock_guard<boost::mutex> lock(m_queuelock);
    return !m_queues.empty();
  }

  void updateMessageQueueList(const MQMessageQueue& mq) {
    boost::lock_guard<boost::mutex> lock(m_queuelock);
    m_queues.push_back(mq);
    string key = mq.getBrokerName() + UtilAll::to_string(mq.getQueueId());
    m_onSerivceQueues[key] = mq;
    if (m_nonSerivceQueues.find(key) != m_nonSerivceQueues.end()) {
      m_nonSerivceQueues.erase(key);  // if topicPublishInfo changed, erase this
                                      // mq from m_nonSerivceQueues to avoid 2
                                      // copies both in m_onSerivceQueues and
                                      // m_nonSerivceQueues
    }
  }

  void op_resumeNonServiceMessageQueueList(boost::system::error_code& ec,
                                           boost::asio::deadline_timer* t) {
    resumeNonServiceMessageQueueList();
    boost::system::error_code e;
    t->expires_from_now(t->expires_from_now() + boost::posix_time::seconds(60),
                        e);
    t->async_wait(boost::bind(
        &TopicPublishInfo::op_resumeNonServiceMessageQueueList, this, e, t));
  }

  void resumeNonServiceMessageQueueList() {
    boost::lock_guard<boost::mutex> lock(m_queuelock);
    for (map<MQMessageQueue, int64>::iterator it = m_brokerTimerMap.begin();
         it != m_brokerTimerMap.end(); ++it) {
      if (UtilAll::currentTimeMillis() - it->second >= 1000 * 60 * 5) {
        string key = it->first.getBrokerName() +
                     UtilAll::to_string(it->first.getQueueId());
        if (m_nonSerivceQueues.find(key) != m_nonSerivceQueues.end()) {
          m_nonSerivceQueues.erase(key);
        }
        m_onSerivceQueues[key] = it->first;
      }
    }
  }

  void updateNonServiceMessageQueue(const MQMessageQueue& mq,
                                    int timeoutMilliseconds) {
    boost::lock_guard<boost::mutex> lock(m_queuelock);

    string key = mq.getBrokerName() + UtilAll::to_string(mq.getQueueId());
    if (m_nonSerivceQueues.find(key) != m_nonSerivceQueues.end()) {
      return;
    }
    LOG_INFO("updateNonServiceMessageQueue of mq:%s", mq.toString().c_str());
    m_brokerTimerMap[mq] = UtilAll::currentTimeMillis();
    m_nonSerivceQueues[key] = mq;
    if (m_onSerivceQueues.find(key) != m_onSerivceQueues.end()) {
      m_onSerivceQueues.erase(key);
    }
  }

  vector<MQMessageQueue>& getMessageQueueList() {
    boost::lock_guard<boost::mutex> lock(m_queuelock);
    return m_queues;
  }

  int getWhichQueue() {
    return m_sendWhichQueue.load(boost::memory_order_acquire);
  }

  MQMessageQueue selectOneMessageQueue(const MQMessageQueue& lastmq,
                                       int& mq_index) {
    boost::lock_guard<boost::mutex> lock(m_queuelock);

    if (m_queues.size() > 0) {
      LOG_DEBUG("selectOneMessageQueue Enter, queue size:" SIZET_FMT "",
                m_queues.size());
      unsigned int pos = 0;
      if (mq_index >= 0) {
        pos = mq_index % m_queues.size();
      } else {
        LOG_ERROR("mq_index is negative");
        return MQMessageQueue();
      }
      if (!lastmq.getBrokerName().empty()) {
        for (size_t i = 0; i < m_queues.size(); i++) {
          if (m_sendWhichQueue.load(boost::memory_order_acquire) ==
              (numeric_limits<int>::max)()) {
            m_sendWhichQueue.store(0, boost::memory_order_release);
          }

          if (pos >= m_queues.size()) pos = pos % m_queues.size();

          ++m_sendWhichQueue;
          MQMessageQueue mq = m_queues.at(pos);
          LOG_DEBUG("lastmq broker not empty, m_sendWhichQueue:%d, pos:%d",
                    m_sendWhichQueue.load(boost::memory_order_acquire), pos);
          if (mq.getBrokerName().compare(lastmq.getBrokerName()) != 0) {
            mq_index = pos;
            return mq;
          }
          ++pos;
        }
        LOG_ERROR("could not find property mq");
        return MQMessageQueue();
      } else {
        if (m_sendWhichQueue.load(boost::memory_order_acquire) ==
            (numeric_limits<int>::max)()) {
          m_sendWhichQueue.store(0, boost::memory_order_release);
        }

        ++m_sendWhichQueue;
        LOG_DEBUG("lastmq broker empty, m_sendWhichQueue:%d, pos:%d",
                  m_sendWhichQueue.load(boost::memory_order_acquire), pos);
        mq_index = pos;
        return m_queues.at(pos);
      }
    } else {
      LOG_ERROR("m_queues empty");
      return MQMessageQueue();
    }
  }

  MQMessageQueue selectOneActiveMessageQueue(const MQMessageQueue& lastmq,
                                             int& mq_index) {
    boost::lock_guard<boost::mutex> lock(m_queuelock);

    if (m_queues.size() > 0) {
      unsigned int pos = 0;
      if (mq_index >= 0) {
        pos = mq_index % m_queues.size();
      } else {
        LOG_ERROR("mq_index is negative");
        return MQMessageQueue();
      }
      if (!lastmq.getBrokerName().empty()) {
        for (size_t i = 0; i < m_queues.size(); i++) {
          if (m_sendWhichQueue.load(boost::memory_order_acquire) ==
              (numeric_limits<int>::max)()) {
            m_sendWhichQueue.store(0, boost::memory_order_release);
          }

          if (pos >= m_queues.size()) pos = pos % m_queues.size();

          ++m_sendWhichQueue;
          MQMessageQueue mq = m_queues.at(pos);
          string key = mq.getBrokerName() + UtilAll::to_string(mq.getQueueId());
          if ((mq.getBrokerName().compare(lastmq.getBrokerName()) != 0) &&
              (m_onSerivceQueues.find(key) != m_onSerivceQueues.end())) {
            mq_index = pos;
            return mq;
          }
          ++pos;
        }

        for (MQMAP::iterator it = m_nonSerivceQueues.begin();
             it != m_nonSerivceQueues.end();
             ++it) {  // if no MQMessageQueue(except lastmq) in
                      // m_onSerivceQueues, search m_nonSerivceQueues
          if (it->second.getBrokerName().compare(lastmq.getBrokerName()) != 0)
            return it->second;
        }
        LOG_ERROR("can not find property mq");
        return MQMessageQueue();
      } else {
        for (size_t i = 0; i < m_queues.size(); i++) {
          if (m_sendWhichQueue.load(boost::memory_order_acquire) ==
              (numeric_limits<int>::max)()) {
            m_sendWhichQueue.store(0, boost::memory_order_release);
          }
          if (pos >= m_queues.size()) pos = pos % m_queues.size();

          ++m_sendWhichQueue;
          LOG_DEBUG("lastmq broker empty, m_sendWhichQueue:%d, pos:%d",
                    m_sendWhichQueue.load(boost::memory_order_acquire), pos);
          mq_index = pos;
          MQMessageQueue mq = m_queues.at(pos);
          string key = mq.getBrokerName() + UtilAll::to_string(mq.getQueueId());
          if (m_onSerivceQueues.find(key) != m_onSerivceQueues.end()) {
            return mq;
          } else {
            ++pos;
          }
        }

        for (MQMAP::iterator it = m_nonSerivceQueues.begin();
             it != m_nonSerivceQueues.end();
             ++it) {  // if no MQMessageQueue(except lastmq) in
                      // m_onSerivceQueues, search m_nonSerivceQueues
          if (it->second.getBrokerName().compare(lastmq.getBrokerName()) != 0)
            return it->second;
        }
        LOG_ERROR("can not find property mq");
        return MQMessageQueue();
      }
    } else {
      LOG_ERROR("m_queues empty");
      return MQMessageQueue();
    }
  }

 private:
  boost::mutex m_queuelock;
  typedef vector<MQMessageQueue> QueuesVec;
  QueuesVec m_queues;
  typedef map<string, MQMessageQueue> MQMAP;
  MQMAP m_onSerivceQueues;
  MQMAP m_nonSerivceQueues;
  boost::atomic<int> m_sendWhichQueue;
  map<MQMessageQueue, int64> m_brokerTimerMap;
  boost::asio::io_service m_async_ioService;
  boost::scoped_ptr<boost::thread> m_async_service_thread;
};

//<!***************************************************************************
}  //<!end namespace;

#endif
