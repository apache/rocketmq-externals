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

package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttMessageForwarder;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttMessageForwarderTest extends AbstractMqttMessageHandlerTest {

    private MqttClient client1;
    private MqttClient client2;
    private EmbeddedChannel channel1;
    private EmbeddedChannel channel2;
    private Subscription subscription1;
    private Subscription subscription2;
    private List<Subscription> subscriptions;
    private String topicName = "test/forward/topic";
    private int packetId = 1;
    private ByteBuf payload = Unpooled.wrappedBuffer("hello world".getBytes());

    @Before
    public void setup() {
        client1 = Mockito.spy(new MqttClient());
        channel1 = new EmbeddedChannel();
        client1.setCtx(channel1.pipeline().lastContext());

        client2 = Mockito.mock(MqttClient.class);
        channel2 = new EmbeddedChannel();
        client2.setCtx(channel2.pipeline().lastContext());

        subscription1 = Mockito.spy(Subscription.Builder.newBuilder().client(client1).build());
        subscription2 = Mockito.spy(Subscription.Builder.newBuilder().client(client2).build());
        subscriptions = new ArrayList<>();
        subscriptions.add(subscription1);
        subscriptions.add(subscription2);
    }

    @After
    public void teardown() {

    }

    @Override public void setupMessage() {
        message.setType(Message.Type.MQTT_PUBLISH);
        message.setPayload(getMqttPublishMessage());
    }

    @Override public void assertConditions() {
        MqttPublishMessage publishMessage1 = channel1.readOutbound();
        Assert.assertEquals(packetId, publishMessage1.variableHeader().packetId());
        Assert.assertEquals(topicName, publishMessage1.variableHeader().topicName());
        byte [] exptectedPayload = new byte [payload.readableBytes()];
        payload.getBytes(0, exptectedPayload);
        byte [] actualPayload1 = new byte [publishMessage1.payload().readableBytes()];
        publishMessage1.payload().getBytes(0, actualPayload1);
        Assert.assertArrayEquals(exptectedPayload, actualPayload1);

        MqttPublishMessage publishMessage2 = channel2.readOutbound();
        Assert.assertEquals(packetId, publishMessage2.variableHeader().packetId());
        Assert.assertEquals(topicName, publishMessage2.variableHeader().topicName());
        byte [] actualPayload2 = new byte [publishMessage1.payload().readableBytes()];
        publishMessage1.payload().getBytes(0, actualPayload2);
        Assert.assertArrayEquals(exptectedPayload, actualPayload2);
    }

    @Override public void mock() {
        Mockito.when(
            subscriptionStore.get(Mockito.anyString())
        ).thenReturn(
            subscriptions
        );
    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttMessageForwarder(subscriptionStore);
    }

    @Test
    public void testHandleMessage() {
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
            payload
        );
    }
}
