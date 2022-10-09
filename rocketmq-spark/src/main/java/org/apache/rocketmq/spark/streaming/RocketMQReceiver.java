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

import org.apache.commons.lang.Validate;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spark.RocketMQConfig;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * RocketMQReceiver uses MQPushConsumer as the default implementation.
 * PushConsumer is a high level consumer API, wrapping the pulling details
 * Looks like broker push messages to consumer
 *
 * NOTE: This is no fault-tolerance guarantees, can lose data on receiver failure.
 * Recommend to use ReliableRocketMQReceiver which is fault-tolerance guarantees.
 */
public class RocketMQReceiver extends Receiver<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(RocketMQReceiver.class);

    protected MQPushConsumer consumer;
    protected boolean ordered;
    protected Properties properties;

    public RocketMQReceiver(Properties properties, StorageLevel storageLevel) {
        super(storageLevel);
        this.properties = properties;
    }

    @Override
    public void onStart() {
        Validate.notEmpty(properties, "Consumer properties can not be empty");
        ordered = RocketMQConfig.getBoolean(properties, RocketMQConfig.CONSUMER_MESSAGES_ORDERLY, false);

        consumer = new DefaultMQPushConsumer();
        RocketMQConfig.buildConsumerConfigs(properties, (DefaultMQPushConsumer) consumer);

        if (ordered) {
            consumer.registerMessageListener(new MessageListenerOrderly() {
                @Override
                public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
                                                           ConsumeOrderlyContext context) {
                    if (process(msgs)) {
                        return ConsumeOrderlyStatus.SUCCESS;
                    } else {
                        return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                    }
                }
            });
        } else {
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                                ConsumeConcurrentlyContext context) {
                    if (process(msgs)) {
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } else {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            });
        }

        try {
            consumer.start();
        } catch (MQClientException e) {
            // TODO add retry
            LOG.error("Failed to start rocketmq consumer because of client exception", e);
            throw new InternalError(e);
        } catch (Exception e) {
            // should not throw spark NonFatal error here
            LOG.error("Failed to start rocketmq consumer because of other exception", e);
            throw new InternalError(e);
        } finally {
            LOG.error("error when consumer start", "xx");
        }
    }

    public boolean process(List<MessageExt> msgs) {
        if (msgs.isEmpty()) {
            return true;
        }
        try {
            for (MessageExt msg : msgs) {
                this.store(msg);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onStop() {
        consumer.shutdown();
    }

}
