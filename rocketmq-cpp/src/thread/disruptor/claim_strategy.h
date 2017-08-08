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

#ifndef DISRUPTOR_CLAIM_STRATEGY_H_ // NOLINT
#define DISRUPTOR_CLAIM_STRATEGY_H_ // NOLINT

#include <boost/thread.hpp>
#include <boost/noncopyable.hpp>
#include <vector>

#include "interface.h"

namespace rocketmq {

enum ClaimStrategyOption {
    kSingleThreadedStrategy,
    kMultiThreadedStrategy
};

// Optimised strategy can be used when there is a single publisher thread
// claiming {@link AbstractEvent}s.
class SingleThreadedStrategy :public noncopyable,  public ClaimStrategyInterface {
 public:
    SingleThreadedStrategy(const int& buffer_size) :
        buffer_size_(buffer_size),
        sequence_(kInitialCursorValue),
        min_gating_sequence_(kInitialCursorValue) {}

    virtual int64_t IncrementAndGet(
            const std::vector<Sequence*>& dependent_sequences) {
        int64_t next_sequence = sequence_.IncrementAndGet(1L);
        WaitForFreeSlotAt(next_sequence, dependent_sequences);
        return next_sequence;
    }

    virtual int64_t IncrementAndGet(const int& delta,
            const std::vector<Sequence*>& dependent_sequences) {
        int64_t next_sequence = sequence_.IncrementAndGet(delta);
        WaitForFreeSlotAt(next_sequence, dependent_sequences);
        return next_sequence;
    }

    virtual bool HasAvalaibleCapacity(
            const std::vector<Sequence*>& dependent_sequences) {
        int64_t wrap_point = sequence_.sequence() + 1L - buffer_size_;
        if (wrap_point > min_gating_sequence_.sequence()) {
            int64_t min_sequence = GetMinimumSequence(dependent_sequences);
            min_gating_sequence_.set_sequence(min_sequence);
            if (wrap_point > min_sequence)
                return false;
        }
        return true;
    }

    virtual void SetSequence(const int64_t& sequence,
            const std::vector<Sequence*>& dependent_sequences) {
        sequence_.set_sequence(sequence);
        WaitForFreeSlotAt(sequence, dependent_sequences);
    }

    virtual void SerialisePublishing(const int64_t& sequence,
                                     const Sequence& cursor,
                                     const int64_t& batch_size) {}

 private:
    SingleThreadedStrategy();

    void WaitForFreeSlotAt(const int64_t& sequence,
            const std::vector<Sequence*>& dependent_sequences) {
        int64_t wrap_point = sequence - buffer_size_;
        if (wrap_point > min_gating_sequence_.sequence()) {
            int64_t min_sequence;
            while (wrap_point > (min_sequence = GetMinimumSequence(dependent_sequences))) {
                boost::this_thread::yield();
            }
        }
    }

    const int buffer_size_;
    PaddedLong sequence_;
    PaddedLong min_gating_sequence_;

};

// Strategy to be used when there are multiple publisher threads claiming
// {@link AbstractEvent}s.
/*
class MultiThreadedStrategy :  public ClaimStrategyInterface {
 public:
    MultiThreadedStrategy(const int& buffer_size) :
        buffer_size_(buffer_size),
        sequence_(kInitialCursorValue),
        min_processor_sequence_(kInitialCursorValue) {}

    virtual int64_t IncrementAndGet(
            const std::vector<Sequence*>& dependent_sequences) {
        WaitForCapacity(dependent_sequences, min_gating_sequence_local_);
        int64_t next_sequence = sequence_.IncrementAndGet();
        WaitForFreeSlotAt(next_sequence,
                          dependent_sequences,
                          min_gating_sequence_local_);
        return next_sequence;
    }

    virtual int64_t IncrementAndGet(const int& delta,
            const std::vector<Sequence*>& dependent_sequences) {
        int64_t next_sequence = sequence_.IncrementAndGet(delta);
        WaitForFreeSlotAt(next_sequence,
                          dependent_sequences,
                          min_gating_sequence_local_);
        return next_sequence;
    }
    virtual void SetSequence(const int64_t& sequence,
            const std::vector<Sequence*>& dependent_sequences) {
        sequence_.set_sequence(sequence);
        WaitForFreeSlotAt(sequence,
                          dependent_sequences,
                          min_gating_sequence_local_);
    }

    virtual bool HasAvalaibleCapacity(
            const std::vector<Sequence*>& dependent_sequences) {
        const int64_t wrap_point = sequence_.sequence() + 1L - buffer_size_;
        if (wrap_point > min_gating_sequence_local_.sequence()) {
            int64_t min_sequence = GetMinimumSequence(dependent_sequences);
            min_gating_sequence_local_.set_sequence(min_sequence);
            if (wrap_point > min_sequence)
                return false;
        }
        return true;
    }

    virtual void SerialisePublishing(const Sequence& cursor,
                                     const int64_t& sequence,
                                     const int64_t& batch_size) {
        int64_t expected_sequence = sequence - batch_size;
        int counter = retries;

        while (expected_sequence != cursor.sequence()) {
            if (0 == --counter) {
                counter = retries;
                std::this_thread::yield();
            }
        }
    }

 private:
    // Methods
    void WaitForCapacity(const std::vector<Sequence*>& dependent_sequences,
                         const MutableLong& min_gating_sequence) {
        const int64_t wrap_point = sequence_.sequence() + 1L - buffer_size_;
        if (wrap_point > min_gating_sequence.sequence()) {
            int counter = retries;
            int64_t min_sequence;
            while (wrap_point > (min_sequence = GetMinimumSequence(dependent_sequences))) {
                counter = ApplyBackPressure(counter);
            }
            min_gating_sequence.set_sequence(min_sequence);
        }
    }

    void WaitForFreeSlotAt(const int64_t& sequence,
                           const std::vector<Sequence*>& dependent_sequences,
                           const MutableLong& min_gating_sequence) {
        const int64_t wrap_point = sequence - buffer_size_;
        if (wrap_point > min_gating_sequence.sequence()) {
            int64_t min_sequence;
            while (wrap_point > (min_sequence = GetMinimumSequence(dependent_sequences))) {
                std::this_thread::yield();
            }
            min_gating_sequence.set_sequence(min_sequence);
        }
    }

    int ApplyBackPressure(int counter) {
        if (0 != counter) {
            --counter;
            std::this_thread::yield();
        } else {
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        }

        return counter;
    }

    const int buffer_size_;
    PaddedSequence sequence_;
    thread_local PaddedLong min_gating_sequence_local_;

    const int retries = 100;

};
*/

ClaimStrategyInterface* CreateClaimStrategy(ClaimStrategyOption option,
                                            const int& buffer_size) {
    switch (option) {
        case kSingleThreadedStrategy:
            return new SingleThreadedStrategy(buffer_size);
        // case kMultiThreadedStrategy:
        //     return new MultiThreadedStrategy(buffer_size);
        default:
            return NULL;
    }
};

};  // namespace rocketmq

#endif // DISRUPTOR_CLAIM_STRATEGY_H_ NOLINT
