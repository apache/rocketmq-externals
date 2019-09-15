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

import java.util.Arrays;

import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;

/**
 * XCLAIM key group consumer min-idle-time ID [ID ...] [IDLE ms] [TIME ms-unix-time] [RETRYCOUNT count] [force]
 */
public class XClaimParser extends AbstractCommandParser {
    @Override
    public KVEntry createBuilder() {
        return RedisEntry.newEntry(FieldType.ARRAY);
    }

    @Override
    public KVEntry handleValue(KVEntry builder, String[] args) {
        builder.param(Options.REDIS_GROUP, args[0]);
        builder.param(Options.REDIS_CONSUMER, args[1]);
        builder.param(Options.REDIS_MIN_IDLE_TIME, Long.parseLong(args[2]));
        int idx = args.length;
        for (int i = 3; i < args.length; i++) {
            if (RedisConstants.IDLE.equals(args[i].toUpperCase()) ||
                RedisConstants.TIME.equals(args[i].toUpperCase()) ||
                RedisConstants.RETRYCOUNT.equals(args[i].toUpperCase()) ||
                RedisConstants.FORCE.equals(args[i].toUpperCase()) ||
                RedisConstants.JUSTID.equals(args[i].toUpperCase())
            ) {
                idx = i;
                break;
            }
        }
        String[] ids = new String[idx - 3];
        System.arraycopy(args, 3, ids, 0, idx - 3);
        builder.value(Arrays.asList(ids));

        while (idx < args.length) {
            switch (args[idx].toUpperCase()) {
                case RedisConstants.IDLE:
                    builder.param(Options.REDIS_IDLE, Long.parseLong(args[idx + 1]));
                    idx += 2;
                    break;
                case RedisConstants.TIME:
                    builder.param(Options.REDIS_TIME, Long.parseLong(args[idx + 1]));
                    idx += 2;
                    break;
                case RedisConstants.RETRYCOUNT:
                    builder.param(Options.REDIS_RETRYCOUNT, Integer.parseInt(args[idx + 1]));
                    idx += 2;
                    break;
                case RedisConstants.FORCE:
                    builder.param(Options.REDIS_FORCE, Boolean.TRUE);
                    idx += 1;
                    break;
                case RedisConstants.JUSTID:
                    builder.param(Options.REDIS_JUSTID, Boolean.TRUE);
                    idx += 1;
                    break;
                default:
                    idx += 1;
                    break;
            }
        }

        return builder;
    }
}
