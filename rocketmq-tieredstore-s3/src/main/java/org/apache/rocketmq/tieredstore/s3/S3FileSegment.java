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

package org.apache.rocketmq.tieredstore.s3;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import io.opentelemetry.api.common.Attributes;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.rocketmq.tieredstore.MessageStoreConfig;
import org.apache.rocketmq.tieredstore.common.FileSegmentType;
import org.apache.rocketmq.tieredstore.exception.TieredStoreErrorCode;
import org.apache.rocketmq.tieredstore.exception.TieredStoreException;
import org.apache.rocketmq.tieredstore.metrics.TieredStoreMetricsManager;
import org.apache.rocketmq.tieredstore.provider.FileSegment;
import org.apache.rocketmq.tieredstore.s3.metadata.S3ChunkMetadata;
import org.apache.rocketmq.tieredstore.s3.metadata.S3FileSegmentMetadata;
import org.apache.rocketmq.tieredstore.s3.object.AbstractS3Storage;
import org.apache.rocketmq.tieredstore.s3.object.S3Storage;
import org.apache.rocketmq.tieredstore.s3.object.bytebuf.ByteBufAlloc;
import org.apache.rocketmq.tieredstore.s3.util.S3PathUtils;
import org.apache.rocketmq.tieredstore.stream.FileSegmentInputStream;
import org.apache.rocketmq.tieredstore.util.MessageStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.tieredstore.metrics.TieredStoreMetricsConstant.LABEL_FILE_TYPE;
import static org.apache.rocketmq.tieredstore.metrics.TieredStoreMetricsConstant.LABEL_PATH;
import static org.apache.rocketmq.tieredstore.s3.constants.S3Constants.MIN_PART_SIZE;
import static org.apache.rocketmq.tieredstore.util.MessageFormatUtil.CONSUME_QUEUE_UNIT_SIZE;

public class S3FileSegment extends FileSegment {

    private static final Logger log = LoggerFactory.getLogger(MessageStoreUtil.TIERED_STORE_LOGGER_NAME);

    private final String basePath;

    /**
     * The path of the chunk file in S3. Format:
     * <pre>
     *     {@link #filePath}/chunk
     * </pre>
     */
    private final String chunkPath;

    /**
     * The path of the segment file in S3. Format:
     * <pre>
     *     {@link #filePath}/segment
     * </pre>
     */
    private final String segmentPath;

    private final TieredS3Storage s3Storage;

    private final S3FileSegmentMetadata metadata;

    private final boolean isObjectMergeEnable = false;

    private final AtomicBoolean compactStatus = new AtomicBoolean(false);

    private final Attributes attributes = TieredStoreMetricsManager.newAttributesBuilder().put(LABEL_PATH, filePath)
        .put(LABEL_FILE_TYPE, this.fileType.name().toLowerCase()).build();

    public S3FileSegment(MessageStoreConfig storeConfig,
        FileSegmentType fileType, String filePath, long baseOffset) {
        super(storeConfig, fileType, filePath, baseOffset);

        // fullPath: clusterBasePath/broker/topic/queueId/fileType/seg-${baseOffset}
        String clusterName = storeConfig.getBrokerClusterName();
        String clusterBasePath = String.format("%s_%s", MessageStoreUtil.getHash(clusterName), clusterName);
        this.basePath = clusterBasePath + S3PathUtils.FILE_SEPARATOR + filePath + S3PathUtils.FILE_SEPARATOR +
            fileType.toString() + S3PathUtils.FILE_SEPARATOR + "seg-" + baseOffset;
        this.chunkPath = S3PathUtils.getBaseChunkPath(basePath);
        this.segmentPath = S3PathUtils.getBaseSegmentPath(basePath);
        this.s3Storage = TieredS3Storage.getInstance(storeConfig);
        this.metadata = new S3FileSegmentMetadata();
        this.initialize();
    }

    private void initialize() {

        S3Storage.ObjectInfo segmentObjectHeader = s3Storage.readHeader(S3PathUtils.getSegmentPath(segmentPath)).join();
        if (segmentObjectHeader != null) {
            this.metadata.setSegment(
                new S3ChunkMetadata(0, (int) segmentObjectHeader.size(), true));
            return;
        }

        S3Storage.ObjectInfo chunkZeroObjectHeader = s3Storage.readHeader(S3PathUtils.getChunkPathByPosition(chunkPath, 0)).join();
        // if chunk start position equal 0, means new file
        if (chunkZeroObjectHeader == null) {
            log.info("create new S3FileSegment {}", basePath);
            return;
        }

        CompletableFuture<List<S3ChunkMetadata>> listChunks = this.s3Storage.listChunks(this.chunkPath);
        List<S3ChunkMetadata> chunks = listChunks.join();
        // add all chunks into metadata
        checkAndLoadChunks(chunks);
        if (log.isDebugEnabled()) {
            log.debug("init file segment metadata successfully. path: {} meta: {}", basePath, JSON.toJSONString(this.metadata));
        }

        log.info("init file segment metadata successfully. path: {}", basePath);

    }

