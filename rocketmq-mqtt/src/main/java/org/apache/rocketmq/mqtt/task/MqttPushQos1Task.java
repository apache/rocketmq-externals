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
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPushQos1Task implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttPushQos1Task.class);
    private static final MqttFixedHeader MQTTFIXEDHEADER_QOS0 = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
    private static final MqttFixedHeader MQTTFIXEDHEADER_QOS1 = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
    private final MqttBridgeController mqttBridgeController;
    private MqttPublishVariableHeader publishVariableHeader;
    private String brokerName;
    private byte[] body;
    private Long queueOffset;

    public MqttPushQos1Task(MqttBridgeController mqttBridgeController, MqttPublishVariableHeader publishVariableHeader,
        byte[] body, Long queueOffset, String brokerName) {
        this.mqttBridgeController = mqttBridgeController;
        this.publishVariableHeader = publishVariableHeader;
        this.brokerName = brokerName;
        this.body = body;
        this.queueOffset = queueOffset;
    }

    @Override
    public void run() {
        final long startTime = System.nanoTime();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
        Set<MQTTSession> clients = iotClientManager.getTopic2Clients().get(MqttUtil.getRootTopic(this.publishVariableHeader.topicName()));
        if (clients == null || clients.size() == 0) {
            return;
        }
        Set<MQTTSession> clientsTobePushed = clients.stream().filter(c -> c.isConnected()).filter(c -> MqttUtil.isContain(c.getMqttSubscriptionDataTable().keySet(), this.publishVariableHeader.topicName()))
            .collect(Collectors.toSet());
        if (clientsTobePushed != null && clientsTobePushed.size() > 0) {
            sendMessage(clientsTobePushed, publishVariableHeader, body);
        }
        log.info("MqttPushQos1Task {} consumes {}ms", this.toString(), (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1));
    }

    private void sendMessage(Set<MQTTSession> clientsTobePushed, MqttPublishVariableHeader variableHeader,
        byte[] body) {
        for (MQTTSession client : clientsTobePushed) {
            if (!client.isConnected()) {
                log.info("Client is not online while sending message. ClientId={}", client.getClientId());
                continue;
            }
            if (MqttUtil.calcMaxRequiredQos(this.publishVariableHeader.topicName(), client.getMqttSubscriptionDataTable()) == 0) {
                client.pushMessageQos0(MQTTFIXEDHEADER_QOS0, variableHeader, body);
            } else {
                Message message = new Message(this.publishVariableHeader.topicName(), 1, this.brokerName, queueOffset, body);
                client.pushMessageQos1(MQTTFIXEDHEADER_QOS1, message);
            }

        }
    }
}
