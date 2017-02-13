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

package org.apache.rocketmq.jms.support;

import com.alibaba.rocketmq.common.message.Message;
import com.alibaba.rocketmq.common.message.MessageConst;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.jms.Constant;
import org.apache.rocketmq.jms.JmsContent;
import org.apache.rocketmq.jms.RocketMQQueue;
import org.apache.rocketmq.jms.RocketMQTopic;
import org.apache.rocketmq.jms.msg.RocketMQBytesMessage;
import org.apache.rocketmq.jms.msg.RocketMQMessage;
import org.apache.rocketmq.jms.msg.RocketMQObjectMessage;
import org.apache.rocketmq.jms.msg.RocketMQTextMessage;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.rocketmq.jms.Constant.NO_MESSAGE_SELECTOR;

public class MessageConverter {
    public static final String EMPTY_STRING = "";
    public static final String JMS_MSGMODEL = "jmsMsgModel";
    /**
     * To adapt this scene: "Notify client try to receive ObjectMessage sent by JMS client" Set notify out message
     * model, value can be textMessage OR objectMessage
     */
    public static final String COMPATIBLE_FIELD_MSGMODEL = "notifyOutMsgModel";
    public static final String MSGMODEL_TEXT = "textMessage";
    public static final String MSGMODEL_BYTES = "bytesMessage";
    public static final String MSGMODEL_OBJ = "objectMessage";

    public static final String MSG_TOPIC = "msgTopic";
    public static final String MSG_TYPE = "msgType";

    public static final byte[] EMPTY_BYTES = new byte[0];

    private static AtomicLong counter = new AtomicLong(1L);

    public static JmsContent getContentFromJms(javax.jms.Message jmsMessage) throws JMSException {
        if (jmsMessage == null) {
            return null;
        }

        JmsContent jmsContent = new JmsContent();
        if (jmsMessage instanceof TextMessage) {
            if (StringUtils.isEmpty(((TextMessage) jmsMessage).getText())) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            jmsContent.setMessageModel(MSGMODEL_TEXT);
            jmsContent.setContent(string2Bytes(((TextMessage) jmsMessage).getText(),
                Charsets.UTF_8.toString()));
        }
        else if (jmsMessage instanceof ObjectMessage) {
            if (((ObjectMessage) jmsMessage).getObject() == null) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            try {
                jmsContent.setMessageModel(MSGMODEL_OBJ);
                jmsContent.setContent(objectSerialize(((ObjectMessage) jmsMessage).getObject()));
            }
            catch (IOException e) {
                throw new JMSException(e.getMessage());
            }
        }
        else if (jmsMessage instanceof BytesMessage) {
            RocketMQBytesMessage bytesMessage = (RocketMQBytesMessage) jmsMessage;
            if (bytesMessage.getBodyLength() == 0) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            jmsContent.setMessageModel(MSGMODEL_BYTES);
            jmsContent.setContent(bytesMessage.getData());
        }
        else {
            throw new IllegalArgumentException("Unknown message type " + jmsMessage.getJMSType());
        }

        return jmsContent;
    }

