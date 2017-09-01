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

import java.math.BigDecimal;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.ExpireCommand;

import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToBytes;
import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToString;

public class ExpireParser implements CommandParser<ExpireCommand> {
    @Override
    public ExpireCommand parse(Object[] command) {
        int idx = 1;
        String key = objToString(command[idx]);
        byte[] rawKey = objToBytes(command[idx]);
        idx++;
        int ex = new BigDecimal(objToString(command[idx++])).intValueExact();
        return new ExpireCommand(key, ex, rawKey);
    }

}
