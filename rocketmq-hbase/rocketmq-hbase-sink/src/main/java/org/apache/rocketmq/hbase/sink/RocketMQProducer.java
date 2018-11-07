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
package org.apache.rocketmq.hbase.sink;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents the RocketMQ producer that effectively pushes the messages to the RocketMQ server.
 */
public class RocketMQProducer {
    private static final String PRODUCER_GROUP_NAME = "HBASE_PRODUCER_GROUP";

    private static final Logger logger = LoggerFactory.getLogger(RocketMQProducer.class);

    private DefaultMQProducer producer;

    private String namesrvAddr;

    private String topic;

    /**
     * Constructor.
     *
     * @param namesrvAddr the nameserver address
     * @param topic the topic to write to
     */
    public RocketMQProducer(String namesrvAddr, String topic) {
        this.namesrvAddr = namesrvAddr;
        this.topic = topic;
    }

    /**
     * Starts the producer.
     *
     * @throws MQClientException
     */
    public void start() throws MQClientException {
        producer = new DefaultMQProducer(PRODUCER_GROUP_NAME);
        producer.setInstanceName("producer-" + System.currentTimeMillis());
        producer.setNamesrvAddr(namesrvAddr);
        producer.start();
    }

    /**
     * Pushes messages to the RocketMQ server.
     *
     * @param json the message to be sent in json format
     * @return the result status code
     * @throws Exception
     */
    public long push(String json) throws Exception {
        logger.debug(json);

        Message message = new Message(topic, json.getBytes("UTF-8"));
        SendResult sendResult = producer.send(message);

        return sendResult.getQueueOffset();
    }

    /**
     * Stops the RocketMQ producer.
     */
    public void stop() {
        producer.shutdown();
    }
}
