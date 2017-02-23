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

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.StreamMessage;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.JMSHeaderEnum;
import org.apache.rocketmq.jms.JMSMessageModelEnum;
import org.apache.rocketmq.jms.RocketMQTopic;
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;
import org.apache.rocketmq.jms.msg.JMSBytesMessage;
import org.apache.rocketmq.jms.msg.JMSMapMessage;
import org.apache.rocketmq.jms.msg.JMSObjectMessage;
import org.apache.rocketmq.jms.msg.JMSTextMessage;

import static java.lang.String.format;
import static org.apache.rocketmq.jms.JMSMessageModelEnum.MSG_MODEL_NAME;

public class MessageConverter {
    public static final String EMPTY_STRING = "";

    public static Object getBodyFromJMSMessage(javax.jms.Message jmsMessage) throws JMSException {
        if (jmsMessage == null) {
            return null;
        }

        if (StreamMessage.class.isInstance(jmsMessage)) {
            throw new UnsupportedOperationException(StreamMessage.class.getSimpleName() + " is not supported");
        }
        return jmsMessage.getBody(Object.class);
    }

    public static Message convert2JMSMessage(MessageExt msg) throws Exception {
        if (msg == null) {
            return null;
        }

        AbstractJMSMessage message;
        final String msgModel = msg.getUserProperty(MSG_MODEL_NAME);
        switch (JMSMessageModelEnum.valueOf(msgModel)) {
            case BYTE:
                message = new JMSBytesMessage(msg.getBody());
                break;
            case MAP:
                message = new JMSMapMessage(JSON.parseObject(new String(msg.getBody()), HashMap.class));
                break;
            case OBJECT:
                message = new JMSObjectMessage(objectDeserialize(msg.getBody()));
                break;
            case STRING:
                message = new JMSTextMessage(bytes2String(msg.getBody(), Charsets.UTF_8));
                break;
            default:
                throw new JMSException(format("The type[%s] is not supported", msgModel));
        }

        //-------------------------set headers-------------------------
        message.setJMSMessageID(msg.getUserProperty(JMSHeaderEnum.JMSMessageID.name()));
        message.setJMSTimestamp(msg.getBornTimestamp());
        message.setJMSExpiration(Long.valueOf(msg.getUserProperty(JMSHeaderEnum.JMSExpiration.name())));
        message.setJMSRedelivered(msg.getReconsumeTimes() > 0 ? true : false);
        //todo: what about Queue?
        message.setJMSDestination(new RocketMQTopic(msg.getTopic()));

        Map<String, String> propertiesMap = msg.getProperties();
        if (propertiesMap != null) {
            for (String properName : propertiesMap.keySet()) {
                String properValue = propertiesMap.get(properName);
                message.setStringProperty(properName, properValue);
            }
        }

        return message;
    }

    public static final String bytes2String(byte[] bs, Charset charset) {
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

    public static MessageExt convert2RMQMessage(AbstractJMSMessage jmsMsg) throws Exception {
        MessageExt rmqMsg = new MessageExt();

        rmqMsg.putUserProperty(JMSHeaderEnum.JMSMessageID.name(), jmsMsg.getJMSMessageID());
        rmqMsg.setBornTimestamp(jmsMsg.getJMSTimestamp());
        rmqMsg.putUserProperty(JMSHeaderEnum.JMSExpiration.name(), String.valueOf(jmsMsg.getJMSExpiration()));
        rmqMsg.setKeys(jmsMsg.getJMSMessageID());

        // 1. Transform message body
        rmqMsg.setBody(MessageConverter.getBodyFromJMSMessage(jmsMsg));

        // 2. Transform message properties
        Properties properties = getAllProperties(jmsMsg);
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            rmqMsg.putUserProperty(name, value);
        }

        return rmqMsg;
    }

    private static Properties getAllProperties(AbstractJMSMessage jmsMsg) throws JMSException {
        Properties userProperties = new Properties();

        Map<String, Object> userProps = jmsMsg.getProperties();
        Iterator<Map.Entry<String, Object>> userPropsIter = userProps.entrySet().iterator();
        while (userPropsIter.hasNext()) {
            Map.Entry<String, Object> entry = userPropsIter.next();
            userProperties.setProperty(entry.getKey(), entry.getValue().toString());
        }

        //Jms message Model
        userProperties.setProperty(MSG_MODEL_NAME, JMSMessageModelEnum.toMsgModelEnum(jmsMsg.getClass()).name())

        return userProperties;
    }
}