// Copyright (c) 2011-present, Facebook, Inc.  All rights reserved.
//  This source code is licensed under both the GPLv2 (found in the
//  COPYING file in the root directory) and Apache 2.0 License
//  (found in the LICENSE.Apache file in the root directory).


#include "utilities/compaction_filters/remove_consumequeue_compactionfilter.h"

#include <string>

#include "rocksdb/slice.h"

namespace ROCKSDB_NAMESPACE {

RemoveConsumeQueueCompactionFilter::RemoveConsumeQueueCompactionFilter(long long minPhyOffset) {
  this->minPhyOffset_ = minPhyOffset;
}

bool RemoveConsumeQueueCompactionFilter::Filter(int /*level*/,
                                                const Slice& key,
                                                const Slice& existing_value,
                                                std::string* /*new_value*/,
                                                bool* /*value_changed*/) const {
  if (key.empty() || existing_value.empty()) {
    return false;
  }
  if (existing_value.size() < CQ_MIN_SIZE) {
    return false;
  }

  long long phyOffset = getLong(existing_value, PHY_OFFSET_OFFSET);
  return phyOffset < this->minPhyOffset_;
}

long long RemoveConsumeQueueCompactionFilter::getLong(const Slice& value, int offset) {
  const char* valueCharData = value.data();
  return ((((long long)valueCharData[offset]          ) << 56) |
          (((long long)valueCharData[offset+1]  & 0xff) << 48) |
          (((long long)valueCharData[offset+2]  & 0xff) << 40) |
          (((long long)valueCharData[offset+3]  & 0xff) << 32) |
          (((long long)valueCharData[offset+4]  & 0xff) << 24) |
          (((long long)valueCharData[offset+5]  & 0xff) << 16) |
          (((long long)valueCharData[offset+6]  & 0xff) <<  8) |
          (((long long)valueCharData[offset+7]  & 0xff)      ));
}

}  // namespace ROCKSDB_NAMESPACE
