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
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.Set;
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

public class MqttPushWillMessageTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttPushWillMessageTask.class);
    private final MqttBridgeController mqttBridgeController;
    private String topic;
    private boolean isRetain;
    private int publishQos;
    private byte[] body;

    public MqttPushWillMessageTask(MqttBridgeController mqttBridgeController,
        String topic, byte[] body, boolean isRetain, int publishQos) {
        this.mqttBridgeController = mqttBridgeController;
        this.topic = topic;
        this.body = body;
        this.isRetain = isRetain;
        this.publishQos = publishQos;
    }

    @Override
    public void run() {
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
        Set<MQTTSession> clients = iotClientManager.getTopic2Clients().get(MqttUtil.getRootTopic(this.topic));
        if (clients == null || clients.size() == 0) {
            return;
        }
        Set<MQTTSession> clientsTobePushed = clients.stream().filter(c -> MqttUtil.isContain(c.getMqttSubscriptionDataTable().keySet(), this.topic))
            .collect(Collectors.toSet());
        if (clientsTobePushed != null && clientsTobePushed.size() > 0) {
            sendWillMessage(clientsTobePushed, this.topic, body);
        }
    }

    private void sendWillMessage(Set<MQTTSession> clientsTobePushed, String topic, byte[] body) {
        for (MQTTSession client : clientsTobePushed) {
            int pushQos = Math.min(MqttUtil.calcMaxRequiredQos(this.topic, client.getMqttSubscriptionDataTable()), this.publishQos);
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(pushQos), isRetain, 0);
            if (client.isConnected()) {
                client.pushRetainAndWillMessage(mqttFixedHeader, topic, body);
                log.info("Send will msg success. WillTopic={}, willMessage={}, clientId={}", topic, new String(body), client.getClientId());
            }
        }
    }
}
