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

package org.apache.rocketmq.replicator.redis.rdb;

import org.apache.rocketmq.replicator.redis.Constants;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.util.ByteArray;
import org.apache.rocketmq.replicator.redis.util.Lzf;

import java.io.IOException;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class BaseRdbParser {
    protected final RedisInputStream in;

    public BaseRdbParser(RedisInputStream in) {
        this.in = in;
    }

    /**
     * "expiry time in seconds". After that, expiry time is read as a 4 byte unsigned int
     *
     * @return seconds
     * @throws IOException when read timeout
     */
    public int rdbLoadTime() throws IOException {
        return in.readInt(4);
    }

    /**
     * "expiry time in ms". After that, expiry time is read as a 8 byte unsigned long
     *
     * @return millisecond
     * @throws IOException when read timeout
     */
    public long rdbLoadMillisecondTime() throws IOException {
        return in.readLong(8);
    }

    /**
     * read bytes 1 or 2 or 5
     * 1. |00xxxxxx| remaining 6 bits represent the length
     * 2. |01xxxxxx|xxxxxxxx| the combined 14 bits represent the length
     * 3. |10xxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| the remaining 6 bits are discarded.Additional 4 bytes represent the length(big endian in version6)
     * 4. |11xxxxxx| the remaining 6 bits are read.and then the next object is encoded in a special format.so we set isencoded = true
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
        if (type == Constants.RDB_ENCVAL) {
            isencoded = true;
            value = rawByte & 0x3f;
        } else if (type == Constants.RDB_6BITLEN) {
            value = rawByte & 0x3f;
        } else if (type == Constants.RDB_14BITLEN) {
            value = ((rawByte & 0x3F) << 8) | in.read();
        } else if (rawByte == Constants.RDB_32BITLEN) {
            value = in.readInt(4, false);
        } else if (rawByte == Constants.RDB_64BITLEN) {
            value = in.readLong(8, false);
        } else {
            throw new AssertionError("unexpected len-type:" + type);
        }
        return new Len(value, isencoded);
    }

    /**
     * @param enctype 0,1,2
     * @param flags  RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return String rdb object
     * @throws IOException when read timeout
     */
    public Object rdbLoadIntegerObject(int enctype, int flags) throws IOException {
        boolean plain = (flags & Constants.RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & Constants.RDB_LOAD_ENC) != 0;
        byte[] value;
        switch (enctype) {
            case Constants.RDB_ENC_INT8:
                value = in.readBytes(1).first();
                break;
            case Constants.RDB_ENC_INT16:
                value = in.readBytes(2).first();
                break;
            case Constants.RDB_ENC_INT32:
                value = in.readBytes(4).first();
                break;
            default:
                value = new byte[]{0x00};
                break;
        }
        if (plain) {
            return new ByteArray(value);
        } else if (encode) {
            return new EncodedString(String.valueOf(in.readInt(value)), new ByteArray(value));
        } else {
            return new ByteArray(value);
        }
    }

    /**
     * |11xxxxxx| remaining 6bit is 3,then lzf compressed string follows
     * lzf format
     * |lzf flag|clen:1 or 2 or 5 bytes|len:1 or 2 or 5 bytes |       lzf compressed bytes           |
     * |11xxxxxx|xxxxxxxx|....|xxxxxxxx|xxxxxxxx|....|xxxxxxxx|xxxxxxxx|xxxxxxxx|............xxxxxxxx|
     *
     * @param flags RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return String rdb object
     * @throws IOException when read timeout
     * @see #rdbLoadLen
     */
    public Object rdbLoadLzfStringObject(int flags) throws IOException {
        boolean plain = (flags & Constants.RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & Constants.RDB_LOAD_ENC) != 0;
        long clen = rdbLoadLen().len;
        long len = rdbLoadLen().len;
        if (plain) {
            return Lzf.decode(in.readBytes(clen), len);
        } else if (encode) {
            ByteArray bytes = Lzf.decode(in.readBytes(clen), len);
            return new EncodedString(new String(bytes.first(), Constants.CHARSET), bytes);
        } else {
            return Lzf.decode(in.readBytes(clen), len);
        }
    }

    /**
     * 1.|11xxxxxx|xxxxxxxx| remaining 6bit is 0, then an 8 bit integer follows
     * 2.|11xxxxxx|xxxxxxxx|xxxxxxxx| remaining 6bit is 1, then an 16 bit integer follows
     * 3.|11xxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx|xxxxxxxx| remaining 6bit is 2, then an 32 bit integer follows
     * 4.|11xxxxxx| remaining 6bit is 3,then lzf compressed string follows
     *
     * @param flags RDB_LOAD_ENC: encoded string.RDB_LOAD_PLAIN | RDB_LOAD_NONE:raw bytes
     * @return String rdb object
     * @throws IOException when read timeout
     * @see #rdbLoadIntegerObject
     * @see #rdbLoadLzfStringObject
     */
    public Object rdbGenericLoadStringObject(int flags) throws IOException {
        boolean plain = (flags & Constants.RDB_LOAD_PLAIN) != 0;
        boolean encode = (flags & Constants.RDB_LOAD_ENC) != 0;
        Len lenObj = rdbLoadLen();
        long len = (int) lenObj.len;
        boolean isencoded = lenObj.isencoded;
        if (isencoded) {
            switch ((int) len) {
                case Constants.RDB_ENC_INT8:
                case Constants.RDB_ENC_INT16:
                case Constants.RDB_ENC_INT32:
                    return rdbLoadIntegerObject((int) len, flags);
                case Constants.RDB_ENC_LZF:
                    return rdbLoadLzfStringObject(flags);
                default:
                    throw new AssertionError("unknown RdbParser encoding type:" + len);
            }
        }

        if (plain) {
            return in.readBytes(len);
        } else if (encode) {
            ByteArray bytes = in.readBytes(len);
            return new EncodedString(new String(bytes.first(), Constants.CHARSET), bytes);
        } else {
            return in.readBytes(len);
        }
    }

    /**
     * @return InputStream rdb object with raw bytes
     * @throws IOException when read timeout
     */
    public ByteArray rdbLoadPlainStringObject() throws IOException {
        return (ByteArray) rdbGenericLoadStringObject(Constants.RDB_LOAD_PLAIN);
    }

    /**
     * @return EncodedString rdb object with UTF-8 string
     * @throws IOException when read timeout
     */
    public EncodedString rdbLoadEncodedStringObject() throws IOException {
        return (EncodedString) rdbGenericLoadStringObject(Constants.RDB_LOAD_ENC);
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
        public static String zipListEntry(RedisInputStream in) throws IOException {
            int prevlen = in.read();
            if (prevlen >= 254) {
                prevlen = in.readInt(4);
            }
            int special = in.read();
            switch (special >> 6) {
                case 0:
                    int len = special & 0x3f;
                    return StringHelper.str(in, len);
                case 1:
                    len = ((special & 0x3f) << 8) | in.read();
                    return StringHelper.str(in, len);
                case 2:
                    //bigEndian
                    len = in.readInt(4, false);
                    return StringHelper.str(in, len);
                default:
                    break;
            }
            switch (special) {
                case Constants.ZIP_INT_8B:
                    return String.valueOf(in.readInt(1));
                case Constants.ZIP_INT_16B:
                    return String.valueOf(in.readInt(2));
                case Constants.ZIP_INT_24B:
                    return String.valueOf(in.readInt(3));
                case Constants.ZIP_INT_32B:
                    return String.valueOf(in.readInt(4));
                case Constants.ZIP_INT_64B:
                    return String.valueOf(in.readLong(8));
                default:
                    //6BIT
                    return String.valueOf(special - 0xf1);
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

    public static class EncodedString {
        public final String string;
        public final byte[] rawBytes;
        public final ByteArray bytes;

        public EncodedString(String string, ByteArray bytes) {
            this.bytes = bytes;
            this.string = string;
            this.rawBytes = bytes.first();
        }
    }
}
