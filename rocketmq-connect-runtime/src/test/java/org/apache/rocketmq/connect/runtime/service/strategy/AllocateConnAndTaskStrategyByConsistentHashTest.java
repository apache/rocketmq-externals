/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AllocateConnAndTaskStrategyByConsistentHashTest {

    @Test
    public void testAllocate() {
        AllocateConnAndTaskStrategyByConsistentHash strategy = new AllocateConnAndTaskStrategyByConsistentHash();

        List<String> allWorker = new ArrayList<String>() {
            {
                add("workId1");
                add("workId2");
                add("workId3");
            }
        };

        Map<String, ConnectKeyValue> connectorConfigs = new HashMap<String, ConnectKeyValue>() {
            {
                put("connectorConfig1", new ConnectKeyValue());
                put("connectorConfig2", new ConnectKeyValue());
                put("connectorConfig3", new ConnectKeyValue());
            }
        };

        List<ConnectKeyValue> connectKVs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ConnectKeyValue kv = new ConnectKeyValue();
            kv.put("index", i);
            connectKVs.add(kv);
        }
        List<ConnectKeyValue> taskConfig1 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKVs.get(0));
            }
        };
        List<ConnectKeyValue> taskConfig2 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKVs.get(1));
                add(connectKVs.get(2));
            }
        };
        List<ConnectKeyValue> taskConfig3 = new ArrayList<ConnectKeyValue>() {
            {
                add(connectKVs.get(3));
                add(connectKVs.get(4));
                add(connectKVs.get(5));
            }
        };

        Map<String, List<ConnectKeyValue>> taskConfigs = new HashMap<String, List<ConnectKeyValue>>() {
            {
                put("connectorConfig1", taskConfig1);
                put("connectorConfig2", taskConfig2);
                put("connectorConfig3", taskConfig3);
            }
        };

        Set<String> connectorChecks = new HashSet<>();
        Map<String, List<ConnectKeyValue>> taskChecks = new HashMap<>();
        taskConfigs.keySet().stream().forEach(key -> {
            taskChecks.put(key, new ArrayList<>());
        });

        for (String worker : allWorker) {
            ConnAndTaskConfigs allocate = strategy.allocate(allWorker, worker, connectorConfigs, taskConfigs);
            assertNotNull(allocate);

            allocate.getConnectorConfigs().keySet().forEach(key -> {
                assertFalse(connectorChecks.contains(key));
                connectorChecks.add(key);
            });

            allocate.getTaskConfigs().forEach((connectorName, allocatedTasks) -> {
                List<ConnectKeyValue> checkKVs = taskChecks.computeIfAbsent(connectorName, k -> new ArrayList<>());
                allocatedTasks.forEach(task -> {
                    assertFalse(checkKVs.contains(task));
                    checkKVs.add(task);
                });
            });
        }
        assertEquals(connectorConfigs.size(), connectorChecks.size());

        long cnt = taskChecks.entrySet().stream().flatMap(entry -> entry.getValue().stream()).count();
        assertEquals(6, cnt);
    }
}
