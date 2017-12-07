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
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.EvalCommand;

import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToBytes;
import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToString;

public class EvalParser implements CommandParser<EvalCommand> {
    @Override
    public EvalCommand parse(Object[] command) {
        int idx = 1;
        String script = objToString(command[idx]);
        byte[] rawScript = objToBytes(command[idx]);
        idx++;
        int numkeys = new BigDecimal(objToString(command[idx++])).intValueExact();
        String[] keys = new String[numkeys];
        byte[][] rawKeys = new byte[numkeys][];
        for (int i = 0; i < numkeys; i++) {
            keys[i] = objToString(command[idx]);
            rawKeys[i] = objToBytes(command[idx]);
            idx++;
        }
        List<String> list = new ArrayList<>();
        List<byte[]> rawList = new ArrayList<>();
        while (idx < command.length) {
            list.add(objToString(command[idx]));
            rawList.add(objToBytes(command[idx]));
            idx++;
        }
        String[] args = new String[list.size()];
        byte[][] rawArgs = new byte[rawList.size()][];
        list.toArray(args);
        rawList.toArray(rawArgs);
        return new EvalCommand(script, numkeys, keys, args, rawScript, rawKeys, rawArgs);
    }

}
