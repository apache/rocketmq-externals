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
package org.apache.rocketmq.hbase.source;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the RocketMQ consumer that effectively pulls the messages from the RocketMQ topics.
 */
public class RocketMQConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RocketMQConsumer.class);

    private DefaultMQPullConsumer consumer;

    private String namesrvAddr;

    private String consumerGroup;

    private MessageModel messageModel;

    private Set<String> topics;

    private int batchSize;

    /**
     * Constructor.
     *
     * @param config the configuration
     */
    public RocketMQConsumer(Config config) {
        this.namesrvAddr = config.getNameserver();
        this.consumerGroup = config.getConsumerGroup();
        this.messageModel = MessageModel.valueOf(config.getMessageModel());
        this.topics = config.getTopics();
        this.batchSize = config.getBatchSize();
    }

    /**
     * Starts the rocketmq consumer.
     *
     * @throws MQClientException
     */
    public void start() throws MQClientException {
        consumer = new DefaultMQPullConsumer(consumerGroup);
        consumer.setNamesrvAddr(namesrvAddr);
        consumer.setMessageModel(messageModel);
        consumer.setRegisterTopics(topics);
        consumer.start();
        logger.info("RocketMQ consumer started.");
    }

    /**
     * Pulls messages from specified topics.
     *
     * @return a map containing a list of messages per topic
     * @throws MQClientException
     * @throws RemotingException
     * @throws InterruptedException
     * @throws MQBrokerException
     */
    public Map<String, List<MessageExt>> pull() throws MQClientException, RemotingException, InterruptedException,
        MQBrokerException {

        final Map<String, List<MessageExt>> messagesPerTopic = new HashMap<>();
        for (String topic : topics) {
            final Set<MessageQueue> msgQueues = consumer.fetchSubscribeMessageQueues(topic);
            for (MessageQueue msgQueue : msgQueues) {
                final long offset = getMessageQueueOffset(msgQueue);
                final PullResult pullResult = consumer.pull(msgQueue, null, offset, batchSize);

                if (pullResult.getPullStatus() == PullStatus.FOUND) {
                    messagesPerTopic.put(topic, pullResult.getMsgFoundList());

                    logger.debug("Pulled {} messages", pullResult.getMsgFoundList().size());
                }
            }
        }
        return messagesPerTopic.size() > 0 ? messagesPerTopic : null;
    }

    private long getMessageQueueOffset(MessageQueue queue) throws MQClientException {
        long offset = consumer.fetchConsumeOffset(queue, false);
        if (offset < 0) {
            offset = 0;
        }

        return offset;
    }

    /**
     * Stops the rocketmq consumer.
     */
    public void stop() {
        consumer.shutdown();
        logger.info("RocketMQ consumer stopped.");
    }
}
