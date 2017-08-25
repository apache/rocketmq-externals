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

#ifndef DISRUPTOR_RING_BUFFER_H_ // NOLINT
#define DISRUPTOR_RING_BUFFER_H_ // NOLINT

#include <boost/array.hpp>
#include <vector>

#include "interface.h"
#include "claim_strategy.h"
#include "wait_strategy.h"
#include "sequencer.h"
#include "sequence_barrier.h"

namespace rocketmq {

// Ring based store of reusable entries containing the data representing an
// event beign exchanged between publisher and {@link EventProcessor}s.
//
// @param <T> implementation storing the data for sharing during exchange
// or parallel coordination of an event.
template<typename T>
class RingBuffer : public Sequencer {
 public:
    // Construct a RingBuffer with the full option set.
    //
    // @param event_factory to instance new entries for filling the RingBuffer.
    // @param buffer_size of the RingBuffer, must be a power of 2.
    // @param claim_strategy_option threading strategy for publishers claiming
    // entries in the ring.
    // @param wait_strategy_option waiting strategy employed by
    // processors_to_track waiting in entries becoming available.
    RingBuffer(EventFactoryInterface<T>* event_factory,
               int buffer_size,
               ClaimStrategyOption claim_strategy_option,
               WaitStrategyOption wait_strategy_option) :
            Sequencer(buffer_size,
                      claim_strategy_option,
                      wait_strategy_option),
            buffer_size_(buffer_size),
            mask_(buffer_size - 1),
            events_(event_factory->NewInstance(buffer_size)) {
    }

    ~RingBuffer() {
        delete[] events_;
    }

    // Get the event for a given sequence in the RingBuffer.
    //
    // @param sequence for the event
    // @return event pointer at the specified sequence position.
    T* Get(const int64_t& sequence) {
        return &events_[sequence & mask_];
    }

 private:
    // Members
    int buffer_size_;
    int mask_;
    T* events_;

};

};  // namespace rocketmq

#endif // DISRUPTOR_RING_BUFFER_H_ NOLINT
