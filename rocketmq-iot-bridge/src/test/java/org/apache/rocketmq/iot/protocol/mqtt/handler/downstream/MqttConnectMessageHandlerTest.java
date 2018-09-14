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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.rmi.MarshalledObject;
import junit.framework.TestFailure;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.event.DisconnectChannelEvent;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttConnectMessageHandler;
import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;

public class MqttConnectMessageHandlerTest extends AbstractMqttMessageHandlerTest {

    private MqttConnectMessage connectMessage;
    private MqttConnAckMessage ackMessage;
    private ChannelInboundHandler mockedHandler;

    @Override public void setupMessage() {
        connectMessage = getConnectMessage();
        message.setType(Message.Type.MQTT_CONNECT);
        message.setPayload(connectMessage);
    }

    @Override public void assertConditions() {

    }

    @Override public void mock() {
        mockedHandler = Mockito.mock(ChannelInboundHandler.class);
        embeddedChannel.pipeline().addLast("mocked-handler", mockedHandler);
    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttConnectMessageHandler(clientManager);
    }

    @Override
    public void testHandleMessage() {
        /* handle legal message*/

        embeddedChannel.writeInbound(message);
        ackMessage = embeddedChannel.readOutbound();
        Assert.assertEquals(MqttConnectReturnCode.CONNECTION_ACCEPTED, ackMessage.variableHeader().connectReturnCode());
        Assert.assertTrue(embeddedChannel.isOpen());

        /* handle CONNECT message when the client has been already connected */
        embeddedChannel.writeInbound(message);
        Assert.assertNull(embeddedChannel.readOutbound());
        try {
            Mockito.verify(mockedHandler).userEventTriggered(Mockito.any(ChannelHandlerContext.class), Mockito.any(DisconnectChannelEvent.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            "test-client",
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
}
