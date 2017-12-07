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

package org.apache.rocketmq.redis.replicator.rdb;

import java.io.IOException;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.event.Event;
import org.apache.rocketmq.redis.replicator.rdb.datatype.DB;

public abstract class RdbVisitor {
    /*
     * rdb prefix
     */
    public String applyMagic(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public int applyVersion(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public int applyType(RedisInputStream in) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * DB
     */
    public DB applySelectDB(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public DB applyResizeDB(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * checksum
     */
    public long applyEof(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * aux
     */
    public Event applyAux(RedisInputStream in, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    /*
     * entity
     */
    public Event applyExpireTime(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyExpireTimeMs(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyString(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyList(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applySet(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSet(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSet2(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHash(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHashZipMap(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyListZipList(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applySetIntSet(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyZSetZipList(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyHashZipList(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyListQuickList(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyModule(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public Event applyModule2(RedisInputStream in, DB db, int version) throws IOException {
        throw new UnsupportedOperationException("must implement this method.");
    }
}