    private void checkAndLoadChunks(List<S3ChunkMetadata> chunks) {
        if (chunks.size() == 0) {
            return;
        }
        for (S3ChunkMetadata chunk : chunks) {
            S3ChunkMetadata newChunk =
                new S3ChunkMetadata(chunk.getStartPosition(),
                    chunk.getChunkSize(), false);
            if (!this.metadata.addChunk(newChunk)) {
                // the chunk is not valid
                log.error("Check and load chunks failed, the path {} the chunk: {} is not valid, now chunks last end position: {}, please check it.",
                    basePath, newChunk,
                    this.metadata.getEndPosition());
                throw new RuntimeException(
                    "The chunk: " + chunk + " is not valid, now chunks last end position: " + this.metadata.getEndPosition() + ", please check it.");
            }
        }
    }

    @Override
    public String getPath() {
        return this.filePath;
    }

    @Override
    public long getSize() {
        return this.metadata.getSize();
    }

    @Override
    public boolean exists() {
        return this.metadata.getSize() > 0;
    }

    @Override
    public void createFile() {

    }

    @Override
    public void destroyFile() {
        this.s3Storage.deleteObjects(basePath).join();
        this.metadata.clear();
    }

    @Override
    public CompletableFuture<ByteBuffer> read0(long position, int length) {
        CompletableFuture<ByteBuffer> completableFuture = new CompletableFuture<>();
        List<S3ChunkMetadata> chunks;
        try {
            chunks = this.metadata.seek(position, length);
        }
        catch (IndexOutOfBoundsException e) {
            log.error("Read position {} and length {} out of range, the file segment size is {}.", position, length, this.metadata.getSize());
            completableFuture.completeExceptionally(new TieredStoreException(TieredStoreErrorCode.DOWNLOAD_LENGTH_NOT_CORRECT,
                "read data from segment error because of position or length not correct"));
            return completableFuture;
        }
        long endPosition = position + length - 1;
        List<CompletableFuture<Void>> subFutures = new ArrayList<>(chunks.size());
        byte[] bytes = new byte[length];
        for (S3ChunkMetadata chunk : chunks) {
            long startPositionInChunk = position >= chunk.getStartPosition() ? position - chunk.getStartPosition() : 0;
            long endPositionInChunk = endPosition <= chunk.getEndPosition() ? endPosition - chunk.getStartPosition() : chunk.getChunkSize() - 1;

            String objectPath = chunk.isSegment() ? S3PathUtils.getSegmentPath(segmentPath) : S3PathUtils.getChunkPathByPosition(chunkPath, chunk.getStartPosition());
            subFutures.add(this.s3Storage.rangeRead(objectPath, startPositionInChunk,
                endPositionInChunk)
                .thenAccept(buf -> {
                    buf.readBytes(bytes, (int) (chunk.getStartPosition() + startPositionInChunk - position), buf.readableBytes());
                    buf.release();
                }).exceptionally(throwable -> {
                    log.error("Failed to read data from s3, chunk: {}, start position: {}, end position: {}", chunk, startPositionInChunk,
                        endPositionInChunk, throwable);
                    return null;
                }));
        }
        CompletableFuture.allOf(subFutures.toArray(new CompletableFuture[0])).thenAccept(v -> {
            completableFuture.complete(ByteBuffer.wrap(bytes));
            TieredStoreMetricsManager.downloadBytes.record(length, attributes);
        })
            .exceptionally(throwable -> {
                log.error("Failed to read data from s3, position: {}, length: {}", position, length, throwable);
                completableFuture.completeExceptionally(
                    new TieredStoreException(TieredStoreErrorCode.IO_ERROR, "wait all sub download tasks complete error"));
                return null;
            });
        return completableFuture;
    }

