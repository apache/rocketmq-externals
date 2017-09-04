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
import org.apache.rocketmq.replicator.redis.cmd.impl.ZRemRangeByLexCommand;

/**
 * @author Leon Chen
 * @since 2.1.1
 */
public class ZRemRangeByLexParser implements CommandParser<ZRemRangeByLexCommand> {

    @Override
    public ZRemRangeByLexCommand parse(Object[] command) {
        int idx = 1;
        String key = (String) command[idx++];
        String min = (String) command[idx++];
        String max = (String) command[idx++];
        return new ZRemRangeByLexCommand(key, min, max);
    }
}
