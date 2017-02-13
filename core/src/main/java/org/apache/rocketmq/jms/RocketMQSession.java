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

package org.apache.rocketmq.jms;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import org.apache.rocketmq.jms.msg.RocketMQBytesMessage;
import org.apache.rocketmq.jms.msg.RocketMQObjectMessage;
import org.apache.rocketmq.jms.msg.RocketMQTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.jms.Constant.DEFAULT_DURABLE;
import static org.apache.rocketmq.jms.Constant.DEFAULT_NO_LOCAL;
import static org.apache.rocketmq.jms.Constant.NO_MESSAGE_SELECTOR;

public class RocketMQSession implements Session {

    private static final Logger log = LoggerFactory.getLogger(RocketMQSession.class);

    private RocketMQConnection connection;

    private int acknowledgeMode;

    private boolean transacted;

    private ConsumeMessageService consumeMessageService;

    private final List<RocketMQProducer> producerList = new ArrayList();

    private final List<RocketMQConsumer> consumerList = new ArrayList();

    private final Set<RocketMQConsumer> asyncConsumerSet = new HashSet();

    private final Set<RocketMQConsumer> syncConsumerSet = new HashSet();

    public RocketMQSession(RocketMQConnection connection, int acknowledgeMode, boolean transacted) {
        this.connection = connection;
        this.acknowledgeMode = acknowledgeMode;
        this.transacted = transacted;

        this.consumeMessageService = new ConsumeMessageService(this);
        this.consumeMessageService.start();
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return new RocketMQBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public Message createMessage() throws JMSException {
        return new RocketMQBytesMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return new RocketMQObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
        return new RocketMQObjectMessage(serializable);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return new RocketMQTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        return new RocketMQTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return this.transacted;
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        return this.acknowledgeMode;
    }

    @Override
    public void commit() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void rollback() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void close() throws JMSException {
        for (RocketMQProducer producer : this.producerList) {
            producer.close();
        }
        for (RocketMQConsumer consumer : this.consumerList) {
            consumer.close();
        }
    }

    @Override
    public void recover() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void run() {
        //todo
        throw new JMSRuntimeException("Not support yet");
    }

    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        RocketMQProducer producer = new RocketMQProducer(this, destination);
        this.producerList.add(producer);
        return producer;
    }

    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return createConsumer(destination, NO_MESSAGE_SELECTOR);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        return createConsumer(destination, messageSelector, DEFAULT_NO_LOCAL);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector,
        boolean noLocal) throws JMSException {
        // ignore noLocal param as RMQ not support
        RocketMQConsumer consumer = new RocketMQConsumer(this, destination, messageSelector, DEFAULT_DURABLE);
        this.consumerList.add(consumer);

        return consumer;
    }

    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws JMSException {
        return createSharedConsumer(topic, sharedSubscriptionName, NO_MESSAGE_SELECTOR);
    }

    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName,
        String messageSelector) throws JMSException {
        RocketMQConsumer consumer = new RocketMQConsumer(this, topic, messageSelector, sharedSubscriptionName, DEFAULT_DURABLE);
        this.consumerList.add(consumer);

        return consumer;
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        return new RocketMQQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        Preconditions.checkNotNull(topicName);
        List<String> msgTuple = Arrays.asList(topicName.split(":"));

        Preconditions.checkState(msgTuple.size() >= 1 && msgTuple.size() <= 2,
            "Destination must match messageTopic:messageType !");

        //If messageType is null, use * instead.
        if (1 == msgTuple.size()) {
            return new RocketMQTopic(topicName);
        }
        return new RocketMQTopic(msgTuple.get(0), msgTuple.get(1));
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        return createDurableSubscriber(topic, name, NO_MESSAGE_SELECTOR, DEFAULT_NO_LOCAL);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector,
        boolean noLocal) throws JMSException {
        RocketMQTopicSubscriber subscriber = new RocketMQTopicSubscriber(this, topic, messageSelector, name, true);
        this.consumerList.add(subscriber);

        return subscriber;
    }

    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name) throws JMSException {
        return createDurableConsumer(topic, name, NO_MESSAGE_SELECTOR, true);
    }

    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector,
        boolean noLocal) throws JMSException {
        RocketMQConsumer consumer = new RocketMQConsumer(this, topic, messageSelector, name, true);
        this.consumerList.add(consumer);

        return consumer;
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws JMSException {
        return createSharedDurableConsumer(topic, name, NO_MESSAGE_SELECTOR);
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name,
        String messageSelector) throws JMSException {
        RocketMQConsumer consumer = new RocketMQConsumer(this, topic, messageSelector, name, true);
        this.consumerList.add(consumer);

        return consumer;
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        //todo
        throw new JMSException("Not support yet");
    }

    public List<RocketMQProducer> getProducerList() {
        return producerList;
    }

    public List<RocketMQConsumer> getConsumerList() {
        return consumerList;
    }

    public RocketMQConnection getConnection() {
        return connection;
    }

    public boolean isTransacted() {
        return transacted;
    }

    public void addSyncConsumer(RocketMQConsumer consumer) {
        this.syncConsumerSet.add(consumer);
    }

    public void addAsyncConsumer(RocketMQConsumer consumer) {
        this.asyncConsumerSet.add(consumer);
    }

    public boolean isAsyncModel() {
        return !this.asyncConsumerSet.isEmpty();
    }

    public boolean isSyncModel() {
        return !this.syncConsumerSet.isEmpty();
    }

    public ConsumeMessageService getConsumeMessageService() {
        return consumeMessageService;
    }
}
