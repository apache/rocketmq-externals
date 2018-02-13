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

#ifndef MEMORYINPUTSTREAM_H_INCLUDED
#define MEMORYINPUTSTREAM_H_INCLUDED

#include "InputStream.h"

namespace rocketmq {
//==============================================================================
/**
    Allows a block of data to be accessed as a stream.

    This can either be used to refer to a shared block of memory, or can make
   its
    own internal copy of the data when the MemoryInputStream is created.
*/
class ROCKETMQCLIENT_API MemoryInputStream : public InputStream {
 public:
  //==============================================================================
  /** Creates a MemoryInputStream.

      @param sourceData               the block of data to use as the stream's
     source
      @param sourceDataSize           the number of bytes in the source data
     block
      @param keepInternalCopyOfData   if false, the stream will just keep a
     pointer to
                                      the source data, so this data shouldn't be
     changed
                                      for the lifetime of the stream; if this
     parameter is
                                      true, the stream will make its own copy of
     the
                                      data and use that.
  */
  MemoryInputStream(const void* sourceData, size_t sourceDataSize,
                    bool keepInternalCopyOfData);

  /** Creates a MemoryInputStream.

      @param data                     a block of data to use as the stream's
     source
      @param keepInternalCopyOfData   if false, the stream will just keep a
     reference to
                                      the source data, so this data shouldn't be
     changed
                                      for the lifetime of the stream; if this
     parameter is
                                      true, the stream will make its own copy of
     the
                                      data and use that.
  */
  MemoryInputStream(const MemoryBlock& data, bool keepInternalCopyOfData);

  /** Destructor. */
  ~MemoryInputStream();

  /** Returns a pointer to the source data block from which this stream is
   * reading. */
  const void* getData() const { return data; }

  /** Returns the number of bytes of source data in the block from which this
   * stream is reading. */
  size_t getDataSize() const { return dataSize; }

  //==============================================================================
  int64 getPosition();
  bool setPosition(int64 pos);
  int64 getTotalLength();
  bool isExhausted();
  int read(void* destBuffer, int maxBytesToRead);

 private:
  //==============================================================================
  const void* data;
  size_t dataSize, position;
  char* internalCopy;

  void createInternalCopy();
};
}
#endif
