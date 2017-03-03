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

package org.apache.rocketmq.jms.util;

import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.jms.BytesMessage;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.jms.domain.JmsBaseConstant;
import org.apache.rocketmq.jms.domain.JmsBaseTopic;
import org.apache.rocketmq.jms.domain.message.JmsBaseMessage;
import org.apache.rocketmq.jms.domain.message.JmsBytesMessage;
import org.apache.rocketmq.jms.domain.message.JmsObjectMessage;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;

import static org.apache.rocketmq.jms.domain.JmsBaseMessageProducer.initRocketMQHeaders;

public class MessageConverter {
    public static byte[] getContentFromJms(javax.jms.Message jmsMessage) throws Exception {
        byte[] content;
        if (jmsMessage instanceof TextMessage) {
            if (StringUtils.isEmpty(((TextMessage) jmsMessage).getText())) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            content = MsgConvertUtil.string2Bytes(((TextMessage) jmsMessage).getText(),
                Charsets.UTF_8.toString());
        }
        else if (jmsMessage instanceof ObjectMessage) {
            if (((ObjectMessage) jmsMessage).getObject() == null) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            content = MsgConvertUtil.objectSerialize(((ObjectMessage) jmsMessage).getObject());
        }
        else if (jmsMessage instanceof BytesMessage) {
            JmsBytesMessage bytesMessage = (JmsBytesMessage) jmsMessage;
            if (bytesMessage.getBodyLength() == 0) {
                throw new IllegalArgumentException("Message body length is zero");
            }
            content = bytesMessage.getData();
        }
        else {
            throw new IllegalArgumentException("Unknown message type " + jmsMessage.getJMSType());
        }

        return content;
    }

    public static JmsBaseMessage convert2JMSMessage(MessageExt msg) throws Exception {
        JmsBaseMessage message;
        if (MsgConvertUtil.MSGMODEL_BYTES.equals(
            msg.getUserProperty(MsgConvertUtil.JMS_MSGMODEL))) {
            message = new JmsBytesMessage(msg.getBody());
        }
        else if (MsgConvertUtil.MSGMODEL_OBJ.equals(
            msg.getUserProperty(MsgConvertUtil.JMS_MSGMODEL))) {
            message = new JmsObjectMessage(MsgConvertUtil.objectDeserialize(msg.getBody()));
        }
        else if (MsgConvertUtil.MSGMODEL_TEXT.equals(
            msg.getUserProperty(MsgConvertUtil.JMS_MSGMODEL))) {
            message = new JmsTextMessage(MsgConvertUtil.bytes2String(msg.getBody(),
                Charsets.UTF_8.toString()));
        }
        else {
            // rocketmq producer sends bytesMessage without setting JMS_MSGMODEL.
            message = new JmsBytesMessage(msg.getBody());
        }

        //-------------------------set headers-------------------------
        Map<String, Object> properties = new HashMap<String, Object>();

        message.setHeader(JmsBaseConstant.JMS_MESSAGE_ID, "ID:" + msg.getMsgId());

        if (msg.getReconsumeTimes() > 0) {
            message.setHeader(JmsBaseConstant.JMS_REDELIVERED, Boolean.TRUE);
        }
        else {
            message.setHeader(JmsBaseConstant.JMS_REDELIVERED, Boolean.FALSE);
        }

        Map<String, String> propertiesMap = msg.getProperties();
        if (propertiesMap != null) {
            for (String properName : propertiesMap.keySet()) {
                String properValue = propertiesMap.get(properName);
                if (JmsBaseConstant.JMS_DESTINATION.equals(properName)) {
                    String destinationStr = properValue;
                    if (null != destinationStr) {
                        List<String> msgTuple = Arrays.asList(destinationStr.split(":"));
                        message.setHeader(JmsBaseConstant.JMS_DESTINATION,
                            new JmsBaseTopic(msgTuple.get(0), msgTuple.get(1)));
                    }
                }
                else if (JmsBaseConstant.JMS_DELIVERY_MODE.equals(properName) ||
                    JmsBaseConstant.JMS_PRIORITY.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (JmsBaseConstant.JMS_TIMESTAMP.equals(properName) ||
                    JmsBaseConstant.JMS_EXPIRATION.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (JmsBaseConstant.JMS_CORRELATION_ID.equals(properName) ||
                    JmsBaseConstant.JMS_TYPE.equals(properName)) {
                    message.setHeader(properName, properValue);
                }
                else if (JmsBaseConstant.JMS_MESSAGE_ID.equals(properName) ||
                    JmsBaseConstant.JMS_REDELIVERED.equals(properName)) {
                    //JMS_MESSAGE_ID should set by msg.getMsgID()
                    continue;
                }
                else {
                    properties.put(properName, properValue);
                }
            }
        }

        //Handle System properties, put into header.
        //add what?
        message.setProperties(properties);

        return message;
    }

    public static Message convert2RMQMessage(JmsBaseMessage jmsMsg) throws Exception {
        Message rocketmqMsg = new MessageExt();
        // 1. Transform message body
        rocketmqMsg.setBody(MessageConverter.getContentFromJms(jmsMsg));

        // 2. Transform topic and messageType
        JmsBaseTopic destination = (JmsBaseTopic) jmsMsg.getHeaders().get(JmsBaseConstant.JMS_DESTINATION);
        String topic = destination.getMessageTopic();
        rocketmqMsg.setTopic(topic);
        String messageType = destination.getMessageType();
        Preconditions.checkState(!messageType.contains("||"),
            "'||' can not be in the destination when sending a message");
        rocketmqMsg.setTags(messageType);

        // 3. Transform message properties
        Properties properties = initRocketMQHeaders(jmsMsg, topic, messageType);
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (MessageConst.PROPERTY_KEYS.equals(name)) {
                rocketmqMsg.setKeys(value);
            } else if (MessageConst.PROPERTY_TAGS.equals(name)) {
                rocketmqMsg.setTags(value);
            } else if (MessageConst.PROPERTY_DELAY_TIME_LEVEL.equals(name)) {
                rocketmqMsg.setDelayTimeLevel(Integer.parseInt(value));
            } else if (MessageConst.PROPERTY_WAIT_STORE_MSG_OK.equals(name)) {
                rocketmqMsg.setWaitStoreMsgOK(Boolean.parseBoolean(value));
            } else if (MessageConst.PROPERTY_BUYER_ID.equals(name)) {
                rocketmqMsg.setBuyerId(value);
            } else {
                rocketmqMsg.putUserProperty(name, value);
            }
        }

        return rocketmqMsg;
    }
}