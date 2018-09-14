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
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.awt.color.CMMException;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttDisconnectMessageHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttDisconnectMessageHandlerTest extends AbstractMqttMessageHandlerTest {

    @Override public void setupMessage() {
        message.setType(Message.Type.MQTT_DISCONNECT);
        message.setPayload(getMqttDisconnectMessage());
    }

    @Override public void assertConditions() {
        Mockito.verify(clientManager).remove(Mockito.any(Channel.class));
        Assert.assertFalse(embeddedChannel.isOpen());
    }

    @Override public void mock() {

    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttDisconnectMessageHandler(clientManager);
    }

    private MqttMessage getMqttDisconnectMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.DISCONNECT,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        return new MqttMessage(fixedHeader, null, null);
    }

}
