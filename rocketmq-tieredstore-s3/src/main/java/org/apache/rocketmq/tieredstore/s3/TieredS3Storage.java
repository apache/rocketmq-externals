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

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.rocketmq.logging.org.slf4j.Logger;
import org.apache.rocketmq.logging.org.slf4j.LoggerFactory;
import org.apache.rocketmq.tieredstore.MessageStoreConfig;
import org.apache.rocketmq.tieredstore.s3.metadata.S3ChunkMetadata;
import org.apache.rocketmq.tieredstore.s3.object.AwsS3Storage;
import org.apache.rocketmq.tieredstore.s3.object.S3Storage;
import org.apache.rocketmq.tieredstore.s3.object.S3URI;
import org.apache.rocketmq.tieredstore.util.MessageStoreUtil;

public class TieredS3Storage {

    private static final Logger log = LoggerFactory.getLogger(MessageStoreUtil.TIERED_STORE_LOGGER_NAME);

    private static final int MAX_OBJECT_STORAGE_CONCURRENCY = 1000;

    private volatile static TieredS3Storage instance;

    private final MessageStoreConfig tieredMessageStoreConfig;

    private final S3Storage objectStorage;

    private final S3FileLock fileLock;

    public static TieredS3Storage getInstance(MessageStoreConfig config) {
        if (instance == null) {
            synchronized (MessageStoreConfig.class) {
                if (instance == null) {
                    instance = new TieredS3Storage(config);
                }
            }
        }
        return instance;
    }

    private TieredS3Storage(MessageStoreConfig config) {
        this.tieredMessageStoreConfig = config;
        S3URI objectURI = S3URI.builder()
            .bucket(tieredMessageStoreConfig.getObjectStoreBucket())
            .endpoint(tieredMessageStoreConfig.getObjectStoreEndpoint())
            .extension(Maps.newHashMap())
            .build();
        objectURI.addExtension(S3URI.ACCESS_KEY_KEY, tieredMessageStoreConfig.getObjectStoreAccessKey());
        objectURI.addExtension(S3URI.SECRET_KEY_KEY, tieredMessageStoreConfig.getObjectStoreSecretKey());
        objectStorage = AwsS3Storage.builder()
            .bucket(objectURI)
            .maxObjectStorageConcurrency(MAX_OBJECT_STORAGE_CONCURRENCY)
            .readWriteIsolate(true)
            .build();
        fileLock = new S3FileLock(this);
    }

    public boolean readinessCheck() {
        return objectStorage.readinessCheck();
    }

    public void start() {

    }

    public void lock() {
        fileLock.lock();
    }

    public void releaseLock() {
        fileLock.release();
    }

    public S3Storage getObjectStorage() {
        return objectStorage;
    }

    public MessageStoreConfig getTieredMessageStoreConfig() {
        return tieredMessageStoreConfig;
    }

    public CompletableFuture<Void> deleteObjects(String prefix) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        listObjects(prefix).thenCompose(objects -> {
            List<String> objectPaths = new ArrayList<>();
            objects.forEach(v -> objectPaths.add(v.key()));
            return objectStorage.delete(objectPaths);
        })
            .thenAccept(v -> future.complete(null))
            .exceptionally(throwable -> {
                log.error("deleteObjects prefix {} exception", prefix);
                future.completeExceptionally(throwable);
                return null;
            });
        return future;
    }

    public CompletableFuture<Void> deleteObject(String path) {
        return objectStorage.delete(Arrays.asList(path));
    }

    CompletableFuture<List<S3Storage.ObjectInfo>> listObjects(String prefix) {
        return objectStorage.list(prefix);
    }

    CompletableFuture<List<S3ChunkMetadata>> listChunks(String prefix) {
        CompletableFuture<List<S3ChunkMetadata>> future = new CompletableFuture<>();
        objectStorage.list(prefix)
            .thenAccept(objectInfos -> {
                future.complete(objectInfos.stream()
                    .map(objectInfo -> {
                        S3ChunkMetadata chunkMetadata = new S3ChunkMetadata();
                        String key = objectInfo.key();
                        chunkMetadata.setChunkSize((int) objectInfo.size());
                        String[] paths = key.split("/");
                        String chunkSubName = paths[paths.length - 1];
                        Integer startPosition = Integer.valueOf(chunkSubName);
                        chunkMetadata.setStartPosition(startPosition);
                        return chunkMetadata;
                    }).sorted((o1, o2) -> (int) (o1.getStartPosition() - o2.getStartPosition())).collect(Collectors.toList()));
            })
            .exceptionally(e -> {
                future.completeExceptionally(e);
                return null;
            });
        return future;
    }

    public CompletableFuture<ByteBuf> rangeRead(String objectPath, long start, long end) {
        return objectStorage.rangeRead(objectPath, start, end);
    }

    public CompletableFuture<S3Storage.ObjectInfo> readHeader(String objectPath) {
        return objectStorage.readHeader(objectPath);
    }

    public boolean isObjectExist(String objectPath) {
        return objectStorage.readHeader(objectPath).join() != null ? true : false;
    }

    public CompletableFuture<Void> writeObject(String objectPath, ByteBuf buf) {
        return objectStorage.write(objectPath, buf);
    }

    public void close() {
        objectStorage.close();
    }

}