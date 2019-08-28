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

package org.apache.rocketmq.connect.redis.parser;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.moilioncircle.redis.replicator.cmd.CommandParser;
import com.moilioncircle.redis.replicator.cmd.CommandParsers;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.moilioncircle.redis.replicator.cmd.CommandParsers.toRune;

public abstract class AbstractCommandParser implements CommandParser<KVEntry> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommandParser.class);

    @Override public KVEntry parse(Object[] command) {
        KVEntry builder = createBuilder();

        if (builder == null) {
            return null;
        }

        if (command.length > 0) {
            String commandStr = toRune(command[0]);
            builder.command(commandStr);
        }

        if (command.length > 1) {
            String commandKeyStr = toRune(command[1]);
            builder.key(commandKeyStr);
        }

        // 有其他参数
        if (command.length > 2) {
            byte[][] real_byte_args = new byte[command.length - 2][];
            System.arraycopy(command, 2, real_byte_args, 0, real_byte_args.length);
            String[] real_args = Arrays.stream(real_byte_args).map(CommandParsers::toRune).toArray(String[]::new);
            try {
                builder = handleValue(builder, real_args);
            } catch (Exception e) {
                LOGGER.error("parser value error: {} {}", Arrays.stream(command).map(CommandParsers::toRune)
                    .collect(Collectors.joining(" ")), e);
            }
        }else {
            try {
                builder = handleNoArgValue(builder);
            } catch (Exception e) {
                LOGGER.error("parser value error: {} {}", Arrays.stream(command).map(CommandParsers::toRune)
                    .collect(Collectors.joining(" ")), e);
            }
        }
        return builder;
    }

    public abstract KVEntry createBuilder();

    public KVEntry handleValue(KVEntry builder, String[] args){
        return builder;
    }

    public KVEntry handleNoArgValue(KVEntry builder){
        return builder;
    }
}
