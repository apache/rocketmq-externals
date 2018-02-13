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

#ifndef OUTPUTSTREAM_H_INCLUDED
#define OUTPUTSTREAM_H_INCLUDED

#include "InputStream.h"
namespace rocketmq {
//==============================================================================
/**
    The base class for streams that write data to some kind of destination.

    Input and output streams are used throughout the library - subclasses can
   override
    some or all of the virtual functions to implement their behaviour.

    @see InputStream, MemoryOutputStream, FileOutputStream
*/
class ROCKETMQCLIENT_API OutputStream {
 protected:
  //==============================================================================
  OutputStream();

 public:
  /** Destructor.

      Some subclasses might want to do things like call flush() during their
      destructors.
  */
  virtual ~OutputStream();

  //==============================================================================
  /** If the stream is using a buffer, this will ensure it gets written
      out to the destination. */
  virtual void flush() = 0;

  /** Tries to move the stream's output position.

      Not all streams will be able to seek to a new position - this will return
      false if it fails to work.

      @see getPosition
  */
  virtual bool setPosition(int64 newPosition) = 0;

  /** Returns the stream's current position.

      @see setPosition
  */
  virtual int64 getPosition() = 0;

  //==============================================================================
  /** Writes a block of data to the stream.

      When creating a subclass of OutputStream, this is the only write method
      that needs to be overloaded - the base class has methods for writing other
      types of data which use this to do the work.

      @param dataToWrite      the target buffer to receive the data. This must
     not be null.
      @param numberOfBytes    the number of bytes to write.
      @returns false if the write operation fails for some reason
  */
  virtual bool write(const void* dataToWrite, size_t numberOfBytes) = 0;

  //==============================================================================
  /** Writes a single byte to the stream.
      @returns false if the write operation fails for some reason
      @see InputStream::readByte
  */
  virtual bool writeByte(char byte);

  /** Writes a boolean to the stream as a single byte.
      This is encoded as a binary byte (not as text) with a value of 1 or 0.
      @returns false if the write operation fails for some reason
      @see InputStream::readBool
  */
  virtual bool writeBool(bool boolValue);

  /** Writes a 16-bit integer to the stream in a big-endian byte order.
      This will write two bytes to the stream: (value >> 8), then (value &
     0xff).
      @returns false if the write operation fails for some reason
      @see InputStream::readShortBigEndian
  */
  virtual bool writeShortBigEndian(short value);

  /** Writes a 32-bit integer to the stream in a big-endian byte order.
      @returns false if the write operation fails for some reason
      @see InputStream::readIntBigEndian
  */
  virtual bool writeIntBigEndian(int value);

  /** Writes a 64-bit integer to the stream in a big-endian byte order.
      @returns false if the write operation fails for some reason
      @see InputStream::readInt64BigEndian
  */
  virtual bool writeInt64BigEndian(int64 value);

  /** Writes a 32-bit floating point value to the stream in a binary format.
      The binary 32-bit encoding of the float is written as a big-endian int.
      @returns false if the write operation fails for some reason
      @see InputStream::readFloatBigEndian
  */
  virtual bool writeFloatBigEndian(float value);

  /** Writes a 64-bit floating point value to the stream in a binary format.
      The eight raw bytes of the double value are written out as a big-endian
     64-bit int.
      @see InputStream::readDoubleBigEndian
      @returns false if the write operation fails for some reason
  */
  virtual bool writeDoubleBigEndian(double value);

  /** Writes a byte to the output stream a given number of times.
      @returns false if the write operation fails for some reason
  */
  virtual bool writeRepeatedByte(uint8 byte, size_t numTimesToRepeat);

  /** Reads data from an input stream and writes it to this stream.

      @param source               the stream to read from
      @param maxNumBytesToWrite   the number of bytes to read from the stream
     (if this is
                                  less than zero, it will keep reading until the
     input
                                  is exhausted)
      @returns the number of bytes written
  */
  virtual int64 writeFromInputStream(InputStream& source,
                                     int64 maxNumBytesToWrite);
};
}

#endif  // OUTPUTSTREAM_H_INCLUDED
