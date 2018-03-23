/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark.streaming;

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spark.RocketMQConfig;
import org.apache.spark.storage.StorageLevel;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The ReliableRocketMQReceiver is fault-tolerance guarantees
 */
public class ReliableRocketMQReceiver extends RocketMQReceiver {
    private BlockingQueue<MessageSet> queue;
    private MessageRetryManager messageRetryManager;
    private MessageSender sender;

    public ReliableRocketMQReceiver(Properties properties, StorageLevel storageLevel) {
        super(properties, storageLevel);
    }

    @Override
    public void onStart() {
        int queueSize = RocketMQConfig.getInteger(properties, RocketMQConfig.QUEUE_SIZE, RocketMQConfig.DEFAULT_QUEUE_SIZE);
        queue = new LinkedBlockingQueue<>(queueSize);

        int maxRetry = RocketMQConfig.getInteger(properties, RocketMQConfig.MESSAGES_MAX_RETRY, RocketMQConfig.DEFAULT_MESSAGES_MAX_RETRY);
        int ttl = RocketMQConfig.getInteger(properties, RocketMQConfig.MESSAGES_TTL, RocketMQConfig.DEFAULT_MESSAGES_TTL);
        this.messageRetryManager = new DefaultMessageRetryManager(queue, maxRetry, ttl);

        this.sender = new MessageSender();
        this.sender.setName("MessageSender");
        this.sender.setDaemon(true);
        this.sender.start();

        super.onStart();
    }

    @Override
    public boolean process(List<MessageExt> msgs) {
        if (msgs.isEmpty()) {
            return true;
        }
        MessageSet messageSet = new MessageSet(msgs);
        try {
            queue.put(messageSet);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void ack(Object msgId) {
        String id = msgId.toString();
        messageRetryManager.ack(id);
    }

    public void fail(Object msgId) {
        String id = msgId.toString();
        messageRetryManager.fail(id);
    }

    @Override
    public void onStop() {
        consumer.shutdown();
    }

    class MessageSender extends Thread {
        @Override
        public void run() {
            while (ReliableRocketMQReceiver.this.isStarted()) {
                MessageSet messageSet = null;
                try {
                    messageSet = queue.take();
                } catch (InterruptedException e) {
                    continue;
                }
                if (messageSet == null) {
                    continue;
                }

                messageRetryManager.mark(messageSet);
                try {
                    // To implement a reliable receiver, you have to use store(multiple-records) to store data
                    ReliableRocketMQReceiver.this.store(messageSet);
                    ack(messageSet.getId());
                } catch (Exception e) {
                    fail(messageSet.getId());
                }
            }
        }
    }

}
