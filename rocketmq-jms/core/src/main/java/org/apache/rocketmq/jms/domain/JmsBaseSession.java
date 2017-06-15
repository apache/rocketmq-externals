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
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
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
import org.apache.rocketmq.jms.domain.message.JmsBytesMessage;
import org.apache.rocketmq.jms.domain.message.JmsObjectMessage;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;
import org.apache.rocketmq.jms.util.ExceptionUtil;

public class JmsBaseSession implements Session {
    protected CommonContext context;
    protected JmsBaseConnection connection;
    protected CopyOnWriteArrayList<JmsBaseMessageConsumer> consumerList =
        new CopyOnWriteArrayList<JmsBaseMessageConsumer>();
    private boolean transacted = true;
    private int acknowledgeMode = AUTO_ACKNOWLEDGE;

    public JmsBaseSession(JmsBaseConnection connection, boolean transacted,
        int acknowledgeMode, CommonContext context) {
        this.context = context;
        this.acknowledgeMode = acknowledgeMode;
        this.transacted = transacted;
        this.connection = connection;
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return new JmsBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Message createMessage() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return new JmsObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        return new JmsObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return new JmsTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        return new JmsTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return this.transacted;
    }

    @Override
    public int getAcknowledgeMode() {
        return this.acknowledgeMode;
    }

    @Override
    public void commit() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void rollback() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void close() throws JMSException {
        for (JmsBaseMessageConsumer messageConsumer : consumerList) {
            messageConsumer.close();
        }
        consumerList.clear();
    }

    @Override
    public void recover() throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return null;
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        return new JmsBaseMessageProducer(destination, context);
    }

    /**
     * Create a MessageConsumer.
     * <p/>
     * <P>Create a durable consumer to the specified destination
     *
     * @param destination Equals to Topic:MessageType in ROCKETMQ
     * @throws javax.jms.JMSException
     * @see <CODE>Destination</CODE>
     */
    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        JmsBaseMessageConsumer messageConsumer = new
            JmsBaseMessageConsumer(destination, this.context, this.connection);
        this.consumerList.addIfAbsent(messageConsumer);
        return messageConsumer;
    }

    /**
     * Create a MessageConsumer with messageSelector.
     * <p/>
     * <P>ROCKETMQ-JMS do not support using messageSelector to filter messages
     *
     * @param destination Equals to Topic:MessageType in ROCKETMQ
     * @param messageSelector For filtering messages
     * @throws javax.jms.JMSException
     * @see <CODE>Destination</CODE>
     */
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector)
        throws JMSException {
        throw new UnsupportedOperationException("Unsupported");

    }

    /**
     * Create a MessageConsumer with messageSelector.
     * <p/>
     * <P>ROCKETMQ-JMS do not support using messageSelector to filter messages and do not support this mechanism to reject
     * messages from localhost.
     *
     * @param destination Equals to Topic:MessageType in ROCKETMQ
     * @param messageSelector For filtering messages
     * @param noLocal If true: reject messages from localhost
     * @throws javax.jms.JMSException
     * @see <CODE>Destination</CODE>
     */
    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector,
        boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        Preconditions.checkNotNull(topicName);
        List<String> msgTuple = Arrays.asList(topicName.split(":"));

        Preconditions.checkState(msgTuple.size() >= 1 && msgTuple.size() <= 2,
            "Destination must match messageTopic:messageType !");

        //If messageType is null, use * instead.
        if (1 == msgTuple.size()) {
            return new JmsBaseTopic(msgTuple.get(0), "*");
        }
        return new JmsBaseTopic(msgTuple.get(0), msgTuple.get(1));
    }

    /**
     * Create a MessageConsumer with durable subscription.
     * <p/>
     * <P>When using <CODE>createConsumer(Destination)</CODE> method, one creates a MessageConsumer with a durable
     * subscription. So use <CODE>createConsumer(Destination)</CODE> instead of these method.
     *
     * @param topic destination
     * @throws javax.jms.JMSException
     * @see <CODE>Topic</CODE>
     */
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name)
        throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    /**
     * Create a MessageConsumer with durable subscription.
     * <p/>
     * <P>When using <CODE>createConsumer(Destination)</CODE> method, one creates a MessageConsumer with a durable
     * subscription. So use <CODE>createConsumer(Destination)</CODE> instead of these method.
     *
     * @param topic destination
     * @throws javax.jms.JMSException
     * @see <CODE>Topic</CODE>
     */
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name,
        String messageSelector,
        boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return new TemporaryQueue() {
            public void delete() throws JMSException {
            }

            public String getQueueName() throws JMSException {
                return UUID.randomUUID().toString();
            }
        };
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return new TemporaryTopic() {
            public void delete() throws JMSException {
            }

            public String getTopicName() throws JMSException {
                return UUID.randomUUID().toString();
            }
        };
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    public void start() throws JMSException {
        for (JmsBaseMessageConsumer messageConsumer : consumerList) {
            messageConsumer.startConsumer();
        }
    }

}
