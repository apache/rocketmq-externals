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

import io.netty.handler.codec.mqtt.MqttMessage;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.mqtt.MqttMessageType.PUBLISH;

public class InnerMqttMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(InnerMqttMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;
    private MqttMessageForwardProcessor mqttMessageForwardProcessor;

    public InnerMqttMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
        this.mqttMessageForwardProcessor = new MqttMessageForwardProcessor(mqttBridgeController);
    }

    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) throws Exception {
        if (message.fixedHeader().messageType().equals(PUBLISH)) {
            return mqttMessageForwardProcessor.processRequest(remotingChannel, message);
        }
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
