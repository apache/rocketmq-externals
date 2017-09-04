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
import org.apache.rocketmq.replicator.redis.cmd.impl.ScriptCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ScriptFlushCommand;
import org.apache.rocketmq.replicator.redis.cmd.impl.ScriptLoadCommand;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ScriptParser implements CommandParser<ScriptCommand> {
    @Override
    public ScriptCommand parse(Object[] command) {
        int idx = 1;
        String keyWord = (String) command[idx++];
        if (keyWord.equalsIgnoreCase("LOAD")) {
            String script = (String) command[idx++];
            return new ScriptLoadCommand(script);
        } else if (keyWord.equalsIgnoreCase("FLUSH")) {
            return new ScriptFlushCommand();
        }
        throw new AssertionError("SCRIPT " + keyWord);
    }


}
