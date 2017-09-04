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

package org.apache.rocketmq.replicator.redis.cmd.parser;

import org.apache.rocketmq.replicator.redis.cmd.CommandParser;
import org.apache.rocketmq.replicator.redis.cmd.impl.ExistType;
import org.apache.rocketmq.replicator.redis.cmd.impl.SetCommand;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class SetParser implements CommandParser<SetCommand> {

    @Override
    public SetCommand parse(Object[] command) {
        String key = (String) command[1];
        String value = (String) command[2];
        int idx = 3;
        ExistType existType = ExistType.NONE;
        Integer ex = null;
        Long px = null;
        boolean et = false, st = false;
        while (idx < command.length) {
            String param = (String) command[idx++];
            if (!et && param.equalsIgnoreCase("NX")) {
                existType = ExistType.NX;
                et = true;
            } else if (!et && param.equalsIgnoreCase("XX")) {
                existType = ExistType.XX;
                et = true;
            }
            if (!st && param.equalsIgnoreCase("EX")) {
                ex = Integer.valueOf((String) command[idx++]);
                st = true;
            } else if (!st && param.equalsIgnoreCase("PX")) {
                px = Long.valueOf((String) command[idx++]);
                st = true;
            }
        }
        return new SetCommand(key, value, ex, px, existType);
    }

}
