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

import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.storage.rocketmq.SubscribeConsumer;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.iot.common.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttSubscribeMessageHandler implements MessageHandler {
    private Logger logger = LoggerFactory.getLogger(MqttSubscribeMessageHandler.class);

    private SubscriptionStore subscriptionStore;
    private SubscribeConsumer subscribeConsumer;

    public MqttSubscribeMessageHandler(SubscriptionStore subscriptionStore, SubscribeConsumer subscribeConsumer) {
        this.subscriptionStore = subscriptionStore;
        this.subscribeConsumer = subscribeConsumer;
    }

    /**
     * handle the SUBSCRIBE message from the client
     * <ol>
     * <li>validate the topic filters in each subscription</li>
     * <li>set actual qos of each filter</li>
     * <li>get the topics matching given filters</li>
     * <li>check the client authorization of each topic</li>
     * <li>generate SUBACK message which includes the subscription result for each TopicFilter</li>
     * <li>send SUBACK message to the client</li>
     * </ol>
     *
     * @param message the message wrapping MqttSubscriptionMessage
     * @return
     */
    @Override public void handleMessage(Message message) {
        Client client = message.getClient();
        String clientId = client.getId();
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) message.getPayload();
        List<MqttTopicSubscription> topicSubscriptions = subscribeMessage.payload().topicSubscriptions();
        List<Integer> grantQoss = new ArrayList<>();
        topicSubscriptions.forEach(topicSubscription -> {
            String mqttTopic = topicSubscription.topicName();
            int actualQos = MessageUtil.actualQos(topicSubscription.qualityOfService().value());
            grantQoss.add(actualQos);
            Subscription subscription = Subscription.Builder.newBuilder()
                    .client((MqttClient) client).qos(actualQos).build();
            if (subscribeConsumer != null) {
                subscribeConsumer.subscribe(mqttTopic, subscription);
            } else {
                subscriptionStore.append(mqttTopic, subscription);
            }
            logger.debug("client[{}] subscribe mqtt topic [{}] success", clientId, mqttTopic);
        });

        MqttSubAckMessage subackMessage = MessageUtil.getMqttSubackMessage(subscribeMessage, new MqttSubAckPayload(grantQoss));
        client.getCtx().writeAndFlush(subackMessage);
    }
}