    public static RocketMQMessage convert2JMSMessage(MessageExt msg) throws JMSException {
        if (msg == null) {
            return null;
        }

        RocketMQMessage message;
        if (MSGMODEL_BYTES.equals(
            msg.getUserProperty(JMS_MSGMODEL))) {
            message = new RocketMQBytesMessage(msg.getBody());
        }
        else if (MSGMODEL_OBJ.equals(
            msg.getUserProperty(JMS_MSGMODEL))) {
            try {
                message = new RocketMQObjectMessage(objectDeserialize(msg.getBody()));
            }
            catch (Exception e) {
                throw new JMSException(e.getMessage());
            }
        }
        else if (MSGMODEL_TEXT.equals(
            msg.getUserProperty(JMS_MSGMODEL))) {
            message = new RocketMQTextMessage(bytes2String(msg.getBody(),
                Charsets.UTF_8.toString()));
        }
        else {
            // rocketmq producer sends bytesMessage without setting JMS_MSGMODEL.
            message = new RocketMQBytesMessage(msg.getBody());
        }

        //-------------------------set headers-------------------------
        Map<String, Object> properties = new HashMap<String, Object>();

        message.setHeader(Constant.JMS_MESSAGE_ID, "ID:" + msg.getMsgId());

        if (msg.getReconsumeTimes() > 0) {
            message.setHeader(Constant.JMS_REDELIVERED, Boolean.TRUE);
        }
        else {
            message.setHeader(Constant.JMS_REDELIVERED, Boolean.FALSE);
        }

        Map<String, String> propertiesMap = msg.getProperties();
        if (propertiesMap != null) {
            for (String properName : propertiesMap.keySet()) {
                String properValue = propertiesMap.get(properName);
                if (Constant.JMS_DESTINATION.equals(properName)) {
                    String destinationStr = properValue;
                    if (null != destinationStr) {
                        List<String> msgTuple = Arrays.asList(destinationStr.split(":"));
                        //todo: what about Queue?
                        RocketMQTopic topic = null;
                        if (msgTuple.size() == 1) {
                            topic = new RocketMQTopic(msgTuple.get(0));
                        }
                        else {
                            topic = new RocketMQTopic(msgTuple.get(0), msgTuple.get(1));
                        }
                        message.setHeader(Constant.JMS_DESTINATION,
                            topic);
                    }
                }
                else if (Constant.JMS_DELIVERY_MODE.equals(properName) ||
                    Constant.JMS_PRIORITY.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (Constant.JMS_TIMESTAMP.equals(properName) ||
                    Constant.JMS_EXPIRATION.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (Constant.JMS_CORRELATION_ID.equals(properName) ||
                    Constant.JMS_TYPE.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (Constant.JMS_MESSAGE_ID.equals(properName) ||
                    Constant.JMS_REDELIVERED.equals(properName)) {
                    //JMS_MESSAGE_ID should set by msg.getMsgID()
                    continue;
                }
                else {
                    properties.put(properName, properValue);
                }
            }
        }

        //Handle System properties, put into header.
        message.setProperties(properties);

        return message;
    }

    public static byte[] objectSerialize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }

    public static Serializable objectDeserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        ois.close();
        bais.close();
        return (Serializable) ois.readObject();
    }

    public static final byte[] string2Bytes(String s, String charset) {
        if (null == s) {
            return EMPTY_BYTES;
        }
        byte[] bs = null;
        try {
            bs = s.getBytes(charset);
        }
        catch (Exception e) {
            // ignore
        }
        return bs;
    }

    public static final String bytes2String(byte[] bs, String charset) {
        if (null == bs) {
            return EMPTY_STRING;
        }
        String s = null;
        try {
            s = new String(bs, charset);
        }
        catch (Exception e) {
            // ignore
        }
        return s;
    }

    public static Message convert2RMQMessage(RocketMQMessage jmsMsg) throws Exception {
        Message rmqMsg = new MessageExt();

        rmqMsg.setKeys(System.currentTimeMillis() + "" + counter.incrementAndGet());

        // 1. Transform message body
        rmqMsg.setBody(MessageConverter.getContentFromJms(jmsMsg).getContent());

        // 2. Transform topic and messageType
        String topic, tag;
        Destination destination = (Destination) jmsMsg.getHeaders().get(Constant.JMS_DESTINATION);
        if (destination instanceof Topic) {
            topic = ((RocketMQTopic) destination).getTopicName();
            tag = ((RocketMQTopic) destination).getTypeName();
        }
        else {
            topic = ((RocketMQQueue) destination).getQueueName();
            tag = NO_MESSAGE_SELECTOR;
        }
        checkState(!tag.contains("||"), "'||' can not be in the destination when sending a message");
        rmqMsg.setTopic(topic);
        rmqMsg.setTags(tag);

        // 3. Transform message properties
        Properties properties = getAllProperties(jmsMsg, topic, tag);
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (MessageConst.PROPERTY_KEYS.equals(name)) {
                rmqMsg.setKeys(value);
            }
            else if (MessageConst.PROPERTY_TAGS.equals(name)) {
                rmqMsg.setTags(value);
            }
            else if (MessageConst.PROPERTY_DELAY_TIME_LEVEL.equals(name)) {
                rmqMsg.setDelayTimeLevel(Integer.parseInt(value));
            }
            else if (MessageConst.PROPERTY_WAIT_STORE_MSG_OK.equals(name)) {
                rmqMsg.setWaitStoreMsgOK(Boolean.parseBoolean(value));
            }
            else if (MessageConst.PROPERTY_BUYER_ID.equals(name)) {
                rmqMsg.setBuyerId(value);
            }
            else {
                rmqMsg.putUserProperty(name, value);
            }
        }

        return rmqMsg;
    }

    private static Properties getAllProperties(RocketMQMessage rmqJmsMsg,
        String topic, String tag) throws JMSException {
        Properties userProperties = new Properties();

        //Jms userProperties to ONS properties
        Map<String, Object> userProps = rmqJmsMsg.getProperties();
        Iterator<Map.Entry<String, Object>> userPropsIter = userProps.entrySet().iterator();
        while (userPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = userPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }
        //Jms systemProperties to ONS properties
        Map<String, Object> sysProps = rmqJmsMsg.getHeaders();
        Iterator<Map.Entry<String, Object>> sysPropsIter = sysProps.entrySet().iterator();
        while (sysPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = sysPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }

        //Jms message Model
        if (rmqJmsMsg instanceof RocketMQBytesMessage) {
            userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_BYTES);
        }
        else if (rmqJmsMsg instanceof RocketMQObjectMessage) {
            userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_OBJ);
        }
        else if (rmqJmsMsg instanceof RocketMQTextMessage) {
            userProperties.setProperty(JMS_MSGMODEL, MSGMODEL_TEXT);
        }

        //message topic and tag
        userProperties.setProperty(MSG_TOPIC, topic);
        userProperties.setProperty(MSG_TYPE, tag);

        return userProperties;
    }
}