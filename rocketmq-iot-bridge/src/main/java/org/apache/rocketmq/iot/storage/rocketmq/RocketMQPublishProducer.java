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

package org.apache.rocketmq.iot.storage.rocketmq;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.constant.MsgPropertyKey;
import org.apache.rocketmq.iot.common.util.MqttUtil;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQPublishProducer implements PublishProducer {
    private Logger logger = LoggerFactory.getLogger(RocketMQPublishProducer.class);

    private MqttBridgeConfig bridgeConfig;
    private DefaultMQProducer producer;

    public RocketMQPublishProducer(MqttBridgeConfig bridgeConfig) throws MQClientException {
        this.bridgeConfig = bridgeConfig;
        initMQProducer();
    }

    private void initMQProducer() throws MQClientException {
        SessionCredentials sessionCredentials = new SessionCredentials(bridgeConfig.getRmqAccessKey(),
            bridgeConfig.getRmqSecretKey());
        RPCHook rpcHook = new AclClientRPCHook(sessionCredentials);

        this.producer = new DefaultMQProducer(bridgeConfig.getRmqProductGroup(), rpcHook);
        this.producer.setNamesrvAddr(bridgeConfig.getRmqNamesrvAddr());
    }

    @Override public void start() throws MQClientException {
        this.producer.start();
    }

    @Override public void send(MqttPublishMessage publishMessage, Client client) throws Exception {
        MqttPublishVariableHeader variableHeader = publishMessage.variableHeader();
        String mqttTopic = variableHeader.topicName();
        String rmqTopic = MqttUtil.getMqttRootTopic(mqttTopic);

        byte[] messageBytes = getMessageBytes(publishMessage);
        Message message = new Message(rmqTopic, messageBytes);

        int packetId = variableHeader.packetId();
        String clientId = client.getId();
        if (null == clientId) {
            logger.error("clientId is null, publish message:" + publishMessage);
            return;
        }

        message.setKeys(clientId);
        message.putUserProperty(MsgPropertyKey.CLIENT_ID, clientId);
        message.putUserProperty(MsgPropertyKey.MQTT_TOPIC, mqttTopic);
        message.putUserProperty(MsgPropertyKey.MSG_PACKET_ID, String.valueOf(packetId));
        putMqttFixedHeader(message, publishMessage.fixedHeader());

        producer.send(message);
    }

    private byte[] getMessageBytes(MqttPublishMessage publishMessage) {
        ByteBuf byteBuf = publishMessage.payload();
        byte[] body = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(0, body);
        return body;
    }

    public Message putMqttFixedHeader(Message message, MqttFixedHeader mqttFixedHeader) {
        boolean isDup = mqttFixedHeader.isDup();
        int qosLevel = mqttFixedHeader.qosLevel().value();
        boolean isRetain = mqttFixedHeader.isRetain();
        int remainingLength = mqttFixedHeader.remainingLength();

        message.putUserProperty(MsgPropertyKey.MSG_IS_DUP, String.valueOf(isDup));
        message.putUserProperty(MsgPropertyKey.MSG_QOS_LEVEL, String.valueOf(qosLevel));
        message.putUserProperty(MsgPropertyKey.MSG_IS_RETAIN, String.valueOf(isRetain));
        message.putUserProperty(MsgPropertyKey.MSG_REMAINING_LENGTH, String.valueOf(remainingLength));

        return message;
    }

    @Override
    public void shutdown() {
        producer.shutdown();
    }
}
