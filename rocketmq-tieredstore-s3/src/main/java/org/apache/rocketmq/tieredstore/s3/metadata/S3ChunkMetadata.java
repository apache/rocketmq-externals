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

package org.apache.rocketmq.tieredstore.s3.metadata;

import org.apache.rocketmq.tieredstore.MessageStoreConfig;

import java.util.Objects;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.rocketmq.tieredstore.s3.S3FileSegment;

/**
 * Metadata of a chunk in S3.
 *
 * <p>
 * There are two types of chunks in S3:
 * <ul>
 * <li>Normal chunk, represents a normal chunk in S3, which size is usually less than {@link
 * MessageStoreConfig#getTieredStoreGroupCommitSize()} ()}
 * <li>Segment chunk, means that this all normal chunks in one logic segment have been merged into a single chunk,
 * which is named as segment chunk, which size is usually equals to {@link MessageStoreConfig#getTieredStoreCommitLogMaxSize()}
 * or {@link MessageStoreConfig#getTieredStoreConsumeQueueMaxSize()}
 * </ul>
 * Once a segment chunk is created, it will never be changed, and we should delete all normal chunks in this segment.
 */
public class S3ChunkMetadata implements Comparable<S3ChunkMetadata> {

    /**
     * Name of the chunk in S3. Format:
     * <p>
     * Chunk:
     * <pre>
     *     {@link S3FileSegment#getPath()} ()}/chunk/${startPosition}
     * </pre>
     * <p>
     * Segment:
     * <pre>
     *     {@link S3FileSegment#getPath()}/segment/${startPosition}
     * </pre>
     */

    @JSONField(ordinal = 1)
    private long startPosition;

    @JSONField(ordinal = 2)
    private int chunkSize;

    private boolean isSegment;

    public S3ChunkMetadata() {

    }

    public S3ChunkMetadata(long startPosition, int chunkSize, boolean isSegment) {
        this.startPosition = startPosition;
        this.chunkSize = chunkSize;
        this.isSegment = isSegment;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(long startPosition) {
        this.startPosition = startPosition;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public long getEndPosition() {
        return startPosition + chunkSize - 1;
    }

    public boolean isSegment() {
        return isSegment;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3ChunkMetadata that = (S3ChunkMetadata) o;
        return startPosition == that.startPosition && chunkSize == that.chunkSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, chunkSize);
    }

    @Override
    public String toString() {
        return "S3ChunkMetadata{" +
            "startPosition=" + startPosition +
            ", chunkSize=" + chunkSize +
            '}';
    }

    @Override
    public int compareTo(S3ChunkMetadata o) {
        return this.startPosition > o.getStartPosition() ? 1 : -1;
    }
}