    @Override
    public CompletableFuture<Boolean> commit0(FileSegmentInputStream inputStream, long position, int length,
        boolean append) {
        // TODO: Deal with the case that the param: append is false
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        // check if now the segment is sealed
        if (this.metadata.isSealed()) {
            log.error("The segment is sealed, the position: {}, the length: {}.", position, length);
            TieredStoreException exception = new TieredStoreException(TieredStoreErrorCode.SEGMENT_SEALED, "the segment is sealed");
            exception.setPosition(this.metadata.getEndPosition() + 1);
            completableFuture.completeExceptionally(exception);
            return completableFuture;
        }
        // check if the position is valid
        if (length < 0 || position != this.metadata.getEndPosition() + 1) {
            log.error("The position is invalid, the position: {}, the length: {}, now segment end position: {}.", position, length,
                this.metadata.getEndPosition());
            TieredStoreException exception = new TieredStoreException(TieredStoreErrorCode.ILLEGAL_OFFSET, "the position is invalid");
            exception.setPosition(this.metadata.getEndPosition() + 1);
            completableFuture.completeExceptionally(exception);
            return completableFuture;
        }
        // upload chunk
        String chunkPath = S3PathUtils.getChunkPathByPosition(this.chunkPath, position);
        inputStream.rewind();
        CompositeByteBuf compositeByteBuf = ByteBufAlloc.compositeByteBuffer();
        for (ByteBuffer byteBuffer : inputStream.getBufferList()) {
            compositeByteBuf.addComponent(true, Unpooled.wrappedBuffer(byteBuffer));
        }

        if (compositeByteBuf.readableBytes() != length) {
            log.error("byteBuffer length not equal compositeByteBuf.readableBytes() length {} readableBytes{} ", length,
                compositeByteBuf.readableBytes());
            TieredStoreException exception = new TieredStoreException(TieredStoreErrorCode.ILLEGAL_OFFSET, "the position is invalid");
            completableFuture.completeExceptionally(exception);
            return completableFuture;
        }
        compositeByteBuf.retain();
        this.s3Storage.writeObject(chunkPath, compositeByteBuf).thenAccept(result -> {
            try {
                S3ChunkMetadata chunk = new S3ChunkMetadata(position, length, false);
                if (!this.metadata.addChunk(chunk)) {
                    // the chunk is not valid
                    log.error(
                        "Add chunk after uploading chunk to S3 failed, the chunk: {} is not valid, now chunks last end position: {}, please check it.",
                        chunk, this.metadata.getEndPosition());
                    throw new RuntimeException(
                        "The chunk: " + chunk + " is not valid, now chunks last end position: " + this.metadata.getEndPosition() +
                            ", please check it.");
                }

                completableFuture.complete(true);

            }
            finally {
                ReferenceCountUtil.safeRelease(compositeByteBuf);
            }
        })
            .exceptionally(throwable -> {
                ReferenceCountUtil.safeRelease(compositeByteBuf);
                log.error("Failed to write data to s3, position: {}, length: {}", position, length, throwable);
                TieredStoreException exception = new TieredStoreException(TieredStoreErrorCode.IO_ERROR, "write data to s3 error");
                exception.setPosition(position);
                completableFuture.completeExceptionally(exception);
                return null;
            });
        return completableFuture;
    }

    public void compactFile() {
        // check if the segment file exists
        if (this.metadata.isSealed() && this.metadata.getChunkCount() == 0) {
            return;
        }

        if (isObjectMergeEnable) {
            return;
        }
        boolean ret = compactStatus.compareAndSet(false, true);
        if (!ret) {
            log.error("compact operator is running,do nothing filePath {}", basePath);
        }

        // merge all chunks into a segment file and delete all chunks
        try {
            compactFile0();
        }
        finally {
            boolean rt = compactStatus.compareAndSet(true, false);
            if (!rt) {
                log.error("compactFile compactStatus update error  {}", basePath);
            }
        }

    }

