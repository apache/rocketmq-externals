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

import com.google.common.base.Stopwatch;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.utils.ThreadUtils;
import org.apache.rocketmq.tieredstore.util.MessageStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpStatusCode;
import software.amazon.awssdk.services.s3.model.S3Exception;

@SuppressWarnings("this-escape")
public abstract class AbstractS3Storage implements S3Storage {
    static final Logger log = LoggerFactory.getLogger(MessageStoreUtil.TIERED_STORE_LOGGER_NAME);
    private final Semaphore inflightReadLimiter;
    private final Semaphore inflightWriteLimiter;
    private final ExecutorService readCallbackExecutor;
    private final ExecutorService writeCallbackExecutor;
    protected final S3URI objectURI;

    protected AbstractS3Storage(
        S3URI objectURI,
        int maxObjectStorageConcurrency,
        boolean readWriteIsolate) {
        this.objectURI = objectURI;
        this.inflightWriteLimiter = new Semaphore(maxObjectStorageConcurrency);
        this.inflightReadLimiter = readWriteIsolate ? new Semaphore(maxObjectStorageConcurrency) : inflightWriteLimiter;

        readCallbackExecutor = ThreadUtils.newThreadPoolExecutor(
            1,
            1,
            TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(Integer.MAX_VALUE),
            new ThreadFactoryImpl("s3-read-cb-executor"));

        writeCallbackExecutor = ThreadUtils.newThreadPoolExecutor(
            1,
            1,
            TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(Integer.MAX_VALUE),
            new ThreadFactoryImpl("s3-write-cb-executor"));
    }

    @Override
    public CompletableFuture<ByteBuf> rangeRead(String objectPath, long start, long end) {
        CompletableFuture<ByteBuf> cf = new CompletableFuture<>();
        CompletableFuture<ByteBuf> retCf = acquireReadPermit(cf);
        if (retCf.isDone()) {
            return retCf;
        }
        if (end != RANGE_READ_TO_END && start > end) {
            IllegalArgumentException ex = new IllegalArgumentException();
            log.error("[UNEXPECTED] rangeRead [{}, {})", start, end, ex);
            cf.completeExceptionally(ex);
            return retCf;
        }
        else if (start == end) {
            cf.complete(Unpooled.EMPTY_BUFFER);
            return retCf;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        doRangeRead(objectPath, start, end).thenAccept(buf -> {
            // the end may be RANGE_READ_TO_END (-1) for read all object
            long dataSize = buf.readableBytes();
            if (log.isDebugEnabled()) {
                log.debug("getObject from path: {}, {}-{}, size: {}, cost: {} ms",
                    objectPath, start, end, dataSize, stopwatch.elapsed(TimeUnit.MILLISECONDS));
            }
            cf.complete(buf);
        })
            .exceptionally(ex -> {
                log.error("GetObject for object {} [{}, {}) fail", objectPath, start, end, ex);
                cf.completeExceptionally(ex);
                return null;
            });
        return retCf;
    }

    @Override
    public CompletableFuture<Void> write(String objectPath, ByteBuf data) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        CompletableFuture<Void> retCf = acquireWritePermit(cf).thenApply(nil -> null);
        if (retCf.isDone()) {
            data.release();
            return retCf;
        }

        doWrite(objectPath, data).thenAccept(aVoid -> {
            data.release();
            retCf.complete(null);
        })
            .exceptionally(throwable -> {
                retCf.completeExceptionally(throwable);
                return null;
            });
        return retCf;
    }

    @Override
    public CompletableFuture<String> createMultipartUpload(String path) {
        CompletableFuture<String> cf = new CompletableFuture<>();
        CompletableFuture<String> retCf = acquireWritePermit(cf);
        if (retCf.isDone()) {
            return retCf;
        }
        createMultipartUpload0(path, cf);
        return retCf;
    }

    private void createMultipartUpload0(String path, CompletableFuture<String> cf) {
        doCreateMultipartUpload(path).thenAccept(uploadId -> {
            cf.complete(uploadId);
        })
            .exceptionally(ex -> {
                log.error("CreateMultipartUpload for object {} fail", path, ex);
                cf.completeExceptionally(ex);
                return null;
            });
    }

    @Override
    public CompletableFuture<ObjectStorageCompletedPart> uploadPart(String path, String uploadId,
        int partNumber, ByteBuf data) {
        CompletableFuture<ObjectStorageCompletedPart> cf = new CompletableFuture<>();
        CompletableFuture<ObjectStorageCompletedPart> refCf = acquireWritePermit(cf);
        if (refCf.isDone()) {
            data.release();
            return refCf;
        }
        doUploadPart(path, uploadId, partNumber, data).thenAccept(part -> {
            data.release();
            cf.complete(part);
        })
            .exceptionally(ex -> {
                log.error("UploadPart for object {}-{} fail", path, partNumber, ex);
                data.release();
                cf.completeExceptionally(ex);
                return null;
            });
        return refCf;
    }

    @Override
    public CompletableFuture<ObjectStorageCompletedPart> uploadPartCopy(String sourcePath,
        String path, long start, long end, String uploadId, int partNumber) {
        CompletableFuture<ObjectStorageCompletedPart> cf = new CompletableFuture<>();
        CompletableFuture<ObjectStorageCompletedPart> retCf = acquireWritePermit(cf);
        if (retCf.isDone()) {
            return retCf;
        }
        doUploadPartCopy(sourcePath, path, start, end, uploadId, partNumber).thenAccept(part -> {
            cf.complete(part);
        })
            .exceptionally(ex -> {
                log.error("UploadPartCopy for object {}-{} [{}, {}] fail", path, partNumber, start, end, ex);
                cf.completeExceptionally(ex);
                return null;
            });
        return retCf;
    }

