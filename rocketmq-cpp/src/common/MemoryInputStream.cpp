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

  memcpy(buffer, data + position, (size_t)num);
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
