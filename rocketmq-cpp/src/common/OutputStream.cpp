#include "OutputStream.h"
#include <limits>

namespace rocketmq {
//==============================================================================
OutputStream::OutputStream() {}

OutputStream::~OutputStream() {}

//==============================================================================
bool OutputStream::writeBool(const bool b) {
  return writeByte(b ? (char)1 : (char)0);
}

bool OutputStream::writeByte(char byte) { return write(&byte, 1); }

bool OutputStream::writeRepeatedByte(uint8 byte, size_t numTimesToRepeat) {
  for (size_t i = 0; i < numTimesToRepeat; ++i)
    if (!writeByte((char)byte)) return false;

  return true;
}

bool OutputStream::writeShort(short value) {
  const unsigned short v = ByteOrder::swapIfBigEndian((unsigned short)value);
  return write(&v, 2);
}

bool OutputStream::writeShortBigEndian(short value) {
  const unsigned short v = ByteOrder::swapIfLittleEndian((unsigned short)value);
  return write(&v, 2);
}

bool OutputStream::writeInt(int value) {
  const unsigned int v = ByteOrder::swapIfBigEndian((unsigned int)value);
  return write(&v, 4);
}

bool OutputStream::writeIntBigEndian(int value) {
  const unsigned int v = ByteOrder::swapIfLittleEndian((unsigned int)value);
  return write(&v, 4);
}

bool OutputStream::writeCompressedInt(int value) {
  unsigned int un = (value < 0) ? (unsigned int)-value : (unsigned int)value;

  uint8 data[5];
  int num = 0;

  while (un > 0) {
    data[++num] = (uint8)un;
    un >>= 8;
  }

  data[0] = (uint8)num;

  if (value < 0) data[0] |= 0x80;

  return write(data, (size_t)num + 1);
}

bool OutputStream::writeInt64(int64 value) {
  const uint64 v = ByteOrder::swapIfBigEndian((uint64)value);
  return write(&v, 8);
}

bool OutputStream::writeInt64BigEndian(int64 value) {
  const uint64 v = ByteOrder::swapIfLittleEndian((uint64)value);
  return write(&v, 8);
}

bool OutputStream::writeFloat(float value) {
  union {
    int asInt;
    float asFloat;
  } n;
  n.asFloat = value;
  return writeInt(n.asInt);
}

bool OutputStream::writeFloatBigEndian(float value) {
  union {
    int asInt;
    float asFloat;
  } n;
  n.asFloat = value;
  return writeIntBigEndian(n.asInt);
}

bool OutputStream::writeDouble(double value) {
  union {
    int64 asInt;
    double asDouble;
  } n;
  n.asDouble = value;
  return writeInt64(n.asInt);
}

bool OutputStream::writeDoubleBigEndian(double value) {
  union {
    int64 asInt;
    double asDouble;
  } n;
  n.asDouble = value;
  return writeInt64BigEndian(n.asInt);
}

int64 OutputStream::writeFromInputStream(InputStream& source,
                                         int64 numBytesToWrite) {
  if (numBytesToWrite < 0) numBytesToWrite = std::numeric_limits<int64>::max();

  int64 numWritten = 0;

  while (numBytesToWrite > 0) {
    char buffer[8192];
    const int num = source.read(
        buffer, (int)std::min(numBytesToWrite, (int64)sizeof(buffer)));

    if (num <= 0) break;

    write(buffer, (size_t)num);

    numBytesToWrite -= num;
    numWritten += num;
  }

  return numWritten;
}
}
