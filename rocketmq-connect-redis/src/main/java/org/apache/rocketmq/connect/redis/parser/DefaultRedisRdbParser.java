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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.Module;
import com.moilioncircle.redis.replicator.rdb.datatype.Stream;
import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DefaultRedisRdbParser implements RedisRdbParser<KVEntry> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(DefaultRedisRdbParser.class);

    @Override
    public KVEntry applyString(KeyValuePair<byte[], byte[]> keyValuePair) throws IOException {
        RedisEntry builder = RedisEntry.newEntry(FieldType.STRING);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(new String(keyValuePair.getValue()))
            ;
    }

    @Override
    public KVEntry applyList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException {
        return commonArray(keyValuePair);
    }

    @Override
    public KVEntry applySet(KeyValuePair<byte[], Set<byte[]>> keyValuePair) throws IOException {
        return commonArray(keyValuePair);
    }

    @Override
    public KVEntry applyZSet(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException {
        return commonZSet(keyValuePair);
    }

    @Override
    public KVEntry applyZSet2(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException {
        return commonZSet(keyValuePair);
    }

    @Override
    public KVEntry applyHash(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException {
        return commonHash(keyValuePair);
    }

    @Override
    public KVEntry applyHashZipMap(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException {
        return commonHash(keyValuePair);
    }

    @Override
    public KVEntry applyListZipList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException {
        return commonArray(keyValuePair);
    }

    @Override
    public KVEntry applySetIntSet(KeyValuePair<byte[], Set<byte[]>> keyValuePair) throws IOException {
        return commonArray(keyValuePair);
    }

    @Override
    public KVEntry applyZSetZipList(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException {
        return commonZSet(keyValuePair);
    }

    @Override
    public KVEntry applyHashZipList(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException {
        return commonHash(keyValuePair);
    }

    @Override
    public KVEntry applyListQuickList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException {
        return commonArray(keyValuePair);
    }

    @Override
    public KVEntry applyModule(KeyValuePair<byte[], Module> keyValuePair) throws IOException {
        return commonModule(keyValuePair);
    }

    @Override
    public KVEntry applyModule2(KeyValuePair<byte[], Module> keyValuePair) throws IOException {
        return commonModule(keyValuePair);
    }

    @Override
    public KVEntry applyStreamListPacks(KeyValuePair<byte[], Stream> keyValuePair) throws IOException {
        return commonStream(keyValuePair);
    }

    private KVEntry commonStream(KeyValuePair<byte[], ? extends Stream> keyValuePair){
        RedisEntry builder = RedisEntry.newEntry(FieldType.ARRAY);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(keyValuePair.getValue())
            ;
    }

    private KVEntry commonModule(KeyValuePair<byte[], ? extends Module> keyValuePair){
        RedisEntry builder = RedisEntry.newEntry(FieldType.ARRAY);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(keyValuePair.getValue())
            ;
    }

    private KVEntry commonArray(KeyValuePair<byte[], ? extends Collection<byte[]>> keyValuePair){
        RedisEntry builder = RedisEntry.newEntry(FieldType.ARRAY);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(keyValuePair.getValue().stream().map(String::new).collect(Collectors.toCollection(ArrayList::new)))
            ;
    }

    private KVEntry commonZSet(KeyValuePair<byte[], ? extends Collection<ZSetEntry>> keyValuePair){
        RedisEntry builder = RedisEntry.newEntry(FieldType.MAP);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(
                keyValuePair.getValue().stream().collect(Collectors.toMap(
                    entry -> new String(entry.getElement()),
                    ZSetEntry::getScore)
                )
            )
            ;
    }

    private KVEntry commonHash(KeyValuePair<byte[], ? extends Map<byte[], byte[]>> keyValuePair){
        RedisEntry builder = RedisEntry.newEntry(FieldType.MAP);
        return builder
            .param(Options.EXPIRED_TYPE, keyValuePair.getExpiredType())
            .param(Options.EXPIRED_TIME, keyValuePair.getExpiredValue())
            .param(Options.EVICT_TYPE, keyValuePair.getEvictType())
            .param(Options.EVICT_VALUE, keyValuePair.getEvictValue())
            .key(new String(keyValuePair.getKey()))
            .value(
                keyValuePair.getValue().entrySet().stream().collect(Collectors.toMap(
                    entry -> new String(entry.getKey()),
                    entry -> new String(entry.getValue())
                ))
            )
            ;
    }
}
