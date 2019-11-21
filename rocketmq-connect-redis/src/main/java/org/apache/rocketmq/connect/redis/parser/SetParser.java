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

import static com.moilioncircle.redis.replicator.cmd.CommandParsers.toRune;
import static com.moilioncircle.redis.replicator.util.Strings.isEquals;

import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.common.RedisConstants;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;

import io.openmessaging.connector.api.data.FieldType;

/**
 * set key value [expiration EX seconds|PX milliseconds] [NX|XX]
 */
public class SetParser extends AbstractCommandParser {
    @Override
    public KVEntry createBuilder() {
        return RedisEntry.newEntry(FieldType.STRING);
    }

    @Override
    public KVEntry handleValue(KVEntry builder, String[] args) {
        builder.value(args[0]);
        int idx = 1;
        while (idx < args.length){
            String param = toRune(args[idx++]);
            if (isEquals(param, RedisConstants.NX)) {
                builder.param(Options.REDIS_NX, Boolean.TRUE);
            } else if (isEquals(param, RedisConstants.XX)) {
                builder.param(Options.REDIS_XX, Boolean.TRUE);
            } else if (isEquals(param, RedisConstants.KEEPTTL)) {
                builder.param(Options.REDIS_KEEPTTL, Boolean.TRUE);
            } else if (isEquals(param, RedisConstants.EX)) {
                builder.param(Options.REDIS_EX, Integer.parseInt(args[idx++]));
            } else if (isEquals(param, RedisConstants.PX)) {
                builder.param(Options.REDIS_PX, Long.parseLong(args[idx++]));
            }
        }
        return builder;
    }
}
