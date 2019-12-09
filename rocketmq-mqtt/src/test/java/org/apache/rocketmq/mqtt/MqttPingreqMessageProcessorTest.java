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

import io.netty.handler.codec.mqtt.MqttMessage;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.processor.MqttPingreqMessageProcessor;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MqttPingreqMessageProcessorTest {
    @Mock
    private RemotingChannel remotingChannel;
    @Mock
    private MqttClientManagerImpl iotClientManager;
    @Mock
    private MqttMessage mqttMessage;
    @Mock
    private MQTTSession client;
    @Mock
    private MqttBridgeController mqttBridgeController;

    private MqttPingreqMessageProcessor mqttPingreqMessageProcessor = new MqttPingreqMessageProcessor(mqttBridgeController);

    @Before
    public void init() throws CloneNotSupportedException {
        mqttBridgeController = new MqttBridgeController(new MqttBridgeConfig());
        when(mqttBridgeController.getMqttClientManager()).thenReturn(iotClientManager);
        when(iotClientManager.getClient(remotingChannel)).thenReturn(client);
        when(client.getClientId()).thenReturn("Mock Client");
    }

    @Test
    public void testHandlerMessageReturnResp() {
        when(client.isConnected()).thenReturn(true);
        MqttMessage message = mqttPingreqMessageProcessor.processRequest(remotingChannel, mqttMessage);
        verify(client).setLastUpdateTimestamp(anyLong());
        assert message != null;
    }

    @Test
    public void testHandlerMessageReturnNull() {
        when(client.isConnected()).thenReturn(false);
        MqttMessage message = mqttPingreqMessageProcessor.processRequest(remotingChannel, mqttMessage);
        assert message == null;
    }
}
