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

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import java.nio.charset.Charset;
import java.util.UUID;
import org.apache.rocketmq.tieredstore.s3.object.bytebuf.ByteBufAlloc;
import org.apache.rocketmq.tieredstore.util.MessageStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3FileLock {

    private static final Logger log = LoggerFactory.getLogger(S3FileLock.class);
    private final TieredS3Storage storage;

    private final String lockObjectPath;

    private final String lockKey;

    public S3FileLock(TieredS3Storage storage) {
        this.storage = storage;
        String clusterName = storage.getTieredMessageStoreConfig().getBrokerClusterName();
        String clusterBasePath = String.format("%s_%s", MessageStoreUtil.getHash(clusterName), clusterName);
        this.lockObjectPath = clusterBasePath + "/" + storage.getTieredMessageStoreConfig().getBrokerName() + "/" + "lock";
        this.lockKey = UUID.randomUUID().toString();
    }

    public void lock() {
        boolean exit = storage.isObjectExist(lockObjectPath);
        if (exit) {
            log.error("s3 storage is locked by other processes,please wait for it to release");
            throw new RuntimeException("s3 storage is locked by other processes,please wait for it to release");
        }
        ByteBuf byteBuf = null;
        try {
            byte[] contentBytes = lockKey.getBytes(Charset.defaultCharset());
            byteBuf = ByteBufAlloc.byteBuffer(contentBytes.length);
            byteBuf.writeBytes(contentBytes);
            byteBuf.retain();
            storage.writeObject(lockObjectPath, byteBuf);
            // double check
            ByteBuf checkContent = storage.rangeRead(lockObjectPath, 0, lockKey.length()).join();
            String checkContentStr = new String(checkContent.array(), Charset.defaultCharset());
            if (!lockKey.equals(checkContentStr)) {
                throw new RuntimeException("lock double check failed");
            }
        }
        catch (Throwable e) {
            log.error("try lock s3 storages path {} failed", lockObjectPath, e);
            throw new RuntimeException(e);
        }
        finally {
            if (byteBuf != null) {
                ReferenceCountUtil.safeRelease(byteBuf);
            }
        }
    }

    public void release() {
        try {
            storage.deleteObject(lockObjectPath).join();
            log.info("release s3 file lock success {}", lockObjectPath);
        }
        catch (Throwable e) {
            log.error("release lock s3 storages path {} failed", lockObjectPath, e);
        }
    }
}
