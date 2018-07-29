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

package org.apache.rocketmq.redis.replicator.mq;

import com.moilioncircle.redis.replicator.event.Event;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.redis.replicator.conf.Configure;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_CONSUMER_GROUP_NAME;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_DATA_TOPIC;
import static org.apache.rocketmq.redis.replicator.conf.ReplicatorConstants.ROCKETMQ_NAMESERVER_ADDRESS;

public class RocketMQRedisConsumer implements MessageListenerOrderly, Closeable {

    private String topic;
    private Serializer<Event> serializer;
    private DefaultMQPushConsumer consumer;
    protected final List<EventListener> eventListeners = new CopyOnWriteArrayList<>();

    public boolean addEventListener(EventListener listener) {
        return eventListeners.add(listener);
    }

    public boolean removeEventListener(EventListener listener) {
        return eventListeners.remove(listener);
    }


    public RocketMQRedisConsumer(Configure configure) throws MQClientException {
        Objects.requireNonNull(configure);
        this.serializer = new KryoEventSerializer();
        this.topic = configure.getString(ROCKETMQ_DATA_TOPIC);
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(configure.getString(ROCKETMQ_NAMESERVER_ADDRESS));
        consumer.setConsumerGroup(configure.getString(ROCKETMQ_CONSUMER_GROUP_NAME));
        consumer.subscribe(topic, "*");
        consumer.registerMessageListener(this);
        this.consumer = consumer;
    }

    @Override
    public ConsumeOrderlyStatus consumeMessage(List<MessageExt> messages, ConsumeOrderlyContext context) {
        for (MessageExt message : messages) {
            Event event = serializer.read(message.getBody());
            if (!eventListeners.isEmpty()) {
                for (EventListener listener : eventListeners) {
                    listener.onEvent(event);
                }
            }
        }
        return ConsumeOrderlyStatus.SUCCESS;
    }

    public void open() throws MQClientException {
        this.consumer.start();
    }

    @Override
    public void close() {
        this.consumer.shutdown();
    }
}
