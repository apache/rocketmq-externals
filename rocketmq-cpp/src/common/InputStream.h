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

#ifndef INPUTSTREAM_H_INCLUDED
#define INPUTSTREAM_H_INCLUDED

#include "dataBlock.h"
//==============================================================================
/** The base class for streams that read data.

    Input and output streams are used throughout the library - subclasses can
   override
    some or all of the virtual functions to implement their behaviour.

    @see OutputStream, MemoryInputStream, BufferedInputStream, FileInputStream
*/
namespace rocketmq {
class ROCKETMQCLIENT_API InputStream {
 public:
  /** Destructor. */
  virtual ~InputStream() {}

  //==============================================================================
  /** Returns the total number of bytes available for reading in this stream.

      Note that this is the number of bytes available from the start of the
      stream, not from the current position.

      If the size of the stream isn't actually known, this will return -1.

      @see getNumBytesRemaining
  */
  virtual int64 getTotalLength() = 0;

  /** Returns the number of bytes available for reading, or a negative value if
      the remaining length is not known.
      @see getTotalLength
  */
  int64 getNumBytesRemaining();

  /** Returns true if the stream has no more data to read. */
  virtual bool isExhausted() = 0;

  //==============================================================================
  /** Reads some data from the stream into a memory buffer.

      This is the only read method that subclasses actually need to implement,
     as the
      InputStream base class implements the other read methods in terms of this
     one (although
      it's often more efficient for subclasses to implement them directly).

      @param destBuffer       the destination buffer for the data. This must not
     be null.
      @param maxBytesToRead   the maximum number of bytes to read - make sure
     the
                              memory block passed in is big enough to contain
     this
                              many bytes. This value must not be negative.

      @returns    the actual number of bytes that were read, which may be less
     than
                  maxBytesToRead if the stream is exhausted before it gets that
     far
  */
  virtual int read(void* destBuffer, int maxBytesToRead) = 0;

  /** Reads a byte from the stream.
      If the stream is exhausted, this will return zero.
      @see OutputStream::writeByte
  */
  virtual char readByte();

  /** Reads a boolean from the stream.
      The bool is encoded as a single byte - non-zero for true, 0 for false.
      If the stream is exhausted, this will return false.
      @see OutputStream::writeBool
  */
  virtual bool readBool();

  /** Reads two bytes from the stream as a little-endian 16-bit value.
      If the next two bytes read are byte1 and byte2, this returns (byte2 |
     (byte1 << 8)).
      If the stream is exhausted partway through reading the bytes, this will
     return zero.
      @see OutputStream::writeShortBigEndian, readShort
  */
  virtual short readShortBigEndian();

  /** Reads four bytes from the stream as a big-endian 32-bit value.

      If the next four bytes are byte1 to byte4, this returns
      (byte4 | (byte3 << 8) | (byte2 << 16) | (byte1 << 24)).

      If the stream is exhausted partway through reading the bytes, this will
     return zero.

      @see OutputStream::writeIntBigEndian, readInt
  */
  virtual int readIntBigEndian();

  /** Reads eight bytes from the stream as a big-endian 64-bit value.

      If the next eight bytes are byte1 to byte8, this returns
      (byte8 | (byte7 << 8) | (byte6 << 16) | (byte5 << 24) | (byte4 << 32) |
     (byte3 << 40) | (byte2 << 48) | (byte1 << 56)).

      If the stream is exhausted partway through reading the bytes, this will
     return zero.

      @see OutputStream::writeInt64BigEndian, readInt64
  */
  virtual int64 readInt64BigEndian();

  /** Reads four bytes as a 32-bit floating point value.
      The raw 32-bit encoding of the float is read from the stream as a
     big-endian int.
      If the stream is exhausted partway through reading the bytes, this will
     return zero.
      @see OutputStream::writeFloatBigEndian, readDoubleBigEndian
  */
  virtual float readFloatBigEndian();

  /** Reads eight bytes as a 64-bit floating point value.
      The raw 64-bit encoding of the double is read from the stream as a
     big-endian int64.
      If the stream is exhausted partway through reading the bytes, this will
     return zero.
      @see OutputStream::writeDoubleBigEndian, readFloatBigEndian
  */
  virtual double readDoubleBigEndian();

  //==============================================================================whole
  // stream and turn it into a string.
  /** Reads from the stream and appends the data to a MemoryBlock.

      @param destBlock            the block to append the data onto
      @param maxNumBytesToRead    if this is a positive value, it sets a limit
     to the number
                                  of bytes that will be read - if it's negative,
     data
                                  will be read until the stream is exhausted.
      @returns the number of bytes that were added to the memory block
  */
  virtual size_t readIntoMemoryBlock(MemoryBlock& destBlock,
                                     size_t maxNumBytesToRead = -1);

  //==============================================================================
  /** Returns the offset of the next byte that will be read from the stream.
      @see setPosition
  */
  virtual int64 getPosition() = 0;

  /** Tries to move the current read position of the stream.

      The position is an absolute number of bytes from the stream's start.

      Some streams might not be able to do this, in which case they should do
      nothing and return false. Others might be able to manage it by resetting
      themselves and skipping to the correct position, although this is
      obviously a bit slow.

      @returns  true if the stream manages to reposition itself correctly
      @see getPosition
  */
  virtual bool setPosition(int64 newPosition) = 0;

  /** Reads and discards a number of bytes from the stream.

      Some input streams might implement this efficiently, but the base
      class will just keep reading data until the requisite number of bytes
      have been done.
  */
  virtual void skipNextBytes(int64 numBytesToSkip);

 protected:
  //==============================================================================
  InputStream() {}
};
}
#endif  // INPUTSTREAM_H_INCLUDED
