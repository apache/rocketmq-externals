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

package org.apache.rocketmq.redis.test.converter;

import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.converter.KVEntryConverter;
import org.apache.rocketmq.connect.redis.converter.RedisEntryConverter;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;
import org.apache.rocketmq.connect.redis.pojo.RedisEntry;
import org.junit.Assert;
import org.junit.Test;

public class RedisEntryConverterTest {

    @Test
    public void test(){
        KVEntry entry = getKVEntry();
        KVEntryConverter converter = new RedisEntryConverter();
        Collection<SourceDataEntry> res = converter.kVEntryToDataEntries(entry);
        Assert.assertNotNull(res);
        Assert.assertEquals(1, res.size());
        Assert.assertEquals("key", ((List<SourceDataEntry>) res).get(0).getPayload()[1]);
        Assert.assertEquals("value", ((List<SourceDataEntry>) res).get(0).getPayload()[2]);
        Assert.assertEquals("set", ((List<SourceDataEntry>) res).get(0).getPayload()[0]);
        Map<String, Object> params = (Map<String, Object>) ((List<SourceDataEntry>) res).get(0).getPayload()[3];
        Assert.assertEquals("replId", params.get(Options.REDIS_REPLID.name()));
    }

    @Test
    public void testListSplit(){
        KVEntryConverter converter = new RedisEntryConverter();


        KVEntry entry = getArrayKVEntry(999);
        Collection res = converter.kVEntryToDataEntries(entry);
        Assert.assertEquals(2, res.size());

        KVEntry entry1 = getArrayKVEntry(1001);
        Collection res1 = converter.kVEntryToDataEntries(entry1);
        Assert.assertEquals(3, res1.size());

        KVEntry entry2 = getArrayKVEntry(1000);
        Collection res2 = converter.kVEntryToDataEntries(entry2);
        Assert.assertEquals(2, res2.size());
    }


    @Test
    public void testMapSplit(){
        KVEntryConverter converter = new RedisEntryConverter();

        RedisEntry entry = RedisEntry.newEntry(FieldType.MAP);
        entry.queueName("queue1");
        entry.key("key");
        entry.entryType(EntryType.UPDATE);
        entry.offset(65535L);
        entry.param(Options.REDIS_INCREMENT, 15L);
        entry.sourceId("123");
        entry.command("set");

        Map<String, String> values = new HashMap<>();
        int num = 10001;
        for (int i = 0; i < num; i++) {
            values.put("a" + i, Integer.toString(i));
        }
        entry.value(values);

        List<SourceDataEntry> entryList = converter.kVEntryToDataEntries(entry);
        Assert.assertNotNull(entryList);
        Assert.assertEquals(21, entryList.size());
        Assert.assertEquals("set", entryList.get(0).getPayload()[0]);
    }

    private KVEntry getKVEntry(){
        KVEntry entry = new RedisEntry(FieldType.STRING);
        return entry.key("key")
                    .value("value")
                    .command("set")
                    .sourceId("replId")
                    .offset(6000L);
    }

    private KVEntry getArrayKVEntry(int size){
        KVEntry entry = new RedisEntry(FieldType.ARRAY);
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(Integer.toString(i));
        }
        return entry.key("key")
            .value(values)
            .command("set")
            .sourceId("replId")
            .offset(6000L);
    }
}
