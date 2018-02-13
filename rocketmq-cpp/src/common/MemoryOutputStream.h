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

#ifndef MEMORYOUTPUTSTREAM_H_INCLUDED
#define MEMORYOUTPUTSTREAM_H_INCLUDED

#include "OutputStream.h"

namespace rocketmq {
//==============================================================================
/**
    Writes data to an internal memory buffer, which grows as required.

    The data that was written into the stream can then be accessed later as
    a contiguous block of memory.
*/
class ROCKETMQCLIENT_API MemoryOutputStream : public OutputStream {
 public:
  //==============================================================================
  /** Creates an empty memory stream, ready to be written into.
      @param initialSize  the intial amount of capacity to allocate for writing
     into
  */
  MemoryOutputStream(size_t initialSize = 256);

  /** Creates a memory stream for writing into into a pre-existing MemoryBlock
     object.

      Note that the destination block will always be larger than the amount of
     data
      that has been written to the stream, because the MemoryOutputStream keeps
     some
      spare capactity at its end. To trim the block's size down to fit the
     actual
      data, call flush(), or delete the MemoryOutputStream.

      @param memoryBlockToWriteTo             the block into which new data will
     be written.
      @param appendToExistingBlockContent     if this is true, the contents of
     the block will be
                                              kept, and new data will be
     appended to it. If false,
                                              the block will be cleared before
     use
  */
  MemoryOutputStream(MemoryBlock& memoryBlockToWriteTo,
                     bool appendToExistingBlockContent);

  /** Creates a MemoryOutputStream that will write into a user-supplied,
     fixed-size
      block of memory.
      When using this mode, the stream will write directly into this memory area
     until
      it's full, at which point write operations will fail.
  */
  MemoryOutputStream(void* destBuffer, size_t destBufferSize);

  /** Destructor.
      This will free any data that was written to it.
  */
  ~MemoryOutputStream();

  //==============================================================================
  /** Returns a pointer to the data that has been written to the stream.
      @see getDataSize
  */
  const void* getData() const;

  /** Returns the number of bytes of data that have been written to the stream.
      @see getData
  */
  size_t getDataSize() const { return size; }

  /** Resets the stream, clearing any data that has been written to it so far.
   */
  void reset();

  /** Increases the internal storage capacity to be able to contain at least the
     specified
      amount of data without needing to be resized.
  */
  void preallocate(size_t bytesToPreallocate);

  /** Returns a copy of the stream's data as a memory block. */
  MemoryBlock getMemoryBlock() const;

  //==============================================================================
  /** If the stream is writing to a user-supplied MemoryBlock, this will trim
     any excess
      capacity off the block, so that its length matches the amount of actual
     data that
      has been written so far.
  */
  void flush();

  bool write(const void*, size_t);
  int64 getPosition() { return (int64)position; }
  bool setPosition(int64);
  int64 writeFromInputStream(InputStream&, int64 maxNumBytesToWrite);
  bool writeRepeatedByte(uint8 byte, size_t numTimesToRepeat);

 private:
  //==============================================================================
  MemoryBlock* const blockToUse;
  MemoryBlock internalBlock;
  void* externalData;
  size_t position, size, availableSize;

  void trimExternalBlockSize();
  char* prepareToWrite(size_t);
};

/** Copies all the data that has been written to a MemoryOutputStream into
 * another stream. */
OutputStream& operator<<(OutputStream& stream,
                         const MemoryOutputStream& streamToRead);
}
#endif  // MEMORYOUTPUTSTREAM_H_INCLUDED
