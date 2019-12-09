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
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.GetMaxOffsetRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.exception.WrongMessageTypeException;
import org.apache.rocketmq.mqtt.task.MqttPublishRetainMessageTask;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class MqttSubscribeMessageProcessor implements MqttRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(MqttSubscribeMessageProcessor.class);

    private final MqttBridgeController mqttBridgeController;

    public MqttSubscribeMessageProcessor(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    /**
     * handle the SUBSCRIBE message from the client <ol> <li>validate the topic filters in each subscription</li>
     * <li>set actual qos of each filter</li> <li>get the topics matching given filters</li> <li>check the client
     * authorization of each topic</li> <li>generate SUBACK message which includes the subscription result for each
     * TopicFilter</li> <li>send SUBACK message to the client</li> </ol>
     *
     * @param message the message wrapping MqttSubscriptionMessage
     * @return
     */
    @Override
    public MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage message) throws Exception {
        if (!(message instanceof MqttSubscribeMessage)) {
            log.error("Wrong message type! Expected type: SUBSCRIBE but {} was received. MqttMessage={}", message.fixedHeader().messageType(), message.toString());
            throw new WrongMessageTypeException("Wrong message type exception.");
        }
        MqttSubscribeMessage mqttSubscribeMessage = (MqttSubscribeMessage) message;
        MqttSubscribePayload payload = mqttSubscribeMessage.payload();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        MQTTSession client = iotClientManager.getClient(remotingChannel);
        for (MqttTopicSubscription mqttTopicSubscription : payload.topicSubscriptions()) {
            MqttUtil.validate(mqttTopicSubscription.topicName(), true);
        }
        //MqttUtil.validate(payload.topicSubscriptions().);
        if (client == null) {
            log.error("Can't find associated client, the connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }
        if (payload.topicSubscriptions() == null || payload.topicSubscriptions().size() == 0) {
            log.error("The payload of a SUBSCRIBE packet MUST contain at least one Topic Filter / QoS pair. This will be treated as protocol violation and the connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }
        if (!isQosLegal(payload.topicSubscriptions())) {
            log.error("The QoS level of Topic Filter / QoS pairs should be 0 or 1 or 2. The connection will be closed. remotingChannel={}, MqttMessage={}", remotingChannel.toString(), message.toString());
            remotingChannel.close();
            return null;
        }
        if (topicStartWithWildcard(payload.topicSubscriptions())) {
            log.error("Client can not subscribe topic starts with wildcards! clientId={}, topicSubscriptions={}", client.getClientId(), payload.topicSubscriptions().toString());

        }
        int messageId = mqttSubscribeMessage.variableHeader().messageId();
        List<Integer> grantQoss = doSubscribe(client, payload.topicSubscriptions());
        //publish retainMessage
        MqttPublishRetainMessageTask mqttPublishRetainMessageTask = new MqttPublishRetainMessageTask(mqttSubscribeMessage, client, this.mqttBridgeController);
        this.mqttBridgeController.getRetainMessageAndWillMessagePushExecutor().execute(mqttPublishRetainMessageTask);

        log.info("订阅成功: MqttSubscribeMessage={}", message.toString());
        MqttSubAckPayload mqttSubAckPayload = new MqttSubAckPayload(grantQoss);
        return new MqttSubAckMessage(
            new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
            MqttMessageIdVariableHeader.from(messageId), mqttSubAckPayload);
    }

    private List<Integer> doSubscribe(MQTTSession client,
        List<MqttTopicSubscription> mqttTopicSubscriptions) throws Exception {
        //do the logic when client sends subscribe packet.
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable = client.getMqttSubscriptionDataTable();
        List<Integer> grantQoss = new ArrayList<>();
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        Set<String> rootTopicsBefore = new HashSet<>(iotClientManager.getTopic2Clients().keySet());
        Collections.list(subscriptionDataTable.keys()).stream().map(MqttUtil::getRootTopic).collect(Collectors.toSet());
        for (MqttTopicSubscription mqttTopicSubscription : mqttTopicSubscriptions) {
            int actualQos = MqttUtil.actualQos(mqttTopicSubscription.qualityOfService().value());
            grantQoss.add(actualQos);
            MqttSubscriptionData subscriptionData = new MqttSubscriptionData(mqttTopicSubscription.topicName(), mqttTopicSubscription.qualityOfService().value());
            subscriptionDataTable.put(mqttTopicSubscription.topicName(), subscriptionData);

            //update topic2Clients(IotClientManager)
            iotClientManager.addTopic2Client(client, MqttUtil.getRootTopic(mqttTopicSubscription.topicName()));
        }
        Set<String> rootTopicsAfter = new HashSet<>(iotClientManager.getTopic2Clients().keySet());
        //update rootTopic->mqttBridge
        this.mqttBridgeController.getPersistService().addRootTopic2MqttBridge(rootTopicsBefore, rootTopicsAfter, this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
        if (!client.isCleanSession()) {
            this.mqttBridgeController.getPersistService().updateOrAddClient(client);
        }
        initConsumeOffset(subscriptionDataTable, client.getClientId());
        return grantQoss;
    }

    private void initConsumeOffset(
        ConcurrentHashMap<String, MqttSubscriptionData> subscriptionDataTable, String clientId) throws Exception {
        //init consumeOffset
        Map<String, Integer> rootTopic2MaxQos = new HashMap<>();
        for (Map.Entry<String, MqttSubscriptionData> entry : subscriptionDataTable.entrySet()) {
            String rootTopic = MqttUtil.getRootTopic(entry.getKey());
            Integer qos = entry.getValue().getQos();
            if (!rootTopic2MaxQos.containsKey(rootTopic)) {
                rootTopic2MaxQos.put(rootTopic, qos);
            } else {
                if (qos > rootTopic2MaxQos.get(rootTopic)) {
                    rootTopic2MaxQos.put(rootTopic, qos);
                }
            }
        }
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        ConcurrentHashMap<String, Long[]> consumeOffsetTable = iotClientManager.getConsumeOffsetTable();
        for (Map.Entry<String, Integer> entry : rootTopic2MaxQos.entrySet()) {
            String rootTopic = entry.getKey();
            int maxQos = entry.getValue();
            TopicRouteData topicRouteData = null;
            try {
                topicRouteData = this.mqttBridgeController.getNnodeService().getTopicRouteDataByTopic(rootTopic, true);
            } catch (Exception e) {
                log.error("Exception was thrown when getTopicRouteDataByTopic. e={}", e.getMessage());
            }
            if (topicRouteData == null || topicRouteData.getBrokerDatas() == null || topicRouteData.getBrokerDatas().size() == 0) {
                continue;
            }
            for (BrokerData brokerData : topicRouteData.getBrokerDatas()) {
                String key = new StringBuilder().append(brokerData.getBrokerName()).append(TOPIC_CLIENTID_SEPARATOR).append(rootTopic).append(TOPIC_CLIENTID_SEPARATOR).append(clientId).toString();
                if (maxQos > 0) {
                    if (!consumeOffsetTable.containsKey(key)) {
                        long persistOffset = this.mqttBridgeController.getPersistService().queryConsumeOffset(key);
                        Long[] offsets = new Long[2];
                        offsets[0] = persistOffset == -1 ? getMaxOffset(brokerData.getBrokerName(), entry.getKey()) : persistOffset;
                        offsets[1] = offsets[0] - 1;
                        Long[] prev = consumeOffsetTable.putIfAbsent(key, offsets);
                        if (prev != null) {
                            log.info("[initConsumeOffset]. ConsumeOffset item already exist. key={}, offset={}", key, consumeOffsetTable.get(key));
                        }
                    }
                } else {

                    consumeOffsetTable.remove(key);
                    this.mqttBridgeController.getPersistService().deleteConsumeOffset(key);

                }
            }
        }
    }

    private long getMaxOffset(String enodeName,
        String topic) throws InterruptedException, RemotingTimeoutException, RemotingCommandException, RemotingSendRequestException, RemotingConnectException {
        GetMaxOffsetRequestHeader requestHeader = new GetMaxOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setQueueId(0);
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_MAX_OFFSET, requestHeader);

        return this.mqttBridgeController.getEnodeService().getMaxOffsetInQueue(enodeName, topic, 0, request);
    }

    private boolean isQosLegal(List<MqttTopicSubscription> mqttTopicSubscriptions) {
        for (MqttTopicSubscription subscription : mqttTopicSubscriptions) {
            if (!MqttUtil.isQosLegal(subscription.qualityOfService())) {
                return false;
            }
        }
        return true;
    }

    private boolean topicStartWithWildcard(List<MqttTopicSubscription> mqttTopicSubscriptions) {
        for (MqttTopicSubscription subscription : mqttTopicSubscriptions) {
            String rootTopic = MqttUtil.getRootTopic(subscription.topicName());
            if (rootTopic.contains(MqttConstant.SUBSCRIPTION_FLAG_PLUS) || rootTopic.contains(MqttConstant.SUBSCRIPTION_FLAG_SHARP) || rootTopic.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rejectRequest() {
        return false;
    }
}
