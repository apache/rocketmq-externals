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

package org.apache.rocketmq.redis.replicator.rdb.skip;

import java.io.IOException;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.BaseRdbParser;

import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT16;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT32;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT8;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_LZF;

public class SkipRdbParser {

    protected final RedisInputStream in;

    public SkipRdbParser(RedisInputStream in) {
        this.in = in;
    }

    public void rdbLoadTime() throws IOException {
        in.skip(4);
    }

    public void rdbLoadMillisecondTime() throws IOException {
        in.skip(8);
    }

    public BaseRdbParser.Len rdbLoadLen() throws IOException {
        return new BaseRdbParser(in).rdbLoadLen();
    }

    public void rdbLoadIntegerObject(int enctype) throws IOException {
        switch (enctype) {
            case RDB_ENC_INT8:
                in.skip(1);
                break;
            case RDB_ENC_INT16:
                in.skip(2);
                break;
            case RDB_ENC_INT32:
                in.skip(4);
                break;
            default:
                break;
        }
    }

    public void rdbLoadLzfStringObject() throws IOException {
        long clen = rdbLoadLen().len;
        rdbLoadLen();
        in.skip(clen);
    }

    public void rdbGenericLoadStringObject() throws IOException {
        BaseRdbParser.Len lenObj = rdbLoadLen();
        long len = (int) lenObj.len;
        boolean isencoded = lenObj.isencoded;
        if (isencoded) {
            switch ((int) len) {
                case RDB_ENC_INT8:
                case RDB_ENC_INT16:
                case RDB_ENC_INT32:
                    rdbLoadIntegerObject((int) len);
                    return;
                case RDB_ENC_LZF:
                    rdbLoadLzfStringObject();
                    return;
                default:
                    throw new AssertionError("unknown RdbParser encoding type:" + len);
            }
        }
        in.skip(len);
    }

    public void rdbLoadPlainStringObject() throws IOException {
        rdbGenericLoadStringObject();
    }

    public void rdbLoadEncodedStringObject() throws IOException {
        rdbGenericLoadStringObject();
    }

    public void rdbLoadDoubleValue() throws IOException {
        int len = in.read();
        switch (len) {
            case 255:
            case 254:
            case 253:
                return;
            default:
                in.skip(len);
        }
    }

    public void rdbLoadBinaryDoubleValue() throws IOException {
        in.skip(8);
    }
}
