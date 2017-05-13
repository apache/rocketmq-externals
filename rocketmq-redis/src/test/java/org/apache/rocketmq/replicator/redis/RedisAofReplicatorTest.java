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

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandListener;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetCommand;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisAofReplicatorTest {
    @Test
    public void open() throws Exception {
        Replicator replicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc.incrementAndGet();
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close open");
                assertEquals(4, acc.get());
            }
        });
        replicator.open();
    }

    @Test
    public void open2() throws Exception {
        Replicator replicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly2.aof"), FileType.AOF,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                if (command instanceof SetCommand && ((SetCommand) command).getKey().startsWith("test_")) {
                    acc.incrementAndGet();
                }
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close open2");
                assertEquals(48000, acc.get());
            }
        });
        replicator.open();
    }

    @Test
    public void open3() throws Exception {
        Replicator replicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly3.aof"), FileType.AOF,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc.incrementAndGet();
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close open3");
                assertEquals(92539, acc.get());
            }
        });
        replicator.open();
    }

    @Test
    public void open4() throws Exception {
        Replicator replicator = new RedisReplicator(
                RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly5.aof"), FileType.AOF,
                Configuration.defaultSetting());
        final AtomicInteger acc = new AtomicInteger(0);
        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                acc.incrementAndGet();
            }
        });
        replicator.addCloseListener(new CloseListener() {
            @Override
            public void handle(Replicator replicator) {
                System.out.println("close open4");
                assertEquals(71, acc.get());
            }
        });
        replicator.open();
    }

}