    private void compactFile0() {
        // merge all chunks
        log.info("compact file segment filePath {} fileType {} baseOffset {}", basePath, fileType,
            baseOffset);
        String segmentName = S3PathUtils.getSegmentPath(segmentPath);

        boolean result = mergeAllChunksIntoSegment(this.metadata.getChunks(), segmentName, fileType);
        if (!result) {
            log.error("Merge chunks into segment failed, chunk path is {}, segment path is {}.", this.chunkPath, this.segmentPath);
            throw new RuntimeException("merge chunks into segment failed");
        }

        List<S3Storage.ObjectInfo> segmentObjects = this.s3Storage.listObjects(segmentPath).join();
        boolean isCorrectCount = segmentObjects.size() == 1;
        boolean isCorrectSize = false;
        if (isCorrectCount) {
            isCorrectSize = segmentObjects.get(0).size() == metadata.getSize();
        }
        if (isCorrectCount && isCorrectSize) {
            this.metadata.setSegment(new S3ChunkMetadata(0, (int) this.metadata.getSize(), true));
            this.metadata.removeAllChunks();
            log.info("merge chunks into segment success, chunk path is {}, segment path is {}", this.chunkPath, this.segmentPath);
            try {
                this.s3Storage.deleteObjects(chunkPath).join();
                log.info("compactFile0 after merge success , delete old objects success path{}", chunkPath);
            }
            catch (Throwable e) {
                log.error("compactFile0 after merge success , delete old objects failed path{}", chunkPath, e);
            }
        }
        else {
            if (!isCorrectCount) {
                log.error("Merge chunks into segment failed, segment count {} wrong  path is {}.", segmentObjects.size(), this.segmentPath);
            }
            if (!isCorrectSize) {
                log.error("Merge chunks into segment failed, segment size {} wrong  path is {}.", segmentObjects.get(0).size(), this.segmentPath);
            }
        }
    }

    private boolean mergeAllChunksIntoSegment(List<S3ChunkMetadata> chunks, String segmentName,
        FileSegmentType fileSegmentType) {
        if (FileSegmentType.COMMIT_LOG.equals(fileSegmentType)) {
            boolean checkObjectSize = checkChunkSizeForMultiUploadCopy();
            if (!checkObjectSize) {
                // The commit log has a minimum limit of Object size less than merge, so it cannot be merged.
                log.warn("commit log file segment contain chunk size too small,can not merge {}", filePath);
                return false;
            }
            return new AsyncS3ChunksMergeCopy(segmentName, chunks).run();
        }
        else if (FileSegmentType.CONSUME_QUEUE.equals(fileSegmentType)) {
            return new AsyncS3ChunksMerge(segmentName, chunks).run();
        }
        else {
            throw new RuntimeException("mergeAllChunksIntoSegment invalid fileSegmentType");
        }
    }

    private boolean checkChunkSizeForMultiUploadCopy() {
        for (int i = 0; i < metadata.getChunks().size(); i++) {
            S3ChunkMetadata chunk = metadata.getChunks().get(i);
            if (i != metadata.getChunks().size() - 1 && chunk.getChunkSize() < MIN_PART_SIZE) {
                return false;
            }
        }
        return true;
    }

    abstract class AbstractChunkMergeTask {

        protected final String segmentKey;
        protected String uploadId;
        protected final List<AbstractS3Storage.ObjectStorageCompletedPart> completedParts;

        protected final List<S3ChunkMetadata> chunks;

        AbstractChunkMergeTask(String segmentKey, List<S3ChunkMetadata> chunks) {
            this.segmentKey = segmentKey;
            this.uploadId = null;
            this.completedParts = new ArrayList<>();
            this.chunks = chunks;
        }

        abstract boolean run();

