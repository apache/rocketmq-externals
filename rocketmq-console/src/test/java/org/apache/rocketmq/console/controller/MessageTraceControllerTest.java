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
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.trace.TraceType;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.topic.TopicValidator;
import org.apache.rocketmq.console.service.impl.MessageServiceImpl;
import org.apache.rocketmq.console.service.impl.MessageTraceServiceImpl;
import org.apache.rocketmq.console.util.MockObjectUtil;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.apache.rocketmq.tools.admin.api.TrackType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MessageTraceControllerTest extends BaseControllerTest {

    @InjectMocks
    private MessageTraceController messageTraceController;

    @Spy
    private MessageServiceImpl messageService;

    @Spy
    private MessageTraceServiceImpl messageTraceService;

    @Before
    public void init() throws MQClientException, InterruptedException {
        super.mockRmqConfigure();
        when(configure.getMsgTrackTopicNameOrDefault()).thenReturn(TopicValidator.RMQ_SYS_TRACE_TOPIC);
        List<MessageExt> messageList = new ArrayList<>(2);
        MessageExt messageExt = MockObjectUtil.createMessageExt();
        messageExt.setBody(MockObjectUtil.createTraceData().getBytes());
        messageList.add(messageExt);
        QueryResult queryResult = new QueryResult(System.currentTimeMillis(), messageList);
        when(mqAdminExt.queryMessage(anyString(), anyString(), anyInt(), anyLong(), anyLong()))
            .thenReturn(queryResult);
    }

    @Test
    public void testViewMessage() throws Exception {
        final String url = "/messageTrace/viewMessage.query";
        {
            MessageExt messageExt = MockObjectUtil.createMessageExt();
            when(mqAdminExt.viewMessage(anyString(), anyString()))
                .thenThrow(new MQClientException(208, "no message"))
                .thenReturn(messageExt);
            MessageTrack track = new MessageTrack();
            track.setConsumerGroup("group_test");
            track.setTrackType(TrackType.CONSUMED);
            List<MessageTrack> tracks = new ArrayList<>();
            tracks.add(track);
            when(mqAdminExt.messageTrackDetail(any()))
                .thenReturn(tracks);
        }
        // no message
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", "topic_test");
        requestBuilder.param("msgId", "0A9A003F00002A9F0000000000000319");
        perform = mockMvc.perform(requestBuilder);
        performErrorExpect(perform);

        // query message success
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messageView.topic").value("topic_test"))
            .andExpect(jsonPath("$.data.messageView.msgId").value("0A9A003F00002A9F0000000000000319"));
    }

    @Test
    public void testViewMessageTraceDetail() throws Exception {
        final String url = "/messageTrace/viewMessageTraceDetail.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("msgId", "0A9A003F00002A9F0000000000000319");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(4)))
            .andExpect(jsonPath("$.data[0].traceType").value(TraceType.Pub.name()))
            .andExpect(jsonPath("$.data[1].traceType").value(TraceType.SubBefore.name()))
            .andExpect(jsonPath("$.data[2].traceType").value(TraceType.SubAfter.name()))
            .andExpect(jsonPath("$.data[3].traceType").value(TraceType.EndTransaction.name()));
    }

    @Test
    public void testViewMessageTraceGraph() throws Exception {
        final String url = "/messageTrace/viewMessageTraceGraph.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("msgId", "0A9A003F00002A9F0000000000000319");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isMap())
            .andExpect(jsonPath("$.data.producerNode.groupName").value("PID_test"))
            .andExpect(jsonPath("$.data.subscriptionNodeList", hasSize(1)))
            .andExpect(jsonPath("$.data.subscriptionNodeList[0].subscriptionGroup").value("group_test"))
            .andExpect(jsonPath("$.data.messageTraceViews").isArray())
            .andExpect(jsonPath("$.data.messageTraceViews", hasSize(4)));
    }

    @Override protected Object getTestController() {
        return messageTraceController;
    }
}
