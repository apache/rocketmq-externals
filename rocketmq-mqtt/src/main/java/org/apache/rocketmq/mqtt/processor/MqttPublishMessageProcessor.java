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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageDecoder;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader;
import org.apache.rocketmq.common.protocol.header.SendMessageResponseHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.NettyChannelImpl;
import org.apache.rocketmq.mqtt.common.TransferData;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.task.MqttPushQos0Task;
import org.apache.rocketmq.mqtt.task.MqttPushQos1Task;
import org.apache.rocketmq.mqtt.task.MqttTransferTask;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttPublishMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttPublishMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttPublishMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel,
        MqttMessage message) throws Exception {
        if (!(message instanceof MqttPublishMessage)) {
            log.error("Wrong message type! Expected type: PUBLISH but {} was received.", message.fixedHeader().messageType());
            throw new WrongMessageTypeException("Wrong message type exception.");
        }
        MqttPublishMessage mqttPublishMessage = (MqttPublishMessage) message;
        MqttFixedHeader fixedHeader = mqttPublishMessage.fixedHeader();
        MqttPublishVariableHeader variableHeader = mqttPublishMessage.variableHeader();
        if (!MqttUtil.isQosLegal(fixedHeader.qosLevel())) {
            log.error("The QoS level should be 0 or 1 or 2. The connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }

        ByteBuf payload = mqttPublishMessage.payload();
        byte[] body = new byte[payload.readableBytes()];
        payload.readBytes(body);
        payload.release();
        handleRetainMessage(fixedHeader.isRetain(), variableHeader.topicName(), body);
        MQTTSession client = this.mqttBridgeController.getMqttClientManager().getClient(remotingChannel);
        switch (fixedHeader.qosLevel()) {
            case AT_MOST_ONCE:
                //For clients connected to the current mqtt bridge, send as qos=0
                MqttPushQos0Task mqttPushQos0Task = new MqttPushQos0Task(this.mqttBridgeController, variableHeader, body);
                this.mqttBridgeController.getQos0MessagePushExecutor().execute(mqttPushQos0Task);
                //For clients that connected to other mqtt bridges, transfer the message
                TransferData transferData = new TransferData(variableHeader.topicName(), 0, body);
                MqttTransferTask mqttTransferTask = new MqttTransferTask(this.mqttBridgeController, transferData);
                this.mqttBridgeController.getTransferMessageExecutor().submit(mqttTransferTask);
                break;
            case AT_LEAST_ONCE:
                // Store msg and invoke callback to publish msg(as qos=1) to subscribers
                // 1. Check if the root topic has been created
                String rootTopic = MqttUtil.getRootTopic(variableHeader.topicName());
                TopicRouteData topicRouteData;

                try {
                    topicRouteData = this.mqttBridgeController.getNnodeService().getTopicRouteDataByTopic(rootTopic, false).cloneTopicRouteData();
                } catch (Exception e) {
                    log.error("The rootTopic {} does not exist. Please create it first.", rootTopic);
                    throw new Exception("Can not find topic:" + rootTopic);
                }

                //2. Store msg
                List<BrokerData> datas = topicRouteData.getBrokerDatas();
                RemotingCommand request = createSendMessageRequest(rootTopic, fixedHeader, variableHeader, body);
                final int retryTimesWhenSendFailed=3;
                AtomicInteger times = new AtomicInteger(0);
                CompletableFuture<RemotingCommand> responseFuture = new CompletableFuture<>();
                this.mqttBridgeController.getEnodeService().sendMessageAsync(datas,request,retryTimesWhenSendFailed,times,responseFuture);
                responseFuture.whenComplete((data, ex) -> {
                    if (ex == null && data != null) {
                        String brokerName = data.getExtFields().get(MqttConstant.ENODE_NAME);
                        MqttMessage response = doResponse(fixedHeader, variableHeader);
                        if (response != null) {
                            ((NettyChannelImpl) remotingChannel).getChannelHandlerContext().writeAndFlush(response);
                        }
                        SendMessageResponseHeader sendMessageResponseHeader;
                        try {
                            sendMessageResponseHeader = (SendMessageResponseHeader) data.decodeCommandCustomHeader(SendMessageResponseHeader.class);
                        } catch (RemotingCommandException e) {
                            log.error("RemotingCommandException was thrown when decode SendMessageResponseHeader.");
                            log.error(e.getLocalizedMessage());
                            return;
                        }
                        //MqttPushQos1Task: find clients that subscribe related topic and publish message
                        MqttPushQos1Task pushQos1Task = new MqttPushQos1Task(this.mqttBridgeController, variableHeader, body, sendMessageResponseHeader.getQueueOffset(), brokerName);
//                        this.mqttBridgeController.getQos1MessagePushOrderedExecutor().executeOrdered(key, SafeRunnable.safeRun(pushQos1Task));
                        this.mqttBridgeController.getQos1MessagePushExecutor().submit(pushQos1Task);
                        //for clients connected to other mqtt bridge, forward msg
                        TransferData transferDataQos1 = new TransferData(variableHeader.topicName(), 1, body, brokerName, sendMessageResponseHeader.getQueueOffset());
                        MqttTransferTask mqttTransferQos1Task = new MqttTransferTask(this.mqttBridgeController, transferDataQos1);
//                        this.mqttBridgeController.getTransferMessageOrderedExecutor().executeOrdered(key, SafeRunnable.safeRun(mqttTransferQos1Task));
                        this.mqttBridgeController.getTransferMessageExecutor().submit(mqttTransferQos1Task);
                    } else {
                        log.error("Store Qos=1 Message error: {}", ex);
                    }
                });
                break;
            case EXACTLY_ONCE:
                throw new MqttRuntimeException("Qos = 2 messages are not supported yet.");
            default:
                break;
        }
        return null;
    }

    @Override
    public MqttMessage doResponse(MqttFixedHeader fixedHeader, MqttPublishVariableHeader variableHeader) {
        if (fixedHeader.qosLevel().value() > 0) {
            if (fixedHeader.qosLevel().equals(MqttQoS.AT_LEAST_ONCE)) {
                return new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2), MqttMessageIdVariableHeader.from(variableHeader.packetId()));
            } else if (fixedHeader.qosLevel().equals(MqttQoS.EXACTLY_ONCE)) {
                //PUBREC/PUBREL/PUBCOMP
            }
        }
        return null;
    }

    private void handleRetainMessage(boolean isRetain, String topic, byte[] body) {
        //delete or add retain message
        if (isRetain) {
            if (body.length == 0) {
                this.mqttBridgeController.getPersistService().deleteRetainMessageByTopic(topic);
            } else {
                this.mqttBridgeController.getPersistService().addOrUpdateTopic2RetainMessage(topic, body);
            }
        }
    }

    @Override public boolean rejectRequest() {
        return false;
    }

    private RemotingCommand createSendMessageRequest(String rootTopic, MqttFixedHeader fixedHeader,
        MqttPublishVariableHeader variableHeader, byte[] body) {
        Message msg = new Message(rootTopic, "", body);
        MessageClientIDSetter.setUniqID(msg);
        SendMessageRequestHeader requestHeader = new SendMessageRequestHeader();
        requestHeader.setProducerGroup(MqttClientManagerImpl.IOT_GROUP);
        requestHeader.setTopic(rootTopic);
        requestHeader.setQueueId(0);
        requestHeader.setBornTimestamp(System.currentTimeMillis());
        requestHeader.setBatch(false);
        requestHeader.setSysFlag(0);
        requestHeader.setFlag(0);
        requestHeader.setDefaultTopic(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC);
        requestHeader.setDefaultTopicQueueNums(1);
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_REAL_TOPIC, variableHeader.topicName());
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TAGS, MqttUtil.getRootTopic(variableHeader.topicName()));
        MessageAccessor.putProperty(msg, MqttConstant.PROPERTY_MQTT_QOS, fixedHeader.qosLevel().name());
        requestHeader.setProperties(MessageDecoder.messageProperties2String(msg.getProperties()));
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.SEND_MESSAGE, requestHeader);
        request.setBody(msg.getBody());
        return request;
    }

}
