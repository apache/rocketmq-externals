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

package org.apache.rocketmq.console.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.MessageTraceView;
import org.apache.rocketmq.console.model.trace.MessageTraceGraph;
import org.apache.rocketmq.console.util.JsonUtil;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MessageTraceServiceImplTest {

    @InjectMocks
    private MessageTraceServiceImpl messageTraceService;
    @Mock
    private MQAdminExt mqAdminExt;
    @Mock
    private RMQConfigure rmqConfigure;

    private static final String TEST_MESSAGE_ID = "7F00000184E6512DDF170F0ADF770000";
    private static final String FAKE_SUBSCRIPTION_GROUP = "CID_JODIE";
    private static final String TEST_KEY = "TEST_KEY";
    private static final String PUB_TRACE = "Pub\u00011625246547213\u0001DefaultRegion\u0001ProducerGroupName\u0001" +
        "TraceProducerTopic\u0001" + TEST_MESSAGE_ID + "\u0001TagA\u0001" +
        "TestSuccess\u0001127.0.0.1:10911\u000111\u000114\u00011\u00017F00000100002A9F00000000004D1274\u0001" +
        "true\u0002EndTransaction\u00011625246547227\u0001DefaultRegion\u0001ProducerGroupName\u0001TraceProducerTopic" +
        "\u0001" + TEST_MESSAGE_ID + "\u0001TagA\u0001TestSuccess\u0001127.0.0.1:10911\u00012\u0001"
        + TEST_MESSAGE_ID + "\u0001COMMIT_MESSAGE\u0001false\u0002";
    private static final String SUB_TRACE1 = "SubAfter\u00017F0000013E70512DDF170A9835290029\u0001" +
        "7F0000013E9D512DDF170A9834F00037\u000124\u0001true\u0001TestSuccess\u00010\u00011625246547266" +
        "\u0001" + FAKE_SUBSCRIPTION_GROUP + "\u0002SubBefore\u00011625246547266\u0001null\u0001" +
        FAKE_SUBSCRIPTION_GROUP + "\u00017F0000013E70512DDF170A983542002C\u0001" + TEST_MESSAGE_ID +
        "\u00010\u0001TestSuccess\u0002";
    private static final String SUB_TRACE2 = "SubAfter\u00017F0000013E70512DDF170A983542002C\u0001" +
        TEST_MESSAGE_ID + "\u000122\u0001true\u0001TestSuccess\u00010\u00011625246547288\u0001" +
        FAKE_SUBSCRIPTION_GROUP + "\u0002SubBefore\u00011625246547293\u0001null\u0001" + FAKE_SUBSCRIPTION_GROUP +
        "\u00017F0000013E70512DDF170A98355D002F\u00017F0000013E9D512DDF170A98351B003F\u00010\u0001TestSuccess\u0002";
    private MessageExt fakeMessageExt;
    private MessageExt fakeMessageExt2;
    private MessageExt fakeMessageExt3;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(rmqConfigure.getMsgTrackTopicName()).thenReturn(null);
        fakeMessageExt = new MessageExt();
        fakeMessageExt.setKeys(Lists.newArrayList(TEST_KEY));
        fakeMessageExt.setBody(PUB_TRACE.getBytes(StandardCharsets.UTF_8));
        fakeMessageExt2 = new MessageExt();
        fakeMessageExt2.setKeys(Lists.newArrayList(TEST_KEY));
        fakeMessageExt2.setBody(SUB_TRACE1.getBytes(StandardCharsets.UTF_8));
        fakeMessageExt3 = new MessageExt();
        fakeMessageExt3.setKeys(Lists.newArrayList(TEST_KEY));
        fakeMessageExt3.setBody(SUB_TRACE2.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SneakyThrows
    public void queryMessageTraceKeyTest() {
        List<MessageExt> messageTraceList = Lists.newArrayList(fakeMessageExt);
        QueryResult queryResult = new QueryResult(1, messageTraceList);
        Mockito.when(mqAdminExt.queryMessage(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(queryResult);
        List<MessageTraceView> views = messageTraceService.queryMessageTraceKey(TEST_MESSAGE_ID);
        Assertions.assertEquals(1, views.size());
    }

    @Test
    @SneakyThrows
    public void queryMessageTraceKeyWithExceptionTest() {
        Mockito.when(mqAdminExt.queryMessage(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong())).thenThrow(new RuntimeException());
        Assertions.assertThrows(RuntimeException.class, () -> messageTraceService.queryMessageTraceKey(TEST_MESSAGE_ID));
    }

    @Test
    @SneakyThrows
    public void queryMessageTraceWithNoResultTest() {
        QueryResult queryResult = new QueryResult(1, Collections.emptyList());
        Mockito.when(mqAdminExt.queryMessage(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(queryResult);
        MessageTraceGraph messageTraceGraph = messageTraceService.queryMessageTraceGraph(TEST_MESSAGE_ID);
        Assertions.assertNotNull(messageTraceGraph);
        Assertions.assertEquals(0, messageTraceGraph.getMessageTraceViews().size());
    }

    @Test
    @SneakyThrows
    public void queryMessageTraceTest() {
        List<MessageExt> messageTraceList = Lists.newArrayList(fakeMessageExt, fakeMessageExt2, fakeMessageExt3);
        QueryResult queryResult = new QueryResult(1, messageTraceList);
        Mockito.when(mqAdminExt.queryMessage(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(queryResult);
        MessageTraceGraph messageTraceGraph = messageTraceService.queryMessageTraceGraph(TEST_MESSAGE_ID);
        Assertions.assertNotNull(messageTraceGraph);
        Assertions.assertEquals(TEST_MESSAGE_ID, messageTraceGraph.getProducerNode().getMsgId());
        Assertions.assertEquals(1, messageTraceGraph.getSubscriptionNodeList().size());
        Assertions.assertEquals(FAKE_SUBSCRIPTION_GROUP, messageTraceGraph.getSubscriptionNodeList()
            .get(0).getSubscriptionGroup());
        Assertions.assertEquals(3, messageTraceGraph.getMessageTraceViews().size());
        for (MessageTraceView view : messageTraceGraph.getMessageTraceViews()) {
            Assertions.assertEquals(0, view.getRetryTimes());
        }
    }
}