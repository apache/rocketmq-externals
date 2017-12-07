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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.redis.replicator.Configuration;
import org.apache.rocketmq.redis.replicator.FileType;
import org.apache.rocketmq.redis.replicator.RedisReplicator;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueSet;
import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueHash;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueList;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueString;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class RdbBinaryParserTest {

    public static final byte[] b1 = new byte[] {-1};
    public static final byte[] b2 = new byte[] {0, -1};
    public static final byte[] b3 = new byte[] {0, 0, -1};
    public static final byte[] b4 = new byte[] {0, 0, 0, -1};
    public static final byte[] b5 = new byte[] {0, 0, 0, 0, -1};

    @Test
    public void testParse() throws Exception {
        ConcurrentHashMap<String, KeyValuePair<?>> map = new ConcurrentHashMap<>();
        String[] resources = new String[] {
            "dictionary.rdb",
            "easily_compressible_string_key.rdb", "empty_database.rdb",
            "hash_as_ziplist.rdb", "integer_keys.rdb", "intset_16.rdb",
            "intset_32.rdb", "intset_64.rdb", "keys_with_expiry.rdb",
            "linkedlist.rdb", "multiple_databases.rdb",
            "parser_filters.rdb", "rdb_version_5_with_checksum.rdb", "regular_set.rdb",
            "regular_sorted_set.rdb", "sorted_set_as_ziplist.rdb", "uncompressible_string_keys.rdb",
            "ziplist_that_compresses_easily.rdb", "ziplist_that_doesnt_compress.rdb",
            "ziplist_with_integers.rdb", "zipmap_that_compresses_easily.rdb",
            "zipmap_that_doesnt_compress.rdb", "zipmap_with_big_values.rdb"};
        for (String resource : resources) {
            template(resource, map);
        }
        //Thread.sleep(5000);

        assertByteArray("zero".getBytes(), map.get("key_in_zeroth_database"));
        assertByteArray("second".getBytes(), map.get("key_in_second_database"));

        assertByteArray("Positive 8 bit integer".getBytes(), map.get("125"));
        assertByteArray("Positive 16 bit integer".getBytes(), map.get("43947"));
        assertByteArray("Positive 32 bit integer".getBytes(), map.get("183358245"));

        assertByteArray("Negative 8 bit integer".getBytes(), map.get("-123"));
        assertByteArray("Negative 16 bit integer".getBytes(), map.get("-29477"));
        assertByteArray("Negative 32 bit integer".getBytes(), map.get("-183358245"));

        assertByteArray(b1, map.get("b1"));
        assertByteArray(b2, map.get("b2"));
        assertByteArray(b3, map.get("b3"));
        assertByteArray(b4, map.get("b4"));
        assertByteArray(b5, map.get("b5"));

        assertByteArray("Key that redis should compress easily".getBytes(), map.get("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

        assertByteArray("2".getBytes(), map.get("zimap_doesnt_compress"), "MKD1G6");
        assertByteArray("F7TI".getBytes(), map.get("zimap_doesnt_compress"), "YNNXK");

        assertEquals(253, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("253bytes").length());
        assertEquals(254, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("254bytes").length());
        assertEquals(255, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("255bytes").length());
        assertEquals(300, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("300bytes").length());
        assertEquals(20000, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("20kbytes").length());

        assertByteArray("aa".getBytes(), map.get("zipmap_compresses_easily"), "a");
        assertByteArray("aaaa".getBytes(), map.get("zipmap_compresses_easily"), "aa");
        assertByteArray("aaaaaaaaaaaaaa".getBytes(), map.get("zipmap_compresses_easily"), "aaaaa");

        assertByteArray("T63SOS8DQJF0Q0VJEZ0D1IQFCYTIPSBOUIAI9SB0OV57MQR1FI".getBytes(), map.get("force_dictionary"), "ZMU5WEJDG7KU89AOG5LJT6K7HMNB3DEI43M6EYTJ83VRJ6XNXQ");
        assertByteArray("6VULTCV52FXJ8MGVSFTZVAGK2JXZMGQ5F8OVJI0X6GEDDR27RZ".getBytes(), map.get("force_dictionary"), "UHS5ESW4HLK8XOGTM39IK1SJEUGVV9WOPK6JYA5QBZSJU84491");

        assertByteArray("aaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 0);
        assertByteArray("aaaaaaaaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 1);
        assertByteArray("aaaaaaaaaaaaaaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 2);
        assertByteArray("aaaaaaaaaaaaaaaaaaaaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 3);
        assertByteArray("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 4);
        assertByteArray("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes(), map.get("ziplist_compresses_easily"), 5);

        assertByteArray("aj2410".getBytes(), map.get("ziplist_doesnt_compress"), 0);
        assertByteArray("cc953a17a8e096e76a44169ad3f9ac87c5f8248a403274416179aa9fbd852344".getBytes(), map.get("ziplist_doesnt_compress"), 1);

        String[] numbers = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "-2", "25", "-61", "63", "16380", "-16000", "65535", "-65523", "4194304", "9223372036854775807"};
        List<String> numlist = Arrays.asList(numbers);
        assertContainsList(numlist, map.get("ziplist_with_integers"));

        List<String> list = (ArrayList<String>) map.get("force_linkedlist").getValue();
        assertEquals(1000, list.size());
        assertByteArray("41PJSO2KRV6SK1WJ6936L06YQDPV68R5J2TAZO3YAR5IL5GUI8".getBytes(), map.get("force_linkedlist"), 0);
        assertByteArray("E41JRQX2DB4P1AQZI86BAT7NHPBHPRIIHQKA4UXG94ELZZ7P3Y".getBytes(), map.get("force_linkedlist"), 1);

        numlist = Arrays.asList("32766", "32765", "32764");
        assertContains(numlist, map.get("intset_16"));

        numlist = Arrays.asList("2147418110", "2147418109", "2147418108");
        assertContains(numlist, map.get("intset_32"));

        numlist = Arrays.asList("9223090557583032318", "9223090557583032317", "9223090557583032316");
        assertContains(numlist, map.get("intset_64"));

        numlist = Arrays.asList("alpha", "beta", "gamma", "delta", "phi", "kappa");
        assertContains(numlist, map.get("regular_set"));

        List<ZSetEntry> zset = new ArrayList<>(((Set<ZSetEntry>) map.get("sorted_set_as_ziplist").getValue()));

        for (ZSetEntry entry : zset) {
            if (entry.getElement().equals("8b6ba6718a786daefa69438148361901")) {
                assertEquals(1d, entry.getScore(), 0.0001);
            }
            if (entry.getElement().equals("cb7a24bb7528f934b841b34c3a73e0c7")) {
                assertEquals(2.37d, entry.getScore(), 0.0001);
            }
            if (entry.getElement().equals("523af537946b79c4f8369ed39ba78605")) {
                assertEquals(3.423d, entry.getScore(), 0.0001);
            }
        }

        assertByteArray("ssssssss".getBytes(), map.get("k1"));
        assertByteArray("wwwwwwww".getBytes(), map.get("k3"));

        assertEquals(true, map.containsKey("z1"));
        assertEquals(true, map.containsKey("z2"));
        assertEquals(true, map.containsKey("z3"));
        assertEquals(true, map.containsKey("z4"));

        assertEquals(0, map.get("key_in_zeroth_database").getDb().getDbNumber());
        assertEquals(2, map.get("key_in_second_database").getDb().getDbNumber());

        assertByteArray("efgh".getBytes(), map.get("abcd"));
        assertByteArray("bar".getBytes(), map.get("foo"));
        assertByteArray("baz".getBytes(), map.get("bar"));
        assertByteArray("abcdef".getBytes(), map.get("abcdef"));
        assertByteArray("thisisalongerstring.idontknowwhatitmeans".getBytes(), map.get("longerstring"));

        assertEquals(new Date(1671963072573L), new Date(map.get("expires_ms_precision").getExpiredMs()));
    }

    public void assertByteArray(byte[] bytes, KeyValuePair<?> kv) {
        if (kv instanceof KeyStringValueString) {
            KeyStringValueString ksvs = (KeyStringValueString) kv;
            assertEquals(true, Arrays.equals(bytes, ksvs.getRawValue()));
        } else {
            fail();
        }
    }

    public void assertByteArray(byte[] bytes, KeyValuePair<?> kv, String field) {
        if (kv instanceof KeyStringValueHash) {
            KeyStringValueHash ksvh = (KeyStringValueHash) kv;
            Map<byte[], byte[]> m = ksvh.getRawValue();
            assertEquals(true, Arrays.equals(bytes, m.get(field.getBytes())));
        } else {
            fail();
        }
    }

    public void assertByteArray(byte[] bytes, KeyValuePair<?> kv, int index) {
        if (kv instanceof KeyStringValueList) {
            KeyStringValueList ksvh = (KeyStringValueList) kv;
            assertEquals(true, Arrays.equals(bytes, ksvh.getRawValue().get(index)));
        } else {
            fail();
        }
    }

    public void assertContains(List<String> list, KeyValuePair<?> kv) {
        List<String> source = new ArrayList<>(list.size());
        for (String s : list) {
            source.add(Arrays.toString(s.getBytes()));
        }
        if (kv instanceof KeyStringValueSet) {
            KeyStringValueSet ksvh = (KeyStringValueSet) kv;
            Set<byte[]> bytes = ksvh.getRawValue();
            List<String> target = new ArrayList<>();
            for (byte[] b : bytes) {
                target.add(Arrays.toString(b));
            }
            for (String s : source) {
                assertEquals(true, target.contains(s));
            }
        } else {
            fail();
        }
    }

    public void assertContainsList(List<String> list, KeyValuePair<?> kv) {
        List<String> source = new ArrayList<>(list.size());
        for (String s : list) {
            source.add(Arrays.toString(s.getBytes()));
        }
        if (kv instanceof KeyStringValueList) {
            KeyStringValueList ksvh = (KeyStringValueList) kv;
            List<byte[]> bytes = ksvh.getRawValue();
            List<String> target = new ArrayList<>();
            for (byte[] b : bytes) {
                target.add(Arrays.toString(b));
            }
            for (String s : source) {
                assertEquals(true, target.contains(s));
            }
        } else {
            fail();
        }
    }

    public void template(String filename, final ConcurrentHashMap<String, KeyValuePair<?>> map) {
        try {
            @SuppressWarnings("resource")
            Replicator replicator = new RedisReplicator(RdbBinaryParserTest.class.
                getClassLoader().getResourceAsStream(filename)
                , FileType.RDB, Configuration.defaultSetting());
            replicator.addRdbListener(new RdbListener.Adaptor() {
                @Override
                public void handle(Replicator replicator, KeyValuePair<?> kv) {
                    map.put(kv.getKey(), kv);
                }
            });
            replicator.open();
        } catch (Exception e) {
            fail();
        }
    }
}