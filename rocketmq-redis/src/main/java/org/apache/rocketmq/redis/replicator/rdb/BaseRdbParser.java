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
import org.apache.rocketmq.redis.replicator.util.ByteArray;
import org.apache.rocketmq.redis.replicator.util.Lzf;

import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_14BITLEN;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_32BITLEN;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_64BITLEN;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_6BITLEN;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENCVAL;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT16;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT32;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_INT8;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_ENC_LZF;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_LOAD_ENC;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_LOAD_PLAIN;
import static org.apache.rocketmq.redis.replicator.RedisConstants.ZIP_INT_16B;
import static org.apache.rocketmq.redis.replicator.RedisConstants.ZIP_INT_24B;
import static org.apache.rocketmq.redis.replicator.RedisConstants.ZIP_INT_32B;
import static org.apache.rocketmq.redis.replicator.RedisConstants.ZIP_INT_64B;
import static org.apache.rocketmq.redis.replicator.RedisConstants.ZIP_INT_8B;

public class BaseRdbParser {
    protected final RedisInputStream in;

    public BaseRdbParser(RedisInputStream in) {
        this.in = in;
    }

    /**
     * "expiry time in seconds". After that, expiry time is read as a 4 byte unsigned int
     * <p>
     *
     * @return seconds
     * @throws IOException when read timeout
     */
    public int rdbLoadTime() throws IOException {
        return in.readInt(4);
    }

    /**
     * "expiry time in ms". After that, expiry time is read as a 8 byte unsigned long
     * <p>
     *
     * @return millisecond
     * @throws IOException when read timeout
     */
    public long rdbLoadMillisecondTime() throws IOException {
        return in.readLong(8);
    }

    /**
     * read bytes 1 or 2 or 5 <p> 1. |00xxxxxx| remaining 6 bits represent the length <p> 2. |01xxxxxx|xxxxxxxx| the
     * combined 14 bits represent the length <p> 3. |10xxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| the remaining 6 bits
     * are discarded.Additional 4 bytes represent the length(big endian in version6) <p> 4. |11xxxxxx| the remaining 6
     * bits are read.and then the next object is encoded in a special format.so we set isencoded = true <p>
     *
     * @return tuple(len, isencoded)
     * @throws IOException when read timeout
     * @see #rdbLoadIntegerObject
     * @see #rdbLoadLzfStringObject
     */
    public Len rdbLoadLen() throws IOException {
        boolean isencoded = false;
        int rawByte = in.read();
        int type = (rawByte & 0xc0) >> 6;
        long value;
        if (type == RDB_ENCVAL) {
            isencoded = true;
            value = rawByte & 0x3f;
        } else if (type == RDB_6BITLEN) {
            value = rawByte & 0x3f;
        } else if (type == RDB_14BITLEN) {
            value = ((rawByte & 0x3F) << 8) | in.read();
        } else if (rawByte == RDB_32BITLEN) {
            value = in.readInt(4, false);
        } else if (rawByte == RDB_64BITLEN) {
            value = in.readLong(8, false);
        } else {
            throw new AssertionError("unexpected len-type:" + type);
        }
        return new Len(value, isencoded);
    }

    /**
     * @param enctype 0,1,2
     * @param flags RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return ByteArray rdb byte array object
     * @throws IOException when read timeout
     */
    public ByteArray rdbLoadIntegerObject(int enctype, int flags) throws IOException {
        boolean plain = (flags & RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & RDB_LOAD_ENC) != 0;
        byte[] value;
        switch (enctype) {
            case RDB_ENC_INT8:
                value = in.readBytes(1).first();
                break;
            case RDB_ENC_INT16:
                value = in.readBytes(2).first();
                break;
            case RDB_ENC_INT32:
                value = in.readBytes(4).first();
                break;
            default:
                value = new byte[] {0x00};
                break;
        }
        if (plain) {
            return new ByteArray(value);
        } else if (encode) {
            // createStringObjectFromLongLong(val);
            return new ByteArray(String.valueOf(in.readInt(value)).getBytes());
        } else {
            // createObject(OBJ_STRING,sdsfromlonglong(val));
            return new ByteArray(value);
        }
    }

