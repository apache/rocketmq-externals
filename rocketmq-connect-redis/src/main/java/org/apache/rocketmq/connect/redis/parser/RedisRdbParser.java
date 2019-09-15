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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.Module;
import com.moilioncircle.redis.replicator.rdb.datatype.Stream;
import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;

/**
 * 处理RDB文件接口
 *
 * @param <T>
 */
public interface RedisRdbParser<T> {

    T applyString(KeyValuePair<byte[], byte[]> keyValuePair) throws IOException;

    T applyList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException;

    T applySet(KeyValuePair<byte[], Set<byte[]>> keyValuePair) throws IOException;

    T applyZSet(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException;

    T applyZSet2(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException;

    T applyHash(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException;

    T applyHashZipMap(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException;

    T applyListZipList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException;

    T applySetIntSet(KeyValuePair<byte[], Set<byte[]>> keyValuePair) throws IOException;

    T applyZSetZipList(KeyValuePair<byte[], Set<ZSetEntry>> keyValuePair) throws IOException;

    T applyHashZipList(KeyValuePair<byte[], Map<byte[], byte[]>> keyValuePair) throws IOException;

    T applyListQuickList(KeyValuePair<byte[], List<byte[]>> keyValuePair) throws IOException;

    T applyModule(KeyValuePair<byte[], Module> keyValuePair) throws IOException;

    T applyModule2(KeyValuePair<byte[], Module> keyValuePair) throws IOException;

    T applyStreamListPacks(KeyValuePair<byte[], Stream> keyValuePair) throws IOException;

}
