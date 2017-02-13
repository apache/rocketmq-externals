package org.apache.rocketmq.jms.msg;

import com.alibaba.rocketmq.common.message.MessageConst;
import com.alibaba.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.RocketMQTopic;
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

public class JmsMessageConvertTest {
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

        //convert to RMQMessage
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
