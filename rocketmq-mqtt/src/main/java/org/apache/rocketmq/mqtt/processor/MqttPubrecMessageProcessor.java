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
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPubrecMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttPubrecMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttPubrecMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    /**
     * handle the PUBREC message from the clinet
     *
     * @param message
     * @return
     */
    @Override public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) {
        return null;
    }

    @Override public boolean rejectRequest() {
        return false;
    }
}
