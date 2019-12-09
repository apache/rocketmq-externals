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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPubackMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttPubackMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttPubackMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    /**
     * handle the PUBACK message from the client <ol> <li>remove the message from the published in-flight messages</li>
     * <li>ack the message in the MessageStore</li> </ol>
     *
     * @param
     * @return
     */
    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) {
        if (!(message instanceof MqttPubAckMessage)) {
            log.error("Wrong message type! Expected type: PUBACK but {} was received. MqttMessage={}", message.fixedHeader().messageType(), message.toString());
            throw new WrongMessageTypeException("Wrong message type exception.");
        }
        MqttPubAckMessage mqttPubAckMessage = (MqttPubAckMessage) message;
        MqttMessageIdVariableHeader variableHeader = mqttPubAckMessage.variableHeader();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        MQTTSession client = iotClientManager.getClient(remotingChannel);
        if (client != null) {
            client.pubAckReceived(variableHeader.messageId());
            drainFromQueue(client);
        }
        return null;
    }

    private void drainFromQueue(MQTTSession client) {
        //如果缓冲队列非空且inflightWindows未满，将缓冲队列中的消息放入inflightWindow并发送
        client.drainFromQueue();
    }

    @Override public boolean rejectRequest() {
        return false;
    }
}
