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

package org.apache.rocketmq.connect.redis.parser;

import java.util.HashMap;
import java.util.Map;

import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;

/**
 * ZADD key [NX|XX] [CH] [INCR] score member [score member ...]
 */
public class ZAddParser extends AbstractCommandParser {
    @Override
    public KVEntry createBuilder() {
        return RedisEntry.newEntry(FieldType.MAP);
    }

    @Override
    public KVEntry handleValue(KVEntry builder, String[] args) {
        int idx = 0;
        loop:
        while (true) {
            String param = args[idx];
            switch (param) {
                case RedisConstants.NX:
                    builder.param(Options.REDIS_NX, Boolean.TRUE);
                    break;
                case RedisConstants.XX:
                    builder.param(Options.REDIS_XX, Boolean.TRUE);
                    break;
                case RedisConstants.CH:
                    builder.param(Options.REDIS_CH, Boolean.TRUE);
                    break;
                case RedisConstants.INCR:
                    builder.param(Options.REDIS_INCR, Boolean.TRUE);
                    break;
                default:
                    break loop;
            }
            idx++;
        }
        Map<String, String> kvMap = new HashMap<>((args.length - idx) / 2);
        for (int i = idx, j = i + 1; i < args.length; i++, i++, j++, j++) {
            kvMap.put(args[j], args[i]);
        }
        builder.value(kvMap);
        return builder;
    }
}
