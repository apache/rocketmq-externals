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

package org.apache.rocketmq.replicator.redis.rdb.module;

import org.apache.rocketmq.replicator.redis.RedisConstants;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.BaseRdbParser;
import org.apache.rocketmq.replicator.redis.util.ByteArray;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class DefaultRdbModuleParser {
    protected static final Logger logger = LoggerFactory.getLogger(DefaultRdbModuleParser.class);

    private final RedisInputStream in;
    private final BaseRdbParser parser;

    public DefaultRdbModuleParser(RedisInputStream in) {
        this.in = in;
        this.parser = new BaseRdbParser(in);
    }

    public RedisInputStream inputStream() {
        return this.in;
    }

    public long loadSigned() throws IOException {
        return parser.rdbLoadLen().len;
    }

    public long loadUnSigned() throws IOException {
        logger.warn("unsupported [loadUnSigned]. using [loadSigned] instead");
        return loadSigned();
    }

    public String loadString() throws IOException {
        ByteArray bytes = (ByteArray) parser.rdbGenericLoadStringObject(RedisConstants.RDB_LOAD_NONE);
        return new String(bytes.first(), RedisConstants.CHARSET);
    }

    public String loadStringBuffer() throws IOException {
        ByteArray bytes = (ByteArray) parser.rdbGenericLoadStringObject(RedisConstants.RDB_LOAD_PLAIN);
        return new String(bytes.first(), RedisConstants.CHARSET);
    }

    public double loadDouble() throws IOException {
        return parser.rdbLoadBinaryDoubleValue();
    }
}
