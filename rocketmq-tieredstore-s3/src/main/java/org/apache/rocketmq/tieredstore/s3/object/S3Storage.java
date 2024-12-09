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
package org.apache.rocketmq.tieredstore.s3.object;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;

public interface S3Storage {
    long RANGE_READ_TO_END = -1L;

    /**
     * Check whether the object storage is available.
     *
     * @return available or not
     */
    boolean readinessCheck();

    void close();

    /**
     * Read object from the object storage. It will throw {@link ObjectNotFoundException} if the object not found.
     */
    default CompletableFuture<ByteBuf> read(String objectPath) {
        return rangeRead(objectPath, 0, RANGE_READ_TO_END);
    }

    /**
     * Range read object from the object storage. It will throw {@link ObjectNotFoundException} if the object not
     * found.
     */
    CompletableFuture<ByteBuf> rangeRead(String objectPath, long start, long end);

    // Low level API
    CompletableFuture<Void> write(String objectPath, ByteBuf buf);

    CompletableFuture<List<ObjectInfo>> list(String prefix);

    //read object header
    CompletableFuture<ObjectInfo> readHeader(String objectPath);

    CompletableFuture<Void> delete(List<String> objectPaths);

    CompletableFuture<String> createMultipartUpload(String path);

    CompletableFuture<ObjectStorageCompletedPart> uploadPart(String path, String uploadId,
        int partNumber,
        ByteBuf data);

    CompletableFuture<ObjectStorageCompletedPart> uploadPartCopy(String sourcePath, String path,
        long start, long end, String uploadId, int partNumber);

    CompletableFuture<Void> completeMultipartUpload(String path, String uploadId,
        List<ObjectStorageCompletedPart> parts);

    CompletableFuture<Boolean> abortMultipartUpload(String key, String uploadId);

    class ObjectInfo {
        private final String key;
        private final long timestamp;
        private final long size;

        public ObjectInfo(String key, long timestamp, long size) {
            this.key = key;
            this.timestamp = timestamp;
            this.size = size;
        }

        public long timestamp() {
            return timestamp;
        }

        public long size() {
            return size;
        }

        public String key() {
            return key;
        }
    }

    class ObjectStorageCompletedPart {
        private final int partNumber;
        private final String partId;
        private final String checkSum;

        public ObjectStorageCompletedPart(int partNumber, String partId, String checkSum) {
            this.partNumber = partNumber;
            this.partId = partId;
            this.checkSum = checkSum;
        }

        public int getPartNumber() {
            return partNumber;
        }

        public String getPartId() {
            return partId;
        }

        public String getCheckSum() {
            return checkSum;
        }

    }

    class ObjectNotFoundException extends Exception {
        public ObjectNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
