// Copyright (c) 2011, François Saint-Jacques
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//     * Redistributions of source code must retain the above copyright
//       notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above copyright
//       notice, this list of conditions and the following disclaimer in the
//       documentation and/or other materials provided with the distribution.
//     * Neither the name of the disruptor-- nor the
//       names of its contributors may be used to endorse or promote products
//       derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL FRANÇOIS SAINT-JACQUES BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

#ifndef DISRUPTOR_EVENT_PROCESSOR_H_ // NOLINT
#define DISRUPTOR_EVENT_PROCESSOR_H_ // NOLINT

#include <stdexcept>
#include "ring_buffer.h"

namespace rocketmq {

template <typename T>
class NoOpEventProcessor : public EventProcessorInterface<T> {
 public:
    NoOpEventProcessor(RingBuffer<T>* ring_buffer) :
        ring_buffer_(ring_buffer) { }

    virtual Sequence* GetSequence() {
        return ring_buffer_->GetSequencePtr();
    }

    virtual void Halt() {}

    virtual void Run() {}

 private:
    RingBuffer<T>* ring_buffer_;
};

template <typename T>
class BatchEventProcessor : public boost::noncopyable, public EventProcessorInterface<T> {
 public:
    BatchEventProcessor(RingBuffer<T>* ring_buffer,
                        SequenceBarrierInterface* sequence_barrier,
                        EventHandlerInterface<T>* event_handler,
                        ExceptionHandlerInterface<T>* exception_handler) :
            running_(false),
            ring_buffer_(ring_buffer),
            sequence_barrier_(sequence_barrier),
            event_handler_(event_handler),
            exception_handler_(exception_handler) {}


    virtual Sequence* GetSequence() { return &sequence_; }

    virtual void Halt() {
        running_.store(false);
        sequence_barrier_->Alert();
    }

    virtual void Run() {
        if (running_.load())
         {
            printf("Thread is already running\r\n");
         }
        running_.store(true);
        sequence_barrier_->ClearAlert();
        event_handler_->OnStart();

        T* event = NULL;
        int64_t next_sequence = sequence_.sequence() + 1L;

        while (true) {
            try {
                int64_t avalaible_sequence = \
                    sequence_barrier_->WaitFor(next_sequence, 300*1000);//wait 300 milliseconds to avoid taskThread blocking on BlockingStrategy::WaitFor when shutdown
                //rocketmq::LOG_INFO("avalaible_sequence:%d, next_sequence:%d", avalaible_sequence,next_sequence);
                while (next_sequence <= avalaible_sequence) {
                    event = ring_buffer_->Get(next_sequence);
                    event_handler_->OnEvent(next_sequence,
                            next_sequence == avalaible_sequence, event);
                    next_sequence++;
                }

                sequence_.set_sequence(next_sequence - 1L);
            } catch(const AlertException& e) {
                //rocketmq::LOG_INFO("catch alertException");
                if (!running_.load())
                    break;
            } catch(const std::exception& e) {
                //rocketmq::LOG_ERROR("catch stdException");
                exception_handler_->Handle(e, next_sequence, event);
                sequence_.set_sequence(next_sequence);
                next_sequence++;
            }
        }
        //rocketmq::LOG_INFO("BatchEventProcessor shutdown");
        event_handler_->OnShutdown();
        running_.store(false);
    }

    void operator()() { Run(); }

 private:
    boost::atomic<bool> running_;
    Sequence sequence_;

    RingBuffer<T>* ring_buffer_;
    SequenceBarrierInterface* sequence_barrier_;
    EventHandlerInterface<T>* event_handler_;
    ExceptionHandlerInterface<T>* exception_handler_;

};


};  // namespace rocketmq

#endif // DISRUPTOR_EVENT_PROCESSOR_H_ NOLINT
