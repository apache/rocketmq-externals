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

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.common.WillMessage;
import org.apache.rocketmq.mqtt.processor.MqttDisconnectMessageProcessor;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MqttDisconnectMessageProcessorTest {

    @Mock
    private RemotingChannel remotingChannel;

    @Mock
    private MqttBridgeController mqttBridgeController;

    private MqttDisconnectMessageProcessor mqttDisconnectMessageProcessor;

    @Before
    public void init() throws CloneNotSupportedException {
        MqttBridgeConfig mqttBridgeConfig = new MqttBridgeConfig();
        mqttBridgeConfig.setMqttClientConfig(new NettyClientConfig());
        mqttBridgeConfig.setMultiNettyServerConfig(new MultiNettyServerConfig());
        mqttBridgeController = new MqttBridgeController(mqttBridgeConfig);
        mqttDisconnectMessageProcessor = new MqttDisconnectMessageProcessor(mqttBridgeController);
    }

    @Test
    public void testProcessRequest() throws Exception {
        WillMessage willMessage = new WillMessage("willTopicTest","hello willMessage".getBytes(),false,0);
        MQTTSession client = new MQTTSession("123456", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60, mqttBridgeController);
        client.setWillMessage(willMessage);
        mqttBridgeController.getMqttClientManager().register(client);
//        mqttBridgeController.getWillMessageService().saveWillMessage(client.getClientId(), new WillMessage());
        MqttMessage mqttDisconnectMessage = new MqttMessage(new MqttFixedHeader(
            MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 200));

        mqttDisconnectMessageProcessor.processRequest(remotingChannel, mqttDisconnectMessage);
    }

    @Test
    public void testWrongFixedHead() {
        MqttMessage mqttWrongDisconnectMessage = new MqttMessage(new MqttFixedHeader(
            MqttMessageType.DISCONNECT, true, MqttQoS.AT_MOST_ONCE, false, 200));
        MqttMessage mqttMessage = mqttDisconnectMessageProcessor.processRequest(remotingChannel, mqttWrongDisconnectMessage);
        verify(remotingChannel).close();
        assert mqttMessage == null;
    }

    @Test
    public void testRemotingChannelalive() {
        WillMessage willMessage = new WillMessage("willTopicTest","hello willMessage".getBytes(),false,0);
        MQTTSession client = new MQTTSession("123456", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60, mqttBridgeController);
        client.setWillMessage(willMessage);
        mqttBridgeController.getMqttClientManager().register(client);
//        mqttBridgeController.getWillMessageService().saveWillMessage(client.getClientId(), new WillMessage());
        MqttMessage mqttDisconnectMessage = new MqttMessage(new MqttFixedHeader(
            MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 200));
        when(remotingChannel.isActive()).thenReturn(true);
        mqttDisconnectMessageProcessor.processRequest(remotingChannel, mqttDisconnectMessage);
        verify(remotingChannel).close();
    }
}
