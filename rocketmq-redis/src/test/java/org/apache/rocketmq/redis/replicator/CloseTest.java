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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.redis.replicator;

import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandListener;
import org.apache.rocketmq.redis.replicator.rdb.RdbListener;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class CloseTest {
    @Test
    public void testRdbClose() throws IOException, InterruptedException {
        Replicator r = new RedisReplicator(
                CloseTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        r.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
                if (acc.get() == 10) {
                    try {
                        replicator.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        r.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testRdbClose");
            }
        });
        r.open();
        assertEquals(10, acc.get());
    }

    @Test
    public void testAofClose() throws IOException {
        Replicator r = new RedisReplicator(
                CloseTest.class.getClassLoader().getResourceAsStream("appendonly5.aof"), FileType.AOF,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        r.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc.incrementAndGet();
                if (acc.get() == 30) {
                    try {
                        replicator.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        r.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testAofClose");
            }
        });
        r.open();
        assertEquals(30, acc.get());
    }

    @Test
    public void testMixClose1() throws IOException {
        Replicator replicator = new RedisReplicator(
                CloseTest.class.getClassLoader().getResourceAsStream("appendonly4.aof"), FileType.MIXED,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        final AtomicInteger acc1 = new AtomicInteger(0);
        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
                if (acc.get() == 100) {
                    try {
                        replicator.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc1.incrementAndGet();
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testMixClose1");
            }
        });
        replicator.open();
        assertEquals(100, acc.get());
        assertEquals(0, acc1.get());
    }

    @Test
    public void testMixClose2() throws IOException {
        Replicator replicator = new RedisReplicator(
                CloseTest.class.getClassLoader().getResourceAsStream("appendonly4.aof"), FileType.MIXED,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        final AtomicInteger acc1 = new AtomicInteger(0);
        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                acc.incrementAndGet();
            }
        });
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc1.incrementAndGet();
                if (acc1.get() == 100) {
                    try {
                        replicator.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close testMixClose1");
            }
        });
        replicator.open();
        assertEquals(244653, acc.get());
        assertEquals(100, acc1.get());
    }
}
