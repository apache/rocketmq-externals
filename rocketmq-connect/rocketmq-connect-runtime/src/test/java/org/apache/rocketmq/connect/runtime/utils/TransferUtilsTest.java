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

package org.apache.rocketmq.connect.runtime.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.config.RuntimeConfigDefine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TransferUtilsTest {

    @Test
    public void testKeyValue2StringKeyValue() {
        ConnectKeyValue connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put("key1", 1);
        connectKeyValue.put("key2", 2L);
        connectKeyValue.put("key3", 3.0);
        connectKeyValue.put("key4", "4");
        String s = TransferUtils.keyValueToString(connectKeyValue);
        ConnectKeyValue connectKeyValue1 = TransferUtils.stringToKeyValue(s);
        assertEquals(1, connectKeyValue1.getInt("key1"));
        assertEquals(2L, connectKeyValue1.getLong("key2"));
        assertTrue(Objects.equals(3.0, connectKeyValue1.getDouble("key3")));
        assertEquals("4", connectKeyValue1.getString("key4"));
    }

    @Test
    public void testKeyValueList2String2KeyValueList() {
        ConnectKeyValue connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put("key1", 1);
        connectKeyValue.put("key2", 2L);
        connectKeyValue.put("key3", 3.0);
        connectKeyValue.put("key4", "4");
        List<ConnectKeyValue> connectKeyValues = new ArrayList<ConnectKeyValue>(8) {
            {
                add(connectKeyValue);
            }
        };
        String s = TransferUtils.keyValueListToString(connectKeyValues);
        List<ConnectKeyValue> connectKeyValues1 = TransferUtils.stringToKeyValueList(s);
        assertNotNull(connectKeyValues1);
        ConnectKeyValue connectKeyValue1 = connectKeyValues1.get(0);
        assertEquals(1, connectKeyValue1.getInt("key1"));
        assertEquals(2L, connectKeyValue1.getLong("key2"));
        assertTrue(Objects.equals(3.0, connectKeyValue1.getDouble("key3")));
        assertEquals("4", connectKeyValue1.getString("key4"));
    }

    @Test
    public void testToJsonStringToConnAndTaskConfigs() {
        String connectName = "testConnector";
        ConnectKeyValue connectKeyValue = new ConnectKeyValue();
        connectKeyValue.put(RuntimeConfigDefine.CONNECTOR_CLASS, "io.openmessaging.connect.runtime.service.TestConnector");
        connectKeyValue.put(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER, "source-record-converter");
        List<ConnectKeyValue> connectKeyValues = new ArrayList<ConnectKeyValue>(8) {
            {
                add(connectKeyValue);
            }
        };
        Map<String, ConnectKeyValue> connectorConfigs = new HashMap<String, ConnectKeyValue>() {
            {
                put(connectName, connectKeyValue);
            }
        };
        Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<String, List<ConnectKeyValue>>() {
            {
                put(connectName, connectKeyValues);
            }
        };
        ConnAndTaskConfigs connAndTaskConfigs = new ConnAndTaskConfigs();
        connAndTaskConfigs.setConnectorConfigs(connectorConfigs);
        connAndTaskConfigs.setTaskConfigs(taskConfigs);
        Map<String, String> connectorMap = new HashMap<>();
        Map<String, String> taskMap = new HashMap<>();
        for (String key : connAndTaskConfigs.getConnectorConfigs().keySet()) {
            connectorMap.put(key, TransferUtils.keyValueToString(connAndTaskConfigs.getConnectorConfigs().get(key)));
        }
        for (String key : connAndTaskConfigs.getTaskConfigs().keySet()) {
            taskMap.put(key, TransferUtils.keyValueListToString(connAndTaskConfigs.getTaskConfigs().get(key)));
        }
        String s = TransferUtils.toJsonString(connectorMap, taskMap);
        ConnAndTaskConfigs connAndTaskConfigs1 = TransferUtils.toConnAndTaskConfigs(s);
        Map<String, ConnectKeyValue> connectorConfigs1 = connAndTaskConfigs1.getConnectorConfigs();

        assertNotNull(connAndTaskConfigs1);

        ConnectKeyValue connectKeyValue1 = connectorConfigs1.get(connectName);

        assertNotNull(connectKeyValue1);
        assertEquals("io.openmessaging.connect.runtime.service.TestConnector", connectKeyValue1.getString(RuntimeConfigDefine.CONNECTOR_CLASS));
        assertEquals("source-record-converter", connectKeyValue1.getString(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER));

        Map<String, List<ConnectKeyValue>> taskConfigs1 = connAndTaskConfigs1.getTaskConfigs();
        List<ConnectKeyValue> connectKeyValues1 = taskConfigs1.get(connectName);

        assertNotNull(connectKeyValues1);

        ConnectKeyValue connectKeyValue2 = connectKeyValues1.get(0);
        assertNotNull(connectKeyValue2);
        assertEquals("io.openmessaging.connect.runtime.service.TestConnector", connectKeyValue2.getString(RuntimeConfigDefine.CONNECTOR_CLASS));
        assertEquals("source-record-converter", connectKeyValue2.getString(RuntimeConfigDefine.SOURCE_RECORD_CONVERTER));
    }

}