    /**
     * |11xxxxxx| remaining 6bit is 3,then lzf compressed string follows
     * <p>
     * lzf format
     * <p>
     * |lzf flag|clen:1 or 2 or 5 bytes|len:1 or 2 or 5 bytes |       lzf compressed bytes           |
     * <p>
     * |11xxxxxx|xxxxxxxx|....|xxxxxxxx|xxxxxxxx|....|xxxxxxxx|xxxxxxxx|xxxxxxxx|............xxxxxxxx|
     * <p>
     *
     * @param flags RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return ByteArray rdb byte array object
     * @throws IOException when read timeout
     * @see #rdbLoadLen
     */
    public ByteArray rdbLoadLzfStringObject(int flags) throws IOException {
        boolean plain = (flags & RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & RDB_LOAD_ENC) != 0;
        long clen = rdbLoadLen().len;
        long len = rdbLoadLen().len;
        // if (plain || sds) {
        //     return val;
        // } else {
        //     return createObject(OBJ_STRING,val);
        // }
        if (plain) {
            return Lzf.decode(in.readBytes(clen), len);
        } else if (encode) {
            return Lzf.decode(in.readBytes(clen), len);
        } else {
            return Lzf.decode(in.readBytes(clen), len);
        }
    }

    /**
     * 1.|11xxxxxx|xxxxxxxx| remaining 6bit is 0, then an 8 bit integer follows
     * <p>
     * 2.|11xxxxxx|xxxxxxxx|xxxxxxxx| remaining 6bit is 1, then an 16 bit integer follows
     * <p>
     * 3.|11xxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| remaining 6bit is 2, then an 32 bit integer follows
     * <p>
     * 4.|11xxxxxx| remaining 6bit is 3,then lzf compressed string follows
     * <p>
     *
     * @param flags RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return ByteArray rdb byte array object
     * @throws IOException when read timeout
     * @see #rdbLoadIntegerObject
     * @see #rdbLoadLzfStringObject
     */
    public ByteArray rdbGenericLoadStringObject(int flags) throws IOException {
        boolean plain = (flags & RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & RDB_LOAD_ENC) != 0;
        Len lenObj = rdbLoadLen();
        long len = (int) lenObj.len;
        boolean isencoded = lenObj.isencoded;
        if (isencoded) {
            switch ((int) len) {
                case RDB_ENC_INT8:
                case RDB_ENC_INT16:
                case RDB_ENC_INT32:
                    return rdbLoadIntegerObject((int) len, flags);
                case RDB_ENC_LZF:
                    return rdbLoadLzfStringObject(flags);
                default:
                    throw new AssertionError("unknown RdbParser encoding type:" + len);
            }
        }
        if (plain) {
            return in.readBytes(len);
        } else if (encode) {
            // createStringObject(NULL,len)
            return in.readBytes(len);
        } else {
            // createRawStringObject(NULL,len);
            return in.readBytes(len);
        }
    }

    /**
     * @return ByteArray rdb object with byte[]
     * @throws IOException when read timeout
     */
    public ByteArray rdbLoadPlainStringObject() throws IOException {
        return rdbGenericLoadStringObject(RDB_LOAD_PLAIN);
    }

    /**
     * @return ByteArray rdb object with byte[]
     * @throws IOException when read timeout
     */
    public ByteArray rdbLoadEncodedStringObject() throws IOException {
        return rdbGenericLoadStringObject(RDB_LOAD_ENC);
    }

    public double rdbLoadDoubleValue() throws IOException {
        int len = in.read();
        switch (len) {
            case 255:
                return Double.NEGATIVE_INFINITY;
            case 254:
                return Double.POSITIVE_INFINITY;
            case 253:
                return Double.NaN;
            default:
                byte[] bytes = in.readBytes(len).first();
                return Double.valueOf(new String(bytes));
        }
    }

    public double rdbLoadBinaryDoubleValue() throws IOException {
        return Double.longBitsToDouble(in.readLong(8));
    }

