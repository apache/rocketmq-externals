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
#include "MemoryOutputStream.h"

namespace rocketmq {
MemoryOutputStream::MemoryOutputStream(const size_t initialSize)
    : blockToUse(&internalBlock),
      externalData(NULL),
      position(0),
      size(0),
      availableSize(0) {
  internalBlock.setSize(initialSize, false);
}

MemoryOutputStream::MemoryOutputStream(MemoryBlock& memoryBlockToWriteTo,
                                       const bool appendToExistingBlockContent)
    : blockToUse(&memoryBlockToWriteTo),
      externalData(NULL),
      position(0),
      size(0),
      availableSize(0) {
  if (appendToExistingBlockContent)
    position = size = memoryBlockToWriteTo.getSize();
}

MemoryOutputStream::MemoryOutputStream(void* destBuffer, size_t destBufferSize)
    : blockToUse(NULL),
      externalData(destBuffer),
      position(0),
      size(0),
      availableSize(destBufferSize) {}

MemoryOutputStream::~MemoryOutputStream() { trimExternalBlockSize(); }

void MemoryOutputStream::flush() { trimExternalBlockSize(); }

void MemoryOutputStream::trimExternalBlockSize() {
  if (blockToUse != &internalBlock && blockToUse != NULL)
    blockToUse->setSize(size, false);
}

void MemoryOutputStream::preallocate(const size_t bytesToPreallocate) {
  if (blockToUse != NULL) blockToUse->ensureSize(bytesToPreallocate + 1);
}

void MemoryOutputStream::reset() {
  position = 0;
  size = 0;
}

char* MemoryOutputStream::prepareToWrite(size_t numBytes) {
  size_t storageNeeded = position + numBytes;

  char* data;

  if (blockToUse != NULL) {
    if (storageNeeded >= (unsigned int)(blockToUse->getSize()))
      blockToUse->ensureSize(
          (storageNeeded + std::min(storageNeeded / 2, (size_t)(1024 * 1024)) +
           32) &
          ~31u);

    data = static_cast<char*>(blockToUse->getData());
  } else {
    if (storageNeeded > availableSize) return NULL;

    data = static_cast<char*>(externalData);
  }

  char* const writePointer = data + position;
  position += numBytes;
  size = std::max(size, position);
  return writePointer;
}

bool MemoryOutputStream::write(const void* const buffer, size_t howMany) {
  if (howMany == 0) return true;

  if (char* dest = prepareToWrite(howMany)) {
    memcpy(dest, buffer, howMany);
    return true;
  }

  return false;
}

bool MemoryOutputStream::writeRepeatedByte(uint8 byte, size_t howMany) {
  if (howMany == 0) return true;

  if (char* dest = prepareToWrite(howMany)) {
    memset(dest, byte, howMany);
    return true;
  }

  return false;
}

MemoryBlock MemoryOutputStream::getMemoryBlock() const {
  return MemoryBlock(getData(), getDataSize());
}

const void* MemoryOutputStream::getData() const {
  if (blockToUse == NULL) return externalData;

  if ((unsigned int)blockToUse->getSize() > size)
    static_cast<char*>(blockToUse->getData())[size] = 0;

  return blockToUse->getData();
}

bool MemoryOutputStream::setPosition(int64 newPosition) {
  if (newPosition <= (int64)size) {
    // ok to seek backwards
    if (newPosition < 0)
      position = 0;
    else
      position = (int64)size < newPosition ? size : newPosition;
    return true;
  }

  // can't move beyond the end of the stream..
  return false;
}

int64 MemoryOutputStream::writeFromInputStream(InputStream& source,
                                               int64 maxNumBytesToWrite) {
  // before writing from an input, see if we can preallocate to make it more
  // efficient..
  int64 availableData = source.getTotalLength() - source.getPosition();

  if (availableData > 0) {
    if (maxNumBytesToWrite > availableData || maxNumBytesToWrite < 0)
      maxNumBytesToWrite = availableData;

    if (blockToUse != NULL)
      preallocate(blockToUse->getSize() + (size_t)maxNumBytesToWrite);
  }

  return OutputStream::writeFromInputStream(source, maxNumBytesToWrite);
}

OutputStream& operator<<(OutputStream& stream,
                         const MemoryOutputStream& streamToRead) {
  const size_t dataSize = streamToRead.getDataSize();

  if (dataSize > 0) stream.write(streamToRead.getData(), dataSize);

  return stream;
}
}
