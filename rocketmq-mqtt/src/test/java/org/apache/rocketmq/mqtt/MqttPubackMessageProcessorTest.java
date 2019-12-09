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

import io.netty.handler.codec.mqtt.*;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.processor.MqttPubackMessageProcessor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class MqttPubackMessageProcessorTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private MqttBridgeController mqttBridgeController;
    private MqttPubackMessageProcessor mqttPubackMessageProcessor;

    @Mock
    private RemotingChannel remotingChannel;

    @Before
    public void before() throws CloneNotSupportedException {
        mqttBridgeController = Mockito.spy(new MqttBridgeController(new MqttBridgeConfig()));
        mqttPubackMessageProcessor = new MqttPubackMessageProcessor(mqttBridgeController);
    }

    @Test
    public void test_handleMessage_wrongMessageType() {
        MqttMessage mqttMessage = new MqttConnectMessage(new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 10), new MqttConnectVariableHeader("name", 0, false, false, false, 1, false, false, 10), new MqttConnectPayload("client1", null, (byte[]) null, null, null));

        exception.expect(WrongMessageTypeException.class);
        mqttPubackMessageProcessor.processRequest(remotingChannel, mqttMessage);
    }

    @Test
    public void test_processRequest() {
        MqttMessage mqttMessage = new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 10), MqttMessageIdVariableHeader.from(1));
        MqttClientManagerImpl iotClientManager = Mockito.mock(MqttClientManagerImpl.class);
        Mockito.when((MqttClientManagerImpl) mqttBridgeController.getMqttClientManager()).thenReturn(iotClientManager);
        MQTTSession mqttSession = Mockito.spy(new MQTTSession("client1", true, true, remotingChannel, System.currentTimeMillis(), null, "127.0.0.1", 60,mqttBridgeController));
        Mockito.when(iotClientManager.getClient(any(RemotingChannel.class))).thenReturn(mqttSession);
        Message message = Mockito.spy(new Message("topicTest", 0, "broker-a", null, "Hello".getBytes()));
        doReturn(message).when(mqttSession).pubAckReceived(anyInt());
        MqttMessage mqttMessage1 = mqttPubackMessageProcessor.processRequest(remotingChannel, mqttMessage);
        assert mqttMessage1 == null;
    }
}
