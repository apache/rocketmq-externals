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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.common.util.MessageUtil;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.storage.message.MessageStore;
import org.apache.rocketmq.iot.storage.rocketmq.PublishProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublishMessageHandler implements MessageHandler {
    private Logger logger = LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

    private MessageStore messageStore;
    private PublishProducer publishProducer;

    public MqttPublishMessageHandler(MessageStore messageStore, PublishProducer publishProducer) {
        this.messageStore = messageStore;
        this.publishProducer = publishProducer;
    }

    @Override public void handleMessage(Message message) {
        Client client = message.getClient();
        MqttPublishMessage publishMessage = (MqttPublishMessage) message.getPayload();

        try {
            publishProducer.send(publishMessage, client);
        } catch (Exception e) {
            logger.error("send msg to rocketMQ failed, clientId:{}", client.getId(), e);
        }

        // TODO: qos1, qos2
        // messageStore.put(message);

        ChannelHandlerContext clientCtx = client.getCtx();
        MqttQoS mqttQoS = publishMessage.fixedHeader().qosLevel();
        switch (mqttQoS) {
            case AT_MOST_ONCE:
                break;

            case AT_LEAST_ONCE:
                MqttPubAckMessage pubAckMessage = MessageUtil.getMqttPubackMessage(publishMessage);
                clientCtx.writeAndFlush(pubAckMessage);
                break;

            case EXACTLY_ONCE:
                MqttMessage pubrecMessage = MessageUtil.getMqttPubrecMessage(publishMessage);
                clientCtx.writeAndFlush(pubrecMessage);
                break;

            default:
                break;
        }
    }

}
