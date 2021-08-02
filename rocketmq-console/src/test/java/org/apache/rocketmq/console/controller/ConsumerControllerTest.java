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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.model.request.ResetOffsetRequest;
import org.apache.rocketmq.console.service.impl.ConsumerServiceImpl;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ConsumerControllerTest extends BaseControllerTest {

    @InjectMocks
    private ConsumerController consumerController;

    @Spy
    private ConsumerServiceImpl consumerService;

    @Before
    public void init() throws Exception {
        super.mockRmqConfigure();
        ClusterInfo clusterInfo = MockObjectUtil.createClusterInfo();
        when(mqAdminExt.examineBrokerClusterInfo()).thenReturn(clusterInfo);
        SubscriptionGroupWrapper wrapper = MockObjectUtil.createSubscriptionGroupWrapper();
        when(mqAdminExt.getAllSubscriptionGroup(anyString(), anyLong())).thenReturn(wrapper);
        ConsumeStats stats = MockObjectUtil.createConsumeStats();
        when(mqAdminExt.examineConsumeStats(anyString())).thenReturn(stats);
        when(mqAdminExt.examineConsumeStats(anyString(), isNull())).thenReturn(stats);
        ConsumerConnection connection = MockObjectUtil.createConsumerConnection();
        when(mqAdminExt.examineConsumerConnectionInfo(anyString())).thenReturn(connection);
        ConsumerRunningInfo runningInfo = MockObjectUtil.createConsumerRunningInfo();
        when(mqAdminExt.getConsumerRunningInfo(anyString(), anyString(), anyBoolean()))
            .thenReturn(runningInfo);
        SubscriptionGroupConfig config = new SubscriptionGroupConfig();
        config.setGroupName("group-test");
        when(mqAdminExt.examineSubscriptionGroupConfig(anyString(), anyString()))
            .thenReturn(config);
    }

    @Test
    public void testList() throws Exception {
        final String url = "/consumer/groupList.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].group").value("group_test"))
            .andExpect(jsonPath("$.data[0].consumeType").value(ConsumeType.CONSUME_ACTIVELY.name()))
            .andExpect(jsonPath("$.data[0].messageModel").value(MessageModel.CLUSTERING.name()));
    }

    @Test
    public void testGroupQuery() throws Exception {
        final String url = "/consumer/group.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.group").value("group_test"))
            .andExpect(jsonPath("$.data.consumeType").value(ConsumeType.CONSUME_ACTIVELY.name()))
            .andExpect(jsonPath("$.data.messageModel").value(MessageModel.CLUSTERING.name()));
    }

    @Test
    public void testSkipAccumulate() throws Exception {
        final String url = "/consumer/skipAccumulate.do";
        resetOffsetOrSkipAccumulate(url, -1L);
    }

    @Test
    public void testResetOffset() throws Exception {
        final String url = "/consumer/resetOffset.do";
        resetOffsetOrSkipAccumulate(url, System.currentTimeMillis());
    }

    private void resetOffsetOrSkipAccumulate(String url, Long resetTime) throws Exception {
        RollbackStats rollbackStats = new RollbackStats();
        rollbackStats.setRollbackOffset(10L);
        rollbackStats.setQueueId(5L);
        rollbackStats.setBrokerName("broker-a");
        Map<MessageQueue, Long> rollbackStatsMap = new HashMap<>(0);
        rollbackStatsMap.put(new MessageQueue("topic_test", "broker-a", 5), 10L);
        {
            MQClientException exception = new MQClientException(ResponseCode.CONSUMER_NOT_ONLINE, "不在线");
            when(mqAdminExt.resetOffsetByTimestamp(anyString(), anyString(), anyLong(), anyBoolean()))
                .thenReturn(rollbackStatsMap).thenThrow(exception);
            when(mqAdminExt.resetOffsetByTimestampOld(anyString(), anyString(), anyLong(), anyBoolean()))
                .thenReturn(Lists.newArrayList(rollbackStats));
        }
        ResetOffsetRequest request = new ResetOffsetRequest();
        String groupId = "group_test";
        request.setTopic("topic_test");
        request.setResetTime(resetTime);
        request.setConsumerGroupList(Lists.newArrayList(groupId));
        // 1、consumer not online
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(request));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data." + groupId + ".rollbackStatsList").isArray())
            .andExpect(jsonPath("$.data." + groupId + ".rollbackStatsList[0].rollbackOffset").value(10L));

        // 2、consumer not online
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk()).andExpect(jsonPath("$.data").isMap());
    }

    @Test
    public void testFetchBrokerNameList() throws Exception {
        final String url = "/consumer/fetchBrokerNameList.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0]").value("broker-a"));
    }

    @Test
    public void testExamineSubscriptionGroupConfig() throws Exception {
        final String url = "/consumer/examineSubscriptionGroupConfig.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    public void testDelete() throws Exception {
        final String url = "/consumer/deleteSubGroup.do";
        {
            doNothing().when(mqAdminExt).deleteSubscriptionGroup(any(), anyString());
        }
        DeleteSubGroupRequest request = new DeleteSubGroupRequest();
        request.setBrokerNameList(Lists.newArrayList("broker-a"));
        request.setGroupName("group_test");
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(request));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testCreateOrUpdate() throws Exception {
        final String url = "/consumer/createOrUpdate.do";
        // 1、clusterName and brokerName all blank
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        ConsumerConfigInfo consumerConfigInfo = new ConsumerConfigInfo();
        requestBuilder.content(JSON.toJSONString(consumerConfigInfo));
        perform = mockMvc.perform(requestBuilder);
        performErrorExpect(perform);

        {
            doNothing().when(mqAdminExt).createAndUpdateSubscriptionGroupConfig(anyString(), any());
        }

        List<String> clusterNameList = Lists.newArrayList("DefaultCluster");
        SubscriptionGroupConfig config = new SubscriptionGroupConfig();
        config.setGroupName("group_test");
        consumerConfigInfo.setClusterNameList(clusterNameList);
        consumerConfigInfo.setSubscriptionGroupConfig(config);
        // 2、create consumer
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(consumerConfigInfo));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    public void testQueryConsumerByTopic() throws Exception {
        final String url = "/consumer/queryTopicByConsumer.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].queueStatInfoList", hasSize(2)));
    }

    @Test
    public void testConsumerConnection() throws Exception {
        final String url = "/consumer/consumerConnection.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.consumeType").value(ConsumeType.CONSUME_ACTIVELY.name()))
            .andExpect(jsonPath("$.data.messageModel").value(MessageModel.CLUSTERING.name()));
    }

    @Test
    public void testGetConsumerRunningInfo() throws Exception {
        final String url = "/consumer/consumerRunningInfo.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("consumerGroup", "group_test");
        requestBuilder.param("clientId", "group_test");
        requestBuilder.param("jstack", "true");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.jstack").value("test"));
    }

    @Override protected Object getTestController() {
        return consumerController;
    }
}
