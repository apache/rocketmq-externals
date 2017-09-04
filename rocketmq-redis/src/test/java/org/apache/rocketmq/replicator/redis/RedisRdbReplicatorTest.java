/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis;

import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueString;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisRdbReplicatorTest {
    @Test
    public void testChecksumV7() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        final AtomicLong atomicChecksum = new AtomicLong(0);
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
                atomicChecksum.compareAndSet(0, checksum);
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testChecksumV7");
                assertEquals(19, acc.get());
                assertEquals(6576517133597126869L, atomicChecksum.get());
            }
        });
        redisReplicator.open();
    }

    @Test
    public void testChecksumV6() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV6.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        final AtomicLong atomicChecksum = new AtomicLong(0);
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
                atomicChecksum.compareAndSet(0, checksum);
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testChecksumV6");
                assertEquals(132, acc.get());
                assertEquals(-3409494954737929802L, atomicChecksum.get());
            }
        });
        redisReplicator.open();

    }

    @Test
    public void testCloseListener1() throws IOException, InterruptedException {
        final AtomicInteger acc = new AtomicInteger(0);
        Replicator replicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV6.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testCloseListener1");
                acc.incrementAndGet();
                assertEquals(1, acc.get());
            }
        });
        replicator.open();
    }

    @Test
    public void testFileV7() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
                if (kv.getKey().equals("abcd")) {
                    KeyStringValueString ksvs = (KeyStringValueString) kv;
                    assertEquals("abcd", ksvs.getValue());
                }
                if (kv.getKey().equals("foo")) {
                    KeyStringValueString ksvs = (KeyStringValueString) kv;
                    assertEquals("bar", ksvs.getValue());
                }
                if (kv.getKey().equals("aaa")) {
                    KeyStringValueString ksvs = (KeyStringValueString) kv;
                    assertEquals("bbb", ksvs.getValue());
                }
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
                assertEquals(19, acc.get());
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testFileV7");
                assertEquals(19, acc.get());
            }
        });
        redisReplicator.open();

    }

    @Test
    public void testFilter() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void preFullSync(Replicator replicator) {
                super.preFullSync(replicator);
                assertEquals(0, acc.get());
            }

            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                if (kv.getValueRdbType() == 0) {
                    acc.incrementAndGet();
                }
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testFilter");
                assertEquals(13, acc.get());
            }
        });
        redisReplicator.open();
    }

    @Test
    public void testFileV6() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV6.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {

            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testFileV6");
                assertEquals(132, acc.get());
            }
        });
        redisReplicator.open();
    }

    @Test
    public void testFileV8() throws IOException, InterruptedException {
        Replicator redisReplicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV8.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        final AtomicInteger acc1 = new AtomicInteger(0);
        redisReplicator.addAuxFieldListener(new AuxFieldListener() {
            @Override
            public void handle(Replicator replicator, AuxField auxField) {
                System.out.println(auxField);
                acc1.incrementAndGet();
            }
        });
        redisReplicator.addRdbListener(new RdbListener.Adaptor() {

            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
            }

            @Override
            public void postFullSync(Replicator replicator, long checksum) {
                super.postFullSync(replicator, checksum);
            }
        });
        redisReplicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testFileV8");
                assertEquals(92499, acc.get());
                assertEquals(7, acc1.get());
            }
        });
        redisReplicator.open();
    }

}