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

package org.apache.rocketmq.mqtt.processor;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.Set;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.TransferData;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.task.MqttPushQos0Task;
import org.apache.rocketmq.mqtt.task.MqttPushQos1Task;
import org.apache.rocketmq.mqtt.task.MqttPushWillMessageTask;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttMessageForwardProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttMessageForwardProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttMessageForwardProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    /**
     * handle messages transferred from other nodes
     *
     * @param message the message that transferred from other node
     */
    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) {
        MqttPublishMessage mqttPublishMessage = (MqttPublishMessage) message;
        MqttFixedHeader fixedHeader = mqttPublishMessage.fixedHeader();
        MqttPublishVariableHeader variableHeader = mqttPublishMessage.variableHeader();
        ByteBuf payload = mqttPublishMessage.payload();
        int length = payload.readableBytes();
        byte[] array = new byte[length];
        payload.getBytes(payload.readerIndex(), array);
        payload.release();
        TransferData transferData = TransferData.decode(array, TransferData.class);
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
        Set<MQTTSession> clients = iotClientManager.getTopic2Clients().get(MqttUtil.getRootTopic(transferData.getTopic()));
        if (clients == null || clients.size() == 0) {
            return doResponse(fixedHeader, variableHeader);
        }
        if (transferData.isWillMessage()) {
            MqttPushWillMessageTask pushWillMessageTask = new MqttPushWillMessageTask(mqttBridgeController, transferData.getTopic(), transferData.getBody(), false, transferData.getQos());
            this.mqttBridgeController.getRetainMessageAndWillMessagePushExecutor().execute(pushWillMessageTask);
        } else {
            switch (MqttQoS.valueOf(transferData.getQos())) {
                case AT_MOST_ONCE:
                    //For clients connected to the current mqtt bridge, send as qos=0 (need to be optimized here)
                    MqttPushQos0Task mqttPushQos0Task = new MqttPushQos0Task(this.mqttBridgeController, variableHeader, transferData.getBody());
                    this.mqttBridgeController.getQos0MessagePushExecutor().execute(mqttPushQos0Task);
                    break;
                case AT_LEAST_ONCE:
                    MqttPushQos1Task mqttPushQos1Task = new MqttPushQos1Task(this.mqttBridgeController, variableHeader, transferData.getBody(), transferData.getQueueOffset(), transferData.getBrokerName());
//                    this.mqttBridgeController.getQos1MessagePushOrderedExecutor().executeOrdered(variableHeader.topicName(), SafeRunnable.safeRun(mqttPushQos1Task));
                    this.mqttBridgeController.getQos1MessagePushExecutor().submit(mqttPushQos1Task);
                    break;
                case EXACTLY_ONCE:
                    throw new MqttRuntimeException("Qos = 2 messages are not supported yet.");
            }
        }
        return doResponse(fixedHeader, variableHeader);
    }

    @Override public boolean rejectRequest() {
        return false;
    }
}
