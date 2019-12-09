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
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.processor.MqttConnectMessageProcessor;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

public class MqttConnectMessageProcessorTest {

    @Mock
    private RemotingChannel remotingChannel;

    @Mock
    private MqttBridgeController mqttBridgeController;

    private MqttConnectMessage mqttConnectMessage ;

    private MqttConnectMessageProcessor mqttConnectMessageProcessor;

    NettyRemotingClient remotingClient = null;

    @Before
    public void init() throws Exception {
        MqttBridgeConfig mqttBridgeConfig = new MqttBridgeConfig();
        mqttBridgeConfig.setMqttClientConfig(new NettyClientConfig());
        mqttBridgeConfig.setMultiNettyServerConfig(new MultiNettyServerConfig());
        //MQTTSession client = new MQTTSession("123456", true, true, remotingChannel, System.currentTimeMillis(), null, mqttBridgeController);
        mqttBridgeController = new MqttBridgeController(mqttBridgeConfig);
        mqttBridgeController.getMqttClientManager().register(null);
        mqttConnectMessage = new MqttConnectMessage(new MqttFixedHeader(
            MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 200), new MqttConnectVariableHeader(null, 4, false, false, false, 0, false, false, 50),
            new MqttConnectPayload("abcd", "ttest", "message".getBytes(), "user", "password".getBytes()));
        mqttConnectMessageProcessor = new MqttConnectMessageProcessor(mqttBridgeController);
    }

    @Test
    public void testProcessRequest() throws Exception {
        this.mqttBridgeController = new MqttBridgeController(new MqttBridgeConfig());
        MqttConnectMessageProcessor processor = new MqttConnectMessageProcessor(mqttBridgeController);
        processor.processRequest(remotingChannel, mqttConnectMessage);
    }
}
