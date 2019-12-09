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

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.processor.MqttPublishMessageProcessor;
import org.apache.rocketmq.mqtt.service.NnodeService;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class MqttPublishMessageProcessorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Mock
    private NnodeService nnodeService;

    private MqttBridgeController mqttBridgeController;

    MqttPublishMessageProcessor mqttPublishMessageProcessor;

    @Mock
    private RemotingChannel remotingChannel;

    private MqttMessage mqttPublishMessage;

    @Before
    public void before() throws CloneNotSupportedException {
        mqttBridgeController = new MqttBridgeController(new MqttBridgeConfig());
        mqttPublishMessageProcessor = new MqttPublishMessageProcessor(mqttBridgeController);
    }

    @Test
    public void test_processRequest_wrongMessageType() throws Exception {
        MqttMessage mqttMessage = new MqttConnectMessage(new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 10), new MqttConnectVariableHeader("name", 0, false, false, false, 1, false, false, 10), new MqttConnectPayload("client1", null, (byte[]) null, null, null));

        exception.expect(WrongMessageTypeException.class);
        mqttPublishMessageProcessor.processRequest(remotingChannel, mqttMessage);
    }

    @Test
    public void test_processRequest_wrongQos() throws Exception {
        mqttPublishMessage = new MqttPublishMessage(new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.FAILURE, false, 10), new MqttPublishVariableHeader("topicTest", 1), Unpooled.copiedBuffer("Hello".getBytes()));

        Mockito.when(remotingChannel.toString()).thenReturn("testRemotingChannel");
        MqttMessage message = mqttPublishMessageProcessor.processRequest(remotingChannel, mqttPublishMessage);
        assert message == null;
        Mockito.verify(remotingChannel).close();
    }

    @Test
    public void test_processRequest_qos0() throws Exception {
        prepareSubscribeData();
        mqttPublishMessage = new MqttPublishMessage(new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 10), new MqttPublishVariableHeader("topic/c", 1), Unpooled.copiedBuffer("Hello".getBytes()));
        Mockito.when(remotingChannel.toString()).thenReturn("testRemotingChannel");
        MqttMessage mqttMessage = mqttPublishMessageProcessor.processRequest(remotingChannel, mqttPublishMessage);
        assert mqttMessage == null;
    }

    @Test
    public void test_processRequest_qos1() throws Exception {
        prepareSubscribeData();
        mqttPublishMessage = new MqttPublishMessage(new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 10), new MqttPublishVariableHeader("topic/c", 1), Unpooled.copiedBuffer("Hello".getBytes()));
        Mockito.when(remotingChannel.toString()).thenReturn("testRemotingChannel");
        TopicRouteData topicRouteData = buildTopicRouteData();
        Mockito.when(nnodeService.getTopicRouteDataByTopic(anyString(), anyBoolean())).thenReturn(topicRouteData);
        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        Mockito.when(this.mqttBridgeController.getEnodeService().sendMessage(anyString(), any(RemotingCommand.class))).thenReturn(future);
//        RemotingCommand response = Mockito.mock(RemotingCommand.class);
        Mockito.when(this.mqttBridgeController.getEnodeService().sendMessage(anyString(), any(RemotingCommand.class))).thenReturn(future);
//        doAnswer(mock -> future.complete(response)).when(this.defaultMqttMessageProcessor.getEnodeService().sendMessage(any(RemotingChannel.class), anyString(), any(RemotingCommand.class)));
        MqttMessage mqttMessage = mqttPublishMessageProcessor.processRequest(remotingChannel, mqttPublishMessage);
        assert mqttMessage != null;
        assertEquals(1, ((MqttMessageIdVariableHeader) mqttMessage.variableHeader()).messageId());
        assertEquals(MqttQoS.AT_MOST_ONCE, mqttMessage.fixedHeader().qosLevel());
        assertEquals(false, mqttMessage.fixedHeader().isDup());
        assertEquals(false, mqttMessage.fixedHeader().isRetain());
        assertEquals(2, mqttMessage.fixedHeader().remainingLength());
    }

    private TopicRouteData buildTopicRouteData() {
        TopicRouteData topicRouteData = new TopicRouteData();
        List<BrokerData> brokerDataList = new ArrayList<>();
        brokerDataList.add(new BrokerData("DefaultCluster", "broker1", null));
        topicRouteData.setBrokerDatas(brokerDataList);
        return topicRouteData;
    }

    private void prepareSubscribeData() {
        MqttClientManagerImpl manager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        ConcurrentHashMap<String, Set<MQTTSession>> topic2Clients = manager.getTopic2Clients();

        ConcurrentHashMap<String, MqttSubscriptionData> table1 = new ConcurrentHashMap<>();
        table1.put("topic/a", new MqttSubscriptionData("topic/a", 1));
        table1.put("topic/+", new MqttSubscriptionData("topic/+", 1));

        ConcurrentHashMap<String, MqttSubscriptionData> table2 = new ConcurrentHashMap<>();
        table2.put("topic/b", new MqttSubscriptionData("topic/b", 1));
        table2.put("topic/+", new MqttSubscriptionData("topic/+", 1));

        ConcurrentHashMap<String, MqttSubscriptionData> table3 = new ConcurrentHashMap<>();
        table3.put("test/c", new MqttSubscriptionData("topic/c", 1));
        table3.put("test/d", new MqttSubscriptionData("topic/d", 1));

        Set<MQTTSession> clients_1 = new HashSet<>();
        MQTTSession client1 = new MQTTSession("client1", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60, mqttBridgeController);
        client1.setMqttSubscriptionDataTable(table1);
        clients_1.add(client1);
        MQTTSession client2 = new MQTTSession("client2", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60, mqttBridgeController);
        client2.setMqttSubscriptionDataTable(table2);
        clients_1.add(client2);

        Set<MQTTSession> clients_2 = new HashSet<>();
        MQTTSession client3 = new MQTTSession("client3", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60, mqttBridgeController);
        client3.setMqttSubscriptionDataTable(table3);
        clients_1.add(client3);

        topic2Clients.put("topic", clients_1);
        topic2Clients.put("test", clients_2);

    }
}
