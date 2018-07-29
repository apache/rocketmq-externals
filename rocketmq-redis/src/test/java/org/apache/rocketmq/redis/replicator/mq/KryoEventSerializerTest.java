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
 *
 */

package org.apache.rocketmq.redis.replicator.mq;

import com.moilioncircle.redis.replicator.cmd.impl.ZUnionStoreCommand;
import com.moilioncircle.redis.replicator.cmd.parser.ZUnionStoreParser;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.PreRdbSyncEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueHash;
import com.moilioncircle.redis.replicator.util.ByteArrayMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class KryoEventSerializerTest {

    @Test
    public void test() {
        Event e = new PreRdbSyncEvent();
        Serializer<Event> serializer = new KryoEventSerializer();
        System.out.println(serializer.read(serializer.write(e)));

        KeyStringValueHash x = new KeyStringValueHash();
        x.setKey("key".getBytes());
        Map<byte[], byte[]> map1 = new ByteArrayMap();
        map1.put("field1".getBytes(), "value1".getBytes());
        map1.put("field2".getBytes(), "value2".getBytes());
        x.setValue(map1);

        KeyStringValueHash kv = (KeyStringValueHash)serializer.read(serializer.write(x));
        ByteArrayMap map2 = (ByteArrayMap)kv.getValue();
        assertNotNull(map2.get("field1".getBytes()));

        ZUnionStoreParser parser = new ZUnionStoreParser();
        ZUnionStoreCommand cmd = parser.parse(toObjectArray("zunionstore des 2 k1 k2 WEIGHTS 2 3 AGGREGATE min".split(" ")));

        ZUnionStoreCommand cmd1 = (ZUnionStoreCommand)serializer.read(serializer.write(cmd));
        for(byte[] bytes : cmd1.getKeys()) {
            System.out.println(new String(bytes));
        }
        System.out.println(cmd1);
    }

    protected Object[] toObjectArray(Object[] raw) {
        Object[] r = new Object[raw.length];
        for (int i = 0; i < r.length; i++) {
            if (raw[i] instanceof String)
                r[i] = ((String) raw[i]).getBytes();
            else
                r[i] = raw[i];
        }
        return r;
    }
}