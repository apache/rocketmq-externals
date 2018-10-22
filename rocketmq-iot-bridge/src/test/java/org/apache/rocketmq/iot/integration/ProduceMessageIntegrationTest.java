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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
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
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.iot.storage.subscription.impl.InMemorySubscriptionStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ProduceMessageIntegrationTest {

    private final String producerId = "test-client";
    private final String topicName = "test-topic";
    private final int packetId = 1;
    private EmbeddedChannel producerChannel;
    private MessageDispatcher dispatcher;
    private ClientManager clientManager;
    private SubscriptionStore subscriptionStore;
    private MqttConnectMessageHandler mqttConnectMessageHandler;
    private MqttMessageForwarder mqttMessageForwarder;
    private List<Subscription> mockedSubscriptions;
    private MqttClient mockedConsuemr;
    private ChannelHandlerContext mockedConsumerCtx;
    private EmbeddedChannel consumerChannel = new EmbeddedChannel();

    @Before

    public void setup() {
        /* start the mocked MQTTBridge */
        clientManager = new ClientManagerImpl();
        subscriptionStore = Mockito.spy(new InMemorySubscriptionStore());

        mqttConnectMessageHandler = new MqttConnectMessageHandler(clientManager);
        mqttMessageForwarder = new MqttMessageForwarder(subscriptionStore);

        dispatcher = new MessageDispatcher(clientManager);

        dispatcher.registerHandler(Message.Type.MQTT_CONNECT, mqttConnectMessageHandler);
        dispatcher.registerHandler(Message.Type.MQTT_PUBLISH, mqttMessageForwarder);

        producerChannel = new EmbeddedChannel(dispatcher);

        mockedConsuemr = Mockito.spy(new MqttClient());
        mockedConsumerCtx = Mockito.mock(ChannelHandlerContext.class);

        mockedSubscriptions = new ArrayList<>();
        mockedSubscriptions.add(
            Subscription.Builder.newBuilder()
                .client(mockedConsuemr)
                .qos(0)
                .build()
        );

        Mockito.when(mockedConsuemr.getCtx()).thenReturn(mockedConsumerCtx);
        Mockito.when(mockedConsumerCtx.writeAndFlush(Mockito.any(MqttPublishMessage.class))).then(new Answer<Object>() {
            @Override public Object answer(InvocationOnMock mock) throws Throwable {
                MqttPublishMessage publishMessage = (MqttPublishMessage) mock.getArguments()[0];
                consumerChannel.writeOutbound(publishMessage);
                return consumerChannel.newSucceededFuture();
            }
        });

    }

    @After
    public void teardown() {

    }

    @Test
    public void test() {
        /* handle the CONNECT message */
        MqttConnectMessage connectMessage = getConnectMessage();

        producerChannel.writeInbound(connectMessage);

        MqttConnAckMessage connAckMessage = producerChannel.readOutbound();

        Client client = clientManager.get(producerChannel);
        Assert.assertNotNull(client);
        Assert.assertEquals(producerId, client.getId());
        Assert.assertEquals(producerChannel, client.getCtx().channel());
        Assert.assertTrue(client.isConnected());
        Assert.assertTrue(producerChannel.isOpen());
        Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, connAckMessage.variableHeader().connectReturnCode());

        producerChannel.releaseInbound();

        /* handle the PUBLISH message when there is no online Consumers */
        MqttPublishMessage publishMessage = getMqttPublishMessage();

        producerChannel.writeInbound(publishMessage);

        MqttMessage pubackMessage = producerChannel.readOutbound();

        /* qos 0 should have no PUBACK message */
        Assert.assertNull(pubackMessage);
        /*
         * the message would be discarded simply because there is no Consumers
         * and the topic should be created
         * */
        Assert.assertTrue(subscriptionStore.hasTopic(topicName));
        producerChannel.releaseInbound();


        /* handle the PUBLISH message when there are online Consumers */

        Mockito.when(subscriptionStore.get(topicName)).thenReturn(mockedSubscriptions);

        publishMessage = getMqttPublishMessage();
        byte [] publishMessagePayload = new byte [publishMessage.payload().readableBytes()];
        publishMessage.payload().getBytes(0, publishMessagePayload);

        producerChannel.writeInbound(publishMessage);

        MqttPublishMessage forwardedMessage = consumerChannel.readOutbound();
        byte [] forwardedMessagePayload = new byte [forwardedMessage.payload().readableBytes()];
        forwardedMessage.payload().getBytes(0, forwardedMessagePayload);

        Assert.assertNotNull(forwardedMessage);
        Assert.assertEquals(publishMessage.variableHeader().topicName(), forwardedMessage.variableHeader().topicName());
        Assert.assertEquals(publishMessage.variableHeader().packetId(), forwardedMessage.variableHeader().packetId());
        Assert.assertArrayEquals(publishMessagePayload, forwardedMessagePayload);
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
            producerId,
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
            packetId
        );
        return new MqttPublishMessage(
            fixedHeader,
            variableHeader,
            Unpooled.buffer().writeBytes("hello world".getBytes())
        );
    }
}
