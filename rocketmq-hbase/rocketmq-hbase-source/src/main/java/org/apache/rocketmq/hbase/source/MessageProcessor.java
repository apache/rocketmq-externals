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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageProcessor is the main class that is responsible for managing the main work flow of pulling messages from
 * RocketMQ topics and writing them into HBase.
 */
public class MessageProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private RocketMQConsumer consumer;

    private HBaseClient hbaseClient;

    private long pullInterval;

    private boolean on = false;

    /**
     * Constructor.
     *
     * @param config the configuration
     */
    public MessageProcessor(Config config) {
        pullInterval = config.getPullInterval();
        consumer = new RocketMQConsumer(config);
        hbaseClient = new HBaseClient(config);
    }

    /**
     * Starts the message processor by starting out the rocketmq consumer and the hbase client.
     *
     * @throws MQClientException
     * @throws IOException
     */
    public void start() throws MQClientException, IOException {
        consumer.start();
        hbaseClient.start();
        on = true;
        final Thread thread = new Thread(this);
        thread.start();
        logger.info("Message processor started.");
    }

    /**
     * This is the main method that runs in a separate thread and does the actual processing of pulling message from
     * rocketmq and writing them into hbase.
     */
    @Override
    public void run() {
        Map<String, List<MessageExt>> messagesPerTopic;
        while (on) {

            try {
                while ((messagesPerTopic = consumer.pull()) == null) {
                    Thread.sleep(pullInterval);
                }

                for (Map.Entry<String, List<MessageExt>> entry : messagesPerTopic.entrySet()) {
                    final String topic = entry.getKey();
                    final List<MessageExt> messages = entry.getValue();
                    hbaseClient.put(topic, messages);
                }

            } catch (Exception e) {
                logger.error("Error while processing messages.", e);
            }
        }
        consumer.stop();
        try {
            hbaseClient.stop();
        } catch (IOException e) {
            logger.error("HBase client failed to stop.", e);
        }
        logger.info("Message processor stopped.");
    }

    /**
     * Stops the message processor.
     */
    public void stop() {
        on = false;
    }
}
