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
import org.apache.rocketmq.replicator.redis.cmd.CommandName;
import org.apache.rocketmq.replicator.redis.cmd.CommandParser;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.replicator.redis.rdb.datatype.Module;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleParser;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.fail;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class UnsupportOperationTest {

    @Test
    public void testRdb() throws IOException {
        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                    Configuration.defaultSetting());
            replicator.addCommandListener(new CommandListener() {
                @Override
                public void handle(Replicator replicator, Command command) {
                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                    Configuration.defaultSetting());
            replicator.removeCommandListener(new CommandListener() {
                @Override
                public void handle(Replicator replicator, Command command) {
                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                    Configuration.defaultSetting());
            replicator.getCommandParser(CommandName.name("PING"));
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                    Configuration.defaultSetting());
            replicator.addCommandParser(CommandName.name("PING"), new CommandParser<Command>() {
                @Override
                public Command parse(Object[] command) {
                    return null;
                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.RDB,
                    Configuration.defaultSetting());
            replicator.removeCommandParser(CommandName.name("PING"));
            replicator.open();
            fail();
        } catch (Exception e) {
        }
    }

    @Test
    public void testAof() throws IOException {
        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("dumpV7.rdb"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.addRdbListener(new RdbListener.Adaptor() {
                @Override
                public void handle(Replicator replicator, KeyValuePair<?> kv) {

                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.removeRdbListener(new RdbListener.Adaptor() {
                @Override
                public void handle(Replicator replicator, KeyValuePair<?> kv) {

                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.getModuleParser("hellotype", 0);
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.addModuleParser("hellotype", 0, new ModuleParser<Module>() {
                @Override
                public Module parse(RedisInputStream in) throws IOException {
                    return null;
                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.removeModuleParser("hellotype", 0);
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.addAuxFieldListener(new AuxFieldListener() {
                @Override
                public void handle(Replicator replicator, AuxField auxField) {

                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }

        try {
            Replicator replicator = new RedisReplicator(
                    RedisSocketReplicatorTest.class.getClassLoader().getResourceAsStream("appendonly1.aof"), FileType.AOF,
                    Configuration.defaultSetting());
            replicator.removeAuxFieldListener(new AuxFieldListener() {
                @Override
                public void handle(Replicator replicator, AuxField auxField) {

                }
            });
            replicator.open();
            fail();
        } catch (Exception e) {
        }
    }
}