    @Override
    public CompletableFuture<Void> completeMultipartUpload(String path, String uploadId,
        List<ObjectStorageCompletedPart> parts) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        CompletableFuture<Void> retCf = acquireWritePermit(cf);
        if (retCf.isDone()) {
            return retCf;
        }

        doCompleteMultipartUpload(path, uploadId, parts).thenAccept(nil -> cf.complete(null))
            .exceptionally(ex -> {
                log.error("CompleteMultipartUpload for object {} fail", path, ex);
                cf.completeExceptionally(ex);
                return null;
            });
        return retCf;
    }

    @Override
    public CompletableFuture<Boolean> abortMultipartUpload(String key, String uploadId) {
        return doAbortMultipartUpload(key, uploadId);
    }

    @Override
    public CompletableFuture<Void> delete(List<String> objectPaths) {
        if (objectPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> cf = new CompletableFuture<>();
        doDeleteObjects(objectPaths).thenAccept(aVoid -> cf.complete(null)).exceptionally(throwable -> {
            log.error("delete objects failed {} fail", objectPaths, throwable);
            cf.completeExceptionally(throwable);
            return null;
        });
        return cf;
    }

    @Override
    public CompletableFuture<List<ObjectInfo>> list(String prefix) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        CompletableFuture<List<ObjectInfo>> cf = doList(prefix);
        cf.thenAccept(keyList ->
            log.info("List objects finished, count: {}, cost: {}ms", keyList.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS)))
            .exceptionally(ex -> {
                log.info("List objects failed, cost: {}, ex: {}", stopwatch.elapsed(TimeUnit.NANOSECONDS), ex.getMessage());
                return null;
            });
        return cf;
    }

    @Override
    public CompletableFuture<ObjectInfo> readHeader(String objectPath) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        CompletableFuture<ObjectInfo> cf = new CompletableFuture<>();
        doReadHeader(objectPath).thenAccept(header -> {
            cf.complete(header);
            log.info("read object header finished, count: {}, cost: {}ms", header.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        })
            .exceptionally(ex -> {
                Throwable cause = futureCause(ex);
                if (cause instanceof S3Exception && ((S3Exception) cause).statusCode() == HttpStatusCode.NOT_FOUND) {
                    // not found
                    cf.complete(null);
                }
                else {
                    cf.completeExceptionally(ex);
                    log.error("read object header, cost: {}", stopwatch.elapsed(TimeUnit.NANOSECONDS), cause);
                }
                return null;
            });
        return cf;
    }

    @Override
    public void close() {
        readCallbackExecutor.shutdown();
        writeCallbackExecutor.shutdown();
        doClose();
    }

    abstract CompletableFuture<ByteBuf> doRangeRead(String path, long start, long end);

    abstract CompletableFuture<Void> doWrite(String path, ByteBuf data);

    abstract CompletableFuture<String> doCreateMultipartUpload(String path);

    abstract CompletableFuture<ObjectStorageCompletedPart> doUploadPart(String path,
        String uploadId, int partNumber, ByteBuf part);

    abstract CompletableFuture<ObjectStorageCompletedPart> doUploadPartCopy(String sourcePath,
        String path, long start, long end, String uploadId, int partNumber);

    abstract CompletableFuture<Void> doCompleteMultipartUpload(String path, String uploadId,
        List<ObjectStorageCompletedPart> parts);

    abstract CompletableFuture<Boolean> doAbortMultipartUpload(String key, String uploadId);

    abstract CompletableFuture<Void> doDeleteObjects(List<String> objectKeys);

    abstract void doClose();

    abstract CompletableFuture<List<ObjectInfo>> doList(String prefix);

    abstract CompletableFuture<ObjectInfo> doReadHeader(String prefix);

    /**
     * Acquire read permit, permit will auto release when cf complete.
     *
     * @return retCf the retCf should be used as method return value to ensure release before following operations.
     */
    <T> CompletableFuture<T> acquireReadPermit(CompletableFuture<T> cf) {
        try {
            inflightReadLimiter.acquire();
            CompletableFuture<T> newCf = new CompletableFuture<>();
            cf.whenComplete((rst, ex) -> {
                inflightReadLimiter.release();
                readCallbackExecutor.execute(() -> {
                    if (ex != null) {
                        newCf.completeExceptionally(ex);
                    }
                    else {
                        newCf.complete(rst);
                    }
                });
            });
            return newCf;
        }
        catch (InterruptedException e) {
            cf.completeExceptionally(e);
            return cf;
        }
    }

    /**
     * Acquire write permit, permit will auto release when cf complete.
     *
     * @return retCf the retCf should be used as method return value to ensure release before following operations.
     */
    <T> CompletableFuture<T> acquireWritePermit(CompletableFuture<T> cf) {
        // this future will be return by the caller
        CompletableFuture<T> newCf = new CompletableFuture<>();

        try {

            inflightWriteLimiter.acquire();
            cf.whenComplete((rst, ex) -> {
                inflightWriteLimiter.release();
                writeCallbackExecutor.execute(() -> {
                    if (ex != null) {
                        newCf.completeExceptionally(ex);
                    }
                    else {
                        newCf.complete(rst);
                    }
                });
            });
            return newCf;
        }
        catch (InterruptedException e) {
            newCf.completeExceptionally(e);
            return newCf;
        }
    }

    public static Throwable futureCause(Throwable ex) {
        if (ex instanceof ExecutionException) {
            if (ex.getCause() != null) {
                return futureCause(ex.getCause());
            }
            else {
                return ex;
            }
        }
        else if (ex instanceof CompletionException) {
            if (ex.getCause() != null) {
                return futureCause(ex.getCause());
            }
            else {
                return ex;
            }
        }
        return ex;
    }

}
