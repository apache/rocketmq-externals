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
import org.apache.rocketmq.redis.replicator.cmd.impl.DefaultCommand;
import org.apache.rocketmq.redis.replicator.util.Arrays;

public class DefaultCommandParser implements CommandParser<DefaultCommand> {
    @Override
    public DefaultCommand parse(Object[] command) {
        byte[][] args = new byte[command.length - 1][];
        for (int i = 1, j = 0; i < command.length; i++) {
            if (command[i] instanceof Long) {
                args[j++] = String.valueOf(command[i]).getBytes();
            } else if (command[i] instanceof byte[]) {
                args[j++] = (byte[]) command[i];
            } else if (command[i] instanceof Object[]) {
                throw new UnsupportedOperationException(Arrays.deepToString(command));
            }
        }
        return new DefaultCommand((byte[]) command[0], args);
    }
}
