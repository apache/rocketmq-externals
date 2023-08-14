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