    /**
     * @return single precision float
     * @throws IOException io exception
     */
    public float rdbLoadBinaryFloatValue() throws IOException {
        return Float.intBitsToFloat(in.readInt(4));
    }

    /**
     * @see #rdbLoadLen
     */
    public static class Len {
        public final long len;
        public final boolean isencoded;

        private Len(long len, boolean isencoded) {
            this.len = len;
            this.isencoded = isencoded;
        }
    }

    public static class StringHelper {
        private StringHelper() {
        }

        public static String str(RedisInputStream in, int len) throws IOException {
            return in.readString(len);
        }

        public static byte[] bytes(RedisInputStream in, int len) throws IOException {
            return in.readBytes(len).first();
        }

        public static long skip(RedisInputStream in, long len) throws IOException {
            return in.skip(len);
        }

        /*
         * length-prev-entry special-flag raw-bytes-of-entry
         * length-prev-entry format
         * |xxxxxxxx| if first byte value &lt 254. then 1 byte as prev len.
         * |xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| if first byte &gt=254 then next 4 byte as prev len.
         * special-flag
         * |00xxxxxx| remaining 6 bit as string len.
         * |01xxxxxx|xxxxxxxx| combined 14 bit as string len.
         * |10xxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| next 4 byte as string len.
         * |11111110|xxxxxxxx| next 1 byte as 8bit int
         * |11000000|xxxxxxxx|xxxxxxxx| next 2 bytes as 16bit int
         * |11110000|xxxxxxxx|xxxxxxxx|xxxxxxxx| next 3 bytes as 24bit int
         * |11010000|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| next 4 bytes as 32bit int
         * |11100000|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| next 8 bytes as 64bit long
         * |11xxxxxx| next 6 bit value as int value
         */
        public static byte[] zipListEntry(RedisInputStream in) throws IOException {
            int prevlen = in.read();
            if (prevlen >= 254) {
                prevlen = in.readInt(4);
            }
            int special = in.read();
            switch (special >> 6) {
                case 0:
                    int len = special & 0x3f;
                    return bytes(in, len);
                case 1:
                    len = ((special & 0x3f) << 8) | in.read();
                    return bytes(in, len);
                case 2:
                    //bigEndian
                    len = in.readInt(4, false);
                    return bytes(in, len);
                default:
                    break;
            }
            switch (special) {
                case ZIP_INT_8B:
                    return String.valueOf(in.readInt(1)).getBytes();
                case ZIP_INT_16B:
                    return String.valueOf(in.readInt(2)).getBytes();
                case ZIP_INT_24B:
                    return String.valueOf(in.readInt(3)).getBytes();
                case ZIP_INT_32B:
                    return String.valueOf(in.readInt(4)).getBytes();
                case ZIP_INT_64B:
                    return String.valueOf(in.readLong(8)).getBytes();
                default:
                    //6BIT
                    return String.valueOf(special - 0xf1).getBytes();
            }
        }
    }

    public static class LenHelper {
        private LenHelper() {
        }

        //zip hash
        public static int zmlen(RedisInputStream in) throws IOException {
            return in.read();
        }

        public static int free(RedisInputStream in) throws IOException {
            return in.read();
        }

        public static int zmElementLen(RedisInputStream in) throws IOException {
            int len = in.read();
            if (len >= 0 && len <= 253) {
                return len;
            } else if (len == 254) {
                return in.readInt(4, false);
            } else {
                return len;
            }
        }

        //zip list
        public static int zlbytes(RedisInputStream in) throws IOException {
            return in.readInt(4);
        }

        public static int zlend(RedisInputStream in) throws IOException {
            return in.read();
        }

        public static int zltail(RedisInputStream in) throws IOException {
            return in.readInt(4);
        }

        public static int zllen(RedisInputStream in) throws IOException {
            return in.readInt(2);
        }

        //int set
        public static int encoding(RedisInputStream in) throws IOException {
            return in.readInt(4);
        }

        public static long lenOfContent(RedisInputStream in) throws IOException {
            return in.readUInt(4);
        }
    }
}
