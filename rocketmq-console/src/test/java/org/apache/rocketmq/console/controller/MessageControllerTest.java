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
import com.google.common.cache.Cache;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.CMResult;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.console.model.QueueOffsetInfo;
import org.apache.rocketmq.console.model.request.MessageQuery;
import org.apache.rocketmq.console.service.impl.MessageServiceImpl;
import org.apache.rocketmq.console.util.MockObjectUtil;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.apache.rocketmq.tools.admin.api.TrackType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MessageControllerTest extends BaseControllerTest {

    @InjectMocks
    private MessageController messageController;

    @Spy
    private MessageServiceImpl messageService;

    private Set<MessageQueue> messageQueues;

    private DefaultMQPullConsumer defaultMQPullConsumer;

    @Before
    public void init() throws Exception {
        super.mockRmqConfigure();
        {
            List<MessageExt> wrappers = new ArrayList<>(1);
            wrappers.add(MockObjectUtil.createMessageExt());
            defaultMQPullConsumer = mock(DefaultMQPullConsumer.class);
            messageQueues = new HashSet<>(1);
            MessageQueue messageQueue = new MessageQueue("topic_test", "broker-a", 0);
            messageQueues.add(messageQueue);
            when(defaultMQPullConsumer.fetchSubscribeMessageQueues(anyString())).thenReturn(messageQueues);
            when(defaultMQPullConsumer.searchOffset(messageQueue, Long.MIN_VALUE)).thenReturn(Long.MIN_VALUE);
            when(defaultMQPullConsumer.searchOffset(messageQueue, Long.MAX_VALUE)).thenReturn(Long.MAX_VALUE - 10L);
            PullResult pullResult = mock(PullResult.class);
            when(defaultMQPullConsumer.pull(any(), anyString(), anyLong(), anyInt())).thenReturn(pullResult);
            when(pullResult.getNextBeginOffset()).thenReturn(Long.MAX_VALUE);
            when(pullResult.getPullStatus()).thenReturn(PullStatus.FOUND);
            when(pullResult.getMsgFoundList()).thenReturn(wrappers);
            when(messageService.buildDefaultMQPullConsumer(any(), anyBoolean())).thenReturn(defaultMQPullConsumer);
        }
    }

    @Test
    public void testViewMessage() throws Exception {
        final String url = "/message/viewMessage.query";
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
                .thenThrow(new MQBrokerException(206, "consumer not online"))
                .thenReturn(tracks);
        }
        // no message
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", "topic_test");
        requestBuilder.param("msgId", "0A9A003F00002A9F0000000000000319");
        perform = mockMvc.perform(requestBuilder);
        performErrorExpect(perform);

        // consumer not online
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messageView.msgId").value("0A9A003F00002A9F0000000000000319"))
            .andExpect(jsonPath("$.data.messageTrackList", hasSize(0)));

        // query message success and has a group consumed.
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.messageView.msgId").value("0A9A003F00002A9F0000000000000319"))
            .andExpect(jsonPath("$.data.messageTrackList", hasSize(1)))
            .andExpect(jsonPath("$.data.messageTrackList[0].consumerGroup").value("group_test"))
            .andExpect(jsonPath("$.data.messageTrackList[0].trackType").value(TrackType.CONSUMED.name()));
    }

    @Test
    public void testQueryMessagePageByTopic() throws Exception {
        final String url = "/message/queryMessagePageByTopic.query";
        MessageQuery query = new MessageQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setTopic("topic_test");
        query.setTaskId("");
        query.setBegin(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000);
        query.setEnd(System.currentTimeMillis());

        // missed cache
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.contentType(MediaType.APPLICATION_JSON_UTF8);
        requestBuilder.content(JSON.toJSONString(query));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page.content", hasSize(1)))
            .andExpect(jsonPath("$.data.page.content[0].msgId").value("0A9A003F00002A9F0000000000000319"));

        String taskId = MessageClientIDSetter.createUniqID();
        {
            List<QueueOffsetInfo> queueOffsetInfos = new ArrayList<>();
            int idx = 0;
            for (MessageQueue messageQueue : messageQueues) {
                Long minOffset = defaultMQPullConsumer.searchOffset(messageQueue, query.getBegin());
                Long maxOffset = defaultMQPullConsumer.searchOffset(messageQueue, query.getEnd()) + 1;
                queueOffsetInfos.add(new QueueOffsetInfo(idx++, minOffset, maxOffset, minOffset, minOffset, messageQueue));
            }
            // Use reflection to add data to the CACHE
            Field field = MessageServiceImpl.class.getDeclaredField("CACHE");
            field.setAccessible(true);
            Cache<String, List<QueueOffsetInfo>> cache = (Cache<String, List<QueueOffsetInfo>>) field.get(messageService);
            cache.put(taskId, queueOffsetInfos);
        }

        // hit cache
        query.setTaskId(taskId);
        requestBuilder.content(JSON.toJSONString(query));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.page.content", hasSize(1)))
            .andExpect(jsonPath("$.data.page.content[0].msgId").value("0A9A003F00002A9F0000000000000319"));
    }

    @Test
    public void testQueryMessageByTopicAndKey() throws Exception {
        final String url = "/message/queryMessageByTopicAndKey.query";
        {
            List<MessageExt> messageList = new ArrayList<>(2);
            messageList.add(MockObjectUtil.createMessageExt());
            QueryResult queryResult = new QueryResult(System.currentTimeMillis(), messageList);
            when(mqAdminExt.queryMessage(anyString(), anyString(), anyInt(), anyLong(), anyLong()))
                .thenReturn(queryResult);
        }
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", "topic_test");
        requestBuilder.param("key", "KeyA");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].msgId").value("0A9A003F00002A9F0000000000000319"));
    }

    @Test
    public void testQueryMessageByTopic() throws Exception {
        final String url = "/message/queryMessageByTopic.query";
        requestBuilder = MockMvcRequestBuilders.get(url);
        requestBuilder.param("topic", "topic_test")
            .param("begin", Long.toString(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000))
            .param("end", Long.toString(System.currentTimeMillis()));
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(1)))
            .andExpect(jsonPath("$.data[0].msgId").value("0A9A003F00002A9F0000000000000319"));
    }

    @Test
    public void testConsumeMessageDirectly() throws Exception {
        final String url = "/message/consumeMessageDirectly.do";
        {
            ConsumeMessageDirectlyResult result1 = new ConsumeMessageDirectlyResult();
            result1.setConsumeResult(CMResult.CR_SUCCESS);
            ConsumeMessageDirectlyResult result2 = new ConsumeMessageDirectlyResult();
            result2.setConsumeResult(CMResult.CR_LATER);
            when(mqAdminExt.consumeMessageDirectly(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(result1).thenReturn(result2);
            ConsumerConnection consumerConnection = MockObjectUtil.createConsumerConnection();
            when(mqAdminExt.examineConsumerConnectionInfo(anyString()))
                .thenReturn(consumerConnection);
        }

        // clientId is not empty
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("topic", "topic_test")
            .param("consumerGroup", "group_test")
            .param("msgId", "0A9A003F00002A9F0000000000000319")
            .param("clientId", "127.0.0.1@37540#2295913058176000");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.consumeResult").value(CMResult.CR_SUCCESS.name()));

        // clientId is empty
        requestBuilder = MockMvcRequestBuilders.post(url);
        requestBuilder.param("topic", "topic_test")
            .param("consumerGroup", "group_test")
            .param("msgId", "0A9A003F00002A9F0000000000000319");
        perform = mockMvc.perform(requestBuilder);
        perform.andExpect(status().isOk())
            .andExpect(jsonPath("$.data.consumeResult").value(CMResult.CR_LATER.name()));
    }

    @Override protected Object getTestController() {
        return messageController;
    }
}
