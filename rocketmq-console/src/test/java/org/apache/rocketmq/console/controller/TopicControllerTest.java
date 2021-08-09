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

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.impl.ConsumerServiceImpl;
import org.apache.rocketmq.console.service.impl.TopicServiceImpl;
import org.apache.rocketmq.console.util.MockObjectUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TopicControllerTest extends BaseControllerTest {

    @InjectMocks
    private TopicController topicController;

    @Spy
    private TopicServiceImpl topicService;

    @Spy
    private ConsumerServiceImpl consumerService;

    private String topicName = "topic_test";

    @Before
    public void init() {
        super.mockRmqConfigure();
    }

    @Test
    public void testList() throws Exception {
        {
            // mock all topics
            TopicList topicList = new TopicList();
            Set<String> topicSet = new HashSet<>();
            topicSet.add("common_topic1");
            topicSet.add("common_topic2");
            topicSet.add("system_topic1");
            topicSet.add("system_topic2");
            topicList.setTopicList(topicSet);
            when(mqAdminExt.fetchAllTopicList()).thenReturn(topicList);
            // mock system topics
            TopicList sysTopicList = new TopicList();
            Set<String> sysTopicSet = new HashSet<>();
            sysTopicSet.add("system_topic1");
            sysTopicSet.add("system_topic2");
            sysTopicList.setTopicList(sysTopicSet);
            DefaultMQProducer producer = mock(DefaultMQProducer.class);
            doNothing().when(producer).start();
            doNothing().when(producer).shutdown();
            DefaultMQProducerImpl defaultMQProducer = mock(DefaultMQProducerImpl.class);
            MQClientInstance mqClientInstance = mock(MQClientInstance.class);
            MQClientAPIImpl mqClientAPIImpl = mock(MQClientAPIImpl.class);
            when(producer.getDefaultMQProducerImpl()).thenReturn(defaultMQProducer);
            when(defaultMQProducer.getmQClientFactory()).thenReturn(mqClientInstance);
            when(mqClientInstance.getMQClientAPIImpl()).thenReturn(mqClientAPIImpl);
            when(mqClientAPIImpl.getSystemTopicList(anyLong())).thenReturn(sysTopicList);
            doReturn(producer).when(topicService).buildDefaultMQProducer(anyString(), any(), anyBoolean());
        }
        final String url = "/topic/list.query";

        // 1、list all topic
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("skipSysProcess", String.valueOf(true));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.topicList", hasSize(4)));

        // 2、filter system topic
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.topicList[2]").value("%SYS%system_topic1"));
    }

    @Test
    public void testStat() throws Exception {
        {
            TopicStatsTable topicStatsTable = MockObjectUtil.createTopicStatsTable();
            when(mqAdminExt.examineTopicStats(anyString())).thenReturn(topicStatsTable);
        }
        final String url = "/topic/stats.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        performOkExpect(perform);
    }

    @Test
    public void testRoute() throws Exception {
        {
            TopicRouteData topicRouteData = MockObjectUtil.createTopicRouteData();
            when(mqAdminExt.examineTopicRouteInfo(anyString())).thenReturn(topicRouteData);
        }
        final String url = "/topic/route.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        performOkExpect(perform);
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        final String url = "/topic/createOrUpdate.do";

        // 1、clusterName and brokerName all blank
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        TopicConfigInfo info = new TopicConfigInfo();
        requestBuilder.content(JSON.toJSONString(info));
        perform = mockMvc.perform(requestBuilder);
        performErrorExpect(perform);

        {
            ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
            when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
            doNothing().when(mqAdminExt).createAndUpdateTopicConfig(anyString(), any());
        }

        List<String> clusterNameList = Lists.newArrayList("DefaultCluster");
        info.setTopicName("topic_test");
        info.setReadQueueNums(4);
        info.setWriteQueueNums(4);
        info.setPerm(6);
        info.setClusterNameList(clusterNameList);
        // 2、create topic
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(info));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testExamineTopicConfig() throws Exception {
        final String url = "/topic/examineTopicConfig.query";
        {
            TopicRouteData topicRouteData = MockObjectUtil.createTopicRouteData();
            when(mqAdminExt.examineTopicRouteInfo(anyString())).thenReturn(topicRouteData);
            ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
            when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
            when(mqAdminExt.examineTopicConfig(anyString(), anyString())).thenReturn(new TopicConfig(topicName));
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].topicName").value(topicName));
    }

    @Test
    public void testQueryConsumerByTopic() throws Exception {
        final String url = "/topic/queryConsumerByTopic.query";
        {
            GroupList list = new GroupList();
            list.setGroupList(Sets.newHashSet("group1"));
            when(mqAdminExt.queryTopicConsumeByWho(anyString())).thenReturn(list);
            ConsumeStats consumeStats = MockObjectUtil.createConsumeStats();
            when(mqAdminExt.examineConsumeStats(anyString(), anyString())).thenReturn(consumeStats);
            when(mqAdminExt.examineConsumerConnectionInfo(anyString())).thenReturn(new ConsumerConnection());
            when(mqAdminExt.getConsumerRunningInfo(anyString(), anyString(), anyBoolean())).thenReturn(new ConsumerRunningInfo());
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    public void testQueryTopicConsumerInfo() throws Exception {
        final String url = "/topic/queryTopicConsumerInfo.query";
        {
            GroupList list = new GroupList();
            list.setGroupList(Sets.newHashSet("group1", "group2", "group3"));
            when(mqAdminExt.queryTopicConsumeByWho(anyString())).thenReturn(list);
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupList", hasSize(3)));
    }

    @Test
    public void testSendTopicMessage() throws Exception {
        final String url = "/topic/sendTopicMessage.do";
        {
            DefaultMQProducer producer = mock(DefaultMQProducer.class);
            doNothing().when(producer).start();
            doNothing().when(producer).shutdown();
            SendResult result = new SendResult(SendStatus.SEND_OK, "7F000001E41A2E5D6D978B82C20F003D",
                "0A8E83C300002A9F00000000000013D3", new MessageQueue(), 1000L);
            when(producer.send(any(Message.class))).thenReturn(result);
            doReturn(producer).when(topicService).buildDefaultMQProducer(anyString(), any(), anyBoolean());
        }

        SendTopicMessageRequest request = new SendTopicMessageRequest();
        request.setTopic(topicName);
        request.setMessageBody("hello world");
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(request));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sendStatus").value(SendStatus.SEND_OK.name()))
            .andExpect(jsonPath("$.data.msgId").value("7F000001E41A2E5D6D978B82C20F003D"));

    }

    @Test
    public void testDelete() throws Exception {
        final String url = "/topic/deleteTopic.do";
        {
            ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
            when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
            doNothing().when(mqAdminExt).deleteTopicInBroker(any(), anyString());
            doNothing().when(mqAdminExt).deleteTopicInNameServer(any(), anyString());
        }

        // 1、clusterName is blank
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("topic", topicName);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));

        // 2、clusterName is not blank
        requestBuilder.param("clusterName", "DefaultCluster");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testDeleteTopicByBroker() throws Exception {
        {
            ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
            when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
            doNothing().when(mqAdminExt).deleteTopicInBroker(any(), anyString());
        }
        final String url = "/topic/deleteTopicByBroker.do";
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("topic", topicName);
        requestBuilder.param("brokerName", "broker-a");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Override protected Object getTestController() {
        return topicController;
    }
}
