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
import org.apache.rocketmq.replicator.redis.cmd.impl.ZAddCommand;
import org.apache.rocketmq.replicator.redis.rdb.datatype.ZSetEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ZAddParser implements CommandParser<ZAddCommand> {

    @Override
    public ZAddCommand parse(Object[] command) {
        int idx = 1;
        Boolean isCh = null, isIncr = null;
        ExistType existType = ExistType.NONE;
        List<ZSetEntry> list = new ArrayList<>();
        String key = (String) command[idx++];
        boolean et = false;
        while (idx < command.length) {
            String param = (String) command[idx];
            if (!et && param.equalsIgnoreCase("NX")) {
                existType = ExistType.NX;
                et = true;
                idx++;
                continue;
            } else if (!et && param.equalsIgnoreCase("XX")) {
                existType = ExistType.XX;
                et = true;
                idx++;
                continue;
            }
            if (isCh == null && param.equalsIgnoreCase("CH")) {
                isCh = true;
            } else if (isIncr == null && param.equalsIgnoreCase("INCR")) {
                isIncr = true;
            } else {
                double score = Double.parseDouble(param);
                idx++;
                String member = (String) command[idx];
                list.add(new ZSetEntry(member, score));
            }
            idx++;
        }
        ZSetEntry[] zSetEntries = new ZSetEntry[list.size()];
        list.toArray(zSetEntries);
        return new ZAddCommand(key, existType, isCh, isIncr, zSetEntries);
    }

}
