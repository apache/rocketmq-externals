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
#ifndef BYTEORDER_H_INCLUDED
#define BYTEORDER_H_INCLUDED

#include <stddef.h>
#include <stdint.h>
#include <algorithm>
#include <boost/detail/endian.hpp>
#include "RocketMQClient.h"
#include "UtilAll.h"
//==============================================================================
/** Contains static methods for converting the byte order between different
    endiannesses.
*/
namespace rocketmq {

class ROCKETMQCLIENT_API ByteOrder {
 public:
  //==============================================================================
  /** Swaps the upper and lower bytes of a 16-bit integer. */
  static uint16 swap(uint16 value);

  /** Reverses the order of the 4 bytes in a 32-bit integer. */
  static uint32 swap(uint32 value);

  /** Reverses the order of the 8 bytes in a 64-bit integer. */
  static uint64 swap(uint64 value);

  //==============================================================================
  /** Swaps the byte order of a 16-bit int if the CPU is big-endian */
  static uint16 swapIfBigEndian(uint16 value);

  /** Swaps the byte order of a 32-bit int if the CPU is big-endian */
  static uint32 swapIfBigEndian(uint32 value);

  /** Swaps the byte order of a 64-bit int if the CPU is big-endian */
  static uint64 swapIfBigEndian(uint64 value);

  /** Swaps the byte order of a 16-bit int if the CPU is little-endian */
  static uint16 swapIfLittleEndian(uint16 value);

  /** Swaps the byte order of a 32-bit int if the CPU is little-endian */
  static uint32 swapIfLittleEndian(uint32 value);

  /** Swaps the byte order of a 64-bit int if the CPU is little-endian */
  static uint64 swapIfLittleEndian(uint64 value);

  //==============================================================================
  /** Turns 4 bytes into a little-endian integer. */
  static uint32 littleEndianInt(const void* bytes);

  /** Turns 8 bytes into a little-endian integer. */
  static uint64 littleEndianInt64(const void* bytes);

  /** Turns 2 bytes into a little-endian integer. */
  static uint16 littleEndianShort(const void* bytes);

  /** Turns 4 bytes into a big-endian integer. */
  static uint32 bigEndianInt(const void* bytes);

  /** Turns 8 bytes into a big-endian integer. */
  static uint64 bigEndianInt64(const void* bytes);

  /** Turns 2 bytes into a big-endian integer. */
  static uint16 bigEndianShort(const void* bytes);

  //==============================================================================
  /** Converts 3 little-endian bytes into a signed 24-bit value (which is
   * sign-extended to 32 bits). */
  static int littleEndian24Bit(const void* bytes);

  /** Converts 3 big-endian bytes into a signed 24-bit value (which is
   * sign-extended to 32 bits). */
  static int bigEndian24Bit(const void* bytes);

  /** Copies a 24-bit number to 3 little-endian bytes. */
  static void littleEndian24BitToChars(int value, void* destBytes);

  /** Copies a 24-bit number to 3 big-endian bytes. */
  static void bigEndian24BitToChars(int value, void* destBytes);

