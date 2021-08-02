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
package org.apache.rocketmq.console.controller;

import java.util.HashMap;
import java.util.Properties;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.console.service.impl.ClusterServiceImpl;
import org.apache.rocketmq.console.util.MockObjectUtil;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ClusterControllerTest extends BaseControllerTest {

    @InjectMocks
    private ClusterController clusterController;

    @Spy
    private ClusterServiceImpl clusterService;

    @Test
    public void testList() throws Exception {
        final String url = "/cluster/list.query";
        {
            ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
            when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
            HashMap<String, String> result = new HashMap<>();
            result.put("commitLogMaxOffset", "78830");
            result.put("commitLogMinOffset", "0");
            KVTable kvTable = new KVTable();
            kvTable.setTable(result);
            when(mqAdminExt.fetchBrokerRuntimeStats(anyString())).thenReturn(kvTable);
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.brokerServer").isMap());
    }

    @Test
    public void testBrokerConfig() throws Exception {
        final String url = "/cluster/brokerConfig.query";
        {
            Properties properties = new Properties();
            properties.setProperty("brokerClusterName", "DefaultCluster");
            properties.setProperty("brokerName", "broker-a");
            properties.setProperty("namesrvAddr", "127.0.0.1:9876");
            when(mqAdminExt.getBrokerConfig(anyString())).thenReturn(properties);
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("brokerAddr", "127.0.0.1:10911");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.brokerName").value("broker-a"))
            .andExpect(jsonPath("$.data.namesrvAddr").value("127.0.0.1:9876"));
    }

    @Override
    protected Object getTestController() {
        return clusterController;
    }
}
