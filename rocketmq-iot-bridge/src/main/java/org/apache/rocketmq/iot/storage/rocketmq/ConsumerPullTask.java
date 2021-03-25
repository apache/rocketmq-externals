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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.consumer.PullResultExt;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.constant.MsgPropertyKey;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.remoting.RPCHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerPullTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerPullTask.class);

    private MqttBridgeConfig bridgeConfig;
    private String rmqTopic;
    private Map<MessageQueue, Long> mqOffsetMap;
    private boolean isRunning;
    private DefaultMQPullConsumer pullConsumer;
    private SubscriptionStore subscriptionStore;

    public ConsumerPullTask(MqttBridgeConfig bridgeConfig, String rmqTopic, SubscriptionStore subscriptionStore) {
        this.bridgeConfig = bridgeConfig;
        this.rmqTopic = rmqTopic;
        this.mqOffsetMap = new HashMap<>();
        this.subscriptionStore = subscriptionStore;
    }

    @Override public void run() {
        this.isRunning = true;
        logger.info("{} task is running.", rmqTopic);
        try {
            startPullConsumer();
            fetchSubscribeMq();
            logger.info("mqOffsetMap:" + mqOffsetMap.keySet());

            while (isRunning && !mqOffsetMap.isEmpty()) {
                Thread.sleep(1);
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
        this.pullConsumer = new DefaultMQPullConsumer(bridgeConfig.getRmqConsumerGroup(), rpcHook);
        this.pullConsumer.setMessageModel(MessageModel.CLUSTERING);
        this.pullConsumer.setNamesrvAddr(bridgeConfig.getRmqNamesrvAddr());
        this.pullConsumer.start();
    }

    private void fetchSubscribeMq() throws MQClientException {
        Set<MessageQueue> mqSet = pullConsumer.fetchSubscribeMessageQueues(rmqTopic);
        for (MessageQueue mq : mqSet) {
            if (!mqOffsetMap.containsKey(mq)) {
                mqOffsetMap.put(mq, Long.MAX_VALUE);
                logger.info("RocketMQ consumer subscribe rocketMQ topic[{}] and messageQueue[{}].", rmqTopic, mq);
            }
        }
    }

    private void pullMessages() {
        for (MessageQueue mq : mqOffsetMap.keySet()) {
            try {
                PullResultExt pullResult = (PullResultExt) pullConsumer.pullBlockIfNotFound(mq,
                    "*", this.mqOffsetMap.get(mq), bridgeConfig.getRmqConsumerPullNums());
                switch (pullResult.getPullStatus()) {
                    case FOUND:
                        this.mqOffsetMap.put(mq, pullResult.getNextBeginOffset());
                        sendSubscriptionClient(pullResult.getMsgFoundList());
                        break;

                    case OFFSET_ILLEGAL:
                        this.mqOffsetMap.put(mq, pullResult.getNextBeginOffset());

                    default:
                        break;
                }
            } catch (Exception e) {
                logger.error("pull task rocketMQ  messages exception, rmq topic: {}", rmqTopic, e);
            }
        }
    }

    private void sendSubscriptionClient(List<MessageExt> messageExtList) {
        for (MessageExt messageExt : messageExtList) {
            boolean isDup = Boolean.parseBoolean(messageExt.getUserProperty(MsgPropertyKey.MSG_IS_DUP));
            MqttQoS qosLevel = MqttQoS.valueOf(Integer.parseInt(messageExt.getUserProperty(MsgPropertyKey.MSG_QOS_LEVEL)));
            boolean isRetain = Boolean.parseBoolean(messageExt.getUserProperty(MsgPropertyKey.MSG_IS_RETAIN));
            int remainingLength = Integer.parseInt(messageExt.getUserProperty(MsgPropertyKey.MSG_REMAINING_LENGTH));
            int packetId = Integer.parseInt(messageExt.getUserProperty(MsgPropertyKey.MSG_PACKET_ID));

            String mqttTopic = messageExt.getUserProperty(MsgPropertyKey.MQTT_TOPIC);
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

    public void stop() throws MQClientException {
        this.isRunning = false;
        if (this.pullConsumer != null) {
            this.pullConsumer.shutdown();
        }
    }
}
