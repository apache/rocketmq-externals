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

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.redis.replicator.cmd.CommandParser;
import org.apache.rocketmq.redis.replicator.cmd.impl.ZAddCommand;
import org.apache.rocketmq.redis.replicator.cmd.impl.ExistType;
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;

import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToBytes;
import static org.apache.rocketmq.redis.replicator.cmd.parser.CommandParsers.objToString;

public class ZAddParser implements CommandParser<ZAddCommand> {

    @Override
    public ZAddCommand parse(Object[] command) {
        int idx = 1;
        Boolean isCh = null, isIncr = null;
        ExistType existType = ExistType.NONE;
        List<ZSetEntry> list = new ArrayList<>();
        String key = objToString(command[idx]);
        byte[] rawKey = objToBytes(command[idx]);
        idx++;
        boolean et = false;
        while (idx < command.length) {
            String param = objToString(command[idx]);
            if (!et && "NX".equalsIgnoreCase(param)) {
                existType = ExistType.NX;
                et = true;
                idx++;
                continue;
            } else if (!et && "XX".equalsIgnoreCase(param)) {
                existType = ExistType.XX;
                et = true;
                idx++;
                continue;
            }
            if (isCh == null && "CH".equalsIgnoreCase(param)) {
                isCh = true;
            } else if (isIncr == null && "INCR".equalsIgnoreCase(param)) {
                isIncr = true;
            } else {
                double score = Double.parseDouble(param);
                idx++;
                String member = objToString(command[idx]);
                byte[] rawMember = objToBytes(command[idx]);
                list.add(new ZSetEntry(member, score, rawMember));
            }
            idx++;
        }
        ZSetEntry[] zSetEntries = new ZSetEntry[list.size()];
        list.toArray(zSetEntries);
        return new ZAddCommand(key, existType, isCh, isIncr, zSetEntries, rawKey);
    }

}
