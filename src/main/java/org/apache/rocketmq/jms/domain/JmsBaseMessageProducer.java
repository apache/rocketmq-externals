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

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.MQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
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

    private static final Object lockObject = new Object();
    private static ConcurrentMap<String, MQProducer> producerMap = new MapMaker().makeMap();
    private final Logger logger = LoggerFactory.getLogger(JmsBaseMessageProducer.class);
    private CommonContext context;

    private Destination destination;

    public JmsBaseMessageProducer(Destination destination, CommonContext context) throws JMSException {
        synchronized (lockObject) {
            checkArgs(destination, context);

            if (null == producerMap.get(this.context.getProducerId())) {
                DefaultMQProducer producer = new DefaultMQProducer(context.getProducerId());
                if (!Strings.isNullOrEmpty(context.getNameServer())) {
                    producer.setNamesrvAddr(context.getNameServer());
                }
                if (!Strings.isNullOrEmpty(context.getInstanceName())) {
                    producer.setInstanceName(context.getInstanceName());
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
        return JmsBaseConstant.defaultTimeToLive;
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
        JmsBaseMessage jmsMsg = (JmsBaseMessage)message;
        initJMSHeaders(jmsMsg, destination);

        com.alibaba.rocketmq.common.message.Message rocketmqMsg = new com.alibaba.rocketmq.common.message.Message();
        try {
            if (context == null) {
                throw new IllegalStateException("Context should be inited");
            }

            // 1. Transform message body
            rocketmqMsg.setBody(MessageConverter.getContentFromJms(jmsMsg));

            // 2. Transform topic and messageType
            String topic = ((JmsBaseTopic)destination).getMessageTopic();
            rocketmqMsg.setTopic(topic);
            String messageType = ((JmsBaseTopic)destination).getMessageType();
            Preconditions.checkState(!messageType.contains("||"),
                "'||' can not be in the destination when sending a message");
            rocketmqMsg.setTags(messageType);

            // 3. Transform message properties
            Properties properties = initOnsHeaders(jmsMsg, topic, messageType);
            for (String name : properties.stringPropertyNames()) {
                //TODO filter sys properties
                rocketmqMsg.putUserProperty(name, properties.getProperty(name));
            }

            // 4. Send the message
            MQProducer producer = producerMap.get(context.getProducerId());
            if (null != producer) {
                SendResult sendResult = producer.send(rocketmqMsg);
                if (sendResult != null) {
                    logger.info("send ons message success! msgId is {}", sendResult.getMsgId());
                    jmsMsg.setHeader(JmsBaseConstant.jmsMessageID, "ID:" + sendResult.getMsgId());
                }
            }
        }
        catch (Exception e) {
            logger.error("Send ons message failure !", e);
            //if fail to send the message, throw out JMSException
            JMSException jmsException = new JMSException("Send ons message failure!");
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

        //jmsDestination default:"topic:message"
        jmsMsg.setHeader(JmsBaseConstant.jmsDestination, destination);
        //jmsDeliveryMode default : PERSISTENT
        jmsMsg.setHeader(JmsBaseConstant.jmsDeliveryMode, javax.jms.Message.DEFAULT_DELIVERY_MODE);
        //jmsTimestamp default : current time
        jmsMsg.setHeader(JmsBaseConstant.jmsTimestamp, System.currentTimeMillis());
        //jmsExpiration default :  3 days
        //jmsExpiration = currentTime + time_to_live
        jmsMsg.setHeader(JmsBaseConstant.jmsExpiration, System.currentTimeMillis() + JmsBaseConstant.defaultTimeToLive);
        //jmsPriority default : 4
        jmsMsg.setHeader(JmsBaseConstant.jmsPriority, javax.jms.Message.DEFAULT_PRIORITY);
        //jmsType default : ons(open notification service)
        jmsMsg.setHeader(JmsBaseConstant.jmsType, JmsBaseConstant.defaultJmsType);
        //jmsReplyTo、jmsCorrelationID default : null
        //jmsMessageID is set by sendResult.
        //jmsRedelivered is set by broker.
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
    private Properties initOnsHeaders(JmsBaseMessage jmsMsg,
        String topic, String messageType) throws JMSException {
        Properties userProperties = new Properties();

        //Jms userProperties to ONS properties
        Map<String, Object> userProps = jmsMsg.getProperties();
        Iterator<Map.Entry<String, Object>> userPropsIter = userProps.entrySet().iterator();
        while (userPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = userPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        //Jms systemProperties to ONS properties
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

