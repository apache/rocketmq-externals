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
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.List;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.consumer.PullResultExt;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.iot.common.config.MqttBridgeConfig;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.constant.MsgPropertyKeyConstant;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerPullTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerPullTask.class);

    private MqttBridgeConfig bridgeConfig;
    private MessageQueue messageQueue;
    private long offset;
    private boolean isRunning;
    private DefaultMQPullConsumer pullConsumer;
    private SubscriptionStore subscriptionStore;

    public ConsumerPullTask(MqttBridgeConfig bridgeConfig, MessageQueue messageQueue,
                            TopicOffset topicOffset, SubscriptionStore subscriptionStore) {
        this.bridgeConfig = bridgeConfig;
        this.messageQueue = messageQueue;
        this.offset = topicOffset.getMaxOffset();
        this.subscriptionStore = subscriptionStore;
    }

    @Override
    public void run() {
        this.isRunning = true;
        logger.info("{} task is running.", messageQueue.toString());
        try {
            startPullConsumer();
            while (isRunning) {
                pullMessages();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPullConsumer() throws MQClientException {
        SessionCredentials sessionCredentials = new SessionCredentials(bridgeConfig.getRmqAccessKey(),
                bridgeConfig.getRmqSecretKey());
        RPCHook rpcHook = new AclClientRPCHook(sessionCredentials);
        this.pullConsumer = new DefaultMQPullConsumer(rpcHook);
        this.pullConsumer.setConsumerGroup(bridgeConfig.getRmqConsumerGroup());
        this.pullConsumer.setMessageModel(MessageModel.CLUSTERING);
        this.pullConsumer.setNamesrvAddr(bridgeConfig.getRmqNamesrvAddr());
        this.pullConsumer.setInstanceName(this.messageQueue.toString());
        this.pullConsumer.start();
    }

    private void pullMessages() {
        try {
            PullResultExt pullResult = (PullResultExt) pullConsumer.pullBlockIfNotFound(messageQueue,
                    "*", offset, bridgeConfig.getRmqConsumerPullNums());
            switch (pullResult.getPullStatus()) {
                case FOUND:
                    offset = pullResult.getNextBeginOffset();
                    sendSubscriptionClient(pullResult.getMsgFoundList());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("consumer pull task messages exception, messageQueue: {}", messageQueue.toString(), e);
        }
    }

    private void sendSubscriptionClient(List<MessageExt> messageExtList) {
        for (MessageExt messageExt : messageExtList) {
            boolean isDup = Boolean.parseBoolean(messageExt.getUserProperty(MsgPropertyKeyConstant.MSG_IS_DUP));
            MqttQoS qosLevel = MqttQoS.valueOf(Integer.parseInt(messageExt.getUserProperty(MsgPropertyKeyConstant.MSG_QOS_LEVEL)));
            boolean isRetain = Boolean.parseBoolean(messageExt.getUserProperty(MsgPropertyKeyConstant.MSG_IS_RETAIN));
            int remainingLength = Integer.parseInt(messageExt.getUserProperty(MsgPropertyKeyConstant.MSG_REMAINING_LENGTH));
            int packetId = Integer.parseInt(messageExt.getUserProperty(MsgPropertyKeyConstant.MSG_PACKET_ID));

            String mqttTopic = messageExt.getUserProperty(MsgPropertyKeyConstant.MQTT_TOPIC);
            byte[] body = messageExt.getBody();

            List<Subscription> subscriptionList = subscriptionStore.get(mqttTopic);
            if(subscriptionList.isEmpty()){
                return;
            }

            for (Subscription subscription : subscriptionList) {
                ByteBuf buffer = Unpooled.buffer();
                buffer.writeBytes(body);
                MqttPublishMessage msg = new MqttPublishMessage(
                        new MqttFixedHeader(
                                MqttMessageType.PUBLISH,
                                isDup,
                                qosLevel,
                                isRetain,
                                remainingLength
                        ),
                        new MqttPublishVariableHeader(
                                mqttTopic,
                                packetId
                        ),
                        buffer
                );

                Client subscriptionClient = subscription.getClient();
                subscriptionClient.getCtx().writeAndFlush(msg);
            }
        }
    }

    public void stop() {
        this.isRunning = false;
        if (this.pullConsumer != null) {
            this.pullConsumer.shutdown();
        }
    }
}