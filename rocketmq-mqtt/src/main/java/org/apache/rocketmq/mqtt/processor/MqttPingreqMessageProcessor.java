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

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPingreqMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttPingreqMessageProcessor.class);
    private final MqttBridgeController mqttBridgeController;

    public MqttPingreqMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    /**
     * handle the PINGREQ message from client <ol> <li>check client exists</li> <li>check client is connected</li>
     * <li>generate the PINGRESP message</li> <li>send the PINGRESP message to the client</li> </ol>
     *
     * @param message
     * @return
     */
    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) {
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        MQTTSession client = iotClientManager.getClient(remotingChannel);
        log.debug("Handle MQTT client: {} Pingreq.", client.getClientId());
        if (client != null && client.isConnected()) {
            client.setLastUpdateTimestamp(System.currentTimeMillis());
            return new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0));
        }
        return null;
    }

    @Override public boolean rejectRequest() {
        return false;
    }
}
