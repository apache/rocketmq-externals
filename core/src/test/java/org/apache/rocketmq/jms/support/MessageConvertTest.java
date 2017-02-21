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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.jms.support;

import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.RocketMQTopic;
import org.apache.rocketmq.jms.msg.RocketMQMessage;
import org.apache.rocketmq.jms.msg.RocketMQTextMessage;
import org.apache.rocketmq.jms.support.MessageConverter;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.rocketmq.jms.Constant.JMS_DESTINATION;
import static org.apache.rocketmq.jms.Constant.JMS_MESSAGE_ID;
import static org.apache.rocketmq.jms.Constant.JMS_REDELIVERED;
import static org.apache.rocketmq.jms.support.MessageConverter.JMS_MSGMODEL;
import static org.apache.rocketmq.jms.support.MessageConverter.MSGMODEL_TEXT;
import static org.apache.rocketmq.jms.support.MessageConverter.MSG_TOPIC;
import static org.apache.rocketmq.jms.support.MessageConverter.MSG_TYPE;

public class MessageConvertTest {
    @Test
    public void testCovert2RMQ() throws Exception {
        //build RmqJmsMessage
        String topic = "TestTopic";
        String messageType = "TagA";

        RocketMQMessage rmqJmsMessage = new RocketMQTextMessage("testText");
        rmqJmsMessage.setHeader(JMS_DESTINATION, new RocketMQTopic(topic, messageType));
        rmqJmsMessage.setHeader(JMS_MESSAGE_ID, "ID:null");
        rmqJmsMessage.setHeader(JMS_REDELIVERED, Boolean.FALSE);

        rmqJmsMessage.setObjectProperty(JMS_MSGMODEL, MSGMODEL_TEXT);
        rmqJmsMessage.setObjectProperty(MSG_TOPIC, topic);
        rmqJmsMessage.setObjectProperty(MSG_TYPE, messageType);
        rmqJmsMessage.setObjectProperty(MessageConst.PROPERTY_TAGS, messageType);
        rmqJmsMessage.setObjectProperty(MessageConst.PROPERTY_KEYS, messageType);

        //convert to RMQMessagemiz
        MessageExt message = (MessageExt)MessageConverter.convert2RMQMessage(rmqJmsMessage);

        //then convert back to RmqJmsMessage
        RocketMQMessage RmqJmsMessageBack = MessageConverter.convert2JMSMessage(message);

        RocketMQTextMessage jmsTextMessage = (RocketMQTextMessage) rmqJmsMessage;
        RocketMQTextMessage jmsTextMessageBack = (RocketMQTextMessage) RmqJmsMessageBack;

        Assert.assertEquals(jmsTextMessage.getText(), jmsTextMessageBack.getText());
        Assert.assertEquals(jmsTextMessage.getJMSDestination().toString(), jmsTextMessageBack.getJMSDestination().toString());
        Assert.assertEquals(jmsTextMessage.getJMSMessageID(), jmsTextMessageBack.getJMSMessageID());
        Assert.assertEquals(jmsTextMessage.getJMSRedelivered(), jmsTextMessageBack.getJMSRedelivered());
        Assert.assertEquals(jmsTextMessage.getHeaders().get(JMS_MSGMODEL), jmsTextMessageBack.getHeaders().get(JMS_MSGMODEL));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MSG_TOPIC), jmsTextMessageBack.getHeaders().get(MSG_TOPIC));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MSG_TYPE), jmsTextMessageBack.getHeaders().get(MSG_TYPE));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MessageConst.PROPERTY_TAGS), jmsTextMessageBack.getHeaders().get(MessageConst.PROPERTY_TAGS));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MessageConst.PROPERTY_KEYS), jmsTextMessageBack.getHeaders().get(MessageConst.PROPERTY_KEYS));

    }
}
