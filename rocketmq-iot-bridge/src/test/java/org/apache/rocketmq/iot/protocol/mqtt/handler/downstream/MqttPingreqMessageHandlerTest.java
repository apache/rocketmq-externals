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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPingreqMessageHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttPingreqMessageHandlerTest {

    private ClientManager clientManager;
    private Client client;
    private ChannelHandlerContext ctx;
    private MqttPingreqMessageHandler handler;

    @Before
    public void setup() {
        clientManager = Mockito.mock(ClientManager.class);
        ctx = Mockito.mock(ChannelHandlerContext.class);
        client = Mockito.spy(new MqttClient());
        client.setConnected(true);
        handler = new MqttPingreqMessageHandler();

        Mockito.when(
            client.getCtx()
        ).thenReturn(
            ctx
        );
    }

    @After
    public void teardown() {
    }

    @Test
    public void testHandleMessage() {
        Message message = new Message();
        message.setClient(client);
        message.setPayload(getMqttPingreqMessage());

        handler.handleMessage(message);

        Mockito.verify(ctx).writeAndFlush(Mockito.any(MqttMessage.class));
    }

    private MqttMessage getMqttPingreqMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.PINGREQ,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        return new MqttMessage(
            fixedHeader,
            null,
            null
        );
    }
}
