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

#ifndef DISRUPTOR_INTERFACE_H_ // NOLINT
#define DISRUPTOR_INTERFACE_H_ // NOLINT

#include <climits>
#include <vector>

#include "sequence.h"
#include "batch_descriptor.h"

namespace rocketmq {

// Strategies employed for claiming the sequence of events in the
// {@link Seqencer} by publishers.
class ClaimStrategyInterface {
 public:
    // Is there available capacity in the buffer for the requested sequence.
    //
    // @param dependent_sequences to be checked for range.
    // @return true if the buffer has capacity for the requested sequence.
    virtual ~ClaimStrategyInterface() {}
    virtual bool HasAvalaibleCapacity(
        const std::vector<Sequence*>& dependent_sequences) = 0;

    // Claim the next sequence in the {@link Sequencer}.
    //
    // @param dependent_sequences to be checked for range.
    // @return the index to be used for the publishing.
    virtual int64_t IncrementAndGet(
            const std::vector<Sequence*>& dependent_sequences) = 0;

    // Claim the next sequence in the {@link Sequencer}.
    //
    // @param delta to increment by.
    // @param dependent_sequences to be checked for range.
    // @return the index to be used for the publishing.
    virtual int64_t IncrementAndGet(const int& delta,
            const std::vector<Sequence*>& dependent_sequences) = 0;

    // Set the current sequence value for claiming an event in the
    // {@link Sequencer}.
    //
    // @param sequence to be set as the current value.
    // @param dependent_sequences to be checked for range.
    virtual void SetSequence(const int64_t& sequence,
            const std::vector<Sequence*>& dependent_sequences) = 0;

    // Serialise publishing in sequence.
    //
    // @param sequence to be applied.
    // @param cursor to be serialise against.
    // @param batch_size of the sequence.
    virtual void SerialisePublishing(const int64_t& sequence,
                                     const Sequence& cursor,
                                     const int64_t& batch_size) = 0;
};

// Coordination barrier for tracking the cursor for publishers and sequence of
// dependent {@link EventProcessor}s for processing a data structure.
class SequenceBarrierInterface {
 public:
    // Wait for the given sequence to be available for consumption.
    //
    // @param sequence to wait for.
    // @return the sequence up to which is available.
    //
    // @throws AlertException if a status change has occurred for the
    // Disruptor.
    virtual ~SequenceBarrierInterface(){}
    virtual int64_t WaitFor(const int64_t& sequence) = 0;

    // Wait for the given sequence to be available for consumption with a
    // time out.
    //
    // @param sequence to wait for.
    // @param timeout in microseconds.
    // @return the sequence up to which is available.
    //
    // @throws AlertException if a status change has occurred for the
    // Disruptor.
    virtual int64_t WaitFor(const int64_t& sequence,
                            const int64_t& timeout_micro) = 0;

    // Delegate a call to the {@link Sequencer#getCursor()}
    //
    //  @return value of the cursor for entries that have been published.
    virtual int64_t GetCursor() const = 0;

    // The current alert status for the barrier.
    //
    // @return true if in alert otherwise false.
    virtual bool IsAlerted() const = 0;

    // Alert the {@link EventProcessor}s of a status change and stay in this
    // status until cleared.
    virtual void Alert() = 0;

    // Clear the current alert status.
    virtual void ClearAlert() = 0;

    // Check if barrier is alerted, if so throws an AlertException
    //
    // @throws AlertException if barrier is alerted
    virtual void CheckAlert() const = 0;
};

// Called by the {@link RingBuffer} to pre-populate all the events to fill the
// RingBuffer.
//
// @param <T> event implementation storing the data for sharing during exchange
// or parallel coordination of an event.
template<typename T>
class EventFactoryInterface {
 public:
    virtual ~EventFactoryInterface(){}
     virtual T* NewInstance(const int& size) const = 0;
};

// Callback interface to be implemented for processing events as they become
// available in the {@link RingBuffer}.
//
// @param <T> event implementation storing the data for sharing during exchange
// or parallel coordination of an event.
template<typename T>
class EventHandlerInterface {
 public:
    // Called when a publisher has published an event to the {@link RingBuffer}
    //
    // @param event published to the {@link RingBuffer}
    // @param sequence of the event being processed
    // @param end_of_batch flag to indicate if this is the last event in a batch
    // from the {@link RingBuffer}
    //
    // @throws Exception if the EventHandler would like the exception handled
    // further up the chain.
    virtual ~EventHandlerInterface(){}
    virtual void OnEvent(const int64_t& sequence,
                         const bool& end_of_batch,
                         T* event)  = 0;

