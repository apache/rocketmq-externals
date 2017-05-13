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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.rocketmq.replicator.redis.Constants;
import org.apache.rocketmq.replicator.redis.Replicator;
import org.apache.rocketmq.replicator.redis.event.Event;
import org.apache.rocketmq.replicator.redis.io.ByteArrayInputStream;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.apache.rocketmq.replicator.redis.rdb.datatype.DB;
import org.apache.rocketmq.replicator.redis.rdb.datatype.ExpiredType;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueHash;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueList;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueModule;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueSet;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueString;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyStringValueZSet;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.replicator.redis.rdb.datatype.Module;
import org.apache.rocketmq.replicator.redis.rdb.datatype.ZSetEntry;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleParser;
import org.apache.rocketmq.replicator.redis.util.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class DefaultRdbVisitor extends RdbVisitor {

    protected static final Logger logger = LoggerFactory.getLogger(DefaultRdbVisitor.class);

    protected final Replicator replicator;

    public DefaultRdbVisitor(final Replicator replicator) {
        this.replicator = replicator;
    }

    @Override
    public String applyMagic(RedisInputStream in) throws IOException {
        String magicString = BaseRdbParser.StringHelper.str(in, 5);//REDIS
        if (!magicString.equals("REDIS")) {
            throw new UnsupportedOperationException("can't read MAGIC STRING [REDIS] ,value:" + magicString);
        }
        return magicString;
    }

    @Override
    public int applyVersion(RedisInputStream in) throws IOException {
        int version = Integer.parseInt(BaseRdbParser.StringHelper.str(in, 4));
        if (version < 2 || version > 8) {
            throw new UnsupportedOperationException(String.valueOf("can't handle RDB format version " + version));
        }
        return version;
    }

    @Override
    public int applyType(RedisInputStream in) throws IOException {
        return in.read();
    }

    @Override
    public DB applySelectDB(RedisInputStream in, int version) throws IOException {
        /*
         * ----------------------------
         * FE $length-encoding         # Previous db ends, next db starts. Database number read using length encoding.
         * ----------------------------
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        long dbNumber = parser.rdbLoadLen().len;
        return new DB(dbNumber);
    }

    @Override
    public DB applyResizeDB(RedisInputStream in, DB db, int version) throws IOException {
        BaseRdbParser parser = new BaseRdbParser(in);
        long dbsize = parser.rdbLoadLen().len;
        long expiresSize = parser.rdbLoadLen().len;
        if (db != null) db.setDbsize(dbsize);
        if (db != null) db.setExpires(expiresSize);
        return db;
    }

    @Override
    public long applyEof(RedisInputStream in, int version) throws IOException {
        /*
         * ----------------------------
         * ...                         # Key value pairs for this database, additonal database
         * FF                          ## End of RDB file indicator
         * 8 byte checksum             ## CRC 64 checksum of the entire file.
         * ----------------------------
         */
        if (version >= 5) return in.readLong(8);
        return 0L;
    }

    @Override
    public Event applyExpireTime(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * ----------------------------
         * FD $unsigned int            # FD indicates "expiry time in seconds". After that, expiry time is read as a 4 byte unsigned int
         * $value-type                 # 1 byte flag indicating the type of value - set, map, sorted set etc.
         * $string-encoded-name         # The name, encoded as a redis string
         * $encoded-value              # The value. Encoding depends on $value-type
         * ----------------------------
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        int expiredSec = parser.rdbLoadTime();
        int valueType = applyType(in);
        KeyValuePair kv = rdbLoadObject(in, db, valueType, version);
        kv.setExpiredType(ExpiredType.SECOND);
        kv.setExpiredValue((long) expiredSec);
        return kv;
    }

    @Override
    public Event applyExpireTimeMs(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * ----------------------------
         * FC $unsigned long           # FC indicates "expiry time in ms". After that, expiry time is read as a 8 byte unsigned long
         * $value-type                 # 1 byte flag indicating the type of value - set, map, sorted set etc.
         * $string-encoded-name         # The name, encoded as a redis string
         * $encoded-value              # The value. Encoding depends on $value-type
         * ----------------------------
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        long expiredMs = parser.rdbLoadMillisecondTime();
        int valueType = applyType(in);
        KeyValuePair kv = rdbLoadObject(in, db, valueType, version);
        kv.setExpiredType(ExpiredType.MS);
        kv.setExpiredValue(expiredMs);
        return kv;
    }

    @Override
    public Event applyAux(RedisInputStream in, int version) throws IOException {
        BaseRdbParser parser = new BaseRdbParser(in);
        String auxKey = parser.rdbLoadEncodedStringObject().string;
        String auxValue = parser.rdbLoadEncodedStringObject().string;
        if (!auxKey.startsWith("%")) {
            logger.info("RDB " + auxKey + ": " + auxValue);
            return new AuxField(auxKey, auxValue);
        } else {
            logger.warn("unrecognized RDB AUX field: " + auxKey + ",value: " + auxValue);
            return null;
        }
    }

    @Override
    public Event applyString(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |       <content>       |
         * |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueString o0 = new KeyStringValueString();
        String key = parser.rdbLoadEncodedStringObject().string;
        BaseRdbParser.EncodedString val = parser.rdbLoadEncodedStringObject();
        o0.setValueRdbType(Constants.RDB_TYPE_STRING);
        o0.setValue(val.string);
        o0.setRawBytes(val.rawBytes);
        o0.setDb(db);
        o0.setKey(key);
        return o0;
    }

    @Override
    public Event applyList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueList o1 = new KeyStringValueList();
        String key = parser.rdbLoadEncodedStringObject().string;
        long len = parser.rdbLoadLen().len;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            String element = parser.rdbLoadEncodedStringObject().string;
            list.add(element);
        }
        o1.setValueRdbType(Constants.RDB_TYPE_LIST);
        o1.setValue(list);
        o1.setDb(db);
        o1.setKey(key);
        return o1;
    }

    @Override
    public Event applySet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueSet o2 = new KeyStringValueSet();
        String key = parser.rdbLoadEncodedStringObject().string;
        long len = parser.rdbLoadLen().len;
        Set<String> set = new LinkedHashSet<>();
        for (int i = 0; i < len; i++) {
            String element = parser.rdbLoadEncodedStringObject().string;
            set.add(element);
        }
        o2.setValueRdbType(Constants.RDB_TYPE_SET);
        o2.setValue(set);
        o2.setDb(db);
        o2.setKey(key);
        return o2;
    }

    @Override
    public Event applyZSet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |        <score>       |
         * | 1 or 5 bytes |    string contents    |    double content    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSet o3 = new KeyStringValueZSet();
        String key = parser.rdbLoadEncodedStringObject().string;
        long len = parser.rdbLoadLen().len;
        Set<ZSetEntry> zset = new LinkedHashSet<>();
        while (len > 0) {
            String element = parser.rdbLoadEncodedStringObject().string;
            double score = parser.rdbLoadDoubleValue();
            zset.add(new ZSetEntry(element, score));
            len--;
        }
        o3.setValueRdbType(Constants.RDB_TYPE_ZSET);
        o3.setValue(zset);
        o3.setDb(db);
        o3.setKey(key);
        return o3;
    }

    @Override
    public Event applyZSet2(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |        <score>       |
         * | 1 or 5 bytes |    string contents    |    binary double     |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSet o5 = new KeyStringValueZSet();
        String key = parser.rdbLoadEncodedStringObject().string;
        /* rdb version 8*/
        long len = parser.rdbLoadLen().len;
        Set<ZSetEntry> zset = new LinkedHashSet<>();
        while (len > 0) {
            String element = parser.rdbLoadEncodedStringObject().string;
            double score = parser.rdbLoadBinaryDoubleValue();
            zset.add(new ZSetEntry(element, score));
            len--;
        }
        o5.setValueRdbType(Constants.RDB_TYPE_ZSET_2);
        o5.setValue(zset);
        o5.setDb(db);
        o5.setKey(key);
        return o5;
    }

    @Override
    public Event applyHash(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueHash o4 = new KeyStringValueHash();
        String key = parser.rdbLoadEncodedStringObject().string;
        long len = parser.rdbLoadLen().len;
        Map<String, String> map = new LinkedHashMap<>();
        while (len > 0) {
            String field = parser.rdbLoadEncodedStringObject().string;
            String value = parser.rdbLoadEncodedStringObject().string;
            map.put(field, value);
            len--;
        }
        o4.setValueRdbType(Constants.RDB_TYPE_HASH);
        o4.setValue(map);
        o4.setDb(db);
        o4.setKey(key);
        return o4;
    }

    @Override
    public Event applyHashZipMap(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zmlen> |   <len>     |"foo"    |    <len>   | <free> |   "bar" |<zmend> |
         * | 1 byte | 1 or 5 byte | content |1 or 5 byte | 1 byte | content | 1 byte |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueHash o9 = new KeyStringValueHash();
        String key = parser.rdbLoadEncodedStringObject().string;
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));
        Map<String, String> map = new LinkedHashMap<>();
        int zmlen = BaseRdbParser.LenHelper.zmlen(stream);
        while (true) {
            int zmEleLen = BaseRdbParser.LenHelper.zmElementLen(stream);
            if (zmEleLen == 255) {
                o9.setValueRdbType(Constants.RDB_TYPE_HASH_ZIPMAP);
                o9.setValue(map);
                o9.setDb(db);
                o9.setKey(key);
                return o9;
            }
            String field = BaseRdbParser.StringHelper.str(stream, zmEleLen);
            zmEleLen = BaseRdbParser.LenHelper.zmElementLen(stream);
            if (zmEleLen == 255) {
                o9.setValueRdbType(Constants.RDB_TYPE_HASH_ZIPMAP);
                o9.setValue(map);
                o9.setDb(db);
                o9.setKey(key);
                return o9;
            }
            int free = BaseRdbParser.LenHelper.free(stream);
            String value = BaseRdbParser.StringHelper.str(stream, zmEleLen);
            BaseRdbParser.StringHelper.skip(stream, free);
            map.put(field, value);
        }
    }

    @Override
    public Event applyListZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueList o10 = new KeyStringValueList();
        String key = parser.rdbLoadEncodedStringObject().string;
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        List<String> list = new ArrayList<>();
        int zlbytes = BaseRdbParser.LenHelper.zlbytes(stream);
        int zltail = BaseRdbParser.LenHelper.zltail(stream);
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        for (int i = 0; i < zllen; i++) {
            list.add(BaseRdbParser.StringHelper.zipListEntry(stream));
        }
        int zlend = BaseRdbParser.LenHelper.zlend(stream);
        if (zlend != 255) {
            throw new AssertionError("zlend expect 255 but " + zlend);
        }
        o10.setValueRdbType(Constants.RDB_TYPE_LIST_ZIPLIST);
        o10.setValue(list);
        o10.setDb(db);
        o10.setKey(key);
        return o10;
    }

    @Override
    public Event applySetIntSet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<encoding>| <length-of-contents>|              <contents>                           |
         * | 4 bytes  |            4 bytes  | 2 bytes lement| 4 bytes element | 8 bytes element |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueSet o11 = new KeyStringValueSet();
        String key = parser.rdbLoadEncodedStringObject().string;
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        Set<String> set = new LinkedHashSet<>();
        int encoding = BaseRdbParser.LenHelper.encoding(stream);
        long lenOfContent = BaseRdbParser.LenHelper.lenOfContent(stream);
        for (long i = 0; i < lenOfContent; i++) {
            switch (encoding) {
                case 2:
                    set.add(String.valueOf(stream.readInt(2)));
                    break;
                case 4:
                    set.add(String.valueOf(stream.readInt(4)));
                    break;
                case 8:
                    set.add(String.valueOf(stream.readLong(8)));
                    break;
                default:
                    throw new AssertionError("expect encoding [2,4,8] but:" + encoding);
            }
        }
        o11.setValueRdbType(Constants.RDB_TYPE_SET_INTSET);
        o11.setValue(set);
        o11.setDb(db);
        o11.setKey(key);
        return o11;
    }

    @Override
    public Event applyZSetZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSet o12 = new KeyStringValueZSet();
        String key = parser.rdbLoadEncodedStringObject().string;
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        Set<ZSetEntry> zset = new LinkedHashSet<>();
        int zlbytes = BaseRdbParser.LenHelper.zlbytes(stream);
        int zltail = BaseRdbParser.LenHelper.zltail(stream);
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        while (zllen > 0) {
            String element = BaseRdbParser.StringHelper.zipListEntry(stream);
            zllen--;
            double score = Double.valueOf(BaseRdbParser.StringHelper.zipListEntry(stream));
            zllen--;
            zset.add(new ZSetEntry(element, score));
        }
        int zlend = BaseRdbParser.LenHelper.zlend(stream);
        if (zlend != 255) {
            throw new AssertionError("zlend expect 255 but " + zlend);
        }
        o12.setValueRdbType(Constants.RDB_TYPE_ZSET_ZIPLIST);
        o12.setValue(zset);
        o12.setDb(db);
        o12.setKey(key);
        return o12;
    }

    @Override
    public Event applyHashZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueHash o13 = new KeyStringValueHash();
        String key = parser.rdbLoadEncodedStringObject().string;
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        Map<String, String> map = new LinkedHashMap<>();
        int zlbytes = BaseRdbParser.LenHelper.zlbytes(stream);
        int zltail = BaseRdbParser.LenHelper.zltail(stream);
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        while (zllen > 0) {
            String field = BaseRdbParser.StringHelper.zipListEntry(stream);
            zllen--;
            String value = BaseRdbParser.StringHelper.zipListEntry(stream);
            zllen--;
            map.put(field, value);
        }
        int zlend = BaseRdbParser.LenHelper.zlend(stream);
        if (zlend != 255) {
            throw new AssertionError("zlend expect 255 but " + zlend);
        }
        o13.setValueRdbType(Constants.RDB_TYPE_HASH_ZIPLIST);
        o13.setValue(map);
        o13.setDb(db);
        o13.setKey(key);
        return o13;
    }

    @Override
    public Event applyListQuickList(RedisInputStream in, DB db, int version) throws IOException {
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueList o14 = new KeyStringValueList();
        String key = parser.rdbLoadEncodedStringObject().string;
        long len = parser.rdbLoadLen().len;
        List<String> byteList = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            ByteArray element = (ByteArray) parser.rdbGenericLoadStringObject(Constants.RDB_LOAD_NONE);
            RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(element));

            List<String> list = new ArrayList<>();
            int zlbytes = BaseRdbParser.LenHelper.zlbytes(stream);
            int zltail = BaseRdbParser.LenHelper.zltail(stream);
            int zllen = BaseRdbParser.LenHelper.zllen(stream);
            for (int j = 0; j < zllen; j++) {
                list.add(BaseRdbParser.StringHelper.zipListEntry(stream));
            }
            int zlend = BaseRdbParser.LenHelper.zlend(stream);
            if (zlend != 255) {
                throw new AssertionError("zlend expect 255 but " + zlend);
            }
            byteList.addAll(list);
        }
        o14.setValueRdbType(Constants.RDB_TYPE_LIST_QUICKLIST);
        o14.setValue(byteList);
        o14.setDb(db);
        o14.setKey(key);
        return o14;
    }

    @Override
    public Event applyModule(RedisInputStream in, DB db, int version) throws IOException {
        //|6|6|6|6|6|6|6|6|6|10|
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueModule o6 = new KeyStringValueModule();
        String key = parser.rdbLoadEncodedStringObject().string;
        char[] c = new char[9];
        long moduleid = parser.rdbLoadLen().len;
        for (int i = 0; i < c.length; i++) {
            c[i] = Constants.MODULE_SET[(int) (moduleid >>> (10 + (c.length - 1 - i) * 6) & 63)];
        }
        String moduleName = new String(c);
        int moduleVersion = (int) (moduleid & 1023);
        ModuleParser<? extends Module> moduleParser = lookupModuleParser(moduleName, moduleVersion);
        if (moduleParser == null)
            throw new NoSuchElementException("module[" + moduleName + "," + moduleVersion + "] not exist.");
        o6.setValueRdbType(Constants.RDB_TYPE_MODULE);
        o6.setValue(moduleParser.parse(in));
        o6.setDb(db);
        o6.setKey(key);
        return o6;
    }

    protected ModuleParser<? extends Module> lookupModuleParser(String moduleName, int moduleVersion) {
        return replicator.getModuleParser(moduleName, moduleVersion);
    }

    protected KeyValuePair rdbLoadObject(RedisInputStream in, DB db, int valueType, int version) throws IOException {
        /*
         * ----------------------------
         * $value-type                 # This name value pair doesn't have an expiry. $value_type guaranteed != to FD, FC, FE and FF
         * $string-encoded-name
         * $encoded-value
         * ----------------------------
         */
        switch (valueType) {
            case Constants.RDB_TYPE_STRING:
                return (KeyValuePair) applyString(in, db, version);
            case Constants.RDB_TYPE_LIST:
                return (KeyValuePair) applyList(in, db, version);
            case Constants.RDB_TYPE_SET:
                return (KeyValuePair) applySet(in, db, version);
            case Constants.RDB_TYPE_ZSET:
                return (KeyValuePair) applyZSet(in, db, version);
            case Constants.RDB_TYPE_ZSET_2:
                return (KeyValuePair) applyZSet2(in, db, version);
            case Constants.RDB_TYPE_HASH:
                return (KeyValuePair) applyHash(in, db, version);
            case Constants.RDB_TYPE_HASH_ZIPMAP:
                return (KeyValuePair) applyHashZipMap(in, db, version);
            case Constants.RDB_TYPE_LIST_ZIPLIST:
                return (KeyValuePair) applyListZipList(in, db, version);
            case Constants.RDB_TYPE_SET_INTSET:
                return (KeyValuePair) applySetIntSet(in, db, version);
            case Constants.RDB_TYPE_ZSET_ZIPLIST:
                return (KeyValuePair) applyZSetZipList(in, db, version);
            case Constants.RDB_TYPE_HASH_ZIPLIST:
                return (KeyValuePair) applyHashZipList(in, db, version);
            case Constants.RDB_TYPE_LIST_QUICKLIST:
                return (KeyValuePair) applyListQuickList(in, db, version);
            case Constants.RDB_TYPE_MODULE:
                return (KeyValuePair) applyModule(in, db, version);
            default:
                throw new AssertionError("unexpected value-type:" + valueType);
        }
    }
}
