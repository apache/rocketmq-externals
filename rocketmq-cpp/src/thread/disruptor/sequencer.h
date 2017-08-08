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

#ifndef DISRUPTOR_SEQUENCER_H_ // NOLINT
#define DISRUPTOR_SEQUENCER_H_ // NOLINT

#include <vector>

#include "batch_descriptor.h"
#include "claim_strategy.h"
#include "interface.h"
#include "sequence_barrier.h"
#include "wait_strategy.h"

namespace rocketmq {

// Coordinator for claiming sequences for access to a data structures while
// tracking dependent {@link Sequence}s
class Sequencer: public boost::noncopyable {
 public:
    // Construct a Sequencer with the selected strategies.
    //
    // @param buffer_size over which sequences are valid.
    // @param claim_strategy_option for those claiming sequences.
    // @param wait_strategy_option for those waiting on sequences.
    Sequencer(int buffer_size,
              ClaimStrategyOption claim_strategy_option,
              WaitStrategyOption wait_strategy_option) :
            buffer_size_(buffer_size),
            claim_strategy_(CreateClaimStrategy(claim_strategy_option,
                                                buffer_size_)),
            wait_strategy_(CreateWaitStrategy(wait_strategy_option)) { }

    ~Sequencer() {
        delete claim_strategy_;
        delete wait_strategy_;
    }

    // Set the sequences that will gate publishers to prevent the buffer
    // wrapping.
    //
    // @param sequences to be gated on.
    void set_gating_sequences(
            const std::vector<Sequence*>& sequences) {
        gating_sequences_ = sequences;
    }

    // Create a {@link SequenceBarrier} that gates on the cursor and a list of
    // {@link Sequence}s.
    //
    // @param sequences_to_track this barrier will track.
    // @return the barrier gated as required.
    ProcessingSequenceBarrier* NewBarrier(
            const std::vector<Sequence*>& sequences_to_track) {
        return new ProcessingSequenceBarrier(wait_strategy_, &cursor_,
                                             sequences_to_track);
    }

    // Create a new {@link BatchDescriptor} that is the minimum of the
    // requested size and the buffer_size.
    //
    // @param size for the new batch.
    // @return the new {@link BatchDescriptor}.
    BatchDescriptor* NewBatchDescriptor(const int& size) {
        return new BatchDescriptor(size<buffer_size_?size:buffer_size_);
    }

    // The capacity of the data structure to hold entries.
    //
    // @return capacity of the data structure.
    int buffer_size() { return buffer_size_; }


    // Get the value of the cursor indicating the published sequence.
    //
    // @return value of the cursor for events that have been published.
    int64_t GetCursor() { return cursor_.sequence(); }

    // Has the buffer capacity left to allocate another sequence. This is a
    // concurrent method so the response should only be taken as an indication
    // of available capacity.
    //
    // @return true if the buffer has the capacity to allocated another event.
    bool HasAvalaibleCapacity() {
        return claim_strategy_->HasAvalaibleCapacity(gating_sequences_);
    }

    // Claim the next event in sequence for publishing to the {@link RingBuffer}.
    //
    // @return the claimed sequence.
    int64_t Next() {
        return claim_strategy_->IncrementAndGet(gating_sequences_);
    }

    // Claim the next batch of sequence numbers for publishing.
    //
    // @param batch_descriptor to be updated for the batch range.
    // @return the updated batch_descriptor.
    BatchDescriptor* Next(BatchDescriptor* batch_descriptor) {
        int64_t sequence = claim_strategy_->IncrementAndGet(batch_descriptor->size(), gating_sequences_);
        batch_descriptor->set_end(sequence);
        return batch_descriptor;
    }

    // Claim a specific sequence when only one publisher is involved.
    //
    // @param sequence to be claimed.
    // @return sequence just claime.
    int64_t Claim(const int64_t& sequence) {
        claim_strategy_->SetSequence(sequence, gating_sequences_);
        return sequence;
    }

    // Publish an event and make it visible to {@link EventProcessor}s.
    //
    // @param sequence to be published.
    void Publish(const int64_t& sequence) {
        Publish(sequence, 1);
    }

    // Publish the batch of events in sequence.
    //
    // @param sequence to be published.
    void Publish(const BatchDescriptor& batch_descriptor) {
        Publish(batch_descriptor.end(), batch_descriptor.size());
    }

    // Force the publication of a cursor sequence.
    //
    // Only use this method when forcing a sequence and you are sure only one
    // publisher exists. This will cause the cursor to advance to this
    // sequence.
    //
    // @param sequence to which is to be forced for publication.
    void ForcePublish(const int64_t& sequence) {
        cursor_.set_sequence(sequence);
        wait_strategy_->SignalAllWhenBlocking();
    }

    // TODO(fsaintjacques): This was added to overcome
    // NoOpEventProcessor::GetSequence(), this is not a clean solution.
    Sequence* GetSequencePtr() {
        return &cursor_;
    }

 private:
    // Helpers
    void Publish(const int64_t& sequence, const int64_t& batch_size) {
        //LOG_DEBUG("publish sequence:%d", sequence);
        claim_strategy_->SerialisePublishing(sequence, cursor_, batch_size);
        cursor_.set_sequence(sequence);
        wait_strategy_->SignalAllWhenBlocking();
    }

    // Members
    const int buffer_size_;

    PaddedSequence cursor_;
    std::vector<Sequence*> gating_sequences_;

    ClaimStrategyInterface* claim_strategy_;
    WaitStrategyInterface* wait_strategy_;

};

};  // namespace rocketmq

#endif // DISRUPTOR_RING_BUFFER_H_ NOLINT
