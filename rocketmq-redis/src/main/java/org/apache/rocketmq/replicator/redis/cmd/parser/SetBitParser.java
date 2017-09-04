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
import org.apache.rocketmq.replicator.redis.cmd.impl.SetBitCommand;

import java.math.BigDecimal;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class SetBitParser implements CommandParser<SetBitCommand> {
    @Override
    public SetBitCommand parse(Object[] command) {
        int idx = 1;
        String key = (String) command[idx++];
        long offset = new BigDecimal((String) command[idx++]).longValueExact();
        int value = new BigDecimal((String) command[idx++]).intValueExact();
        return new SetBitCommand(key, offset, value);
    }

}
