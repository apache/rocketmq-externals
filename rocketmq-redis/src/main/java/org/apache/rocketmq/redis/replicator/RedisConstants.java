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

package org.apache.rocketmq.redis.replicator;

public class RedisConstants {

    private RedisConstants() {
    }

    public static final char[] MODULE_SET = new char[] {
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };

    /**
     * len type
     */
    public static final int RDB_ENCVAL = 3;
    public static final int RDB_6BITLEN = 0;
    public static final int RDB_14BITLEN = 1;
    public static final int RDB_32BITLEN = 0x80;
    public static final int RDB_64BITLEN = 0x81;

    /**
     * string type
     */
    public static final int RDB_LOAD_NONE = 0;
    public static final int RDB_LOAD_ENC = 1 << 0;
    public static final int RDB_LOAD_PLAIN = 1 << 1;

    /**
     * string encoding
     */
    public static final int RDB_ENC_INT8 = 0;
    public static final int RDB_ENC_INT16 = 1;
    public static final int RDB_ENC_INT32 = 2;
    public static final int RDB_ENC_LZF = 3;

    /**
     * rdb protocol
     */
    public static final int RDB_OPCODE_AUX = 0xfa; /*250*/
    public static final int RDB_OPCODE_RESIZEDB = 0xfb; /*251*/
    public static final int RDB_OPCODE_EXPIRETIME_MS = 0xfc;/* 252 */
    public static final int RDB_OPCODE_EXPIRETIME = 0xfd; /* 253 */
    public static final int RDB_OPCODE_SELECTDB = 0xfe; /* 254 */
    public static final int RDB_OPCODE_EOF = 0xff; /* 255 */

    /**
     * rdb object encoding
     */
    public static final int RDB_TYPE_STRING = 0;
    public static final int RDB_TYPE_LIST = 1;
    public static final int RDB_TYPE_SET = 2;
    public static final int RDB_TYPE_ZSET = 3;
    public static final int RDB_TYPE_HASH = 4;
    public static final int RDB_TYPE_ZSET_2 = 5;
    public static final int RDB_TYPE_MODULE = 6;
    public static final int RDB_TYPE_MODULE_2 = 7;
    public static final int RDB_TYPE_HASH_ZIPMAP = 9;
    public static final int RDB_TYPE_LIST_ZIPLIST = 10;
    public static final int RDB_TYPE_SET_INTSET = 11;
    public static final int RDB_TYPE_ZSET_ZIPLIST = 12;
    public static final int RDB_TYPE_HASH_ZIPLIST = 13;
    public static final int RDB_TYPE_LIST_QUICKLIST = 14;

    /**
     * Module serialized values sub opcodes
     */
    public static final int RDB_MODULE_OPCODE_EOF = 0; /* End of module value. */
    // this variable only used in rdb check tool. useless for now.
    public static final int RDB_MODULE_OPCODE_SINT = 1; /* Signed integer. */
    public static final int RDB_MODULE_OPCODE_UINT = 2; /* Unsigned integer. */
    public static final int RDB_MODULE_OPCODE_FLOAT = 3; /* Float. */
    public static final int RDB_MODULE_OPCODE_DOUBLE = 4; /* Double. */
    public static final int RDB_MODULE_OPCODE_STRING = 5; /* String. */

    /**
     * zip entry
     */
    public static final int ZIP_INT_8B = 0xfe; /*11111110*/
    public static final int ZIP_INT_16B = 0xc0 | 0 << 4; /* 11000000*/
    public static final int ZIP_INT_24B = 0xc0 | 3 << 4; /* 11110000*/
    public static final int ZIP_INT_32B = 0xc0 | 1 << 4; /* 11010000*/
    public static final int ZIP_INT_64B = 0xc0 | 2 << 4; /* 11100000*/

    /**
     * transfer protocol
     */
    public static final byte DOLLAR = '$';
    public static final byte STAR = '*';
    public static final byte PLUS = '+';
    public static final byte MINUS = '-';
    public static final byte COLON = ':';

}
