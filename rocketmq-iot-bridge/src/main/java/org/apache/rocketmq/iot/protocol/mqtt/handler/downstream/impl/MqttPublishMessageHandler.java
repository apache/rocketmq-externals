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

package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl;

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.storage.message.MessageStore;
import org.apache.rocketmq.iot.common.util.MessageUtil;

public class MqttPublishMessageHandler implements MessageHandler {

    private MessageStore messageStore;

    public MqttPublishMessageHandler(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @Override public void handleMessage(Message message) {
        Client client = message.getClient();
        MqttPublishMessage publishMessage = (MqttPublishMessage) message.getPayload();
        messageStore.put(message);
        int qos = MessageUtil.actualQos(publishMessage.fixedHeader().qosLevel().value());
        if (qos == MqttQoS.AT_LEAST_ONCE.value()) {
            MqttPubAckMessage pubAckMessage = MessageUtil.getMqttPubackMessage(publishMessage);
            client.getCtx().writeAndFlush(pubAckMessage);
        } else if (qos == MqttQoS.EXACTLY_ONCE.value()) {
            MqttMessage pubrecMessage = MessageUtil.getMqttPubrecMessage(publishMessage);
            client.getCtx().writeAndFlush(pubrecMessage);
        }
    }

}
