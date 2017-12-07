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

package org.apache.rocketmq.redis.replicator.cmd.parser;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.MSetCommand;
import org.apache.rocketmq.redis.replicator.util.ByteArrayMap;

public class MSetParser implements CommandParser<MSetCommand> {
    @Override
    public MSetCommand parse(Object[] command) {
        if (command.length == 1)
            return new MSetCommand(null, null);
        int idx = 1;
        Map<String, String> kv = new LinkedHashMap<>();
        ByteArrayMap<byte[]> rawKv = new ByteArrayMap<>();
        while (idx < command.length) {
            String key = CommandParsers.objToString(command[idx]);
            byte[] rawKey = CommandParsers.objToBytes(command[idx]);
            idx++;
            String value = idx == command.length ? null : CommandParsers.objToString(command[idx]);
            byte[] rawValue = idx == command.length ? null : CommandParsers.objToBytes(command[idx]);
            idx++;
            kv.put(key, value);
            rawKv.put(rawKey, rawValue);
        }
        return new MSetCommand(kv, rawKv);
    }

}
