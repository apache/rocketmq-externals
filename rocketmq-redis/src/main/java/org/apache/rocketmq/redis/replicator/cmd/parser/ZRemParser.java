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

import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZRemCommand;

public class ZRemParser implements CommandParser<ZRemCommand> {

    @Override
    public ZRemCommand parse(Object[] command) {
        int idx = 1, newIdx = 0;
        String key = CommandParsers.objToString(command[idx]);
        byte[] rawKey = CommandParsers.objToBytes(command[idx]);
        idx++;
        String[] members = new String[command.length - 2];
        byte[][] rawMembers = new byte[command.length - 2][];
        while (idx < command.length) {
            members[newIdx] = CommandParsers.objToString(command[idx]);
            rawMembers[newIdx] = CommandParsers.objToBytes(command[idx]);
            newIdx++;
            idx++;
        }
        return new ZRemCommand(key, members, rawKey, rawMembers);
    }

}
