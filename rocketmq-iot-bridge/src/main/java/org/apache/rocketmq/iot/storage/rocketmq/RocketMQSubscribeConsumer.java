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

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.iot.common.config.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.util.MqttUtil;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RocketMQSubscribeConsumer implements SubscribeConsumer{
    private Logger logger = LoggerFactory.getLogger(RocketMQSubscribeConsumer.class);

    private MqttBridgeConfig bridgeConfig;
    private SubscriptionStore subscriptionStore;
    private final ExecutorService taskExecutor;
    private RPCHook rpcHook;
    private DefaultMQAdminExt mqAdminExt;

    private Map<String, Runnable> rmqQueueToTaskMap = new ConcurrentHashMap<>();
    private Map<String, Future> rmqQueueToFutureMap = new ConcurrentHashMap<>();

    public RocketMQSubscribeConsumer(MqttBridgeConfig bridgeConfig, SubscriptionStore subscriptionStore) {
        this.bridgeConfig = bridgeConfig;
        this.subscriptionStore = subscriptionStore;
        this.taskExecutor = Executors.newCachedThreadPool();
        SessionCredentials sessionCredentials = new SessionCredentials(bridgeConfig.getRmqAccessKey(),
                bridgeConfig.getRmqSecretKey());
        this.rpcHook = new AclClientRPCHook(sessionCredentials);
    }

    @Override
    public void start() {
        try {
            this.mqAdminExt = new DefaultMQAdminExt(rpcHook);
            this.mqAdminExt.setNamesrvAddr(bridgeConfig.getRmqNamesrvAddr());
            this.mqAdminExt.setAdminExtGroup(bridgeConfig.getRmqNamesrvAddr());
            this.mqAdminExt.setInstanceName(MqttUtil.createInstanceName(bridgeConfig.getRmqNamesrvAddr()));
            this.mqAdminExt.start();
            logger.debug("rocketMQ mqAdminExt started.");
        } catch (MQClientException e) {
            logger.error("init rocketMQ mqAdminExt failed.", e);
        }
    }

    @Override
    public void subscribe(String mqttTopic, Subscription subscription) {
        String rmqTopic = MqttUtil.getRootTopic(mqttTopic);
        Map<MessageQueue, TopicOffset> queueOffsetTable;
        try {
            queueOffsetTable = mqAdminExt.examineTopicStats(rmqTopic).getOffsetTable();
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
                    logger.debug("rocketMQ consumer submit pull task success, messageQueue:{}", messageQueue.toString());
                }
            }
        }
        logger.info("client[{}] subscribe the mqtt topic [{}] success.", subscription.getClient().getId(), mqttTopic);
    }

    @Override
    public void unsubscribe(String mqttTopic, Client client) {
        subscriptionStore.remove(mqttTopic, client);
        logger.info("client[{}] unsubscribe mqttTopic[{}] success.", client.getId(), mqttTopic);

        String rmqTopic = MqttUtil.getRootTopic(mqttTopic);
        Map<MessageQueue, TopicOffset> queueOffsetTable;
        try {
            queueOffsetTable = mqAdminExt.examineTopicStats(rmqTopic).getOffsetTable();
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
                    logger.debug("rocketMQ consumer cancel pull task success, messageQueue:{}", messageQueue.toString());
                }
            }
        }
    }

    @Override
    public void shutdown() {
        taskExecutor.shutdown();
    }
}
