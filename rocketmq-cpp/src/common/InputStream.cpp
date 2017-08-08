#include "InputStream.h"
#include <algorithm>
#include "MemoryOutputStream.h"

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

short InputStream::readShort() {
  char temp[2];

  if (read(temp, 2) == 2) return (short)ByteOrder::littleEndianShort(temp);

  return 0;
}

short InputStream::readShortBigEndian() {
  char temp[2];

  if (read(temp, 2) == 2) return (short)ByteOrder::bigEndianShort(temp);

  return 0;
}

int InputStream::readInt() {
  char temp[4];

  if (read(temp, 4) == 4) return (int)ByteOrder::littleEndianInt(temp);

  return 0;
}

int InputStream::readIntBigEndian() {
  char temp[4];

  if (read(temp, 4) == 4) return (int)ByteOrder::bigEndianInt(temp);

  return 0;
}

int InputStream::readCompressedInt() {
  const uint8 sizeByte = (uint8)readByte();
  if (sizeByte == 0) return 0;

  const int numBytes = (sizeByte & 0x7f);
  if (numBytes > 4) {
    return 0;
  }

  char bytes[4] = {0, 0, 0, 0};
  if (read(bytes, numBytes) != numBytes) return 0;

  const int num = (int)ByteOrder::littleEndianInt(bytes);
  return (sizeByte >> 7) ? -num : num;
}

int64 InputStream::readInt64() {
  union {
    uint8 asBytes[8];
    uint64 asInt64;
  } n;

  if (read(n.asBytes, 8) == 8)
    return (int64)ByteOrder::swapIfBigEndian(n.asInt64);

  return 0;
}

int64 InputStream::readInt64BigEndian() {
  union {
    uint8 asBytes[8];
    uint64 asInt64;
  } n;

  if (read(n.asBytes, 8) == 8)
    return (int64)ByteOrder::swapIfLittleEndian(n.asInt64);

  return 0;
}

float InputStream::readFloat() {
  // the union below relies on these types being the same size...
  union {
    int32 asInt;
    float asFloat;
  } n;
  n.asInt = (int32)readInt();
  return n.asFloat;
}

float InputStream::readFloatBigEndian() {
  union {
    int32 asInt;
    float asFloat;
  } n;
  n.asInt = (int32)readIntBigEndian();
  return n.asFloat;
}

double InputStream::readDouble() {
  union {
    int64 asInt;
    double asDouble;
  } n;
  n.asInt = readInt64();
  return n.asDouble;
}

double InputStream::readDoubleBigEndian() {
  union {
    int64 asInt;
    double asDouble;
  } n;
  n.asInt = readInt64BigEndian();
  return n.asDouble;
}

size_t InputStream::readIntoMemoryBlock(MemoryBlock& block, ssize_t numBytes) {
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
