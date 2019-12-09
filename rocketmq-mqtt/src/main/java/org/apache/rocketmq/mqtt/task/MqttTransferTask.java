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

import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.TransferData;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttTransferTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(MqttTransferTask.class);
    private final MqttBridgeController mqttBridgeController;
    private TransferData transferData;

    public MqttTransferTask(MqttBridgeController mqttBridgeController, TransferData transferData) {
        this.mqttBridgeController = mqttBridgeController;
        this.transferData = transferData;
    }

    @Override
    public void run() {
        final long startTime = System.nanoTime();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        //访问redis获取要转发的mqtt节点
        Set<String> mqttBridgeNeedTobeTranfer = this.mqttBridgeController.getPersistService().getMqttBridgeByRootTopic(MqttUtil.getRootTopic(transferData.getTopic()));
        if (mqttBridgeNeedTobeTranfer == null || mqttBridgeNeedTobeTranfer.size() == 0) {
            return;
        }
        mqttBridgeNeedTobeTranfer.retainAll(iotClientManager.getLiveMqttBridgeIps());
        mqttBridgeNeedTobeTranfer.remove(this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
        for (String mqttBridgeIP : mqttBridgeNeedTobeTranfer) {
            try {
                iotClientManager.transferMessage(mqttBridgeIP, transferData);
            } catch (MqttException e) {
                log.error("Transfer message failed. MqttBridgeAddress={}, transferData={}. Exception: {}", mqttBridgeIP, transferData.toString(), e.getMessage());
            }
        }
        log.info("MqttTransferTask {} consumes {}ms", this.toString(), (System.nanoTime() - startTime) / (double) TimeUnit.MILLISECONDS.toNanos(1));
    }
}
