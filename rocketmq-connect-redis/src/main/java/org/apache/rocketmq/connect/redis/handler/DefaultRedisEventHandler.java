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

package org.apache.rocketmq.connect.redis.handler;

import java.io.IOException;

import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import org.apache.rocketmq.connect.redis.common.Config;
import org.apache.rocketmq.connect.redis.common.SyncMod;
import org.apache.rocketmq.connect.redis.parser.DefaultRedisRdbParser;
import org.apache.rocketmq.connect.redis.parser.RedisRdbParser;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_HASH_ZIPMAP;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_QUICKLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_LIST_ZIPLIST;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_MODULE_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_SET_INTSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STREAM_LISTPACKS;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_STRING;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_2;
import static com.moilioncircle.redis.replicator.Constants.RDB_TYPE_ZSET_ZIPLIST;

/**
 * Handling various types of redis events
 */
public class DefaultRedisEventHandler implements RedisEventHandler {
    protected final Logger LOGGER = LoggerFactory.getLogger(DefaultRedisEventHandler.class);
    /**
     * config info
     */
    private Config config;

    private RedisRdbParser<KVEntry> redisRdbParser;

    public DefaultRedisEventHandler(Config config) {
        this.config = config;
        this.redisRdbParser = new DefaultRedisRdbParser();
    }

    /**
     * Handle redis commands
     *
     * @param replId
     * @param replOffset
     * @param command
     * @return
     * @throws Exception
     */
    @Override public KVEntry handleCommand(String replId, Long replOffset, Command command) throws Exception {
        if (command instanceof KVEntry) {
            return ((KVEntry)command).sourceId(replId).offset(replOffset);
        } else {
            return this.handleOtherEvent(replId, replOffset, command);
        }
    }

    /**
     * Handle kV data in RDB files
     *
     * @param replId
     * @param replOffset
     * @param keyValuePair
     * @return
     * @throws Exception
     */
    @Override public KVEntry handleKVString(String replId, Long replOffset, KeyValuePair keyValuePair) throws Exception {
        //Increment mode asynchronous RDB files
        if (SyncMod.LAST_OFFSET.equals(this.config.getSyncMod())) {
            return null;
        }
        KVEntry entry = parseRdbData(this.redisRdbParser, keyValuePair);
        if(entry == null){
            return null;
        }
        return entry.sourceId(replId).offset(replOffset);
    }

    /**
     * Handle batch kV event
     *
     * @param replId
     * @param replOffset
     * @param batchedKeyValuePair
     * @return
     * @throws Exception
     */
    @Override public KVEntry handleBatchKVString(String replId, Long replOffset, BatchedKeyValuePair batchedKeyValuePair)
        throws Exception {
        LOGGER.warn("skip handle batch event: {}", batchedKeyValuePair);
        return null;
    }

    /**
     * Handle redis other operation instructions
     *
     * @param replId
     * @param replOffset
     * @param event
     * @return
     * @throws Exception
     */

    @Override public KVEntry handleOtherEvent(String replId, Long replOffset, Event event) throws Exception {
        LOGGER.warn("skip handle other event: {}", event.getClass());
        return null;
    }

    private <T extends KVEntry> T parseRdbData(RedisRdbParser<T> redisRdbParser, KeyValuePair keyValuePair)
        throws IOException {
        int rdbValueType = keyValuePair.getValueRdbType();

        switch (rdbValueType) {
            case RDB_TYPE_STRING:
                return (T)redisRdbParser.applyString(keyValuePair).command("SET");
            case RDB_TYPE_LIST:
                return (T)redisRdbParser.applyList(keyValuePair).command("RPUSH");
            case RDB_TYPE_SET:
                return (T)redisRdbParser.applySet(keyValuePair).command("SADD");
            case RDB_TYPE_ZSET:
                return (T)redisRdbParser.applyZSet(keyValuePair).command("ZADD");
            case RDB_TYPE_ZSET_2:
                return (T)redisRdbParser.applyZSet2(keyValuePair).command("ZADD");
            case RDB_TYPE_HASH:
                return (T)redisRdbParser.applyHash(keyValuePair).command("HMSET");
            case RDB_TYPE_HASH_ZIPMAP:
                return (T)redisRdbParser.applyHashZipMap(keyValuePair).command("HMSET");
            case RDB_TYPE_LIST_ZIPLIST:
                return (T)redisRdbParser.applyListZipList(keyValuePair).command("RPUSH");
            case RDB_TYPE_SET_INTSET:
                return (T)redisRdbParser.applySetIntSet(keyValuePair).command("ZADD");
            case RDB_TYPE_ZSET_ZIPLIST:
                return (T)redisRdbParser.applyZSetZipList(keyValuePair).command("ZADD");
            case RDB_TYPE_HASH_ZIPLIST:
                return (T)redisRdbParser.applyHashZipList(keyValuePair).command("HMSET");
            case RDB_TYPE_LIST_QUICKLIST:
                return (T)redisRdbParser.applyListQuickList(keyValuePair).command("RPUSH");
            case RDB_TYPE_MODULE:
                return (T)redisRdbParser.applyModule(keyValuePair);
            case RDB_TYPE_MODULE_2:
                return (T)redisRdbParser.applyModule2(keyValuePair);
            case RDB_TYPE_STREAM_LISTPACKS:
                return (T)redisRdbParser.applyStreamListPacks(keyValuePair);
            default:
                throw new AssertionError("unexpected value type:" + rdbValueType);
        }
    }
}
