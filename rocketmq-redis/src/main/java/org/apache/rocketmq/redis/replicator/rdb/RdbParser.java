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
import org.apache.rocketmq.redis.replicator.event.Event;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.datatype.DB;
import org.apache.rocketmq.redis.replicator.AbstractReplicator;
import org.apache.rocketmq.redis.replicator.event.PostFullSyncEvent;
import org.apache.rocketmq.redis.replicator.event.PreFullSyncEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_AUX;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_EOF;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_EXPIRETIME;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_EXPIRETIME_MS;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_RESIZEDB;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_OPCODE_SELECTDB;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH_ZIPLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH_ZIPMAP;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST_QUICKLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST_ZIPLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_MODULE;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_MODULE_2;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET_INTSET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_STRING;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET_2;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET_ZIPLIST;
import static org.apache.rocketmq.redis.replicator.Status.CONNECTED;

public class RdbParser {

    protected final RedisInputStream in;
    protected final RdbVisitor rdbVisitor;
    protected final AbstractReplicator replicator;
    protected static final Logger LOGGER = LoggerFactory.getLogger(RdbParser.class);

    public RdbParser(RedisInputStream in, AbstractReplicator replicator) {
        this.in = in;
        this.replicator = replicator;
        this.rdbVisitor = this.replicator.getRdbVisitor();
    }

    /**
     * ----------------------------# RDB is a binary format. There are no new lines or spaces in the file. <p> 52 45 44
     * 49 53              # Magic String "REDIS" <p> 30 30 30 33                 # RDB Version Number in big endian. In
     * this case, version = 0003 = 3 <p> ---------------------------- <p> FE 00                       # FE = code that
     * indicates database selector. db number = 00 <p> ----------------------------# Key-Value pair starts <p> FD
     * $unsigned int            # FD indicates "expiry time in seconds". After that, expiry time is read as a 4 byte
     * unsigned int <p> $value-type                 # 1 byte flag indicating the type of value - set, map, sorted set
     * etc. <p> $string-encoded-name         # The name, encoded as a redis string <p> $encoded-value              # The
     * value. Encoding depends on $value-type <p> ---------------------------- <p> FC $unsigned long           # FC
     * indicates "expiry time in ms". After that, expiry time is read as a 8 byte unsigned long <p> $value-type
     *        # 1 byte flag indicating the type of value - set, map, sorted set etc. <p> $string-encoded-name         #
     * The name, encoded as a redis string <p> $encoded-value              # The value. Encoding depends on $value-type
     * <p> ---------------------------- <p> $value-type                 # This name value pair doesn't have an expiry.
     * $value_type guaranteed != to FD, FC, FE and FF <p> $string-encoded-name <p> $encoded-value <p>
     * ---------------------------- <p> FE $length-encoding         # Previous db ends, next db starts. Database number
     * read using length encoding. <p> ---------------------------- <p> ...                         # Key value pairs
     * for this database, additonal database <p> FF                          ## End of RDB file indicator <p> 8 byte
     * checksum             ## CRC 64 checksum of the entire file. <p>
     *
     * @return read bytes
     * @throws IOException when read timeout
     */
    public long parse() throws IOException {
        /*
         * ----------------------------
         * 52 45 44 49 53              # Magic String "REDIS"
         * 30 30 30 33                 # RDB Version Number in big endian. In this case, version = 0003 = 3
         * ----------------------------
         */
        this.replicator.submitEvent(new PreFullSyncEvent());
        rdbVisitor.applyMagic(in);
        int version = rdbVisitor.applyVersion(in);
        DB db = null;
        /*
         * rdb
         */
        loop:
        while (this.replicator.getStatus() == CONNECTED) {
            int type = rdbVisitor.applyType(in);
            Event event = null;
            switch (type) {
                case RDB_OPCODE_EXPIRETIME:
                    event = rdbVisitor.applyExpireTime(in, db, version);
                    break;
                case RDB_OPCODE_EXPIRETIME_MS:
                    event = rdbVisitor.applyExpireTimeMs(in, db, version);
                    break;
                case RDB_OPCODE_AUX:
                    event = rdbVisitor.applyAux(in, version);
                    break;
                case RDB_OPCODE_RESIZEDB:
                    rdbVisitor.applyResizeDB(in, db, version);
                    break;
                case RDB_OPCODE_SELECTDB:
                    db = rdbVisitor.applySelectDB(in, version);
                    break;
                case RDB_OPCODE_EOF:
                    long checksum = rdbVisitor.applyEof(in, version);
                    this.replicator.submitEvent(new PostFullSyncEvent(checksum));
                    break loop;
                case RDB_TYPE_STRING:
                    event = rdbVisitor.applyString(in, db, version);
                    break;
                case RDB_TYPE_LIST:
                    event = rdbVisitor.applyList(in, db, version);
                    break;
                case RDB_TYPE_SET:
                    event = rdbVisitor.applySet(in, db, version);
                    break;
                case RDB_TYPE_ZSET:
                    event = rdbVisitor.applyZSet(in, db, version);
                    break;
                case RDB_TYPE_ZSET_2:
                    event = rdbVisitor.applyZSet2(in, db, version);
                    break;
                case RDB_TYPE_HASH:
                    event = rdbVisitor.applyHash(in, db, version);
                    break;
                case RDB_TYPE_HASH_ZIPMAP:
                    event = rdbVisitor.applyHashZipMap(in, db, version);
                    break;
                case RDB_TYPE_LIST_ZIPLIST:
                    event = rdbVisitor.applyListZipList(in, db, version);
                    break;
                case RDB_TYPE_SET_INTSET:
                    event = rdbVisitor.applySetIntSet(in, db, version);
                    break;
                case RDB_TYPE_ZSET_ZIPLIST:
                    event = rdbVisitor.applyZSetZipList(in, db, version);
                    break;
                case RDB_TYPE_HASH_ZIPLIST:
                    event = rdbVisitor.applyHashZipList(in, db, version);
                    break;
                case RDB_TYPE_LIST_QUICKLIST:
                    event = rdbVisitor.applyListQuickList(in, db, version);
                    break;
                case RDB_TYPE_MODULE:
                    event = rdbVisitor.applyModule(in, db, version);
                    break;
                case RDB_TYPE_MODULE_2:
                    event = rdbVisitor.applyModule2(in, db, version);
                    break;
                default:
                    throw new AssertionError("unexpected value type:" + type + ", check your ModuleParser or ValueIterableRdbVisitor.");
            }
            if (event == null)
                continue;
            if (replicator.verbose())
                LOGGER.info(event.toString());
            this.replicator.submitEvent(event);
        }
        return in.total();
    }
}

