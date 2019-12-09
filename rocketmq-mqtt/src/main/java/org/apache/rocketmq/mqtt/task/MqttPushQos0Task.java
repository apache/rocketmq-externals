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
package org.apache.rocketmq.mqtt.task;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPushQos0Task implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttPushQos0Task.class);
    private final MqttBridgeController mqttBridgeController;
    private MqttPublishVariableHeader variableHeader;
    private byte[] body;

    public MqttPushQos0Task(MqttBridgeController mqttBridgeController,
        MqttPublishVariableHeader mqttPublishVariableHeader, byte[] body) {
        this.mqttBridgeController = mqttBridgeController;
        this.variableHeader = mqttPublishVariableHeader;
        this.body = body;
    }

    @Override
    public void run() {
        final long startTime = System.nanoTime();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
        Set<MQTTSession> clients = iotClientManager.getTopic2Clients().get(MqttUtil.getRootTopic(variableHeader.topicName()));
        if (clients == null || clients.size() == 0) {
            return;
        }
        Set<MQTTSession> clientsTobePushed = clients.stream().filter(c -> c.isConnected()).filter(c -> MqttUtil.isContain(c.getMqttSubscriptionDataTable().keySet(), variableHeader.topicName()))
            .collect(Collectors.toSet());
        if (clientsTobePushed != null && clientsTobePushed.size() > 0) {
            sendMessageQos0(clientsTobePushed, variableHeader, body);
        }
        log.info("MqttPushQos0Task {} consumes {}ms", this.toString(), (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1));
    }

    private void sendMessageQos0(Set<MQTTSession> clientsTobePushed, MqttPublishVariableHeader variableHeader,
        byte[] body) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
        for (MQTTSession client : clientsTobePushed) {
            if (!client.isConnected()) {
                log.info("Client is not online while sending message. ClientId={}", client.getClientId());
                continue;
            }
            client.pushMessageQos0(mqttFixedHeader, variableHeader, body);
        }
    }
}
