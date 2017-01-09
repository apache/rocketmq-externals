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

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.MQPushConsumer;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import org.apache.rocketmq.jms.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsBaseMessageConsumer implements MessageConsumer {

    private static final Object lockObject = new Object();
    private static ConcurrentMap<String/**consumerId*/, RocketmqConsumerExt> consumerMap = new MapMaker().makeMap();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private CommonContext context;
    private Destination destination;
    private MessageListener messageListener;

    public JmsBaseMessageConsumer(Destination destination, CommonContext commonContext,
        JmsBaseConnection connection) throws JMSException {
        synchronized (lockObject) {
            checkArgs(destination, commonContext);

            if (null == consumerMap.get(context.getConsumerId())) {
                DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(context.getConsumerId());
                if (context.getConsumeThreadNums() > 0) {
                    consumer.setConsumeThreadMax(context.getConsumeThreadNums());
                    consumer.setConsumeThreadMin(context.getConsumeThreadNums());
                }
                //TODO subscribe
                RocketmqConsumerExt rocketmqConsumerExt = new RocketmqConsumerExt(consumer);
                consumerMap.putIfAbsent(context.getConsumerId(), rocketmqConsumerExt);
            }

            consumerMap.get(context.getConsumerId()).incrementAndGet();

            //If the connection has been started, start the consumer right now.
            //TODO start status
            MQPushConsumer consumer = consumerMap.get(context.getConsumerId()).getConsumer();
            if (connection.isStarted()) {
                try {
                    consumer.start();
                }
                catch (MQClientException mqe) {
                    JMSException jmsException = new JMSException("Start consumer failed " + context.getConsumerId());
                    jmsException.initCause(mqe);
                    throw jmsException;
                }
            }
        }

    }

    private void checkArgs(Destination destination, CommonContext context) throws JMSException {
        Preconditions.checkNotNull(context.getConsumerId(), "ConsumerId can not be null!");
        Preconditions.checkNotNull(destination.toString(), "Destination can not be null!");
        this.context = context;
        this.destination = destination;
    }

    @Override
    public String getMessageSelector() throws JMSException {
        return null;
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return this.messageListener;
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        RocketmqConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
        if (null != rocketmqConsumerExt) {
            try {
                MQPushConsumer consumer = rocketmqConsumerExt.getConsumer();
                if (null != consumer && this.context != null) {
                    String messageTopic = ((JmsBaseTopic)destination).getMessageTopic();
                    String messageType = ((JmsBaseTopic)destination).getMessageType();
                    JmsBaseMessageListener jmsBaseMessageListener = new JmsBaseMessageListener(listener);
                    consumer.registerMessageListener(jmsBaseMessageListener);
                    consumer.subscribe(messageTopic, messageType);
                    logger.info("Subscribe message->[topic={},type={}] success!", messageTopic, messageType);
                }
                this.messageListener = listener;
            }
            catch (MQClientException mqe) {
                //TODO
                throw new JMSException(mqe.getMessage());
            }

        }

    }

    @Override
    public Message receive() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    @Override
    public Message receive(long timeout) throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    @Override
    public Message receiveNoWait() throws JMSException {
        throw new UnsupportedOperationException("Unsupported!");
    }

    @Override
    public void close() throws JMSException {
        synchronized (lockObject) {
            if (closed.compareAndSet(false, true)) {
                RocketmqConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
                if (null != rocketmqConsumerExt && 0 == rocketmqConsumerExt.decrementAndGet()) {
                    rocketmqConsumerExt.getConsumer().shutdown();
                    consumerMap.remove(context.getConsumerId());
                }
            }
        }
    }

    /**
     * Start the consumer to get message from the Broker.
     */
    public void startConsumer() throws JMSException {
        RocketmqConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
        if (null != rocketmqConsumerExt) {
            MQPushConsumer consumer = rocketmqConsumerExt.getConsumer();
            if (null != consumer) {
                try {
                    consumer.start();
                }
                catch (MQClientException mqe) {
                    throw ExceptionUtil.convertToJmsException(mqe, "Start consumer failed");
                }
            }
        }
    }

    public Destination getDestination() throws JMSException {
        return this.destination;
    }
}
