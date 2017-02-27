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

import java.util.UUID;
import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.exception.UnsupportDeliveryModelException;
import org.apache.rocketmq.jms.hook.SendMessageHook;
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;
import org.apache.rocketmq.jms.msg.convert.JMS2RMQMessageConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_DELIVERY_MODE_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_DELIVERY_TIME_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_PRIORITY_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMS_TIME_TO_LIVE_DEFAULT_VALUE;
import static org.apache.rocketmq.jms.support.ObjectTypeCast.cast2Object;

public class RocketMQProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQProducer.class);
    private RocketMQSession session;
    private DefaultMQProducer rocketMQProducer;
    private Destination destination;

    private boolean disableMessageID;
    private boolean disableMessageTimestamp;
    private long timeToLive = JMS_TIME_TO_LIVE_DEFAULT_VALUE;
    private int deliveryMode = JMS_DELIVERY_MODE_DEFAULT_VALUE;
    private int priority = JMS_PRIORITY_DEFAULT_VALUE;
    private long deliveryDelay = JMS_DELIVERY_TIME_DEFAULT_VALUE;

    private SendMessageHook sendMessageHook;

    public RocketMQProducer() {
    }

    public RocketMQProducer(RocketMQSession session, Destination destination) {
        this.session = session;
        this.destination = destination;

        this.rocketMQProducer = new DefaultMQProducer(UUID.randomUUID().toString());
        ClientConfig clientConfig = this.session.getConnection().getClientConfig();
        this.rocketMQProducer.setNamesrvAddr(clientConfig.getNamesrvAddr());
        this.rocketMQProducer.setInstanceName(clientConfig.getInstanceName());
        try {
            this.rocketMQProducer.start();
        }
        catch (MQClientException e) {
            throw new JMSRuntimeException(format("Fail to start producer, error msg:%s", getStackTrace(e)));
        }

        this.sendMessageHook = new SendMessageHook(this);
    }

    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        this.disableMessageID = value;
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        return this.disableMessageID;
    }

    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        this.disableMessageTimestamp = value;
    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        return this.disableMessageTimestamp;
    }

    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        throw new UnsupportDeliveryModelException();
    }

    @Override
    public int getDeliveryMode() throws JMSException {
        return this.deliveryMode;
    }

    @Override
    public void setPriority(int priority) throws JMSException {
        this.priority = priority;
    }

    @Override
    public int getPriority() throws JMSException {
        return this.priority;
    }

    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        this.timeToLive = timeToLive;
    }

    @Override
    public long getTimeToLive() throws JMSException {
        return this.timeToLive;
    }

    @Override
    public void setDeliveryDelay(long deliveryDelay) throws JMSException {
        this.deliveryDelay = deliveryDelay;
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        return this.deliveryDelay;
    }

    @Override
    public Destination getDestination() throws JMSException {
        return this.destination;
    }

    @Override
    public void close() throws JMSException {
        this.rocketMQProducer.shutdown();
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
        this.send(destination, message, getDeliveryMode(), getPriority(), getTimeToLive());
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority,
        long timeToLive) throws JMSException {

        sendMessageHook.before(message, destination, deliveryMode, priority, timeToLive);

        MessageExt rmqMsg = createRocketMQMessage(message);

        SendResult sendResult = sendSync(rmqMsg);
        if (sendResult != null && sendResult.getSendStatus() == SendStatus.SEND_OK) {
            log.debug("Success to send message[key={}]", rmqMsg.getKeys());
            return;
        }
        else {
            throw new JMSException(format("Sending message error with result status:%s", sendResult.getSendStatus().name()));
        }
    }

    private SendResult sendSync(org.apache.rocketmq.common.message.Message rmqMsg) throws JMSException {

        try {
            return rocketMQProducer.send(rmqMsg);
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
        }
    }

    private void sendAsync(org.apache.rocketmq.common.message.Message rmqMsg,
        CompletionListener completionListener) throws JMSException {
        try {
            rocketMQProducer.send(rmqMsg, new SendCompletionListener(completionListener));
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to send message. Error: %s", getStackTrace(e)));
        }
    }

    private MessageExt createRocketMQMessage(Message jmsMsg) throws JMSException {
        AbstractJMSMessage abstractJMSMessage = cast2Object(jmsMsg, AbstractJMSMessage.class);
        try {
            return JMS2RMQMessageConvert.convert(abstractJMSMessage);
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to convert to RocketMQ jmsMsg. Error: %s", getStackTrace(e)));
        }
    }

    @Override
    public void send(Message message, CompletionListener completionListener) throws JMSException {
        this.send(this.destination, message, getDeliveryMode(), getPriority(), getTimeToLive(), completionListener);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive,
        CompletionListener completionListener) throws JMSException {
        this.send(this.destination, message, deliveryMode, priority, timeToLive, completionListener);
    }

    @Override
    public void send(Destination destination, Message message,
        CompletionListener completionListener) throws JMSException {
        this.send(destination, message, getDeliveryMode(), getPriority(), getTimeToLive(), completionListener);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive,
        CompletionListener completionListener) throws JMSException {

        sendMessageHook.before(message, destination, deliveryMode, priority, timeToLive);

        MessageExt rmqMsg = createRocketMQMessage(message);

        sendAsync(rmqMsg, completionListener);
    }

    public RocketMQSession getSession() {
        return session;
    }

    public void setSession(RocketMQSession session) {
        this.session = session;
    }

    public void setRocketMQProducer(DefaultMQProducer rocketMQProducer) {
        this.rocketMQProducer = rocketMQProducer;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public void setSendMessageHook(SendMessageHook sendMessageHook) {
        this.sendMessageHook = sendMessageHook;
    }
}