  //==============================================================================
  /** Returns true if the current CPU is big-endian. */
  static bool isBigEndian();
};

//==============================================================================

inline uint16 ByteOrder::swap(uint16 n) {
  return static_cast<uint16>((n << 8) | (n >> 8));
}

inline uint32 ByteOrder::swap(uint32 n) {
  return (n << 24) | (n >> 24) | ((n & 0xff00) << 8) | ((n & 0xff0000) >> 8);
}

inline uint64 ByteOrder::swap(uint64 value) {
  return (((uint64)swap((uint32)value)) << 32) | swap((uint32)(value >> 32));
}

#if __BYTE_ORDER__ == \
    __ORDER_LITTLE_ENDIAN__  //__BYTE_ORDER__ is defined by GCC
inline uint16 ByteOrder::swapIfBigEndian(const uint16 v) { return v; }
inline uint32 ByteOrder::swapIfBigEndian(const uint32 v) { return v; }
inline uint64 ByteOrder::swapIfBigEndian(const uint64 v) { return v; }
inline uint16 ByteOrder::swapIfLittleEndian(const uint16 v) { return swap(v); }
inline uint32 ByteOrder::swapIfLittleEndian(const uint32 v) { return swap(v); }
inline uint64 ByteOrder::swapIfLittleEndian(const uint64 v) { return swap(v); }
inline uint32 ByteOrder::littleEndianInt(const void* const bytes) {
  return *static_cast<const uint32*>(bytes);
}
inline uint64 ByteOrder::littleEndianInt64(const void* const bytes) {
  return *static_cast<const uint64*>(bytes);
}
inline uint16 ByteOrder::littleEndianShort(const void* const bytes) {
  return *static_cast<const uint16*>(bytes);
}
inline uint32 ByteOrder::bigEndianInt(const void* const bytes) {
  return swap(*static_cast<const uint32*>(bytes));
}
inline uint64 ByteOrder::bigEndianInt64(const void* const bytes) {
  return swap(*static_cast<const uint64*>(bytes));
}
inline uint16 ByteOrder::bigEndianShort(const void* const bytes) {
  return swap(*static_cast<const uint16*>(bytes));
}
inline bool ByteOrder::isBigEndian() { return false; }
#else
inline uint16 ByteOrder::swapIfBigEndian(const uint16 v) { return swap(v); }
inline uint32 ByteOrder::swapIfBigEndian(const uint32 v) { return swap(v); }
inline uint64 ByteOrder::swapIfBigEndian(const uint64 v) { return swap(v); }
inline uint16 ByteOrder::swapIfLittleEndian(const uint16 v) { return v; }
inline uint32 ByteOrder::swapIfLittleEndian(const uint32 v) { return v; }
inline uint64 ByteOrder::swapIfLittleEndian(const uint64 v) { return v; }
inline uint32 ByteOrder::littleEndianInt(const void* const bytes) {
  return swap(*static_cast<const uint32*>(bytes));
}
inline uint64 ByteOrder::littleEndianInt64(const void* const bytes) {
  return swap(*static_cast<const uint64*>(bytes));
}
inline uint16 ByteOrder::littleEndianShort(const void* const bytes) {
  return swap(*static_cast<const uint16*>(bytes));
}
inline uint32 ByteOrder::bigEndianInt(const void* const bytes) {
  return *static_cast<const uint32*>(bytes);
}
inline uint64 ByteOrder::bigEndianInt64(const void* const bytes) {
  return *static_cast<const uint64*>(bytes);
}
inline uint16 ByteOrder::bigEndianShort(const void* const bytes) {
  return *static_cast<const uint16*>(bytes);
}
inline bool ByteOrder::isBigEndian() { return true; }
#endif

inline int ByteOrder::littleEndian24Bit(const void* const bytes) {
  return (((int)static_cast<const int8*>(bytes)[2]) << 16) |
         (((int)static_cast<const uint8*>(bytes)[1]) << 8) |
         ((int)static_cast<const uint8*>(bytes)[0]);
}
inline int ByteOrder::bigEndian24Bit(const void* const bytes) {
  return (((int)static_cast<const int8*>(bytes)[0]) << 16) |
         (((int)static_cast<const uint8*>(bytes)[1]) << 8) |
         ((int)static_cast<const uint8*>(bytes)[2]);
}
inline void ByteOrder::littleEndian24BitToChars(const int value,
                                                void* const destBytes) {
  static_cast<uint8*>(destBytes)[0] = (uint8)value;
  static_cast<uint8*>(destBytes)[1] = (uint8)(value >> 8);
  static_cast<uint8*>(destBytes)[2] = (uint8)(value >> 16);
}
inline void ByteOrder::bigEndian24BitToChars(const int value,
                                             void* const destBytes) {
  static_cast<uint8*>(destBytes)[0] = (uint8)(value >> 16);
  static_cast<uint8*>(destBytes)[1] = (uint8)(value >> 8);
  static_cast<uint8*>(destBytes)[2] = (uint8)value;
}
}
#endif  // BYTEORDER_H_INCLUDED
