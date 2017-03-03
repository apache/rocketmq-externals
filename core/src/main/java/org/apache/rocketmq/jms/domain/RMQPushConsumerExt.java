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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms.domain;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.rocketmq.client.consumer.MQPushConsumer;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.util.MessageConverter;

public class RMQPushConsumerExt {
    private final MQPushConsumer consumer;
    private final ConcurrentHashMap<String/* Topic */, javax.jms.MessageListener> subscribeTable = new ConcurrentHashMap<String, javax.jms.MessageListener>();

    private AtomicInteger referenceCount = new AtomicInteger(0);
    private AtomicBoolean started = new AtomicBoolean(false);

    public RMQPushConsumerExt(MQPushConsumer consumer) {
        this.consumer = consumer;
    }

    public MQPushConsumer getConsumer() {
        return consumer;
    }

    public int incrementAndGet() {
        return referenceCount.incrementAndGet();
    }

    public int decrementAndGet() {
        return referenceCount.decrementAndGet();
    }

    public int getReferenceCount() {
        return referenceCount.get();
    }
    public void start() throws MQClientException {
        if (consumer == null) {
            throw new MQClientException(-1, "consumer is null");
        }

        if (this.started.compareAndSet(false, true)) {
            this.consumer.registerMessageListener(new MessageListenerImpl());
            this.consumer.start();
        }
    }


    public void close() {
        if (this.started.compareAndSet(true, false)) {
            this.consumer.shutdown();
        }
    }

    public void subscribe(String topic, String subExpression, javax.jms.MessageListener listener) throws MQClientException {
        if (null == topic) {
            throw new MQClientException(-1, "topic is null");
        }

        if (null == listener) {
            throw new MQClientException(-1, "listener is null");
        }

        try {
            this.subscribeTable.put(topic, listener);
            this.consumer.subscribe(topic, subExpression);
        } catch (MQClientException e) {
            throw new MQClientException("consumer subscribe exception", e);
        }
    }

    public void unsubscribe(String topic) {
        if (null != topic) {
            this.consumer.unsubscribe(topic);
        }
    }

    class MessageListenerImpl implements MessageListenerConcurrently {

        @Override
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgsRMQList, ConsumeConcurrentlyContext contextRMQ) {
            MessageExt msgRMQ = msgsRMQList.get(0);
            javax.jms.MessageListener listener = RMQPushConsumerExt.this.subscribeTable.get(msgRMQ.getTopic());
            if (null == listener) {
                throw new RuntimeException("MessageListener is null");
            }

            try {
                listener.onMessage(MessageConverter.convert2JMSMessage(msgRMQ));
            }
            catch (Exception e) {
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }


    public boolean isStarted() {
        return started.get();
    }


    public boolean isClosed() {
        return !isStarted();
    }
}
