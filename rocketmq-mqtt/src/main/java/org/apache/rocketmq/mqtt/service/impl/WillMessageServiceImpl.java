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

package org.apache.rocketmq.mqtt.service.impl;

import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.TransferData;
import org.apache.rocketmq.mqtt.common.WillMessage;
import org.apache.rocketmq.mqtt.service.WillMessageService;
import org.apache.rocketmq.mqtt.task.MqttPushWillMessageTask;
import org.apache.rocketmq.mqtt.task.MqttTransferTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WillMessageServiceImpl implements WillMessageService {

    private static final Logger log = LoggerFactory.getLogger(WillMessageServiceImpl.class);

    private MqttBridgeController mqttBridgeController;

    public WillMessageServiceImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public void sendWillMessage(MQTTSession client) {

        WillMessage willMessage = client.getWillMessage();
        String topic = willMessage.getWillTopic();
        int qosLevel = willMessage.getQos();
        boolean isRetain = willMessage.isRetain();
        byte[] body = willMessage.getBody();
        MqttPushWillMessageTask pushWillMessageTask = new MqttPushWillMessageTask(mqttBridgeController, topic, body, isRetain, qosLevel);
        this.mqttBridgeController.getRetainMessageAndWillMessagePushExecutor().execute(pushWillMessageTask);

        //For clients that connected to other mqtt bridges, transfer will message
        TransferData transferData = new TransferData(topic, qosLevel, body, true);
        MqttTransferTask mqttTransferTask = new MqttTransferTask(this.mqttBridgeController, transferData);
//        this.mqttBridgeController.getTransferMessageOrderedExecutor().execute(mqttTransferTask);
        this.mqttBridgeController.getTransferMessageExecutor().submit(mqttTransferTask);
        //If will retain is true, add the will message to retain message.
        if (isRetain) {
            this.mqttBridgeController.getPersistService().addOrUpdateTopic2RetainMessage(topic, body);
        }
    }
}
