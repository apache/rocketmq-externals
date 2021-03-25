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
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.util.MqttUtil;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQSubscribeConsumer implements SubscribeConsumer {
    private Logger logger = LoggerFactory.getLogger(RocketMQSubscribeConsumer.class);

    private MqttBridgeConfig bridgeConfig;
    private SubscriptionStore subscriptionStore;
    private final ExecutorService taskExecutor;
    private Map<String, Future> rmqTopicToFutureMap;

    public RocketMQSubscribeConsumer(MqttBridgeConfig bridgeConfig, SubscriptionStore subscriptionStore)
        throws MQClientException {
        this.bridgeConfig = bridgeConfig;
        this.subscriptionStore = subscriptionStore;
        this.taskExecutor = Executors.newCachedThreadPool();
        this.rmqTopicToFutureMap = new ConcurrentHashMap<>();
    }

    @Override public void start() throws MQClientException {

    }

    @Override public synchronized void subscribe(String mqttTopic) throws MQClientException {
        String rmqTopic = MqttUtil.getMqttRootTopic(mqttTopic);
        if (!rmqTopicToFutureMap.containsKey(rmqTopic)) {
            ConsumerPullTask pullTask = new ConsumerPullTask(bridgeConfig, rmqTopic, subscriptionStore);
            Future future = taskExecutor.submit(pullTask);
            rmqTopicToFutureMap.put(rmqTopic, future);
            logger.info("RocketMQ consumer subscribe rmqTopic:{}", rmqTopic);
        }
    }

    @Override public synchronized void unsubscribe(String mqttTopic) throws MQClientException {
        String rmqTopic = MqttUtil.getMqttRootTopic(mqttTopic);
        Set<String> mqttTopicSet = subscriptionStore.getSubTopicList(rmqTopic);
        if (mqttTopicSet.isEmpty()) {
            rmqTopicToFutureMap.get(rmqTopic).cancel(true);
            rmqTopicToFutureMap.remove(rmqTopic);
            logger.info("RocketMQ consumer unsubscribe rmqTopic:{}", rmqTopic);
        }
    }

    @Override public void shutdown() {
        taskExecutor.shutdown();
    }
}
