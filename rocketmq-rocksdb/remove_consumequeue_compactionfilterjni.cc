// Copyright (c) 2011-present, Facebook, Inc.  All rights reserved.
//  This source code is licensed under both the GPLv2 (found in the
//  COPYING file in the root directory) and Apache 2.0 License
//  (found in the LICENSE.Apache file in the root directory).

#include <jni.h>

#include "include/org_rocksdb_RemoveConsumeQueueCompactionFilter.h"
#include "utilities/compaction_filters/remove_consumequeue_compactionfilter.h"

/*
 * Class:     org_rocksdb_RemoveConsumeQueueCompactionFilter
 * Method:    createNewRemoveConsumeQueueCompactionFilter0
 * Signature: (J)J
 */
jlong Java_org_rocksdb_RemoveConsumeQueueCompactionFilter_createNewRemoveConsumeQueueCompactionFilter0(
    JNIEnv* /*env*/, jclass /*jcls*/, jlong minPhyOffset) {
  auto* compaction_filter =
          new ROCKSDB_NAMESPACE::RemoveConsumeQueueCompactionFilter(minPhyOffset);

  // set the native handle to our native compaction filter
  return reinterpret_cast<jlong>(compaction_filter);
}
