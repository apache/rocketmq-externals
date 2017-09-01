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

package org.apache.rocketmq.redis.replicator.rdb.iterable;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.UncheckedIOException;
import org.apache.rocketmq.redis.replicator.event.Event;
import org.apache.rocketmq.redis.replicator.io.ByteArrayInputStream;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.BaseRdbParser;
import org.apache.rocketmq.redis.replicator.rdb.datatype.DB;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueByteArrayIterator;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueZSetEntryIterator;
import org.apache.rocketmq.redis.replicator.util.ByteArray;
import org.apache.rocketmq.redis.replicator.rdb.DefaultRdbVisitor;
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueMapEntryIterator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_LOAD_NONE;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH_ZIPLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_HASH_ZIPMAP;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST_QUICKLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_LIST_ZIPLIST;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET_INTSET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET_2;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_ZSET_ZIPLIST;

public class ValueIterableRdbVisitor extends DefaultRdbVisitor {

    public ValueIterableRdbVisitor(Replicator replicator) {
        super(replicator);
    }

    @Override
    public Event applyList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueByteArrayIterator o1 = new KeyStringValueByteArrayIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        long len = parser.rdbLoadLen().len;
        o1.setValue(new Iter<byte[]>(len, parser) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public byte[] next() {
                try {
                    byte[] element = parser.rdbLoadEncodedStringObject().first();
                    condition--;
                    return element;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o1.setValueRdbType(RDB_TYPE_LIST);
        o1.setDb(db);
        o1.setKey(new String(key, UTF_8));
        o1.setRawKey(key);
        return o1;
    }

    @Override
    public Event applySet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueByteArrayIterator o2 = new KeyStringValueByteArrayIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        long len = parser.rdbLoadLen().len;
        o2.setValue(new Iter<byte[]>(len, parser) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public byte[] next() {
                try {
                    byte[] element = parser.rdbLoadEncodedStringObject().first();
                    condition--;
                    return element;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o2.setValueRdbType(RDB_TYPE_SET);
        o2.setDb(db);
        o2.setKey(new String(key, UTF_8));
        o2.setRawKey(key);
        return o2;
    }

    @Override
    public Event applyZSet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |        <score>       |
         * | 1 or 5 bytes |    string contents    |    double content    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSetEntryIterator o3 = new KeyStringValueZSetEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        long len = parser.rdbLoadLen().len;
        o3.setValue(new Iter<ZSetEntry>(len, parser) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public ZSetEntry next() {
                try {
                    byte[] element = parser.rdbLoadEncodedStringObject().first();
                    double score = parser.rdbLoadDoubleValue();
                    condition--;
                    return new ZSetEntry(new String(element, UTF_8), score, element);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o3.setValueRdbType(RDB_TYPE_ZSET);
        o3.setDb(db);
        o3.setKey(new String(key, UTF_8));
        o3.setRawKey(key);
        return o3;
    }

    @Override
    public Event applyZSet2(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |        <score>       |
         * | 1 or 5 bytes |    string contents    |    binary double     |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSetEntryIterator o5 = new KeyStringValueZSetEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        /* rdb version 8*/
        long len = parser.rdbLoadLen().len;
        o5.setValue(new Iter<ZSetEntry>(len, parser) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public ZSetEntry next() {
                try {
                    byte[] element = parser.rdbLoadEncodedStringObject().first();
                    double score = parser.rdbLoadBinaryDoubleValue();
                    condition--;
                    return new ZSetEntry(new String(element, UTF_8), score, element);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o5.setValueRdbType(RDB_TYPE_ZSET_2);
        o5.setDb(db);
        o5.setKey(new String(key, UTF_8));
        o5.setRawKey(key);
        return o5;
    }

    @Override
    public Event applyHash(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |    <len>     |       <content>       |
         * | 1 or 5 bytes |    string contents    |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueMapEntryIterator o4 = new KeyStringValueMapEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        long len = parser.rdbLoadLen().len;
        o4.setValue(new Iter<Map.Entry<byte[], byte[]>>(len, parser) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public Map.Entry<byte[], byte[]> next() {
                try {
                    byte[] field = parser.rdbLoadEncodedStringObject().first();
                    byte[] value = parser.rdbLoadEncodedStringObject().first();
                    condition--;
                    return new AbstractMap.SimpleEntry<>(field, value);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o4.setValueRdbType(RDB_TYPE_HASH);
        o4.setDb(db);
        o4.setKey(new String(key, UTF_8));
        o4.setRawKey(key);
        return o4;
    }

    @Override
    public Event applyHashZipMap(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zmlen> |   <len>     |"foo"    |    <len>   | <free> |   "bar" |<zmend> |
         * | 1 byte | 1 or 5 byte | content |1 or 5 byte | 1 byte | content | 1 byte |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueMapEntryIterator o9 = new KeyStringValueMapEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        ByteArray aux = parser.rdbLoadPlainStringObject();
        RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));
        BaseRdbParser.LenHelper.zmlen(stream); // zmlen
        o9.setValue(new HashZipMapIter(stream));
        o9.setValueRdbType(RDB_TYPE_HASH_ZIPMAP);
        o9.setDb(db);
        o9.setKey(new String(key, UTF_8));
        o9.setRawKey(key);
        return o9;
    }

    @Override
    public Event applyListZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueByteArrayIterator o10 = new KeyStringValueByteArrayIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        ByteArray aux = parser.rdbLoadPlainStringObject();
        final RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        BaseRdbParser.LenHelper.zlbytes(stream); // zlbytes
        BaseRdbParser.LenHelper.zltail(stream); // zltail
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        o10.setValue(new Iter<byte[]>(zllen, null) {
            @Override
            public boolean hasNext() {
                if (condition > 0)
                    return true;
                try {
                    int zlend = BaseRdbParser.LenHelper.zlend(stream);
                    if (zlend != 255) {
                        throw new AssertionError("zlend expect 255 but " + zlend);
                    }
                    return false;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public byte[] next() {
                try {
                    byte[] e = BaseRdbParser.StringHelper.zipListEntry(stream);
                    condition--;
                    return e;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o10.setValueRdbType(RDB_TYPE_LIST_ZIPLIST);
        o10.setDb(db);
        o10.setKey(new String(key, UTF_8));
        o10.setRawKey(key);
        return o10;
    }

    @Override
    public Event applySetIntSet(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<encoding>| <length-of-contents>|              <contents>                           |
         * | 4 bytes  |            4 bytes  | 2 bytes lement| 4 bytes element | 8 bytes element |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueByteArrayIterator o11 = new KeyStringValueByteArrayIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        ByteArray aux = parser.rdbLoadPlainStringObject();
        final RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        final int encoding = BaseRdbParser.LenHelper.encoding(stream);
        long lenOfContent = BaseRdbParser.LenHelper.lenOfContent(stream);
        o11.setValue(new Iter<byte[]>(lenOfContent, null) {
            @Override
            public boolean hasNext() {
                return condition > 0;
            }

            @Override
            public byte[] next() {
                try {
                    switch (encoding) {
                        case 2:
                            String element = String.valueOf(stream.readInt(2));
                            condition--;
                            return element.getBytes();
                        case 4:
                            element = String.valueOf(stream.readInt(4));
                            condition--;
                            return element.getBytes();
                        case 8:
                            element = String.valueOf(stream.readLong(8));
                            condition--;
                            return element.getBytes();
                        default:
                            throw new AssertionError("expect encoding [2,4,8] but:" + encoding);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o11.setValueRdbType(RDB_TYPE_SET_INTSET);
        o11.setDb(db);
        o11.setKey(new String(key, UTF_8));
        o11.setRawKey(key);
        return o11;
    }

    @Override
    public Event applyZSetZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueZSetEntryIterator o12 = new KeyStringValueZSetEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        ByteArray aux = parser.rdbLoadPlainStringObject();
        final RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        BaseRdbParser.LenHelper.zlbytes(stream); // zlbytes
        BaseRdbParser.LenHelper.zltail(stream); // zltail
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        o12.setValue(new Iter<ZSetEntry>(zllen, null) {
            @Override
            public boolean hasNext() {
                if (condition > 0)
                    return true;
                try {
                    int zlend = BaseRdbParser.LenHelper.zlend(stream);
                    if (zlend != 255) {
                        throw new AssertionError("zlend expect 255 but " + zlend);
                    }
                    return false;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public ZSetEntry next() {
                try {
                    byte[] element = BaseRdbParser.StringHelper.zipListEntry(stream);
                    condition--;
                    double score = Double.valueOf(new String(BaseRdbParser.StringHelper.zipListEntry(stream), UTF_8));
                    condition--;
                    return new ZSetEntry(new String(element, UTF_8), score, element);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o12.setValueRdbType(RDB_TYPE_ZSET_ZIPLIST);
        o12.setDb(db);
        o12.setKey(new String(key, UTF_8));
        o12.setRawKey(key);
        return o12;
    }

    @Override
    public Event applyHashZipList(RedisInputStream in, DB db, int version) throws IOException {
        /*
         * |<zlbytes>| <zltail>| <zllen>| <entry> ...<entry> | <zlend>|
         * | 4 bytes | 4 bytes | 2bytes | zipListEntry ...   | 1byte  |
         */
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueMapEntryIterator o13 = new KeyStringValueMapEntryIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        ByteArray aux = parser.rdbLoadPlainStringObject();
        final RedisInputStream stream = new RedisInputStream(new ByteArrayInputStream(aux));

        BaseRdbParser.LenHelper.zlbytes(stream); // zlbytes
        BaseRdbParser.LenHelper.zltail(stream); // zltail
        int zllen = BaseRdbParser.LenHelper.zllen(stream);
        o13.setValue(new Iter<Map.Entry<byte[], byte[]>>(zllen, null) {
            @Override
            public boolean hasNext() {
                if (condition > 0)
                    return true;
                try {
                    int zlend = BaseRdbParser.LenHelper.zlend(stream);
                    if (zlend != 255) {
                        throw new AssertionError("zlend expect 255 but " + zlend);
                    }
                    return false;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public Map.Entry<byte[], byte[]> next() {
                try {
                    byte[] field = BaseRdbParser.StringHelper.zipListEntry(stream);
                    condition--;
                    byte[] value = BaseRdbParser.StringHelper.zipListEntry(stream);
                    condition--;
                    return new AbstractMap.SimpleEntry<>(field, value);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        o13.setValueRdbType(RDB_TYPE_HASH_ZIPLIST);
        o13.setDb(db);
        o13.setKey(new String(key, UTF_8));
        o13.setRawKey(key);
        return o13;
    }

    @Override
    public Event applyListQuickList(RedisInputStream in, DB db, int version) throws IOException {
        BaseRdbParser parser = new BaseRdbParser(in);
        KeyStringValueByteArrayIterator o14 = new KeyStringValueByteArrayIterator();
        byte[] key = parser.rdbLoadEncodedStringObject().first();
        long len = parser.rdbLoadLen().len;
        o14.setValue(new QuickListIter(len, parser));
        o14.setValueRdbType(RDB_TYPE_LIST_QUICKLIST);
        o14.setDb(db);
        o14.setKey(new String(key, UTF_8));
        o14.setRawKey(key);
        return o14;
    }

    private static abstract class Iter<T> implements Iterator<T> {

        protected long condition;
        protected final BaseRdbParser parser;

        private Iter(long condition, BaseRdbParser parser) {
            this.condition = condition;
            this.parser = parser;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class HashZipMapIter extends Iter<Map.Entry<byte[], byte[]>> {

        protected int zmEleLen;
        protected final RedisInputStream stream;

        private HashZipMapIter(RedisInputStream stream) {
            super(0, null);
            this.stream = stream;
        }

        @Override
        public boolean hasNext() {
            try {
                return (this.zmEleLen = BaseRdbParser.LenHelper.zmElementLen(stream)) != 255;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public Map.Entry<byte[], byte[]> next() {
            try {
                byte[] field = BaseRdbParser.StringHelper.bytes(stream, zmEleLen);
                this.zmEleLen = BaseRdbParser.LenHelper.zmElementLen(stream);
                if (this.zmEleLen == 255) {
                    return new AbstractMap.SimpleEntry<>(field, null);
                }
                int free = BaseRdbParser.LenHelper.free(stream);
                byte[] value = BaseRdbParser.StringHelper.bytes(stream, zmEleLen);
                BaseRdbParser.StringHelper.skip(stream, free);
                return new AbstractMap.SimpleEntry<>(field, value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static class QuickListIter extends Iter<byte[]> {

        protected int zllen = -1;
        protected RedisInputStream stream;

        private QuickListIter(long condition, BaseRdbParser parser) {
            super(condition, parser);
        }

        @Override
        public boolean hasNext() {
            return zllen > 0 || condition > 0;
        }

        @Override
        public byte[] next() {
            try {
                if (zllen == -1 && condition > 0) {
                    ByteArray element = parser.rdbGenericLoadStringObject(RDB_LOAD_NONE);
                    this.stream = new RedisInputStream(new ByteArrayInputStream(element));
                    BaseRdbParser.LenHelper.zlbytes(stream); // zlbytes
                    BaseRdbParser.LenHelper.zltail(stream); // zltail
                    this.zllen = BaseRdbParser.LenHelper.zllen(stream);
                    if (zllen == 0) {
                        int zlend = BaseRdbParser.LenHelper.zlend(stream);
                        if (zlend != 255) {
                            throw new AssertionError("zlend expect 255 but " + zlend);
                        }
                        zllen = -1;
                        condition--;
                    }
                    if (hasNext())
                        return next();
                    throw new IllegalStateException("end of iterator");
                } else {
                    byte[] e = BaseRdbParser.StringHelper.zipListEntry(stream);
                    zllen--;
                    if (zllen == 0) {
                        int zlend = BaseRdbParser.LenHelper.zlend(stream);
                        if (zlend != 255) {
                            throw new AssertionError("zlend expect 255 but " + zlend);
                        }
                        zllen = -1;
                        condition--;
                    }
                    return e;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
