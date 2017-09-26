#include "OutputStream.h"
#include <limits>
#include "big_endian.h"

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

bool OutputStream::writeShortBigEndian(short value) {
  unsigned short v;
  char pShort[sizeof(v)];
  WriteBigEndian(pShort, (unsigned short)value);
  return write(pShort, 2);
}

bool OutputStream::writeIntBigEndian(int value) {
  unsigned int v;
  char pInt[sizeof(v)];
  WriteBigEndian(pInt, (unsigned int)value);
  return write(pInt, 4);
}

bool OutputStream::writeInt64BigEndian(int64 value) {
  uint64 v;
  char pUint64[sizeof(v)];
  WriteBigEndian(pUint64, (uint64)value);
  return write(pUint64, 8);
}

bool OutputStream::writeFloatBigEndian(float value) {
  union {
    int asInt;
    float asFloat;
  } n;
  n.asFloat = value;
  return writeIntBigEndian(n.asInt);
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
