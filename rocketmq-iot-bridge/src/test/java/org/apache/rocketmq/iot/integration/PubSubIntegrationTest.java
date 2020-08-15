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
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.connection.client.impl.ClientManagerImpl;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
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

public class PubSubIntegrationTest {

    private final String producerId = "test-producer-id";
    private final String consumerId = "test-consumer-id";
    private final String topicName = "test-topic";
    private final int producerPublishId = 2;
    private final int consumerSubscribeId = 4;
    private ClientManager clientManager;
    private SubscriptionStore subscriptionStore;
    private MqttClient producer;
    private MqttClient consumer;
    private EmbeddedChannel producerChannel;
    private EmbeddedChannel consumerChannel;
    private MessageDispatcher messageDispatcher;
    private MqttConnectMessageHandler mqttConnectMessageHandler;
    private MqttMessageForwarder mqttMessageForwarder;
    private MqttSubscribeMessageHandler mqttSubscribeMessageHandler;
    private List<MqttTopicSubscription> topicSubscriptions = new ArrayList<>();

    @Before
    public void setup() {
        clientManager = new ClientManagerImpl();
        subscriptionStore = new InMemorySubscriptionStore();

        messageDispatcher = new MessageDispatcher(clientManager);
        mqttConnectMessageHandler = new MqttConnectMessageHandler(clientManager);
        mqttSubscribeMessageHandler = new MqttSubscribeMessageHandler(subscriptionStore);
        mqttMessageForwarder = new MqttMessageForwarder(subscriptionStore);

        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mqttConnectMessageHandler);
        messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, mqttMessageForwarder);
        messageDispatcher.registerHandler(Message.Type.MQTT_SUBSCRIBE, mqttSubscribeMessageHandler);

        producer = Mockito.spy(new MqttClient());
        producer.setId(producerId);

        consumer = Mockito.spy(new MqttClient());
        consumer.setId(consumerId);

        producerChannel = new EmbeddedChannel(messageDispatcher);
        producer.setCtx(producerChannel.pipeline().context("producer-ctx"));

        consumerChannel = new EmbeddedChannel(messageDispatcher);
        consumer.setCtx(consumerChannel.pipeline().context("consumer-ctx"));

        topicSubscriptions.add(new MqttTopicSubscription(topicName, MqttQoS.AT_MOST_ONCE));
//        Mockito.when(consumerCtx.writeAndFlush(Mockito.any(MqttMessage.class))).then(
//            new Answer<Object>() {
//                @Override public Object answer(InvocationOnMock mock) throws Throwable {
//                    MqttMessage message = (MqttMessage) mock.getArguments()[0];
//                    consumerChannel.writeOutbound(message);
//                    return consumerChannel.newSucceededFuture();
//                }
//            }
//        );
    }

    @After
    public void teardown() {

    }

    @Test
    public void test() {
        /* the consumer connect and subscribe */

        /* handle CONNECT message from consumer */
        MqttConnectMessage consuemrConnectMessage = getConnectMessage(consumerId);
        consumerChannel.writeInbound(consuemrConnectMessage);

        MqttClient managedConsuemr = (MqttClient) clientManager.get(consumerChannel);

        MqttConnAckMessage consumerConnAckMessage = consumerChannel.readOutbound();

        Assert.assertNotNull(managedConsuemr);
        Assert.assertEquals(consumerId, managedConsuemr.getId());
        Assert.assertTrue(managedConsuemr.isConnected());
        Assert.assertTrue(consumerChannel.isOpen());
        Assert.assertEquals(consumerId, consuemrConnectMessage.payload().clientIdentifier());
        Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, consumerConnAckMessage.variableHeader().connectReturnCode());
        Assert.assertEquals(consuemrConnectMessage.variableHeader().isCleanSession(), consumerConnAckMessage.variableHeader().isSessionPresent());

        consumerChannel.releaseInbound();

        /* handle SUBSCRIBE message from consumer*/
        MqttSubscribeMessage consumerSubscribeMessage = getMqttSubscribeMessage();
        consumerChannel.writeInbound(consumerSubscribeMessage);

        MqttSubAckMessage consuemrSubAckMessage = consumerChannel.readOutbound();

        Assert.assertNotNull(consuemrSubAckMessage);
        Assert.assertEquals(consumerSubscribeMessage.variableHeader().messageId(), consuemrSubAckMessage.variableHeader().messageId());
        Assert.assertEquals(topicSubscriptions.size(), consuemrSubAckMessage.payload().grantedQoSLevels().size());

        consumerChannel.releaseInbound();

        /* the producer publish message to the topic */

        /* handle CONNECT message from producer */
        MqttConnectMessage producerConnectMessage = getConnectMessage(producerId);

        producerChannel.writeInbound(producerConnectMessage);

        MqttConnAckMessage producerConnAckMessage = producerChannel.readOutbound();
        MqttClient managedProducer = (MqttClient) clientManager.get(producerChannel);

        Assert.assertNotNull(managedProducer);
        Assert.assertNotNull(producerConnAckMessage);
        Assert.assertTrue(managedProducer.isConnected());
        Assert.assertTrue(producerChannel.isOpen());
        Assert.assertEquals(producerId, managedProducer.getId());
        Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, producerConnAckMessage.variableHeader().connectReturnCode());
        Assert.assertEquals(producerConnectMessage.variableHeader().isCleanSession(), producerConnAckMessage.variableHeader().isSessionPresent());

        producerChannel.releaseInbound();

        /* handle PUBLISH message from producer */
        MqttPublishMessage producerPublishMessage = getMqttPublishMessage();

        byte [] expectedPayload = new byte[producerPublishMessage.payload().readableBytes()];
        producerPublishMessage.payload().getBytes(0, expectedPayload);

        producerChannel.writeInbound(producerPublishMessage);

        MqttPublishMessage consumerReceivedMessage = consumerChannel.readOutbound();
        byte [] actualPayload = new byte[consumerReceivedMessage.payload().readableBytes()];
        consumerReceivedMessage.payload().getBytes(0, actualPayload);


        Assert.assertNotNull(consumerReceivedMessage);
        Assert.assertEquals(producerPublishMessage.variableHeader().packetId(), consumerReceivedMessage.variableHeader().packetId());
        Assert.assertEquals(producerPublishMessage.variableHeader().topicName(), consumerReceivedMessage.variableHeader().topicName());
        Assert.assertArrayEquals(expectedPayload, actualPayload);

    }

    private MqttConnectMessage getConnectMessage(String clientId) {
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
            clientId,
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
            producerPublishId
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
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(consumerSubscribeId);
        MqttSubscribePayload payload = new MqttSubscribePayload(
            topicSubscriptions
        );
        return new MqttSubscribeMessage(
            fixedHeader,
            variableHeader,
            payload
        );
    }
}