        protected CompletableFuture<String> initiateUpload() {
            return s3Storage.getObjectStorage().createMultipartUpload(segmentKey)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error initiating multi part {} upload: ", segmentKey, error);
                    }
                    else {
                        uploadId = result;
                    }
                });
        }

        protected CompletableFuture<AbstractS3Storage.ObjectStorageCompletedPart> uploadPartCopy(int partNumber,
            String chunkKey, long chunkSize) {
            return s3Storage.getObjectStorage()
                .uploadPartCopy(chunkKey, segmentKey, 0, chunkSize, uploadId, partNumber)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error uploading part copy, chunkKey: {}, partNumber: {}, uploadId: {}, error:", chunkKey, partNumber, uploadId,
                            error);
                    }
                    else {
                        completedParts.add(result);
                    }
                });
        }

        protected CompletableFuture<AbstractS3Storage.ObjectStorageCompletedPart> uploadPart(int partNumber,
            ByteBuf data) {
            return s3Storage.getObjectStorage()
                .uploadPart(segmentKey, uploadId, partNumber, data)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error uploading part, segmentKey: {}, partNumber: {}, uploadId: {}, error:", segmentKey, partNumber, uploadId,
                            error);
                    }
                    else {
                        completedParts.add(result);
                    }
                });
        }

        protected CompletableFuture<Boolean> completeUpload() {
            Collections.sort(completedParts, Comparator.comparingInt(AbstractS3Storage.ObjectStorageCompletedPart::getPartNumber));
            return s3Storage.getObjectStorage().completeMultipartUpload(segmentKey, uploadId, completedParts)
                .thenApply(resp -> true)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error completing multi part upload, uploadId: {}, ", uploadId, error);
                    }
                });
        }

        protected CompletableFuture<Boolean> abortUpload() {
            return s3Storage.getObjectStorage().abortMultipartUpload(segmentKey, uploadId)
                .thenApply(resp -> true)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error abort multi part upload, uploadId: {}, ", uploadId, error);
                    }
                });
        }
    }

    class AsyncS3ChunksMergeCopy extends AbstractChunkMergeTask {

        public AsyncS3ChunksMergeCopy(String segmentKey,
            List<S3ChunkMetadata> chunks) {
            super(segmentKey, chunks);
        }

        public boolean run() {
            try {
                initiateUpload().join();
                for (int i = 0; i < chunks.size(); i++) {
                    String chunkKey = S3PathUtils.getChunkPathByPosition(chunkPath, chunks.get(i).getStartPosition());
                    int partNumber = i + 1;
                    uploadPartCopy(partNumber, chunkKey, chunks.get(i).getChunkSize()).join();
                }
                completeUpload().join();
                return true;
            }
            catch (Throwable e) {
                log.error("Merge all chunks into segment by copy failed, chunks: {}, segmentName: {}, region: {}, bucket: {}", chunks, segmentKey,
                    storeConfig.getObjectStoreEndpoint(), storeConfig.getObjectStoreBucket(), e);
                abortUpload().join();
                return false;
            }
        }
    }

    class AsyncS3ChunksMerge extends AbstractChunkMergeTask {

        public AsyncS3ChunksMerge(String segmentKey,
            List<S3ChunkMetadata> chunks) {
            super(segmentKey, chunks);
        }

        @Override
        boolean run() {
            try {
                initiateUpload().join();
                CompositeByteBuf compositeByteBuf = null;
                try {
                    int partNumber = 0;
                    compositeByteBuf = ByteBufAlloc.compositeByteBuffer();
                    S3ChunkMetadata pre = null;
                    for (S3ChunkMetadata chunk : chunks) {

                        if (pre != null && pre.getEndPosition() + 1 != chunk.getStartPosition()) {
                            log.error("AsyncS3ChunksMerge chunk position check failed {}", segmentKey);
                            throw new RuntimeException("AsyncS3ChunksMerge chunk position check failed");
                        }

                        ByteBuf byteBuf = readByChunk(chunk).join();
                        if (byteBuf.readableBytes() != chunk.getChunkSize() || byteBuf.readableBytes() % CONSUME_QUEUE_UNIT_SIZE != 0) {
                            log.error("AsyncS3ChunksMerge read chunk data failed chunk size invalid segmentKey {} chunkStartPosition {}", segmentKey, chunk.getStartPosition());
                            throw new RuntimeException("AsyncS3ChunksMerge read chunk data failed chunk size invalid");
                        }
                        compositeByteBuf.addComponents(true, byteBuf);

                        if (compositeByteBuf.readableBytes() >= MIN_PART_SIZE) {
                            partNumber++;
                            try {
                                compositeByteBuf.retain();
                                uploadPart(partNumber, compositeByteBuf).join();
                            }
                            finally {
                                ReferenceCountUtil.safeRelease(compositeByteBuf);
                            }
                            compositeByteBuf = ByteBufAlloc.compositeByteBuffer();
                        }

                        pre = chunk;
                    }

                    if (compositeByteBuf.readableBytes() > 0) {
                        partNumber++;
                        compositeByteBuf.retain();
                        uploadPart(partNumber, compositeByteBuf).join();
                    }
                }
                finally {
                    if (compositeByteBuf != null) {
                        ReferenceCountUtil.safeRelease(compositeByteBuf);
                    }
                }

                completeUpload().join();
                return true;
            }
            catch (Throwable e) {
                log.error("Merge all chunks into segment failed, chunks: {}, segmentName: {}, region: {}, bucket: {}", chunks, segmentKey,
                    storeConfig.getObjectStoreEndpoint(), storeConfig.getObjectStoreBucket(), e);
                abortUpload().join();
                return false;
            }
        }

        private CompletableFuture<ByteBuf> readByChunk(S3ChunkMetadata chunk) {
            String objectPath = S3PathUtils.getChunkPathByPosition(chunkPath, chunk.getStartPosition());
            return s3Storage.rangeRead(objectPath, 0, chunk.getChunkSize());
        }
    }
}
