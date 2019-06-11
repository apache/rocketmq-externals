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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.common;

import io.openmessaging.KeyValue;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectKeyValueTest {

    @Test
    public void testKeyValueOperations() {

        KeyValue keyValue = new ConnectKeyValue();
        keyValue.put("StringKey", "StringValue");
        keyValue.put("IntegerKey", 0);
        keyValue.put("LongKey", 1L);
        keyValue.put("DoubleKey", 5.2);

        assertEquals("StringValue", keyValue.getString("StringKey"));
        assertEquals(0, keyValue.getInt("IntegerKey"));
        assertEquals(1L, keyValue.getLong("LongKey"));
        assertEquals(5.2, keyValue.getDouble("DoubleKey"), 0.0);

        assertEquals("StringValue1", keyValue.getString("StringKey1", "StringValue1"));
        assertEquals(2, keyValue.getInt("IntegerKey1", 2));
        assertEquals(2L, keyValue.getLong("LongKey1", 2L));
        assertEquals(5.0, keyValue.getDouble("DoubleKey1", 5.0), 0.0);

        Set<String> keySet = keyValue.keySet();
        Set<String> compareKeySet = new HashSet<>();
        compareKeySet.add("StringKey");
        compareKeySet.add("IntegerKey");
        compareKeySet.add("LongKey");
        compareKeySet.add("DoubleKey");

        assertEquals(keySet, compareKeySet);

        assertTrue(keyValue.containsKey("StringKey"));
        assertTrue(keyValue.containsKey("IntegerKey"));
        assertTrue(keyValue.containsKey("LongKey"));
        assertTrue(keyValue.containsKey("DoubleKey"));

    }
}