    // Called once on thread start before processing the first event.
    virtual void OnStart() = 0;

    // Called once on thread stop just before shutdown.
    virtual void OnShutdown() = 0;
};

// Implementations translate another data representations into events claimed
// for the {@link RingBuffer}.
//
// @param <T> event implementation storing the data for sharing during exchange
// or parallel coordination of an event.
template<typename T>
class EventTranslatorInterface {
 public:
     // Translate a data representation into fields set in given event
     //
     // @param event into which the data should be translated.
     // @param sequence that is assigned to events.
     // @return the resulting event after it has been translated.
     virtual ~EventTranslatorInterface(){}
     virtual T* TranslateTo(const int64_t& sequence, T* event) { return NULL;}
};

// EventProcessors wait for events to become available for consumption from
// the {@link RingBuffer}. An event processor should be associated with a
// thread.
//
// @param <T> event implementation storing the data for sharing during exchange
// or parallel coordination of an event.
template<typename T>
class EventProcessorInterface {
 public:
     // Get a pointer to the {@link Sequence} being used by this
     // {@link EventProcessor}.
     //
     // @return pointer to the {@link Sequence} for this
     // {@link EventProcessor}
     virtual ~EventProcessorInterface(){}
    virtual Sequence* GetSequence() = 0;

    // Signal that this EventProcessor should stop when it has finished
    // consuming at the next clean break.
    // It will call {@link DependencyBarrier#alert()} to notify the thread to
    // check status.
    virtual void Halt() = 0;
};

// Callback handler for uncaught exception in the event processing cycle
// of the {@link BatchEventProcessor}.
//
// @param <T> event type stored in the {@link RingBuffer}.
template<typename T>
class ExceptionHandlerInterface {
 public:
    // Strategy for handling uncaught exceptions when processing an event.
    // If the strategy wishes to suspend further processing by the
    // {@link BatchEventProcessor} then it should throw a std::runtime_error.
    //
    // @param exception that propagated from the {@link EventHandler}.
    // @param sequence of the event which caused the exception.
    // @param event being processed when the exception occured.
    virtual ~ExceptionHandlerInterface(){}
    virtual void Handle(const std::exception& exception,
                        const int64_t& sequence,
                        T* event) = 0;
};

// Strategy employed for making {@link EventProcessor}s wait on a cursor
// {@link Sequence}.
class WaitStrategyInterface: public boost::noncopyable {
 public:
    //  Wait for the given sequence to be available for consumption.
    //
    //  @param dependents further back the chain that must advance first.
    //  @param cursor on which to wait.
    //  @param barrier the consumer is waiting on.
    //  @param sequence to be waited on.
    //  @return the sequence that is available which may be greater than the
    //  requested sequence.
    //
    //  @throws AlertException if the status of the Disruptor has changed.
    virtual ~WaitStrategyInterface(){}
    virtual int64_t WaitFor(const std::vector<Sequence*>& dependents,
                            const Sequence& cursor,
                            const SequenceBarrierInterface& barrier,
                            const int64_t& sequence) = 0;

    //  Wait for the given sequence to be available for consumption in a
    //  {@link RingBuffer} with a timeout specified.
    //
    //  @param dependents further back the chain that must advance first
    //  @param cursor on which to wait.
    //  @param barrier the consumer is waiting on.
    //  @param sequence to be waited on.
    //  @param timeout value in micro seconds to abort after.
    //  @return the sequence that is available which may be greater than the
    //  requested sequence.
    //
    //  @throws AlertException if the status of the Disruptor has changed.
    //  @throws InterruptedException if the thread is interrupted.
    virtual int64_t WaitFor(const std::vector<Sequence*>& dependents,
                            const Sequence& cursor,
                            const SequenceBarrierInterface& barrier,
                            const int64_t & sequence,
                            const int64_t & timeout_micros) = 0;

    // Signal those waiting that the cursor has advanced.
    virtual void SignalAllWhenBlocking() = 0;
};

};  // namespace rocketmq

#endif // DISRUPTOR_INTERFACE_H_ NOLINT
