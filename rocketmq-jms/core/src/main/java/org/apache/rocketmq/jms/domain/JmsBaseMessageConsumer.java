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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.jms.util.ExceptionUtil;

public class JmsBaseMessageConsumer implements MessageConsumer {

    private static final Object LOCK_OBJECT = new Object();
    //all shared consumers
    private static ConcurrentMap<String/**consumerId*/, RMQPushConsumerExt> consumerMap = new MapMaker().makeMap();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private CommonContext context;
    private Destination destination;
    private MessageListener messageListener;

    public JmsBaseMessageConsumer(Destination destination, CommonContext commonContext,
        JmsBaseConnection connection) throws JMSException {
        synchronized (LOCK_OBJECT) {
            checkArgs(destination, commonContext);

            if (null == consumerMap.get(context.getConsumerId())) {
                DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(context.getConsumerId());
                if (context.getConsumeThreadNums() > 0) {
                    consumer.setConsumeThreadMax(context.getConsumeThreadNums());
                    consumer.setConsumeThreadMin(context.getConsumeThreadNums());
                }
                if (!Strings.isNullOrEmpty(context.getNameServer())) {
                    consumer.setNamesrvAddr(context.getNameServer());
                }
                if (!Strings.isNullOrEmpty(context.getInstanceName())) {
                    consumer.setInstanceName(context.getInstanceName());
                }
                consumer.setConsumeMessageBatchMaxSize(1);
                //add subscribe?
                RMQPushConsumerExt rocketmqConsumerExt = new RMQPushConsumerExt(consumer);
                consumerMap.putIfAbsent(context.getConsumerId(), rocketmqConsumerExt);
            }

            consumerMap.get(context.getConsumerId()).incrementAndGet();

            //If the connection has been started, start the consumer right now.
            //add start status?
            RMQPushConsumerExt consumerExt = consumerMap.get(context.getConsumerId());
            if (connection.isStarted()) {
                try {
                    consumerExt.start();
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
        RMQPushConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
        if (null != rocketmqConsumerExt) {
            try {
                this.messageListener = listener;
                String messageTopic = ((JmsBaseTopic) destination).getMessageTopic();
                String messageType = ((JmsBaseTopic) destination).getMessageType();
                rocketmqConsumerExt.subscribe(messageTopic, messageType, listener);
            }
            catch (MQClientException mqe) {
                //add what?
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
        synchronized (LOCK_OBJECT) {
            if (closed.compareAndSet(false, true)) {
                RMQPushConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
                if (null != rocketmqConsumerExt && 0 == rocketmqConsumerExt.decrementAndGet()) {
                    rocketmqConsumerExt.close();
                    consumerMap.remove(context.getConsumerId());
                }
            }
        }
    }

    /**
     * Start the consumer to get message from the Broker.
     */
    public void startConsumer() throws JMSException {
        RMQPushConsumerExt rocketmqConsumerExt = consumerMap.get(context.getConsumerId());
        if (null != rocketmqConsumerExt) {
            try {
                rocketmqConsumerExt.start();
            }
            catch (MQClientException mqe) {
                throw ExceptionUtil.convertToJmsException(mqe, "Start consumer failed");
            }
        }
    }

    public Destination getDestination() throws JMSException {
        return this.destination;
    }
}
