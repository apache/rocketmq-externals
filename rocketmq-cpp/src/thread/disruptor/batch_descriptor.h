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

#ifndef DISRUPTOR_BATCH_DESCRIPTOR_H_  // NOLINT
#define DISRUPTOR_BATCH_DESCRIPTOR_H_  // NOLINT

#include "sequence.h"

namespace rocketmq {

// Used to record the batch of sequences claimed via {@link Sequencer}.
class BatchDescriptor {
 public:
    // Create a holder for tracking a batch of claimed sequences in a
    // {@link Sequencer}
    //
    // @param size of the batch to claim.
    BatchDescriptor(int size) :
        size_(size),
        end_(kInitialCursorValue) {}

    // Get the size of the batch
    int size() const { return size_; }

    // Get the end sequence of a batch.
    //
    // @return the end sequence in the batch.
    int64_t end() const { return end_; }

    // Set the end sequence of a batch.
    //
    // @param end sequence in the batch.
    void set_end(int64_t end) { end_ = end; }


    // Get the starting sequence of the batch.
    //
    // @return starting sequence in the batch.
    int64_t Start() const { return end_ - size_ + 1L; }

 private:
    int size_;
    int64_t end_;
};

};  // namespace rocketmq

#endif // DISRUPTOR_SEQUENCE_BATCH_H_  NOLINT
