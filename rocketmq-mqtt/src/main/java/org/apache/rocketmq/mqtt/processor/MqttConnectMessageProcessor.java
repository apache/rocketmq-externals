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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.InFlightPacket;
import org.apache.rocketmq.mqtt.client.MqttClientManager;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.NettyChannelImpl;
import org.apache.rocketmq.mqtt.common.WillMessage;
import org.apache.rocketmq.mqtt.exception.MqttConnectException;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.task.MqttPushOfflineMessageTask;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.KEEP_ALIVE_INTERVAL_DEFAULT;
import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class MqttConnectMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttConnectMessageProcessor.class);
    private static final int MIN_AVAILABLE_VERSION = 3;
    private static final int MAX_AVAILABLE_VERSION = 4;
    private final MqttBridgeController mqttBridgeController;

    public MqttConnectMessageProcessor(final MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) {
        if (!(message instanceof MqttConnectMessage)) {
            log.error("Wrong message type! Expected type: CONNECT but {} was received.", message.fixedHeader().messageType());
            throw new WrongMessageTypeException("Wrong message type exception.");
        }
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        //treat a second CONNECT packet on one TCP connection as a protocol violation and disconnect
        if (iotClientManager.getClient(remotingChannel) != null) {
            //调用onClose方法清理数据
            iotClientManager.onClose(remotingChannel);
            log.error("This client has been connected. The second CONNECT packet is treated as a protocol violation and will be closed.");
            remotingChannel.close();
        }
        MqttConnectMessage mqttConnectMessage = (MqttConnectMessage) message;
        MqttConnectPayload payload = mqttConnectMessage.payload();
        MqttConnectVariableHeader variableHeader = mqttConnectMessage.variableHeader();
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 2);
        /* TODO when clientId.length=0 and cleanSession=0, the server should assign a unique clientId to the client.*/
        //validate clientId

        if (!isClientIdValid(payload.clientIdentifier())) {
            log.error("ClientID [{}] is not match the pattern required", payload.clientIdentifier());
            return new MqttConnAckMessage(mqttFixedHeader, new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false));
        }

        boolean sessionPresent = false;

        MQTTSession client = new MQTTSession(payload.clientIdentifier(), true, mqttConnectMessage.variableHeader().isCleanSession(),
            remotingChannel, System.currentTimeMillis(), payload.userName(), this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP(),
            variableHeader.keepAliveTimeSeconds(), this.mqttBridgeController);

        boolean cleanExistSessionDone = false;
        do {
            MQTTSession existSession = getExistSession(payload.clientIdentifier());
            if (existSession == null) {
                cleanExistSessionDone = true;
            }
            RemotingChannel oldChannel = null;
            if (existSession != null) {
                oldChannel = iotClientManager.getChannel(payload.clientIdentifier());
                if (oldChannel != null && oldChannel.isActive()) {
                    //如果前一个连接在当前节点且正常连接：1.移除旧的channel相关数据并关闭 2.如果之前是持久化连接继续使用原先的MQTTSession
                    iotClientManager.onClose(oldChannel);
                    oldChannel.close();
                    cleanExistSessionDone = true;
                }
            }
            if (!variableHeader.isCleanSession()) {
                //如果存在clientId占用
                if (existSession != null) {
                    if (oldChannel == null || !oldChannel.isActive()) {
                        //前一次连接在当前节点，有三种情况：
                        // 1.前一次为非持久化连接，mqttBridge异常终止，导致redis数据没有删除
                        // 2.前一次为持久化连接，mqttBridge异常终止，导致redis上client.isConnected=true
                        // 3.前一次为持久化连接，正常断开
                        if (existSession.getMqttBridgeAddr().equals(this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP())) {
                            //非持久化连接，清理会话数据
                            if (existSession.isCleanSession()) {
                                iotClientManager.cleanSessionState(existSession);
                            }
                            cleanExistSessionDone = true;
                        } else {
                            //前一个连接在其他节点
                            cleanExistSessionDone = this.cleanExistSessionOnRemoteBridge(variableHeader.isCleanSession(), existSession);
                        }
                    }
                    if (!existSession.isCleanSession() && cleanExistSessionDone) {
                        sessionPresent = true;
                        existSession.setMqttBridgeController(this.mqttBridgeController);
                        existSession.setMqttBridgeAddr(this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
                        existSession.setRemotingChannel(remotingChannel);
                        existSession.setConnected(true);
                        existSession.setAccessKey(payload.userName());
                        restoreTopic2Client(existSession);
                        client = existSession;
                    }
                }
            } else {
                //此次是非持久化连接，丢弃之前的会话
                if (existSession != null) {
                    //前一次连接在当前节点，有三种情况：
                    // 1.前一次为非持久化连接，mqttBridge异常终止，导致redis数据没有删除
                    // 2.前一次为持久化连接，mqttBridge异常终止，导致redis上client.isConnected=true
                    // 3.前一次为持久化连接，正常断开
                    if (existSession.getMqttBridgeAddr().equals(this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP())) {
                        //清理上一次的会话数据
                        iotClientManager.cleanSessionState(existSession);
                        //删除Redis中的消费进度记录（由持久化连接变为非持久化连接）
                        iotClientManager.deletePersistConsumeOffset(existSession);
                        cleanExistSessionDone = true;
                    } else {
                        //前一个连接在其他节点
                        cleanExistSessionDone = this.cleanExistSessionOnRemoteBridge(variableHeader.isCleanSession(), existSession);
                    }
                }
            }
        }
        while (!cleanExistSessionDone);

        //save will message into persist store if have
        WillMessage willMessage = null;
        if (mqttConnectMessage.variableHeader().isWillFlag()) {
            if (payload.willTopic() == null || payload.willMessageInBytes() == null || payload.willMessageInBytes().length == 0) {
                log.error("Will message and will topic can not be empty.");
                remotingChannel.close();
                throw new MqttConnectException("Will message and will topic can not be empty.");
            }
            willMessage = new WillMessage(payload.willTopic(), payload.willMessageInBytes(), mqttConnectMessage.variableHeader().isWillRetain(), mqttConnectMessage.variableHeader().willQos());
        }
        //register client
        client.setWillMessage(willMessage);
        iotClientManager.register(client);

        if (variableHeader.keepAliveTimeSeconds() != KEEP_ALIVE_INTERVAL_DEFAULT) {
            ((NettyChannelImpl) remotingChannel).getChannel().pipeline().replace(IdleStateHandler.class, null, new IdleStateHandler(0, 0, Math.round(variableHeader.keepAliveTimeSeconds() * 1.5f)));
        }
        log.info("连接成功: MqttConnectMessage={}", message.toString());
        MqttConnAckMessage mqttConnAckMessage = new MqttConnAckMessage(mqttFixedHeader, new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED,
            sessionPresent));
        MQTTSession finalClient = client;
        ((NettyChannelImpl) remotingChannel).getChannelHandlerContext().writeAndFlush(mqttConnAckMessage).addListener(new ChannelFutureListener() {
            @Override public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    //trigger to push offline messages to this client
                    if (!finalClient.isCleanSession() && finalClient.getMqttSubscriptionDataTable().size() > 0) {
                        pushNotAckedMessage(finalClient, iotClientManager);
                        pushOfflineMessages(finalClient);
                    }
                }
            }
        });
        return null;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }

    private boolean cleanExistSessionOnRemoteBridge(boolean currentCleanSession, MQTTSession existSession) {
        boolean cleanExistSessionDone = false;
        if (!existSession.isConnected()) {
            this.cleanExistSessionBySelf(currentCleanSession, existSession);
            cleanExistSessionDone = true;
            return cleanExistSessionDone;
        }

        int maxRetryRequestCounter = 3;
        RemotingCommand response = null;
        for (int i = 0; i < maxRetryRequestCounter; i++) {
            response = this.mqttBridgeController.getBridgeService().closeClientConnection(currentCleanSession, existSession);
            if (response != null) {
                break;
            }
        }
        // 连接远程节点失败,当前节点负责清理redis相关信息
        if (response == null) {
            this.cleanExistSessionBySelf(currentCleanSession, existSession);
            cleanExistSessionDone = true;
        } else if (response.getCode() == ResponseCode.SUCCESS) {
            cleanExistSessionDone = true;
        }
        // bridge集群时，有可能远程bridge再次收到清理请求时已经不是已存在client的管理节点，清理失败
        return cleanExistSessionDone;
    }

    private void cleanExistSessionBySelf(boolean currentCleanSession, MQTTSession existSession) {
        if (existSession.isCleanSession() || currentCleanSession) {
            MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
            iotClientManager.cleanSessionState(existSession);
            iotClientManager.deletePersistConsumeOffset(existSession);
        }
    }

    private void pushNotAckedMessage(MQTTSession client, MqttClientManagerImpl iotClientManager) {
        Map<Integer, Message> inflightWindow = client.getInflightWindow();
        if (inflightWindow.size() == 0) {
            return;
        }
        log.info("Begin sending inflight-message. ClientId={}", client.getClientId());
        for (Map.Entry<Integer, Message> entry : inflightWindow.entrySet()) {
            Message inFlightMessage = entry.getValue();
            String key = new StringBuilder().append(inFlightMessage.getBrokerName()).append(TOPIC_CLIENTID_SEPARATOR).append(MqttUtil.getRootTopic(inFlightMessage.getTopicName())).append(TOPIC_CLIENTID_SEPARATOR).append(client.getClientId()).toString();
            MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(
                new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.valueOf(inFlightMessage.getPushQos()), false, 0),
                new MqttPublishVariableHeader(inFlightMessage.getTopicName(), entry.getKey()), Unpooled.copiedBuffer(inFlightMessage.getBody()));

            iotClientManager.getInflightTimeouts().add(new InFlightPacket(client, entry.getKey(), this.mqttBridgeController.getMqttBridgeConfig().getMsgFlyTimeBeforeResend()));
            if (inFlightMessage.getBrokerName() != null) {
                Message message = new Message(inFlightMessage.getQueueOffset(), inFlightMessage.getBody());
                iotClientManager.put2processTable(key, message);
            }
            if (!client.isConnected()) {
                return;
            }
            client.pushMessage2Client(mqttPublishMessage);
        }
    }

    private void pushOfflineMessages(MQTTSession client) {
        Enumeration<String> keys = client.getMqttSubscriptionDataTable().keys();
        Set<String> topicFilters = new HashSet<>();
        while (keys.hasMoreElements()) {
            String topicFilter = keys.nextElement();
            String rootTopic = MqttUtil.getRootTopic(topicFilter);
            topicFilters.add(rootTopic);
        }
        for (String rootTopic : topicFilters) {
            TopicRouteData topicRouteData;
            try {
                topicRouteData = this.mqttBridgeController.getNnodeService().getTopicRouteDataByTopic(rootTopic, false);
            } catch (Exception e) {
                log.error("Exception was thrown when get topicRouteData. topic={}, e={}", rootTopic, e.getLocalizedMessage());
                throw new MqttRuntimeException("Exception was thrown when get topicRouteData.");
            }
            List<BrokerData> brokerDatas = topicRouteData.getBrokerDatas();
            for (BrokerData brokerData : brokerDatas) {
                MqttPushOfflineMessageTask mqttPushOfflineMessageTask = new MqttPushOfflineMessageTask(this.mqttBridgeController, rootTopic, client, brokerData.getBrokerName());
                //add task to orderedExecutor
//                String key = brokerData.getBrokerName() + rootTopic + client.getClientId();
//                this.mqttBridgeController.getQos1MessagePushOrderedExecutor().executeOrdered(key, SafeRunnable.safeRun(mqttPushOfflineMessageTask));
                this.mqttBridgeController.getQos1MessagePushExecutor().submit(mqttPushOfflineMessageTask);
            }

        }

    }

    private void restoreTopic2Client(MQTTSession client) {
        Set<String> rootTopics = client.getMqttSubscriptionDataTable().keySet().stream().map(t -> MqttUtil.getRootTopic(t)).collect(Collectors.toSet());
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        Set<String> rootTopicsBefore = new HashSet<>(iotClientManager.getTopic2Clients().keySet());
        for (String rootTopic : rootTopics) {
            iotClientManager.addTopic2Client(client, rootTopic);
        }
        Set<String> rootTopicsAfter = new HashSet<>(iotClientManager.getTopic2Clients().keySet());
        this.mqttBridgeController.getPersistService().addRootTopic2MqttBridge(rootTopicsBefore, rootTopicsAfter, this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
    }

    private MQTTSession getExistSession(String clientId) {
        MqttClientManager mqttClientManager = this.mqttBridgeController.getMqttClientManager();
        RemotingChannel remotingChannel = mqttClientManager.getChannel(clientId);

        if (remotingChannel != null) {
            MQTTSession client = mqttClientManager.getClient(remotingChannel);
            if (client != null) {
                return client;
            }
        }

        MQTTSession mqttSession = (MQTTSession) this.mqttBridgeController.getPersistService().getClientByClientId(clientId);
        if (mqttSession != null) {
            return mqttSession;
        }
        return null;
    }

    private boolean isServiceAvailable(MqttConnectMessage connectMessage) {
        int version = connectMessage.variableHeader().version();
        return version >= MIN_AVAILABLE_VERSION && version <= MAX_AVAILABLE_VERSION;
    }

    private boolean isClientIdValid(String clientId) {
        if (StringUtils.isBlank(clientId))
            return false;
        return clientId.length() <= 64;
    }
}
