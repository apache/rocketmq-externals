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

import lombok.SneakyThrows;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.console.config.RMQConfigure;
import org.apache.rocketmq.console.model.MessageTraceView;
import org.apache.rocketmq.console.model.trace.MessageTraceGraph;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

public class MessageTraceServiceImplTest {

    @InjectMocks
    private MessageTraceServiceImpl messageTraceService;
    @Mock
    private MQAdminExt mqAdminExt;
    @Mock
    private RMQConfigure rmqConfigure;

    private static final String TEST_MESSAGE_ID = "7F0000016D48512DDF172E788E5E0000";
    private static final String FAKE_SUBSCRIPTION_GROUP = "CID_JODIE";
    private static final String TEST_KEY = "TEST_KEY";
    private static final String PUB_TRACE = "Pub\u00011625848452706\u0001DefaultRegion\u0001sendGroup\u0001" +
        "TopicTraceTest\u0001" + TEST_MESSAGE_ID + "\u0001TagA\u0001OrderID188\u0001" +
        "192.168.0.101:10911\u000111\u000117\u00010\u0001C0A8006500002A9F0000000000003866\u0001true\u0002";
    private static final String SUB_TRACE1 = "SubBefore\u00011625848452722\u0001null\u0001" + FAKE_SUBSCRIPTION_GROUP +
        "\u00017F0000016801512DDF172E788E720014\u0001" + TEST_MESSAGE_ID + "\u00010\u0001OrderID188\u0002";
    private static final String SUB_TRACE2 = "SubAfter\u00017F0000016801512DDF172E788E720014\u0001" + TEST_MESSAGE_ID +
        "\u000140\u0001true\u0001OrderID188\u00010\u0002";
    private static final String END_TRANSACTION_TRACE = "EndTransaction\u00011625913838389\u0001DefaultRegion\u0001" +
        FAKE_SUBSCRIPTION_GROUP + "\u0001TopicTraceTest\u0001" + TEST_MESSAGE_ID +
        "\u0001TagA\u0001OrderID188\u0001192.168.0.101:10911\u00012\u00017F000001ACFE512DDF17325DBAEA0000" +
        "\u0001UNKNOW\u0001true\u0002";
    private MessageExt fakeMessageExt;
    private MessageExt fakeMessageExt2;
    private MessageExt fakeMessageExt3;
    private MessageExt fakeMessageExt4;

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
        fakeMessageExt4 = new MessageExt();
        fakeMessageExt4.setKeys(Lists.newArrayList(TEST_KEY));
        fakeMessageExt4.setBody(END_TRANSACTION_TRACE.getBytes(StandardCharsets.UTF_8));
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
        List<MessageExt> messageTraceList = Lists.newArrayList(fakeMessageExt, fakeMessageExt2, fakeMessageExt3, fakeMessageExt4);
        QueryResult queryResult = new QueryResult(1, messageTraceList);
        Mockito.when(mqAdminExt.queryMessage(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyInt(), Mockito.anyLong(), Mockito.anyLong())).thenReturn(queryResult);
        MessageTraceGraph messageTraceGraph = messageTraceService.queryMessageTraceGraph(TEST_MESSAGE_ID);
        Assertions.assertNotNull(messageTraceGraph);
        Assertions.assertEquals(TEST_MESSAGE_ID, messageTraceGraph.getProducerNode().getMsgId());
        Assertions.assertEquals(1, messageTraceGraph.getSubscriptionNodeList().size());
        Assertions.assertEquals(FAKE_SUBSCRIPTION_GROUP, messageTraceGraph.getSubscriptionNodeList()
            .get(0).getSubscriptionGroup());
        Assertions.assertEquals(4, messageTraceGraph.getMessageTraceViews().size());
        Assertions.assertEquals(1, messageTraceGraph.getProducerNode().getTransactionNodeList().size());
        Assertions.assertEquals(LocalTransactionState.UNKNOW.name(),
            messageTraceGraph.getProducerNode().getTransactionNodeList().get(0).getTransactionState());
        for (MessageTraceView view : messageTraceGraph.getMessageTraceViews()) {
            Assertions.assertEquals(0, view.getRetryTimes());
        }
    }
}