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

import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;

/**
 * Restore key ttl serialized-value [REPLACE]
 */
public class RestoreParser extends AbstractCommandParser {
    @Override
    public KVEntry createBuilder() {
        return RedisEntry.newEntry(FieldType.STRING);
    }

    @Override
    public KVEntry handleValue(KVEntry builder, String[] args) {
        if (args.length > 1) {
            long ttl = Long.parseLong(args[0]);
            builder.param(Options.REDIS_TTL, ttl).value(args[1]);
            if (args.length > 2) {
                builder.param(Options.REDIS_REPLACE, Boolean.TRUE);
            }
        }
        return builder;
    }
}
