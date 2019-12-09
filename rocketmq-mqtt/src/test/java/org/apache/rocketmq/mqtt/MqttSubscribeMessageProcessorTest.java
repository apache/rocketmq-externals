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

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.internal.StringUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.persistence.service.DefaultPersistService;
import org.apache.rocketmq.mqtt.processor.MqttSubscribeMessageProcessor;
import org.apache.rocketmq.mqtt.service.EnodeService;
import org.apache.rocketmq.mqtt.service.NnodeService;
import org.apache.rocketmq.mqtt.service.impl.NnodeServiceImpl;
import org.apache.rocketmq.mqtt.service.impl.RemoteEnodeServiceImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;

@RunWith(MockitoJUnitRunner.class)
public class MqttSubscribeMessageProcessorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private MqttBridgeController mqttBridgeController;

    private MqttSubscribeMessageProcessor mqttSubscribeMessageProcessor;

    @Mock
    private RemotingChannel remotingChannel;

    @Before
    public void init() {
        mqttSubscribeMessageProcessor = new MqttSubscribeMessageProcessor(mqttBridgeController);
    }

    @Test
    public void test_topicStartWithWildcard() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = MqttSubscribeMessageProcessor.class.getDeclaredMethod("topicStartWithWildcard", List.class);
        method.setAccessible(true);

        List<MqttTopicSubscription> subscriptions1 = new ArrayList<>();
        subscriptions1.add(new MqttTopicSubscription("+/test", MqttQoS.AT_MOST_ONCE));
        boolean invoke1 = (boolean) method.invoke(mqttSubscribeMessageProcessor, subscriptions1);
        assert invoke1;

        List<MqttTopicSubscription> subscriptions2 = new ArrayList<>();
        subscriptions2.add(new MqttTopicSubscription("test/topic", MqttQoS.AT_MOST_ONCE));
        boolean invoke2 = (boolean) method.invoke(mqttSubscribeMessageProcessor, subscriptions2);
        assert !invoke2;

        List<MqttTopicSubscription> subscriptions3 = new ArrayList<>();
        subscriptions3.add(new MqttTopicSubscription("/test/topic", MqttQoS.AT_MOST_ONCE));
        boolean invoke3 = (boolean) method.invoke(mqttSubscribeMessageProcessor, subscriptions3);
        assert invoke3;
    }

    @Test
    public void test_handleMessage_wrongMessageType() throws Exception {
        MqttConnectMessage mqttConnectMessage = new MqttConnectMessage(new MqttFixedHeader(
            MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 200), new MqttConnectVariableHeader(null, 4, false, false, false, 0, false, false, 50), new MqttConnectPayload("abcd", "ttest", "message".getBytes(), "user", "password".getBytes()));

        exception.expect(WrongMessageTypeException.class);
        mqttSubscribeMessageProcessor.processRequest(remotingChannel, mqttConnectMessage);
    }

    @Test
    public void test_handleMessage_clientNotFound() throws Exception {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        subscriptions.add(new MqttTopicSubscription("test/a", MqttQoS.AT_MOST_ONCE));
        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(new MqttFixedHeader(
            MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 200), MqttMessageIdVariableHeader.from(1), new MqttSubscribePayload(subscriptions));

        MqttMessage mqttMessage = mqttSubscribeMessageProcessor.processRequest(remotingChannel, mqttSubscribeMessage);
        assert null == mqttBridgeController.getMqttClientManager().getClient(remotingChannel);
        assert mqttMessage == null;
    }

    @Test
    public void test_handleMessage_emptyTopicFilter() throws Exception {
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        MqttSubscribeMessage mqttSubscribeMessage = new MqttSubscribeMessage(new MqttFixedHeader(
            MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 200), MqttMessageIdVariableHeader.from(1), new MqttSubscribePayload(subscriptions));

        MQTTSession mqttSession = Mockito.mock(MQTTSession.class);
        Mockito.when(mqttSession.getRemotingChannel()).thenReturn(remotingChannel);
//        Mockito.when(mqttSubscribeMessage.toString()).thenReturn("toString");
        mqttBridgeController.getMqttClientManager().register(mqttSession);
        MqttMessage message = mqttSubscribeMessageProcessor.processRequest(remotingChannel, mqttSubscribeMessage);
        assertNotNull(mqttBridgeController.getMqttClientManager().getClient(remotingChannel));
        assert message == null;
    }

    @Test
    public void test_MqttSubscribePayload_toString() {
        List<MqttTopicSubscription> topicSubscriptions = new ArrayList<>();
        topicSubscriptions.add(new MqttTopicSubscription("test/topic", MqttQoS.AT_MOST_ONCE));

        StringBuilder builder = new StringBuilder(StringUtil.simpleClassName(this)).append('[');
        for (int i = 0; i <= topicSubscriptions.size() - 1; i++) {
            builder.append(topicSubscriptions.get(i)).append(", ");
        }
        if (builder.substring(builder.length() - 2).equals(", ")) {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append(']');
        System.out.println(builder.toString());
    }

    @Test
    public void test_initConsumeOffset() throws Exception {
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable = new ConcurrentHashMap<>();
        subscriptionDataTable.put("topic1/a", new MqttSubscriptionData("topic1/a", 0));
        subscriptionDataTable.put("topic1/b", new MqttSubscriptionData("topic1/b", 1));
        subscriptionDataTable.put("topic2/c", new MqttSubscriptionData("topic2/c", 0));
        subscriptionDataTable.put("topic2/d", new MqttSubscriptionData("topic2/d", 0));
        subscriptionDataTable.put("topic3/e", new MqttSubscriptionData("topic3/e", 1));
        subscriptionDataTable.put("topic3/f", new MqttSubscriptionData("topic3/f", 1));
        TopicRouteData topicRouteData = new TopicRouteData();
        List<BrokerData> brokerDataList = new ArrayList<>();
        brokerDataList.add(new BrokerData(null, "broker-a", null));
        brokerDataList.add(new BrokerData(null, "broker-b", null));
        topicRouteData.setBrokerDatas(brokerDataList);

        NnodeService nnodeService = Mockito.mock(NnodeServiceImpl.class);
        EnodeService enodeService = Mockito.mock(RemoteEnodeServiceImpl.class);
        DefaultPersistService persistService = Mockito.mock(DefaultPersistService.class);
        Mockito.when(this.mqttBridgeController.getNnodeService()).thenReturn(nnodeService);
        Mockito.when(this.mqttBridgeController.getEnodeService()).thenReturn(enodeService);
        Mockito.when(nnodeService.getTopicRouteDataByTopic(anyString(), anyBoolean())).thenReturn(topicRouteData);
        Mockito.when(enodeService.getMaxOffsetInQueue(anyString(), anyString(), anyInt(), anyObject())).thenReturn(100L);
        Mockito.when(this.mqttBridgeController.getPersistService()).thenReturn(persistService);
        MqttClientManagerImpl iotClientManager = new MqttClientManagerImpl(this.mqttBridgeController);
        Mockito.when(persistService.queryConsumeOffset(anyString())).thenReturn(-1L);
        Mockito.when(persistService.updateConsumeOffset(anyString(), anyLong())).thenReturn(true);
        Mockito.when(this.mqttBridgeController.getMqttClientManager()).thenReturn(iotClientManager);
//        mqttSubscribeMessageProcessor.initConsumeOffset(subscriptionDataTable, "testClientId");
        ConcurrentHashMap<String, Long[]> table = iotClientManager.getConsumeOffsetTable();
        assert table.get("broker-a^topic1^testClientId")[0] == 99L;
        assert table.get("broker-a^topic1^testClientId")[1] == 99L;
        assert table.get("broker-b^topic1^testClientId")[1] == 99L;
        assert table.get("broker-b^topic1^testClientId")[1] == 99L;
        assert !table.contains("broker-a^topic2^testClientId");
        assert !table.contains("broker-b^topic2^testClientId");
        assert table.get("broker-a^topic3^testClientId")[0] == 99L;
        assert table.get("broker-a^topic3^testClientId")[1] == 99L;
        assert table.get("broker-b^topic3^testClientId")[0] == 99L;
        assert table.get("broker-b^topic3^testClientId")[1] == 99L;
    }
}
