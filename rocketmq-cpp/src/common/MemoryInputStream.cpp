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
#include "MemoryInputStream.h"

namespace rocketmq {
MemoryInputStream::MemoryInputStream(const void* const sourceData,
                                     const size_t sourceDataSize,
                                     const bool keepInternalCopy)
    : data(sourceData),
      dataSize(sourceDataSize),
      position(0),
      internalCopy(NULL) {
  if (keepInternalCopy) createInternalCopy();
}

MemoryInputStream::MemoryInputStream(const MemoryBlock& sourceData,
                                     const bool keepInternalCopy)
    : data(sourceData.getData()),
      dataSize(sourceData.getSize()),
      position(0),
      internalCopy(NULL) {
  if (keepInternalCopy) createInternalCopy();
}

void MemoryInputStream::createInternalCopy() {
  std::free(internalCopy);
  internalCopy = static_cast<char*>(std::malloc(dataSize));
  memcpy(internalCopy, data, dataSize);
  data = internalCopy;
}

MemoryInputStream::~MemoryInputStream() { std::free(internalCopy); }

int64 MemoryInputStream::getTotalLength() { return (int64)dataSize; }

int MemoryInputStream::read(void* const buffer, const int howMany) {
  const int num = std::min(howMany, (int)(dataSize - position));
  if (num <= 0) return 0;

  memcpy((char*)buffer, (char*)data + position, (size_t)num);
  position += (unsigned int)num;
  return num;
}

bool MemoryInputStream::isExhausted() { return position >= dataSize; }

bool MemoryInputStream::setPosition(const int64 pos) {
  if (pos < 0)
    position = 0;
  else
    position = (int64)dataSize < pos ? (int64)dataSize : pos;

  return true;
}

int64 MemoryInputStream::getPosition() { return (int64)position; }
}
