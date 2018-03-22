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
#ifndef __DATABLOCK_H__
#define __DATABLOCK_H__

#include <stddef.h>
#include <stdint.h>
#include <algorithm>
#include <cstdlib>
#include <cstring>

#include "RocketMQClient.h"

namespace rocketmq {

class ROCKETMQCLIENT_API MemoryBlock {
 public:
  //==============================================================================
  /** Create an uninitialised block with 0 size. */
  MemoryBlock();

  /** Creates a memory block with a given initial size.

      @param initialSize          the size of block to create
      @param initialiseToZero     whether to clear the memory or just leave it
     uninitialised
  */
  MemoryBlock(const int initialSize, bool initialiseToZero = false);

  /** Creates a memory block using a copy of a block of data.

      @param dataToInitialiseFrom     some data to copy into this block
      @param sizeInBytes              how much space to use
  */
  MemoryBlock(const void* dataToInitialiseFrom, size_t sizeInBytes);

  /** Creates a copy of another memory block. */
  MemoryBlock(const MemoryBlock&);

  /** Destructor. */
  ~MemoryBlock();

  /** Copies another memory block onto this one.
      This block will be resized and copied to exactly match the other one.
  */
  MemoryBlock& operator=(const MemoryBlock&);

  //==============================================================================
  /** Compares two memory blocks.
      @returns true only if the two blocks are the same size and have identical
     contents.
  */
  bool operator==(const MemoryBlock& other) const;

  /** Compares two memory blocks.
      @returns true if the two blocks are different sizes or have different
     contents.
  */
  bool operator!=(const MemoryBlock& other) const;

  //==============================================================================
  /** Returns a void pointer to the data.

      Note that the pointer returned will probably become invalid when the
      block is resized.
  */
  char* getData() const { return data; }

  /** Returns a byte from the memory block.
      This returns a reference, so you can also use it to set a byte.
  */
  template <typename Type>
  char& operator[](const Type offset) const {
    return data[offset];
  }

  /** Returns true if the data in this MemoryBlock matches the raw bytes
   * passed-in. */
  bool matches(const void* data, int dataSize) const;

  //==============================================================================
  /** Returns the block's current allocated size, in bytes. */
  int getSize() const { return size; }

  /** Resizes the memory block.

      Any data that is present in both the old and new sizes will be retained.
      When enlarging the block, the new space that is allocated at the end can
     either be
      cleared, or left uninitialised.

      @param newSize                      the new desired size for the block
      @param initialiseNewSpaceToZero     if the block gets enlarged, this
     determines
                                          whether to clear the new section or
     just leave it
                                          uninitialised
      @see ensureSize
  */
  void setSize(const int newSize, bool initialiseNewSpaceToZero = false);

  /** Increases the block's size only if it's smaller than a given size.

      @param minimumSize                  if the block is already bigger than
     this size, no action
                                          will be taken; otherwise it will be
     increased to this size
      @param initialiseNewSpaceToZero     if the block gets enlarged, this
     determines
                                          whether to clear the new section or
     just leave it
                                          uninitialised
      @see setSize
  */
  void ensureSize(const int minimumSize, bool initialiseNewSpaceToZero = false);

  /** Frees all the blocks data, setting its size to 0. */
  void reset();

  //==============================================================================
  /** Fills the entire memory block with a repeated byte value.
      This is handy for clearing a block of memory to zero.
  */
  void fillWith(int valueToUse);

  /** Adds another block of data to the end of this one.
      The data pointer must not be null. This block's size will be increased
     accordingly.
  */
  void append(const void* data, int numBytes);

  /** Resizes this block to the given size and fills its contents from the
     supplied buffer.
      The data pointer must not be null.
  */
  void replaceWith(const void* data, int numBytes);

  /** Inserts some data into the block.
      The dataToInsert pointer must not be null. This block's size will be
     increased accordingly.
      If the insert position lies outside the valid range of the block, it will
     be clipped to
      within the range before being used.
  */
  void insert(const void* dataToInsert, int numBytesToInsert,
              int insertPosition);

  /** Chops out a section  of the block.

      This will remove a section of the memory block and close the gap around
     it,
      shifting any subsequent data downwards and reducing the size of the block.

      If the range specified goes beyond the size of the block, it will be
     clipped.
  */
  void removeSection(int startByte, int numBytesToRemove);

  //==============================================================================
  /** Copies data into this MemoryBlock from a memory address.

      @param srcData              the memory location of the data to copy into
     this block
      @param destinationOffset    the offset in this block at which the data
     being copied should begin
      @param numBytes             how much to copy in (if this goes beyond the
     size of the memory block,
                                  it will be clipped so not to do anything
     nasty)
  */
  void copyFrom(const void* srcData, int destinationOffset, int numBytes);

  /** Copies data from this MemoryBlock to a memory address.

      @param destData         the memory location to write to
      @param sourceOffset     the offset within this block from which the copied
     data will be read
      @param numBytes         how much to copy (if this extends beyond the
     limits of the memory block,
                              zeros will be used for that portion of the data)
  */
  void copyTo(void* destData, int sourceOffset, int numBytes) const;

 private:
  //==============================================================================
  int size;
  char* data;
};
}

#endif
