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
#include "InputStream.h"
#include <algorithm>
#include "MemoryOutputStream.h"
#include "big_endian.h"

namespace rocketmq {
int64 InputStream::getNumBytesRemaining() {
  int64 len = getTotalLength();

  if (len >= 0) len -= getPosition();

  return len;
}

char InputStream::readByte() {
  char temp = 0;
  read(&temp, 1);
  return temp;
}

bool InputStream::readBool() { return readByte() != 0; }

short InputStream::readShortBigEndian() {
  char temp[2];

  if (read(temp, 2) == 2) {
    short int v;
    ReadBigEndian(temp, &v);
    return v;
  }

  return 0;
}

int InputStream::readIntBigEndian() {
  char temp[4];

  if (read(temp, 4) == 4) {
    int v;
    ReadBigEndian(temp, &v);
    return v;
  }
  return 0;
}

int64 InputStream::readInt64BigEndian() {
  char asBytes[8];
  uint64 asInt64;

  if (read(asBytes, 8) == 8) {
    ReadBigEndian(asBytes, &asInt64);
    return asInt64;
  }
  return 0;
}

float InputStream::readFloatBigEndian() {
  union {
    int32 asInt;
    float asFloat;
  } n;
  n.asInt = (int32)readIntBigEndian();
  return n.asFloat;
}

double InputStream::readDoubleBigEndian() {
  union {
    int64 asInt;
    double asDouble;
  } n;
  n.asInt = readInt64BigEndian();
  return n.asDouble;
}

size_t InputStream::readIntoMemoryBlock(MemoryBlock& block, size_t numBytes) {
  MemoryOutputStream mo(block, true);
  return (size_t)mo.writeFromInputStream(*this, numBytes);
}

//==============================================================================
void InputStream::skipNextBytes(int64 numBytesToSkip) {
  if (numBytesToSkip > 0) {
    const int skipBufferSize = (int)std::min(numBytesToSkip, (int64)16384);
    char* temp = static_cast<char*>(std::malloc(skipBufferSize * sizeof(char)));

    while (numBytesToSkip > 0 && !isExhausted())
      numBytesToSkip -=
          read(temp, (int)std::min(numBytesToSkip, (int64)skipBufferSize));

    std::free(temp);
  }
}
}
