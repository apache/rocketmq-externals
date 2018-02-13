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

#ifndef DISRUPTOR_SEQUENCE_BARRIER_H_ // NOLINT
#define DISRUPTOR_SEQUENCE_BARRIER_H_ // NOLINT

#include <memory>
#include <vector>

#include "exceptions.h"
#include "interface.h"
namespace rocketmq {

class ProcessingSequenceBarrier : SequenceBarrierInterface {
 public:
    ProcessingSequenceBarrier(WaitStrategyInterface* wait_strategy,
                              Sequence* sequence,
                              const std::vector<Sequence*>& sequences) :
        wait_strategy_(wait_strategy),
        cursor_(sequence),
        dependent_sequences_(sequences),
        alerted_(false) {
    }

    virtual int64_t WaitFor(const int64_t& sequence) {
        return wait_strategy_->WaitFor(dependent_sequences_, *cursor_, *this,
                                       sequence);
    }

    virtual int64_t WaitFor(const int64_t& sequence,
                            const int64_t& timeout_micros) {
        return wait_strategy_->WaitFor(dependent_sequences_, *cursor_, *this,
                                       sequence, timeout_micros);
    }

    virtual int64_t GetCursor() const {
        return cursor_->sequence();
    }

    virtual bool IsAlerted() const {
        return alerted_.load(boost::memory_order_acquire);
    }

    virtual void Alert() {
        //rocketmq::LOG_INFO("set alert to true");
        alerted_.store(true, boost::memory_order_release);
    }

    virtual void ClearAlert() {
        alerted_.store(false, boost::memory_order_release);
    }

    virtual void CheckAlert() const {
        if (IsAlerted())
        {
            //rocketmq::LOG_INFO("throw alert exception\r\n");
            throw AlertException();
        }
    }

 private:
    WaitStrategyInterface* wait_strategy_;
    Sequence* cursor_;
    std::vector<Sequence*> dependent_sequences_;
    boost::atomic<bool> alerted_;
};

};  // namespace rocketmq

#endif // DISRUPTOR_DEPENDENCY_BARRIER_H_ NOLINT
