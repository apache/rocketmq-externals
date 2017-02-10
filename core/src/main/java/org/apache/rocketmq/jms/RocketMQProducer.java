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

import com.alibaba.rocketmq.client.ClientConfig;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.client.producer.SendStatus;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import org.apache.rocketmq.jms.support.JmsHelper;
import org.apache.rocketmq.jms.support.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javax.jms.Message.DEFAULT_DELIVERY_MODE;
import static javax.jms.Message.DEFAULT_PRIORITY;
import static javax.jms.Message.DEFAULT_TIME_TO_LIVE;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.apache.rocketmq.jms.support.MessageConverter.JMS_MSGMODEL;

public class RocketMQProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQProducer.class);

    private RocketMQSession session;

    private final DefaultMQProducer mqProducer;

    private Destination destination;

    private AtomicLong counter = new AtomicLong(1L);

    public RocketMQProducer(RocketMQSession session, Destination destination) {
        this.session = session;
        this.destination = destination;

        this.mqProducer = new DefaultMQProducer(UUID.randomUUID().toString());
        ClientConfig clientConfig = this.session.getConnection().getClientConfig();
        this.mqProducer.setNamesrvAddr(clientConfig.getNamesrvAddr());
        this.mqProducer.setInstanceName(clientConfig.getInstanceName());
        try {
            this.mqProducer.start();
        }
        catch (MQClientException e) {
            throw new JMSRuntimeException(format("Fail to start producer, error msg:%s", getStackTrace(e)));
        }
    }

    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        //todo
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        //todo
        return false;
    }

    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        //todo
    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        //todo
        return false;
    }

    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        //todo
    }

    @Override
    public int getDeliveryMode() throws JMSException {
        //todo
        return 0;
    }

    @Override
    public void setPriority(int defaultPriority) throws JMSException {
        //todo
    }

    @Override
    public int getPriority() throws JMSException {
        //todo
        return 0;
    }

    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        //todo
    }

    @Override
    public long getTimeToLive() throws JMSException {
        //todo
        return 0;
    }

    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        //todo
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        //todo
        return 0;
    }

    @Override
    public Destination getDestination() throws JMSException {
        //todo
        return null;
    }

    @Override
    public void close() throws JMSException {
        this.mqProducer.shutdown();
    }

    @Override
    public void send(Message message) throws JMSException {
        this.send(this.destination, message);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        this.send(this.destination, message, deliveryMode, priority, timeToLive);
    }

    @Override
    public void send(Destination destination, Message message) throws JMSException {
        //todo: DEFAULT_TIME_TO_LIVE is zero which means message never expires. This feature is not supported by RMQ now.
        this.send(destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority,
        long timeToLive) throws JMSException {
        String topicName = JmsHelper.getTopicName(destination);

        com.alibaba.rocketmq.common.message.Message rmqMsg = createRmqMessage(message, topicName);

        sendSync(rmqMsg);
    }

    private void sendSync(com.alibaba.rocketmq.common.message.Message rmqMsg) throws JMSException {
        SendResult sendResult;

        try {
            sendResult = mqProducer.send(rmqMsg);
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
        }

        if (sendResult != null && sendResult.getSendStatus() == SendStatus.SEND_OK) {
            log.debug("Success to send message[key={}]", rmqMsg.getKeys());
            return;
        }
        else {
            throw new JMSException(format("Sending message error with result status:%s", sendResult.getSendStatus().name()));
        }
    }

    private void sendAsync(com.alibaba.rocketmq.common.message.Message rmqMsg,
        CompletionListener completionListener) throws JMSException {
        try {
            mqProducer.send(rmqMsg, new SendCompletionListener(completionListener));
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
        }
    }

    private com.alibaba.rocketmq.common.message.Message createRmqMessage(Message message,
        String topicName) throws JMSException {
        JmsContent jmsContent = MessageConverter.getContentFromJms(message);
        com.alibaba.rocketmq.common.message.Message rmqMsg = new com.alibaba.rocketmq.common.message.Message(topicName, jmsContent.getContent());
        rmqMsg.putUserProperty(JMS_MSGMODEL, jmsContent.getMessageModel());
        rmqMsg.setKeys(System.currentTimeMillis() + "" + counter.incrementAndGet());
        return rmqMsg;
    }

    @Override
    public void send(Message message, CompletionListener completionListener) throws JMSException {
        this.send(this.destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive,
        CompletionListener completionListener) throws JMSException {
        this.send(this.destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
    }

    @Override
    public void send(Destination destination, Message message,
        CompletionListener completionListener) throws JMSException {
        this.send(destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
        CompletionListener completionListener) throws JMSException {
        String topicName = JmsHelper.getTopicName(destination);

        com.alibaba.rocketmq.common.message.Message rmqMsg = createRmqMessage(message, topicName);

        sendAsync(rmqMsg, completionListener);
    }

}
