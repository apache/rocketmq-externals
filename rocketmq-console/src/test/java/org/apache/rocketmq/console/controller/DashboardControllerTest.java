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

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.console.service.impl.DashboardCollectServiceImpl;
import org.apache.rocketmq.console.service.impl.DashboardServiceImpl;
import org.apache.rocketmq.console.util.JsonUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DashboardControllerTest extends BaseControllerTest {

    @InjectMocks
    private DashboardController dashboardController;

    @Spy
    private DashboardServiceImpl dashboardService;

    @Spy
    private DashboardCollectServiceImpl dashboardCollectService;

    private String nowDateStr;

    private String yesterdayDateStr;

    private File topicDataFile;

    private File brokerDataFile;

    @Before
    public void init() throws Exception {
        super.mockRmqConfigure();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        nowDateStr = format.format(new Date());
        yesterdayDateStr = format.format(new Date((System.currentTimeMillis() - 24 * 60 * 60 * 1000)));
        // generate today's brokerData and topicData cache file
        brokerDataFile = this.createBrokerTestCollectDataFile(nowDateStr);
        topicDataFile = this.createTopicTestCollectDataFile(nowDateStr);
        when(configure.getConsoleCollectData()).thenReturn("");
    }

    @After
    public void after() {
        // delete test file
        if (brokerDataFile != null && brokerDataFile.exists()) {
            brokerDataFile.delete();
        }
        if (topicDataFile != null && topicDataFile.exists()) {
            topicDataFile.delete();
        }
    }

    @Test
    public void testBroker() throws Exception {
        final String url = "/dashboard/broker.query";

        // 1、no broker cache data
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", yesterdayDateStr);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data").isEmpty());

        // 2、the broker's data is cached locally
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", nowDateStr);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(jsonPath("$.data", hasKey("broker-a:0")))
            .andExpect(jsonPath("$.data.broker-a:0").isArray())
            .andExpect(jsonPath("$.data.broker-a:0", hasSize(100)));

    }

    @Test
    public void testTopic() throws Exception {
        final String url = "/dashboard/topic.query";
        // 1、topicName is empty
        // 1.1、no topic cache data
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", yesterdayDateStr);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data").isEmpty());

        // 1.2、the topic's data is cached locally
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", nowDateStr);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data").isNotEmpty())
            .andExpect(jsonPath("$.data", hasKey("topic_test")))
            .andExpect(jsonPath("$.data.topic_test").isArray())
            .andExpect(jsonPath("$.data.topic_test", hasSize(100)));

        // 2、topicName is not empty
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", nowDateStr);
        requestBuilder.param("topicName", "topic_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data", hasSize(100)));

        // 2、topicName is not empty but the no topic cache data
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("date", nowDateStr);
        requestBuilder.param("topicName", "topic_test1");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk());

    }

    @Test
    public void testTopicCurrent() throws Exception {
        final String url = "/dashboard/topicCurrent";
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("topic_test,100"));
    }

    @Override
    protected Object getTestController() {
        return dashboardController;
    }

    private File createBrokerTestCollectDataFile(String date) throws Exception {
        File brokerFile = new File(date + ".json");
        brokerFile.createNewFile();
        Map<String /*brokerName:brokerId*/, List<String/*timestamp,tps*/>> resultMap = Maps.newHashMap();
        List<String> brokerData = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            BigDecimal tps = new BigDecimal(i).divide(BigDecimal.valueOf(10), 3, BigDecimal.ROUND_HALF_UP);
            brokerData.add((new Date().getTime() + i * 60 * 1000) + "," + tps.toString());
        }
        resultMap.put("broker-a:0", brokerData);
        Files.write(JsonUtil.obj2String(resultMap).getBytes(), brokerFile);
        return brokerFile;
    }

    private File createTopicTestCollectDataFile(String date) throws Exception {
        File topicFile = new File(date + "_topic" + ".json");
        topicFile.createNewFile();
        Map<String /*topicName*/, List<String/*timestamp,inTps,inMsgCntToday,outTps,outMsgCntToday*/>> resultMap = Maps.newHashMap();
        List<String> topicData = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            String inTps = new BigDecimal(i).divide(BigDecimal.valueOf(10), 3, BigDecimal.ROUND_HALF_UP).toString();
            String outTps = inTps;
            StringBuilder sb = new StringBuilder();
            sb.append((new Date().getTime() + i * 60 * 1000))
                .append(',').append(inTps)
                .append(',').append(i)
                .append(',').append(outTps)
                .append(',').append(i);
            topicData.add(sb.toString());
        }
        resultMap.put("topic_test", topicData);
        Files.write(JsonUtil.obj2String(resultMap).getBytes(), topicFile);
        return topicFile;
    }
}
