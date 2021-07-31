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

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.console.service.checker.RocketMqChecker;
import org.apache.rocketmq.console.service.checker.impl.ClusterHealthCheckerImpl;
import org.apache.rocketmq.console.service.checker.impl.TopicOnlyOneBrokerCheckerImpl;
import org.apache.rocketmq.console.service.impl.OpsServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OpsControllerTest extends BaseControllerTest {
    @InjectMocks
    private OpsController opsController;

    @Spy
    private OpsServiceImpl opsService;

    @Before
    public void init() {
        super.mockRmqConfigure();
    }

    @Test
    public void testHomePage() throws Exception {
        final String url = "/ops/homePage.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data.useVIPChannel").value(false))
            .andExpect(jsonPath("$.data.namesvrAddrList").isArray())
            .andExpect(jsonPath("$.data.namesvrAddrList", hasSize(1)))
            .andExpect(jsonPath("$.data.namesvrAddrList[0]").value("127.0.0.1:9876"));
    }

    @Test
    public void testUpdateNameSvrAddr() throws Exception {
        final String url = "/ops/updateNameSvrAddr.do";
        {
            doNothing().when(configure).setNamesrvAddr(anyString());
        }
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("nameSvrAddrList", "127.0.0.1:9876");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        Assert.assertEquals(configure.getNamesrvAddr(), "127.0.0.1:9876");
    }

    @Test
    public void testUpdateIsVIPChannel() throws Exception {
        final String url = "/ops/updateIsVIPChannel.do";
        {
            doNothing().when(configure).setIsVIPChannel(anyString());
        }
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("useVIPChannel", "true");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testClusterStatus() throws Exception {
        final String url = "/ops/rocketMqStatus.query";
        {
            List<RocketMqChecker> rocketMqCheckerList = new ArrayList<>();
            rocketMqCheckerList.add(new ClusterHealthCheckerImpl());
            rocketMqCheckerList.add(new TopicOnlyOneBrokerCheckerImpl());
            ReflectionTestUtils.setField(opsService, "rocketMqCheckerList", rocketMqCheckerList);
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data.CLUSTER_HEALTH_CHECK").isEmpty())
            .andExpect(jsonPath("$.data.TOPIC_ONLY_ONE_BROKER_CHECK").isEmpty());
    }

    @Override
    protected Object getTestController() {
        return opsController;
    }

}