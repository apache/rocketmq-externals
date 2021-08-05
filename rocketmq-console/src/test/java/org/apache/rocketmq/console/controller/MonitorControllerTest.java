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

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.console.model.ConsumerMonitorConfig;
import org.apache.rocketmq.console.service.impl.MonitorServiceImpl;
import org.apache.rocketmq.console.util.JsonUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MonitorControllerTest extends BaseControllerTest {

    @InjectMocks
    private MonitorController monitorController;

    @Spy
    private MonitorServiceImpl monitorService;

    private String filePath;

    private String consumeGroupName = "group_test";

    private String consumeGroupName1 = "group_test1";

    @Before
    public void init() {
        super.mockRmqConfigure();
        when(configure.getRocketMqConsoleDataPath()).thenReturn("/tmp/rocketmq-console/test/data");
        Map<String, ConsumerMonitorConfig> configMap = new ConcurrentHashMap<>();
        configMap.put(consumeGroupName, new ConsumerMonitorConfig(0, 100));
        configMap.put(consumeGroupName1, new ConsumerMonitorConfig(10, 200));
        ReflectionTestUtils.setField(monitorService, "configMap", configMap);
        filePath = configure.getRocketMqConsoleDataPath()
            + File.separatorChar + "monitor" + File.separatorChar + "consumerMonitorConfig.json";
    }

    @Test
    public void testCreateOrUpdateConsumerMonitor() throws Exception {
        final String url = "/monitor/createOrUpdateConsumerMonitor.do";
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("consumeGroupName", consumeGroupName)
            .param("minCount", String.valueOf(0))
            .param("maxDiffTotal", String.valueOf(100));
        perform = mockMvc.perform(requestBuilder);

        Map<String, ConsumerMonitorConfig> map =
            JsonUtil.string2Obj(MixAll.file2String(filePath),
                new TypeReference<Map<String, ConsumerMonitorConfig>>() {
                });
        Assert.assertEquals(map.size(), 2);
        Assert.assertEquals(map.get(consumeGroupName).getMaxDiffTotal(), 100);

        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testConsumerMonitorConfig() throws Exception {
        final String url = "/monitor/consumerMonitorConfig.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data.group_test.minCount").value(0))
            .andExpect(jsonPath("$.data.group_test.maxDiffTotal").value(100));
    }

    @Test
    public void testConsumerMonitorConfigByGroupName() throws Exception {
        final String url = "/monitor/consumerMonitorConfigByGroupName.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumeGroupName", consumeGroupName);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.minCount").value(0))
            .andExpect(jsonPath("$.data.maxDiffTotal").value(100));
    }

    @Test
    public void testDeleteConsumerMonitor() throws Exception {
        final String url = "/monitor/deleteConsumerMonitor.do";
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("consumeGroupName", consumeGroupName);
        perform = mockMvc.perform(requestBuilder);

        Map<String, ConsumerMonitorConfig> map =
            JsonUtil.string2Obj(MixAll.file2String(filePath),
                new TypeReference<Map<String, ConsumerMonitorConfig>>() {
                });
        Assert.assertEquals(map.size(), 1);
        Assert.assertEquals(map.get(consumeGroupName1).getMaxDiffTotal(), 200);

        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @After
    public void after() {
        File file = new File(filePath);
        File bakFile = new File(filePath + ".bak");
        if (file != null && file.exists()) {
            file.delete();
        }
        if (bakFile != null && bakFile.exists()) {
            bakFile.delete();
        }
    }

    @Override protected Object getTestController() {
        return monitorController;
    }
}
