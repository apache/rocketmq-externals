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

package org.apache.rocketmq.iot.integration;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.connection.client.impl.ClientManagerImpl;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageDispatcher;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttConnectMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttMessageForwarder;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttSubscribeMessageHandler;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.iot.storage.subscription.impl.InMemorySubscriptionStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ConsumeMessageIntegrationTest {

    private ClientManager clientManager;
    private SubscriptionStore subscriptionStore;
    private MessageDispatcher messageDispatcher;
    private MqttConnectMessageHandler mqttConnectMessageHandler;
    private MqttSubscribeMessageHandler mqttSubscribeMessageHandler;
    private MqttMessageForwarder mqttMessageForwarder;
    private EmbeddedChannel embeddedChannel;
    private MqttClient consuemr;
    private ChannelHandlerContext consuermCtx;

    private final String consumerId = "test-consumer-id";
    private final String topicName = "test-topic";
    private final int subscribePacketId = 1;
    private final int publishPakcetId = 2;
    private List<MqttTopicSubscription> subscriptions;

    @Before
    public void setup() {
        clientManager = new ClientManagerImpl();
        subscriptionStore = new InMemorySubscriptionStore();

        messageDispatcher = new MessageDispatcher(clientManager);

        mqttConnectMessageHandler = new MqttConnectMessageHandler(clientManager);
        mqttSubscribeMessageHandler = new MqttSubscribeMessageHandler(subscriptionStore);
        mqttMessageForwarder = new MqttMessageForwarder(subscriptionStore);

        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mqttConnectMessageHandler);
        messageDispatcher.registerHandler(Message.Type.MQTT_SUBSCRIBE, mqttSubscribeMessageHandler);
        messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, mqttMessageForwarder);

        embeddedChannel = new EmbeddedChannel(messageDispatcher);

        subscriptions = new ArrayList<>();
        subscriptions.add(new MqttTopicSubscription(topicName, MqttQoS.AT_MOST_ONCE));

        consuemr = Mockito.spy(new MqttClient());
        consuermCtx = Mockito.mock(ChannelHandlerContext.class);

        Mockito.when(consuemr.getCtx()).thenReturn(consuermCtx);
    }

    @After
    public void teardown() {

    }

    @Test
    public void test() {
        /* handle the CONNECT message */
        MqttConnectMessage connectMessage = getConnectMessage();

        embeddedChannel.writeInbound(connectMessage);

        MqttConnAckMessage connAckMessage = embeddedChannel.readOutbound();
        Client client = clientManager.get(embeddedChannel);

        Assert.assertNotNull(client);
        Assert.assertTrue(consumerId.equals(client.getId()));
        Assert.assertEquals(consumerId, client.getId());
        Assert.assertTrue(client.isConnected());
        Assert.assertEquals(embeddedChannel, client.getCtx().channel());
        Assert.assertTrue(client.isConnected());
        Assert.assertTrue(embeddedChannel.isOpen());
        Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, connAckMessage.variableHeader().connectReturnCode());

        embeddedChannel.releaseInbound();

        /* handle the SUBSCRIBE message */
        MqttSubscribeMessage subscribeMessage = getMqttSubscribeMessage();

        embeddedChannel.writeInbound(subscribeMessage);

        List<Subscription> subscriptions = subscriptionStore.get(topicName);
        List<String> topics = subscriptionStore.getTopics(topicName);
        MqttSubAckMessage ackMessage = embeddedChannel.readOutbound();

        Assert.assertNotNull(subscriptionStore.get(topicName));
        Assert.assertNotNull(subscriptions);
        Assert.assertNotNull(ackMessage);
        Assert.assertEquals(subscribePacketId, ackMessage.variableHeader().messageId());
        Assert.assertTrue(topics.contains(topicName));
        Assert.assertTrue(isClientInSubscriptions(subscriptions, client));

        embeddedChannel.releaseInbound();

        /* send message to the client */
        MqttPublishMessage publishMessage = getMqttPublishMessage();
        byte [] expectedPayload = new byte[publishMessage.payload().readableBytes()];
        publishMessage.payload().getBytes(0, expectedPayload);

        embeddedChannel.writeInbound(publishMessage);

        MqttPublishMessage receivedMessage = embeddedChannel.readOutbound();
        byte [] actualPayload = new byte[receivedMessage.payload().readableBytes()];
        receivedMessage.payload().getBytes(0, actualPayload);

        Assert.assertEquals(publishMessage.variableHeader().packetId(), receivedMessage.variableHeader().packetId());
        Assert.assertEquals(publishMessage.variableHeader().topicName(), receivedMessage.variableHeader().topicName());
        Assert.assertArrayEquals(expectedPayload, actualPayload);
    }

private MqttConnectMessage getConnectMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.CONNECT,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
            "MQTT",
            4,
            false,
            false,
            false,
            MqttQoS.AT_MOST_ONCE.value(),
            true,
            true,
            60
        );
        MqttConnectPayload payload = new MqttConnectPayload(
            consumerId,
            "test-will-topic",
            "the test client is down".getBytes(),
            null,
            null
        );
        return new MqttConnectMessage(
            fixedHeader,
            variableHeader,
            payload
        );
    }

    private MqttPublishMessage getMqttPublishMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.PUBLISH,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(
            topicName,
            publishPakcetId
        );
        return new MqttPublishMessage(
            fixedHeader,
            variableHeader,
            Unpooled.buffer().writeBytes("hello world".getBytes())
        );
    }

    private MqttSubscribeMessage getMqttSubscribeMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.SUBSCRIBE,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(subscribePacketId);
        MqttSubscribePayload payload = new MqttSubscribePayload(
            subscriptions
        );
        return new MqttSubscribeMessage(
            fixedHeader,
            variableHeader,
            payload
        );
    }

    private boolean isClientInSubscriptions(List<Subscription> subscriptions, Client client) {
        if (subscriptions == null || subscriptions.isEmpty()) {
            return false;
        }
        for (Subscription subscription: subscriptions) {
            if (subscription.getClient() == client) {
                return true;
            }
        }
        return false;
    }
}
