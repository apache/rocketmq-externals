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

package org.apache.rocketmq.mqtt;

import io.netty.handler.codec.mqtt.*;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.persistence.service.DefaultPersistService;
import org.apache.rocketmq.mqtt.processor.MqttUnsubscribeMessageProcessor;
import org.apache.rocketmq.mqtt.service.NnodeService;
import org.apache.rocketmq.mqtt.service.impl.NnodeServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class MqttUnsubscribeMessageProcessorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private MqttBridgeController mqttBridgeController;

    private MqttUnsubscribeMessageProcessor mqttUnsubscribeMessageProcessor;

    @Mock
    private RemotingChannel remotingChannel;

    @Before
    public void init() {
        mqttUnsubscribeMessageProcessor = new MqttUnsubscribeMessageProcessor(mqttBridgeController);
    }

    @Test
    public void test_processRequest() throws Exception {

        TopicRouteData topicRouteData = new TopicRouteData();
        List<BrokerData> brokerDataList = new ArrayList<>();
        brokerDataList.add(new BrokerData(null, "broker-a", null));
        brokerDataList.add(new BrokerData(null, "broker-b", null));
        topicRouteData.setBrokerDatas(brokerDataList);

        MQTTSession mqttSession = Mockito.spy(new MQTTSession("testClientId", true, true, remotingChannel, 0, null, null, 10, mqttBridgeController));
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable = new ConcurrentHashMap<>();
        subscriptionDataTable.put("topic1/a", new MqttSubscriptionData("topic1/a", 0));
        subscriptionDataTable.put("topic1/b", new MqttSubscriptionData("topic1/b", 1));
        subscriptionDataTable.put("topic2/c", new MqttSubscriptionData("topic2/c", 0));
        subscriptionDataTable.put("topic2/d", new MqttSubscriptionData("topic2/d", 0));
        subscriptionDataTable.put("topic3/e", new MqttSubscriptionData("topic3/e", 1));
        subscriptionDataTable.put("topic3/f", new MqttSubscriptionData("topic3/f", 1));
        mqttSession.setMqttSubscriptionDataTable(subscriptionDataTable);

        NnodeService nnodeService = Mockito.mock(NnodeServiceImpl.class);
        DefaultPersistService persistService = Mockito.mock(DefaultPersistService.class);
        Mockito.when(this.mqttBridgeController.getPersistService()).thenReturn(persistService);
        MqttClientManagerImpl iotClientManager = new MqttClientManagerImpl(this.mqttBridgeController);
        iotClientManager.register(mqttSession);
        Mockito.when(persistService.deleteConsumeOffset(anyString())).thenReturn(true);
//        Mockito.when(persistService.dele(any(), any())).thenReturn(true);
        Mockito.when(this.mqttBridgeController.getNnodeService()).thenReturn(nnodeService);
        Mockito.when(nnodeService.getTopicRouteDataByTopic(anyString(), anyBoolean())).thenReturn(topicRouteData);
        Mockito.when(this.mqttBridgeController.getMqttClientManager()).thenReturn(iotClientManager);
        MqttBridgeConfig mqttBridgeConfig = new MqttBridgeConfig();
        Mockito.when(this.mqttBridgeController.getMqttBridgeConfig()).thenReturn(mqttBridgeConfig);
//        Mockito.when(iotClientManager.getClient(remotingChannel)).thenReturn(mqttSession);
        Set<MQTTSession> mqttSessionSet = new HashSet<>();
        mqttSessionSet.add(mqttSession);
        ConcurrentHashMap<String, Set<MQTTSession>> topic2Clients = iotClientManager.getTopic2Clients();
        topic2Clients.put("topic1", mqttSessionSet);
        topic2Clients.put("topic2", mqttSessionSet);
        topic2Clients.put("topic3", mqttSessionSet);
//        Mockito.when(iotClientManager.getTopic2Clients()).thenReturn(topic2Clients);
        ConcurrentHashMap<String, Long[]> consumeOffsetTable = iotClientManager.getConsumeOffsetTable();
        consumeOffsetTable.put("broker-a^topic1^testClientId", new Long[] {100L, 100L});
        consumeOffsetTable.put("broker-a^topic3^testClientId", new Long[] {90L, 90L});
        consumeOffsetTable.put("broker-b^topic1^testClientId", new Long[] {80L, 80L});
        consumeOffsetTable.put("broker-b^topic3^testClientId", new Long[] {70L, 70L});
//        Mockito.when(iotClientManager.getConsumeOffsetTable()).thenReturn(consumeOffsetTable);
        List<String> topics = new ArrayList<>();
        topics.add("topic1/b");
        topics.add("topic3/f");
        MqttUnsubscribeMessage mqttUnsubscribeMessage = new MqttUnsubscribeMessage(new MqttFixedHeader(
            MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0), MqttMessageIdVariableHeader.from(1), new MqttUnsubscribePayload(topics));
        MqttUnsubAckMessage message = (MqttUnsubAckMessage) mqttUnsubscribeMessageProcessor.processRequest(remotingChannel, mqttUnsubscribeMessage);
        assertNotNull(message);

        ConcurrentHashMap<String, Long[]> table = iotClientManager.getConsumeOffsetTable();
        assert table.get("broker-a^topic1^testClientId") == null;
        assert table.get("broker-b^topic1^testClientId") == null;
        assert table.get("broker-a^topic3^testClientId")[0] == 90L;
        assert table.get("broker-a^topic3^testClientId")[1] == 90L;
        assert table.get("broker-b^topic3^testClientId")[0] == 70L;
        assert table.get("broker-b^topic3^testClientId")[1] == 70L;
    }
}
