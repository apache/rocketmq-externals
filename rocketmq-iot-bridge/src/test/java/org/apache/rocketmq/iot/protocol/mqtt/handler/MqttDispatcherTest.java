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

package org.apache.rocketmq.iot.protocol.mqtt.handler;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.common.util.MessageUtil;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttDispatcherTest {

    private MessageDispatcher messageDispatcher;
    private ClientManager clientManager;
    private MessageHandler mockedConnectMessageHandler;
    private MessageHandler mockedDisconnectMessageHandler;


    @Before
    public void setup() {
        clientManager = Mockito.mock(ClientManager.class);
        messageDispatcher = new MessageDispatcher(clientManager);
        mockedConnectMessageHandler = Mockito.mock(MessageHandler.class);
        mockedDisconnectMessageHandler = Mockito.mock(MessageHandler.class);
    }

    @After public void teardown() {

    }

    @Test
    public void testRegisterHandler() throws IllegalAccessException {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mockedConnectMessageHandler);
        messageDispatcher.registerHandler(Message.Type.MQTT_DISCONNECT, mockedDisconnectMessageHandler);

        Map<Message.Type, MessageHandler> type2handler = (Map<Message.Type, MessageHandler>) FieldUtils.getField(MessageDispatcher.class, "type2handler", true).get(messageDispatcher);

        Assert.assertEquals(mockedConnectMessageHandler, type2handler.get(Message.Type.MQTT_CONNECT));
        Assert.assertEquals(mockedDisconnectMessageHandler, type2handler.get(Message.Type.MQTT_DISCONNECT));
    }

    public void testChanelRead0 () {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mockedConnectMessageHandler);

        MqttConnectMessage mockedConnectMessage = Mockito.mock(MqttConnectMessage.class);

        Message mockedMessage = Mockito.spy(new Message());

        Mockito.when(MessageUtil.getMessage(mockedConnectMessage)).thenReturn(mockedMessage);

        EmbeddedChannel embeddedChannel = new EmbeddedChannel(messageDispatcher);

        embeddedChannel.writeInbound(mockedConnectMessage);

        Mockito.verify(mockedConnectMessageHandler).handleMessage(mockedMessage);

    }
}
