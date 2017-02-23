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
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;
import org.apache.rocketmq.jms.support.MessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;
import static javax.jms.Message.DEFAULT_DELIVERY_MODE;
import static javax.jms.Message.DEFAULT_PRIORITY;
import static javax.jms.Message.DEFAULT_TIME_TO_LIVE;
import static org.apache.commons.lang.exception.ExceptionUtils.getStackTrace;
import static org.apache.rocketmq.jms.Constant.DEFAULT_JMS_TYPE;
import static org.apache.rocketmq.jms.Constant.JMS_DELIVERY_MODE;
import static org.apache.rocketmq.jms.Constant.JMS_DESTINATION;
import static org.apache.rocketmq.jms.Constant.JMS_EXPIRATION;
import static org.apache.rocketmq.jms.Constant.JMS_PRIORITY;
import static org.apache.rocketmq.jms.Constant.JMS_TIMESTAMP;
import static org.apache.rocketmq.jms.Constant.JMS_TYPE;
import static org.apache.rocketmq.jms.Constant.MESSAGE_ID_PREFIX;
import static org.apache.rocketmq.jms.support.DirectTypeConverter.convert2Object;

public class RocketMQProducer implements MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQProducer.class);
    private RocketMQSession session;
    private final DefaultMQProducer rocketMQProducer;
    private Destination destination;

    private boolean disableMessageID;
    private boolean disableMessageTimestamp;
    private long timeToLive;

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
        this.timeToLive = timeToLive;
    }

    @Override
    public long getTimeToLive() throws JMSException {
        return this.getTimeToLive();
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
        //todo: DEFAULT_TIME_TO_LIVE is zero which means message never expires. This feature is not supported by RMQ now.
        this.send(destination, message, DEFAULT_DELIVERY_MODE, DEFAULT_PRIORITY, DEFAULT_TIME_TO_LIVE);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority,
        long timeToLive) throws JMSException {

        before(message);

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

    private MessageExt createRocketMQMessage(Message message) throws JMSException {
        AbstractJMSMessage jmsMsg = convert2Object(message, AbstractJMSMessage.class);
        initJMSHeaders(jmsMsg, destination);
        try {
            return MessageConverter.convert2RMQMessage(jmsMsg);
        }
        catch (Exception e) {
            throw new JMSException(format("Fail to convert to RocketMQ message. Error: %s", getStackTrace(e)));
        }
    }

    /**
     * Init the JmsMessage Headers.
     * <p/>
     * <P>JMS providers init message's headers. Do not allow user to set these by yourself.
     *
     * @param jmsMsg message
     * @param destination
     * @throws javax.jms.JMSException
     * @see <CODE>Destination</CODE>
     */
    private void initJMSHeaders(AbstractJMSMessage jmsMsg, Destination destination) throws JMSException {

        //JMS_DESTINATION default:"topic:message"
        jmsMsg.setHeader(JMS_DESTINATION, destination);
        //JMS_DELIVERY_MODE default : PERSISTENT
        jmsMsg.setHeader(JMS_DELIVERY_MODE, javax.jms.Message.DEFAULT_DELIVERY_MODE);
        //JMS_TIMESTAMP default : current time
        jmsMsg.setHeader(JMS_TIMESTAMP, System.currentTimeMillis());
        //JMS_EXPIRATION default :  3 days
        //JMS_EXPIRATION = currentTime + time_to_live
        jmsMsg.setHeader(JMS_EXPIRATION, System.currentTimeMillis() + DEFAULT_TIME_TO_LIVE);
        //JMS_PRIORITY default : 4
        jmsMsg.setHeader(JMS_PRIORITY, javax.jms.Message.DEFAULT_PRIORITY);
        //JMS_TYPE default : ons(open notification service)
        jmsMsg.setHeader(JMS_TYPE, DEFAULT_JMS_TYPE);
        //JMS_REPLY_TO,JMS_CORRELATION_ID default : null
        //JMS_MESSAGE_ID is set by sendResult.
        //JMS_REDELIVERED is set by broker.
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

        before(message);

        MessageExt rmqMsg = createRocketMQMessage(message);

        sendAsync(rmqMsg, completionListener);
    }

    private void before(Message message) throws JMSException {
        // timestamp
        if (!getDisableMessageTimestamp()) {
            message.setJMSTimestamp(System.currentTimeMillis());
        }

        // messageID is also required in async model, so {@link MessageExt#getMsgId()} can't be used.
        if (!getDisableMessageID()) {
            message.setJMSMessageID(new StringBuffer(MESSAGE_ID_PREFIX).append(UUID.randomUUID().getLeastSignificantBits()).toString());
        }

        // expiration
        if (getTimeToLive() != 0) {
            message.setJMSExpiration(System.currentTimeMillis() + getTimeToLive());
        }
        else {
            message.setJMSExpiration(0l);
        }
    }

}
