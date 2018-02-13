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
#include "dataBlock.h"
#include <algorithm>

namespace rocketmq {
MemoryBlock::MemoryBlock() : size(0), data(NULL) {}

MemoryBlock::MemoryBlock(const int initialSize, const bool initialiseToZero)
    : size(0), data(NULL) {
  if (initialSize > 0) {
    size = initialSize;
    data = static_cast<char*>(initialiseToZero
                                  ? std::calloc(initialSize, sizeof(char))
                                  : std::malloc(initialSize * sizeof(char)));
  }
}

MemoryBlock::MemoryBlock(const void* const dataToInitialiseFrom,
                         const size_t sizeInBytes)
    : size(sizeInBytes), data(NULL) {
  if (size > 0) {
    data = static_cast<char*>(std::malloc(size * sizeof(char)));

    if (dataToInitialiseFrom != NULL) memcpy(data, dataToInitialiseFrom, size);
  }
}

MemoryBlock::MemoryBlock(const MemoryBlock& other)
    : size(other.size), data(NULL) {
  if (size > 0) {
    data = static_cast<char*>(std::malloc(size * sizeof(char)));
    memcpy(data, other.data, size);
  }
}

MemoryBlock::~MemoryBlock() { std::free(data); }

MemoryBlock& MemoryBlock::operator=(const MemoryBlock& other) {
  if (this != &other) {
    setSize(other.size, false);
    memcpy(data, other.data, size);
  }

  return *this;
}

//==============================================================================
bool MemoryBlock::operator==(const MemoryBlock& other) const {
  return matches(other.data, other.size);
}

bool MemoryBlock::operator!=(const MemoryBlock& other) const {
  return !operator==(other);
}

bool MemoryBlock::matches(const void* dataToCompare, int dataSize) const {
  return size == dataSize && memcmp(data, dataToCompare, size) == 0;
}

//==============================================================================
// this will resize the block to this size
void MemoryBlock::setSize(const int newSize, const bool initialiseToZero) {
  if (size != newSize) {
    if (newSize <= 0) {
      reset();
    } else {
      if (data != NULL) {
        data = static_cast<char*>(
            data == NULL ? std::malloc(newSize * sizeof(char))
                         : std::realloc(data, newSize * sizeof(char)));

        if (initialiseToZero && (newSize > size))
          memset(data + size, 0, newSize - size);
      } else {
        std::free(data);
        data = static_cast<char*>(initialiseToZero
                                      ? std::calloc(newSize, sizeof(char))
                                      : std::malloc(newSize * sizeof(char)));
      }

      size = newSize;
    }
  }
}

void MemoryBlock::reset() {
  std::free(data);
  data = NULL;
  size = 0;
}

void MemoryBlock::ensureSize(const int minimumSize,
                             const bool initialiseToZero) {
  if (size < minimumSize) setSize(minimumSize, initialiseToZero);
}

//==============================================================================
void MemoryBlock::fillWith(const int value) { memset(data, (int)value, size); }

void MemoryBlock::append(const void* const srcData, const int numBytes) {
  if (numBytes > 0) {
    const int oldSize = size;
    setSize(size + numBytes);
    memcpy(data + oldSize, srcData, numBytes);
  }
}

void MemoryBlock::replaceWith(const void* const srcData, const int numBytes) {
  if (numBytes > 0) {
    setSize(numBytes);
    memcpy(data, srcData, numBytes);
  }
}

void MemoryBlock::insert(const void* const srcData, const int numBytes,
                         int insertPosition) {
  if (numBytes > 0) {
    insertPosition = std::min(insertPosition, size);
    const int trailingDataSize = size - insertPosition;
    setSize(size + numBytes, false);

    if (trailingDataSize > 0)
      memmove(data + insertPosition + numBytes, data + insertPosition,
              trailingDataSize);

    memcpy(data + insertPosition, srcData, numBytes);
  }
}

void MemoryBlock::removeSection(const int startByte,
                                const int numBytesToRemove) {
  if (startByte + numBytesToRemove >= size) {
    setSize(startByte);
  } else if (numBytesToRemove > 0) {
    memmove(data + startByte, data + startByte + numBytesToRemove,
            size - (startByte + numBytesToRemove));

    setSize(size - numBytesToRemove);
  }
}

void MemoryBlock::copyFrom(const void* const src, int offset, int num) {
  const char* d = static_cast<const char*>(src);

  if (offset < 0) {
    d -= offset;
    num += (size_t)-offset;
    offset = 0;
  }

  if ((size_t)offset + num > (unsigned int)size) num = size - (size_t)offset;

  if (num > 0) memcpy(data + offset, d, num);
}

void MemoryBlock::copyTo(void* const dst, int offset, int num) const {
  char* d = static_cast<char*>(dst);

  if (offset < 0) {
    memset(d, 0, (size_t)-offset);
    d -= offset;
    num -= (size_t)-offset;
    offset = 0;
  }

  if ((size_t)offset + num > (unsigned int)size) {
    const int newNum = (size_t)size - (size_t)offset;
    memset(d + newNum, 0, num - newNum);
    num = newNum;
  }

  if (num > 0) memcpy(d, data + offset, num);
}
}
