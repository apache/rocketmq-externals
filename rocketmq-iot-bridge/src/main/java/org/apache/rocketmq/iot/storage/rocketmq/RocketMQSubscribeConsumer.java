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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.util.MqttUtil;
import org.apache.rocketmq.iot.common.util.RocketAdminTools;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQSubscribeConsumer implements SubscribeConsumer {
    private Logger logger = LoggerFactory.getLogger(RocketMQSubscribeConsumer.class);

    private MqttBridgeConfig bridgeConfig;
    private SubscriptionStore subscriptionStore;

    private final ExecutorService taskExecutor;
    private RocketAdminTools rocketAdminTools;

    private Map<String, Runnable> rmqQueueToTaskMap = new ConcurrentHashMap<>();
    private Map<String, Future> rmqQueueToFutureMap = new ConcurrentHashMap<>();

    public RocketMQSubscribeConsumer(MqttBridgeConfig bridgeConfig, SubscriptionStore subscriptionStore)
        throws MQClientException {
        this.bridgeConfig = bridgeConfig;
        this.subscriptionStore = subscriptionStore;
        this.taskExecutor = Executors.newCachedThreadPool();
    }

    @Override public void start() {
        this.rocketAdminTools = RocketAdminTools.getInstance(bridgeConfig);
    }

    @Override public void subscribe(String mqttTopic, Subscription subscription) {
        String rmqTopic = MqttUtil.getMqttRootTopic(mqttTopic);
        Map<MessageQueue, TopicOffset> queueOffsetTable;
        try {
            queueOffsetTable = rocketAdminTools.getTopicQueueOffset(rmqTopic);
            if (queueOffsetTable.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            logger.error("examine rmqTopic[{}] offsetTable error.", rmqTopic, e);
            return;
        }

        synchronized (subscriptionStore) {
            subscriptionStore.append(mqttTopic, subscription);
        }

        for (MessageQueue messageQueue : queueOffsetTable.keySet()) {
            synchronized (rmqQueueToFutureMap) {
                if (!rmqQueueToFutureMap.containsKey(messageQueue.toString())) {
                    ConsumerPullTask pullTask = new ConsumerPullTask(bridgeConfig, messageQueue,
                        queueOffsetTable.get(messageQueue), subscriptionStore);
                    Future future = taskExecutor.submit(pullTask);
                    rmqQueueToFutureMap.put(messageQueue.toString(), future);
                    rmqQueueToTaskMap.put(messageQueue.toString(), pullTask);
                    logger.info("rocketMQ consumer submit pull task success, messageQueue:{}", messageQueue.toString());
                }
            }
        }
        logger.info("client[{}] subscribe the mqtt topic [{}] success.", subscription.getClient().getId(), mqttTopic);
    }

    @Override public void unsubscribe(String mqttTopic, Client client) {
        subscriptionStore.remove(mqttTopic, client);
        logger.info("client[{}] unsubscribe mqttTopic[{}] success.", client.getId(), mqttTopic);

        String rmqTopic = MqttUtil.getMqttRootTopic(mqttTopic);
        Map<MessageQueue, TopicOffset> queueOffsetTable;
        try {
            queueOffsetTable = rocketAdminTools.getTopicQueueOffset(rmqTopic);
            if (queueOffsetTable.isEmpty()) {
                return;
            }
        } catch (Exception e) {
            logger.error("examine rmqTopic[{}] offsetTable error.", rmqTopic, e);
            return;
        }

        synchronized (subscriptionStore) {
            Set<String> mqttTopicSet = subscriptionStore.getSubTopicList(rmqTopic);
            if (!mqttTopicSet.isEmpty()) {
                return;
            }

            for (MessageQueue messageQueue : queueOffsetTable.keySet()) {
                Runnable runnable = rmqQueueToTaskMap.get(messageQueue.toString());
                if (runnable != null && (runnable instanceof ConsumerPullTask)) {
                    ConsumerPullTask pullTask = (ConsumerPullTask) runnable;
                    pullTask.stop();
                    rmqQueueToTaskMap.remove(messageQueue.toString());
                }

                Future future = rmqQueueToFutureMap.get(messageQueue.toString());
                if (future != null) {
                    future.cancel(true);
                    rmqQueueToFutureMap.remove(messageQueue.toString());
                    logger.info("rocketMQ consumer cancel pull task success, messageQueue:{}", messageQueue.toString());
                }
            }
        }
    }

    @Override public void shutdown() {
        this.taskExecutor.shutdown();
        this.rocketAdminTools.shutdown();
    }
}
