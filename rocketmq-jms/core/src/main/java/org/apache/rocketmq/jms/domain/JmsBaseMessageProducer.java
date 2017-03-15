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
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.jms.domain.message.JmsBaseMessage;
import org.apache.rocketmq.jms.domain.message.JmsBytesMessage;
import org.apache.rocketmq.jms.domain.message.JmsObjectMessage;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;
import org.apache.rocketmq.jms.util.ExceptionUtil;
import org.apache.rocketmq.jms.util.MessageConverter;
import org.apache.rocketmq.jms.util.MsgConvertUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsBaseMessageProducer implements MessageProducer {

    private static final Object LOCK_OBJECT = new Object();
    private static ConcurrentMap<String, MQProducer> producerMap = new MapMaker().makeMap();
    private final Logger logger = LoggerFactory.getLogger(JmsBaseMessageProducer.class);
    private CommonContext context;

    private Destination destination;

    public JmsBaseMessageProducer(Destination destination, CommonContext context) throws JMSException {
        synchronized (LOCK_OBJECT) {
            checkArgs(destination, context);

            if (null == producerMap.get(this.context.getProducerId())) {
                DefaultMQProducer producer = new DefaultMQProducer(context.getProducerId());
                if (!Strings.isNullOrEmpty(context.getNameServer())) {
                    producer.setNamesrvAddr(context.getNameServer());
                }
                if (!Strings.isNullOrEmpty(context.getInstanceName())) {
                    producer.setInstanceName(context.getInstanceName());
                }
                if (context.getSendMsgTimeoutMillis() > 0) {
                    producer.setSendMsgTimeout(context.getSendMsgTimeoutMillis());
                }
                try {
                    producer.start();
                }
                catch (MQClientException mqe) {
                    throw ExceptionUtil.convertToJmsException(mqe, String.format("Start producer failed:%s", context.getProducerId()));
                }
                producerMap.putIfAbsent(this.context.getProducerId(), producer);
            }

        }
    }

    private void checkArgs(Destination destination, CommonContext context) throws JMSException {
        Preconditions.checkNotNull(context.getProducerId(), "ProducerId can not be null!");
        Preconditions.checkNotNull(destination.toString(), "Destination can not be null!");
        this.context = context;
        this.destination = destination;
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        return false;
    }

    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        return false;
    }

    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public int getDeliveryMode() throws JMSException {
        return javax.jms.Message.DEFAULT_DELIVERY_MODE;
    }

    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public int getPriority() throws JMSException {
        return javax.jms.Message.DEFAULT_PRIORITY;
    }

    @Override
    public void setPriority(int defaultPriority) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public long getTimeToLive() throws JMSException {
        return JmsBaseConstant.DEFAULT_TIME_TO_LIVE;
    }

    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        ExceptionUtil.handleUnSupportedException();
    }

    @Override
    public Destination getDestination() throws JMSException {
        return this.destination;
    }

    @Override
    public void close() throws JMSException {
        //Nothing to do
    }

    @Override
    public void send(javax.jms.Message message) throws JMSException {
        this.send(getDestination(), message);
    }

    /**
     * Send the message to the defined Destination success---return normally. Exception---throw out JMSException.
     *
     * @param destination see <CODE>Destination</CODE>
     * @param message the message to be sent.
     * @throws javax.jms.JMSException
     */
    @Override
    public void send(Destination destination, javax.jms.Message message) throws JMSException {
        JmsBaseMessage jmsMsg = (JmsBaseMessage) message;
        initJMSHeaders(jmsMsg, destination);

        try {
            if (context == null) {
                throw new IllegalStateException("Context should be inited");
            }
            org.apache.rocketmq.common.message.Message rocketmqMsg = MessageConverter.convert2RMQMessage(jmsMsg);

            MQProducer producer = producerMap.get(context.getProducerId());

            if (producer == null) {
                throw new Exception("producer is null ");
            }
            SendResult sendResult = producer.send(rocketmqMsg);
            if (sendResult != null && sendResult.getSendStatus() == SendStatus.SEND_OK) {
                jmsMsg.setHeader(JmsBaseConstant.JMS_MESSAGE_ID, "ID:" + sendResult.getMsgId());
            } else {
                throw new Exception("SendResult is " + (sendResult == null ? "null" : sendResult.toString()));
            }
        }
        catch (Exception e) {
            logger.error("Send rocketmq message failure !", e);
            //if fail to send the message, throw out JMSException
            JMSException jmsException = new JMSException("Send rocketmq message failure!");
            jmsException.setLinkedException(e);
            throw jmsException;
        }
    }

    @Override
    public void send(javax.jms.Message message, int deliveryMode, int priority,
        long timeToLive) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public void send(Destination destination, javax.jms.Message message, int deliveryMode,
        int priority, long timeToLive) throws JMSException {
        throw new UnsupportedOperationException("Unsupported");
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
    private void initJMSHeaders(JmsBaseMessage jmsMsg, Destination destination) throws JMSException {

        //JMS_DESTINATION default:"topic:message"
        jmsMsg.setHeader(JmsBaseConstant.JMS_DESTINATION, destination);
        //JMS_DELIVERY_MODE default : PERSISTENT
        jmsMsg.setHeader(JmsBaseConstant.JMS_DELIVERY_MODE, javax.jms.Message.DEFAULT_DELIVERY_MODE);
        //JMS_TIMESTAMP default : current time
        jmsMsg.setHeader(JmsBaseConstant.JMS_TIMESTAMP, System.currentTimeMillis());
        //JMS_EXPIRATION default :  3 days
        //JMS_EXPIRATION = currentTime + time_to_live
        jmsMsg.setHeader(JmsBaseConstant.JMS_EXPIRATION, System.currentTimeMillis() + JmsBaseConstant.DEFAULT_TIME_TO_LIVE);
        //JMS_PRIORITY default : 4
        jmsMsg.setHeader(JmsBaseConstant.JMS_PRIORITY, javax.jms.Message.DEFAULT_PRIORITY);
        //JMS_TYPE default : open notification service
        jmsMsg.setHeader(JmsBaseConstant.JMS_TYPE, JmsBaseConstant.DEFAULT_JMS_TYPE);
        //JMS_REPLY_TO,JMS_CORRELATION_ID default : null
        //JMS_MESSAGE_ID is set by sendResult.
        //JMS_REDELIVERED is set by broker.
    }

    /**
     * Init the OnsMessage Headers.
     * <p/>
     * <P>When converting JmsMessage to OnsMessage, should read from the JmsMessage's Properties and write to the
     * OnsMessage's Properties.
     *
     * @param jmsMsg message
     * @throws javax.jms.JMSException
     */
    public static Properties initRocketMQHeaders(JmsBaseMessage jmsMsg,
        String topic, String messageType) throws JMSException {
        Properties userProperties = new Properties();

        //Jms userProperties to properties
        Map<String, Object> userProps = jmsMsg.getProperties();
        Iterator<Map.Entry<String, Object>> userPropsIter = userProps.entrySet().iterator();
        while (userPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = userPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        //Jms systemProperties to ROCKETMQ properties
        Map<String, Object> sysProps = jmsMsg.getHeaders();
        Iterator<Map.Entry<String, Object>> sysPropsIter = sysProps.entrySet().iterator();
        while (sysPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = sysPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }

        //Jms message Model
        if (jmsMsg instanceof JmsBytesMessage) {
            userProperties.setProperty(MsgConvertUtil.JMS_MSGMODEL, MsgConvertUtil.MSGMODEL_BYTES);
        }
        else if (jmsMsg instanceof JmsObjectMessage) {
            userProperties.setProperty(MsgConvertUtil.JMS_MSGMODEL, MsgConvertUtil.MSGMODEL_OBJ);
        }
        else if (jmsMsg instanceof JmsTextMessage) {
            userProperties.setProperty(MsgConvertUtil.JMS_MSGMODEL, MsgConvertUtil.MSGMODEL_TEXT);
        }

        //message topic and tag
        userProperties.setProperty(MsgConvertUtil.MSG_TOPIC, topic);
        userProperties.setProperty(MsgConvertUtil.MSG_TYPE, messageType);

        return userProperties;
    }

}

