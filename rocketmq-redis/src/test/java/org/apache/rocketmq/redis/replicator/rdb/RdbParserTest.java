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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.redis.replicator.FileType;
import org.apache.rocketmq.redis.replicator.Configuration;
import org.apache.rocketmq.redis.replicator.RedisReplicator;
import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class RdbParserTest {

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

        assertEquals("zero", map.get("key_in_zeroth_database").getValue());
        assertEquals("second", map.get("key_in_second_database").getValue());

        assertEquals("Positive 8 bit integer", map.get("125").getValue());
        assertEquals("Positive 16 bit integer", map.get("43947").getValue());
        assertEquals("Positive 32 bit integer", map.get("183358245").getValue());

        assertEquals("Negative 8 bit integer", map.get("-123").getValue());
        assertEquals("Negative 16 bit integer", map.get("-29477").getValue());
        assertEquals("Negative 32 bit integer", map.get("-183358245").getValue());

        assertEquals("Key that redis should compress easily", map.get("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").getValue());

        assertEquals("2", ((HashMap<String, String>) map.get("zimap_doesnt_compress").getValue()).get("MKD1G6"));
        assertEquals("F7TI", ((HashMap<String, String>) map.get("zimap_doesnt_compress").getValue()).get("YNNXK"));

        assertEquals(253, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("253bytes").length());
        assertEquals(254, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("254bytes").length());
        assertEquals(255, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("255bytes").length());
        assertEquals(300, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("300bytes").length());
        assertEquals(20000, ((HashMap<String, String>) map.get("zipmap_with_big_values").getValue()).get("20kbytes").length());

        assertEquals("aa", ((HashMap<String, String>) map.get("zipmap_compresses_easily").getValue()).get("a"));
        assertEquals("aaaa", ((HashMap<String, String>) map.get("zipmap_compresses_easily").getValue()).get("aa"));
        assertEquals("aaaaaaaaaaaaaa", ((HashMap<String, String>) map.get("zipmap_compresses_easily").getValue()).get("aaaaa"));

        assertEquals("T63SOS8DQJF0Q0VJEZ0D1IQFCYTIPSBOUIAI9SB0OV57MQR1FI", ((HashMap<String, String>) map.get("force_dictionary").getValue()).get("ZMU5WEJDG7KU89AOG5LJT6K7HMNB3DEI43M6EYTJ83VRJ6XNXQ"));
        assertEquals("6VULTCV52FXJ8MGVSFTZVAGK2JXZMGQ5F8OVJI0X6GEDDR27RZ", ((HashMap<String, String>) map.get("force_dictionary").getValue()).get("UHS5ESW4HLK8XOGTM39IK1SJEUGVV9WOPK6JYA5QBZSJU84491"));

        List<String> list = (ArrayList<String>) map.get("ziplist_compresses_easily").getValue();
        assertEquals("aaaaaa", list.get(0));
        assertEquals("aaaaaaaaaaaa", list.get(1));
        assertEquals("aaaaaaaaaaaaaaaaaa", list.get(2));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaa", list.get(3));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", list.get(4));
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", list.get(5));

        list = (ArrayList<String>) map.get("ziplist_doesnt_compress").getValue();
        assertEquals("aj2410", list.get(0));
        assertEquals("cc953a17a8e096e76a44169ad3f9ac87c5f8248a403274416179aa9fbd852344", list.get(1));

        String[] numbers = new String[] {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "-2", "25", "-61", "63", "16380", "-16000", "65535", "-65523", "4194304", "9223372036854775807"};
        List<String> numlist = Arrays.asList(numbers);

        list = (ArrayList<String>) map.get("ziplist_with_integers").getValue();
        for (String str : list) {
            assertEquals(true, numlist.contains(str));
        }

        list = (ArrayList<String>) map.get("force_linkedlist").getValue();
        assertEquals(1000, list.size());
        assertEquals("41PJSO2KRV6SK1WJ6936L06YQDPV68R5J2TAZO3YAR5IL5GUI8", list.get(0));
        assertEquals("E41JRQX2DB4P1AQZI86BAT7NHPBHPRIIHQKA4UXG94ELZZ7P3Y", list.get(1));

        numlist = Arrays.asList("32766", "32765", "32764");
        list = new ArrayList<>(((Set<String>) map.get("intset_16").getValue()));
        for (String str : list) {
            assertEquals(true, numlist.contains(str));
        }

        numlist = Arrays.asList("2147418110", "2147418109", "2147418108");
        list = new ArrayList<>(((Set<String>) map.get("intset_32").getValue()));
        for (String str : list) {
            assertEquals(true, numlist.contains(str));
        }

        numlist = Arrays.asList("9223090557583032318", "9223090557583032317", "9223090557583032316");
        list = new ArrayList<>(((Set<String>) map.get("intset_64").getValue()));
        for (String str : list) {
            assertEquals(true, numlist.contains(str));
        }

        numlist = Arrays.asList("alpha", "beta", "gamma", "delta", "phi", "kappa");
        list = new ArrayList<>(((Set<String>) map.get("regular_set").getValue()));
        for (String str : list) {
            assertEquals(true, numlist.contains(str));
        }

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

        assertEquals("ssssssss", map.get("k1").getValue());
        assertEquals("wwwwwwww", map.get("k3").getValue());

        assertEquals(true, map.containsKey("z1"));
        assertEquals(true, map.containsKey("z2"));
        assertEquals(true, map.containsKey("z3"));
        assertEquals(true, map.containsKey("z4"));

        assertEquals(0, map.get("key_in_zeroth_database").getDb().getDbNumber());
        assertEquals(2, map.get("key_in_second_database").getDb().getDbNumber());

        assertEquals("efgh", map.get("abcd").getValue());
        assertEquals("bar", map.get("foo").getValue());
        assertEquals("baz", map.get("bar").getValue());
        assertEquals("abcdef", map.get("abcdef").getValue());
        assertEquals("thisisalongerstring.idontknowwhatitmeans", map.get("longerstring").getValue());

        assertEquals(new Date(1671963072573L), new Date(map.get("expires_ms_precision").getExpiredMs()));
    }

    @SuppressWarnings("resource")
    public void template(String filename, final ConcurrentHashMap<String, KeyValuePair<?>> map) {
        try {
            Replicator replicator = new RedisReplicator(RdbParserTest.class.
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