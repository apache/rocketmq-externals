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

package org.apache.rocketmq.connect.runtime.service.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.connect.runtime.common.ConnAndTaskConfigs;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultAllocateConnAndTaskStrategyTest {

    @Test
    public void testAllocate() {
        DefaultAllocateConnAndTaskStrategy defaultAllocateConnAndTaskStrategy = new DefaultAllocateConnAndTaskStrategy();
        List<String> allWorker = new ArrayList<String>() {
            {
                add("workId1");
                add("workId2");
                add("workId3");
            }
        };
        String curWorker = "workId3";
        Map<String, ConnectKeyValue> connectorConfigs = new HashMap<String, ConnectKeyValue>() {
            {
                put("connectorConfig1", new ConnectKeyValue());
                put("connectorConfig2", new ConnectKeyValue());
                put("connectorConfig3", new ConnectKeyValue());
            }
        };

        ConnectKeyValue connectKeyValue1 = new ConnectKeyValue();
        ConnectKeyValue connectKeyValue2 = new ConnectKeyValue();
        ConnectKeyValue connectKeyValue3 = new ConnectKeyValue();
        ConnectKeyValue connectKeyValue4 = new ConnectKeyValue();
        ConnectKeyValue connectKeyValue5 = new ConnectKeyValue();
        ConnectKeyValue connectKeyValue6 = new ConnectKeyValue();
        List<ConnectKeyValue> taskConfig1 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKeyValue1);
            }
        };
        List<ConnectKeyValue> taskConfig2 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKeyValue2);
                add(connectKeyValue3);
            }
        };
        List<ConnectKeyValue> taskConfig3 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKeyValue4);
                add(connectKeyValue5);
                add(connectKeyValue6);
            }
        };
        Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<String, List<ConnectKeyValue>>() {
            {
                put("connectorConfig1", taskConfig1);
                put("connectorConfig2", taskConfig2);
                put("connectorConfig3", taskConfig3);
            }
        };
        ConnAndTaskConfigs allocate = defaultAllocateConnAndTaskStrategy.allocate(allWorker, curWorker, connectorConfigs, taskConfigs);
        assertNotNull(allocate);
        Map<String, ConnectKeyValue> connectorConfigs3 = allocate.getConnectorConfigs();
        assertNotNull(connectorConfigs3);

        assertNotNull(connectorConfigs3.get("connectorConfig3"));
        assertNull(connectorConfigs3.get("connectorConfig2"));
        assertNull(connectorConfigs3.get("connectorConfig1"));

        Map<String, List<ConnectKeyValue>> taskConfigs1 = allocate.getTaskConfigs();
        assertNotNull(taskConfigs1);

        assertNull(taskConfigs1.get("connectorConfig1"));

        List<ConnectKeyValue> connectorConfig1 = taskConfigs1.get("connectorConfig2");
        assertNotNull(connectorConfig1);

        assertEquals(1, connectorConfig1.size());
        assertEquals(connectKeyValue3, connectorConfig1.get(0));

        List<ConnectKeyValue> connectorConfig2 = taskConfigs1.get("connectorConfig3");
        assertNotNull(connectorConfig2);

        assertEquals(1, connectorConfig2.size());
        assertEquals(connectKeyValue6, connectorConfig2.get(0));

    }

}