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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribePayload;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

/**
 * handle the UNSUBSCRIBE message from the client
 */
public class MqttUnsubscribeMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttUnsubscribeMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttUnsubscribeMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) throws Exception {
        if (!(message instanceof MqttUnsubscribeMessage)) {
            log.error("Wrong message type! Expected type: UNSUBSCRIBE but {} was received. MqttMessage={}", message.fixedHeader().messageType(), message.toString());
            throw new WrongMessageTypeException("Wrong message type exception.");
        }
        MqttUnsubscribeMessage unsubscribeMessage = (MqttUnsubscribeMessage) message;
        MqttFixedHeader fixedHeader = unsubscribeMessage.fixedHeader();
        if (fixedHeader.isDup() || !fixedHeader.qosLevel().equals(MqttQoS.AT_LEAST_ONCE) || fixedHeader.isRetain()) {
            log.error("Malformed value of reserved bits(bits 3,2,1,0) of fixed header. Expected=0010, received={}{}{}{}", fixedHeader.isDup() ? 1 : 0, Integer.toBinaryString(fixedHeader.qosLevel().value()), fixedHeader.isRetain() ? 1 : 0);
            remotingChannel.close();
            return null;
        }
        MqttUnsubscribePayload payload = unsubscribeMessage.payload();
        if (payload.topics() == null || payload.topics().size() == 0) {
            log.error("The payload of a UNSUBSCRIBE packet MUST contain at least one Topic Filter. This will be treated as protocol violation and the connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        MQTTSession client = iotClientManager.getClient(remotingChannel);
        if (client == null) {
            log.error("Can't find associated client, the connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }

        doUnsubscribe(client, payload.topics(), iotClientManager);
        log.info("取消订阅成功: MqttUnsubscribeMessage={}", message.toString());
        return new MqttUnsubAckMessage(
            new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2),
            MqttMessageIdVariableHeader.from(unsubscribeMessage.variableHeader().messageId()));
    }

    @Override public boolean rejectRequest() {
        return false;
    }

    private void doUnsubscribe(MQTTSession client, List<String> topics,
        MqttClientManagerImpl iotClientManager) throws Exception {
        ConcurrentHashMap<String, Set<MQTTSession>> topic2Clients = iotClientManager.getTopic2Clients();
        ConcurrentHashMap<String, MqttSubscriptionData> mqttSubscriptionDataTable = client.getMqttSubscriptionDataTable();
        Set<String> rootTopicsBefore = new HashSet<>(topic2Clients.keySet());
        //被取消订阅的topic
        List<String> removedTopics = new ArrayList<>();
        for (String topic : topics) {
            if (mqttSubscriptionDataTable.containsKey(topic)) {
                mqttSubscriptionDataTable.remove(topic);
                removedTopics.add(topic);
            }
        }

        final ConcurrentHashMap.KeySetView<String, MqttSubscriptionData> currentMqttSubscriptions = mqttSubscriptionDataTable.keySet();
        Set<String> currentRootTopics = currentMqttSubscriptions.stream().map(MqttUtil::getRootTopic).collect(Collectors.toSet());
        for (String rootTopic : removedTopics.stream().map(t -> MqttUtil.getRootTopic(t)).collect(Collectors.toSet())) {
            if (!currentRootTopics.contains(rootTopic)) {
                Set<MQTTSession> mqttSessions = topic2Clients.get(rootTopic);
                if (mqttSessions != null && mqttSessions.size() > 0) {
                    mqttSessions.remove(client);
                    if (mqttSessions.size() == 0) {
                        topic2Clients.remove(rootTopic);
                    }
                }
            }
            if (!currentRootTopics.contains(rootTopic) || (currentRootTopics.contains(rootTopic) && maxQos0(mqttSubscriptionDataTable, rootTopic)))
                removeConsumeOffsest(rootTopic, client.getClientId());
        }
        Set<String> rootTopicsAfter = new HashSet<>(topic2Clients.keySet());
        //update rootTopic->mqttBridge
        this.mqttBridgeController.getPersistService().deleteRootTopic2MqttBridge(rootTopicsBefore, rootTopicsAfter, this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
        if (!client.isCleanSession() && removedTopics.size() > 0) {
            this.mqttBridgeController.getPersistService().updateOrAddClient(client);
        }
    }

    private boolean maxQos0(ConcurrentHashMap<String, MqttSubscriptionData> subscriptionTable, String rootTopic) {
        int maxQos = 0;
        for (Map.Entry<String, MqttSubscriptionData> entry : subscriptionTable.entrySet()) {
            if (MqttUtil.getRootTopic(entry.getKey()).equals(rootTopic)) {
                if (entry.getValue().getQos() > maxQos) {
                    maxQos = entry.getValue().getQos();
                }
            }
        }
        return maxQos == 0;
    }

    private void removeConsumeOffsest(String rootTopic, String clientId) throws Exception {
        TopicRouteData topicRouteData;
        try {
            topicRouteData = this.mqttBridgeController.getNnodeService().getTopicRouteDataByTopic(rootTopic, false);
        } catch (Exception e) {
            log.error("Exception was thrown when getTopicRouteDataByTopic. e={}", e.getMessage());
            throw new Exception("Exception was thrown when getTopicRouteDataByTopic. e=" + e.getMessage());
        }
        if (topicRouteData == null || topicRouteData.getBrokerDatas() == null || topicRouteData.getBrokerDatas().size() == 0) {
            return;
        }
        ConcurrentHashMap<String, Long[]> consumeOffsetTable = ((MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager()).getConsumeOffsetTable();
        for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
            String offsetKey = new StringBuilder().append(brokerData.getBrokerName()).append(TOPIC_CLIENTID_SEPARATOR).append(rootTopic).append(TOPIC_CLIENTID_SEPARATOR).append(clientId).toString();
            if (consumeOffsetTable.containsKey(offsetKey)) {
                consumeOffsetTable.remove(offsetKey);
                this.mqttBridgeController.getPersistService().deleteConsumeOffset(offsetKey);
            }
        }
    }
}
