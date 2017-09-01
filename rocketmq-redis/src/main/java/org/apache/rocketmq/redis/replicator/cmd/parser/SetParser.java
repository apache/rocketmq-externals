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
import org.apache.rocketmq.redis.replicator.cmd.impl.SetCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ExistType;

public class SetParser implements CommandParser<SetCommand> {

    @Override
    public SetCommand parse(Object[] command) {
        String key = CommandParsers.objToString(command[1]);
        byte[] rawKey = CommandParsers.objToBytes(command[1]);
        String value = CommandParsers.objToString(command[2]);
        byte[] rawValue = CommandParsers.objToBytes(command[2]);
        int idx = 3;
        ExistType existType = ExistType.NONE;
        Integer ex = null;
        Long px = null;
        boolean et = false, st = false;
        while (idx < command.length) {
            String param = CommandParsers.objToString(command[idx++]);
            if (!et && "NX".equalsIgnoreCase(param)) {
                existType = ExistType.NX;
                et = true;
            } else if (!et && "XX".equalsIgnoreCase(param)) {
                existType = ExistType.XX;
                et = true;
            }

            if (!st && "EX".equalsIgnoreCase(param)) {
                ex = Integer.valueOf(CommandParsers.objToString(command[idx++]));
                st = true;
            } else if (!st && "PX".equalsIgnoreCase(param)) {
                px = Long.valueOf(CommandParsers.objToString(command[idx++]));
                st = true;
            }
        }
        return new SetCommand(key, value, ex, px, existType, rawKey, rawValue);
    }

}
