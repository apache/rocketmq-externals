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
#ifndef _DISRUPTORLFQ_
#define _DISRUPTORLFQ_

#include <disruptor/event_processor.h>
#include <disruptor/event_publisher.h>
#include <disruptor/exception_handler.h>
#include <disruptor/interface.h>

#include <boost/asio/io_service.hpp>
#include <boost/bind.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/thread/thread.hpp>

namespace rocketmq {
class Task;
class taskEventFactory : public EventFactoryInterface<Task> {
 public:
  virtual Task* NewInstance(const int& size) const;
};

class taskBatchHandler : public EventHandlerInterface<Task> {
 public:
  taskBatchHandler(int pullMsgThreadPoolNum);
  virtual ~taskBatchHandler() {}

  virtual void OnEvent(const int64_t& sequence, const bool& end_of_batch,
                       Task* event);
  virtual void OnStart() {}
  virtual void OnShutdown() {}
  void runTaskEvent(Task event, int64_t sequence);
  void stopIOService();

 private:
  boost::asio::io_service m_ioService;
  boost::thread_group m_threadpool;
  boost::asio::io_service::work m_ioServiceWork;
};

class taskEventTranslator : public EventTranslatorInterface<Task> {
 public:
  taskEventTranslator(Task* event);
  virtual ~taskEventTranslator() {}
  virtual Task* TranslateTo(const int64_t& sequence, Task* event);

 private:
  Task* m_taskEvent;
};

class taskExceptionHandler : public ExceptionHandlerInterface<Task> {
 public:
  virtual void Handle(const std::exception& exception, const int64_t& sequence,
                      Task* event) {}
};

class disruptorLFQ {
 public:
  disruptorLFQ(int threadCount) {
    m_task_factory.reset(new taskEventFactory());
    m_ring_buffer.reset(new RingBuffer<Task>(
        m_task_factory.get(),
        1024,  // default size is 1024, must be n power of 2
        kSingleThreadedStrategy,
        kBlockingStrategy));  // load normal, lowest CPU occupy, but
                                     // largest consume latency

    m_sequence_to_track.reset(new std::vector<Sequence*>(0));
    m_sequenceBarrier.reset(
        m_ring_buffer->NewBarrier(*(m_sequence_to_track.get())));

    m_task_handler.reset(new taskBatchHandler(threadCount));
    m_task_exception_handler.reset(new taskExceptionHandler());
    m_processor.reset(new BatchEventProcessor<Task>(
        m_ring_buffer.get(),
        (SequenceBarrierInterface*)m_sequenceBarrier.get(),
        m_task_handler.get(), m_task_exception_handler.get()));

    /*
    Publisher will try to publish BUFFER_SIZE + 1 events.
    The last event should wait for at least one consume before publishing, thus
    preventing an overwrite.
    After the single consume, the publisher should resume and publish the last
    event.
    */
    m_gating_sequences.push_back(m_processor.get()->GetSequence());
    m_ring_buffer->set_gating_sequences(
        m_gating_sequences);  // prevent overlap, publishEvent will be blocked
                              // on ring_buffer_->Next();

    m_publisher.reset(new EventPublisher<Task>(m_ring_buffer.get()));
  }
  virtual ~disruptorLFQ() {}

 public:
  boost::scoped_ptr<taskEventFactory> m_task_factory;
  boost::scoped_ptr<taskBatchHandler> m_task_handler;
  boost::scoped_ptr<taskExceptionHandler> m_task_exception_handler;
  boost::scoped_ptr<std::vector<Sequence*>> m_sequence_to_track;
  boost::scoped_ptr<RingBuffer<Task>> m_ring_buffer;
  boost::scoped_ptr<ProcessingSequenceBarrier> m_sequenceBarrier;
  boost::scoped_ptr<BatchEventProcessor<Task>> m_processor;
  boost::scoped_ptr<EventPublisher<Task>> m_publisher;
  std::vector<Sequence*> m_gating_sequences;
};
}
//<!***************************************************************************

#endif
