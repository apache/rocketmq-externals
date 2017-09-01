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

package org.apache.rocketmq.redis.replicator.rdb;

import java.io.IOException;
import java.util.Arrays;
import org.apache.rocketmq.redis.replicator.Configuration;
import org.apache.rocketmq.redis.replicator.FileType;
import org.apache.rocketmq.redis.replicator.RedisReplicator;
import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.cmd.Command;
import org.apache.rocketmq.redis.replicator.cmd.CommandListener;
import org.apache.rocketmq.redis.replicator.cmd.CommandName;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueModule;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.module.ModuleParser;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;
import org.apache.rocketmq.redis.replicator.rdb.module.DefaultRdbModuleParser;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

public class ModuleTest {
    @Test
    public void testModule() throws IOException {
        @SuppressWarnings("resource")
        Replicator replicator = new RedisReplicator(ModuleTest.class.getClassLoader().getResourceAsStream("module.rdb"), FileType.RDB,
            Configuration.defaultSetting());
        replicator.addModuleParser("hellotype", 0, new HelloTypeModuleParser());
        replicator.addRdbListener(new RdbListener.Adaptor() {
            @Override
            public void handle(Replicator replicator, KeyValuePair<?> kv) {
                if (kv instanceof KeyStringValueModule) {
                    KeyStringValueModule ksvm = (KeyStringValueModule) kv;
                    System.out.println(kv);
                    assertEquals(12123123112L, ((HelloTypeModule) ksvm.getValue()).getValue()[0]);
                }
            }
        });

        replicator.open();

        replicator = new RedisReplicator(ModuleTest.class.getClassLoader().getResourceAsStream("appendonly6.aof"), FileType.AOF,
            Configuration.defaultSetting());
        replicator.addCommandParser(CommandName.name("hellotype.insert"), new HelloTypeParser());

        replicator.addCommandListener(new CommandListener() {
            @Override
            public void handle(Replicator replicator, Command command) {
                if (command instanceof HelloTypeCommand) {
                    HelloTypeCommand htc = (HelloTypeCommand) command;
                    System.out.println(command);
                    assertEquals(12123123112L, htc.getValue());
                }
            }
        });

        replicator.open();
    }

    public static class HelloTypeModuleParser implements ModuleParser<HelloTypeModule> {

        @Override
        public HelloTypeModule parse(RedisInputStream in, int version) throws IOException {
            DefaultRdbModuleParser parser = new DefaultRdbModuleParser(in);
            int elements = parser.loadUnsigned(version).intValue();
            long[] ary = new long[elements];
            int i = 0;
            while (elements-- > 0) {
                ary[i++] = parser.loadSigned(version);
            }
            return new HelloTypeModule(ary);
        }
    }

    public static class HelloTypeModule implements Module {

        private static final long serialVersionUID = 1L;

        private final long[] value;

        public HelloTypeModule(long[] value) {
            this.value = value;
        }

        public long[] getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "HelloTypeModule{" +
                "value=" + Arrays.toString(value) +
                '}';
        }
    }

    public static class HelloTypeParser implements CommandParser<HelloTypeCommand> {
        @Override
        public HelloTypeCommand parse(Object[] command) {
            String key = new String((byte[]) command[1], UTF_8);
            long value = Long.parseLong(new String((byte[]) command[2], UTF_8));
            return new HelloTypeCommand(key, value);
        }
    }

    public static class HelloTypeCommand implements Command {
        private static final long serialVersionUID = 1L;
        private final String key;
        private final long value;

        public long getValue() {
            return value;
        }

        public String getKey() {
            return key;
        }

        public HelloTypeCommand(String key, long value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "HelloTypeCommand{" +
                "key='" + key + '\'' +
                ", value=" + value +
                '}';
        }

    }
}
