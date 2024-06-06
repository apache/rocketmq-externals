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

#pragma once

#include <string>

#include "rocksdb/compaction_filter.h"
#include "rocksdb/slice.h"

namespace ROCKSDB_NAMESPACE {

class RemoveConsumeQueueCompactionFilter : public CompactionFilter {
  static const int CQ_MIN_SIZE = 28;
    static const int PHY_OFFSET_OFFSET = 0;

private:
    long long minPhyOffset_;

 public:
  RemoveConsumeQueueCompactionFilter(long long minPhyOffset);

  static const char* kClassName() { return "RemoveConsumeQueueCompactionFilter"; }

  const char* Name() const override { return kClassName(); }

  bool Filter(int level, const Slice& key, const Slice& existing_value,
              std::string* new_value, bool* value_changed) const override;

  static long long getLong(const Slice& value, int offset);
};

}  // namespace ROCKSDB_NAMESPACE
