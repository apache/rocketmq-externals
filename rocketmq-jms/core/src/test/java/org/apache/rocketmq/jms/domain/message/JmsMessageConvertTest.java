package org.apache.rocketmq.jms.domain.message;

import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.domain.JmsBaseConstant;
import org.apache.rocketmq.jms.domain.JmsBaseTopic;
import org.apache.rocketmq.jms.util.MessageConverter;
import org.apache.rocketmq.jms.util.MsgConvertUtil;
import org.junit.Assert;
import org.junit.Test;

public class JmsMessageConvertTest {
    @Test
    public void testCovert2RMQ() throws Exception {
        //init jmsBaseMessage
        String topic = "TestTopic";
        String messageType = "TagA";

        JmsBaseMessage jmsBaseMessage = new JmsTextMessage("testText");
        jmsBaseMessage.setHeader(JmsBaseConstant.JMS_DESTINATION, new JmsBaseTopic(topic, messageType));
        jmsBaseMessage.setHeader(JmsBaseConstant.JMS_MESSAGE_ID, "ID:null");
        jmsBaseMessage.setHeader(JmsBaseConstant.JMS_REDELIVERED, Boolean.FALSE);

        jmsBaseMessage.setObjectProperty(MsgConvertUtil.JMS_MSGMODEL, MsgConvertUtil.MSGMODEL_TEXT);
        jmsBaseMessage.setObjectProperty(MsgConvertUtil.MSG_TOPIC, topic);
        jmsBaseMessage.setObjectProperty(MsgConvertUtil.MSG_TYPE, messageType);
        jmsBaseMessage.setObjectProperty(MessageConst.PROPERTY_TAGS, messageType);
        jmsBaseMessage.setObjectProperty(MessageConst.PROPERTY_KEYS, messageType);

        //convert to RMQMessage
        MessageExt message = (MessageExt)MessageConverter.convert2RMQMessage(jmsBaseMessage);

        System.out.println(message);

        //then convert back to jmsBaseMessage
        JmsBaseMessage jmsBaseMessageBack = MessageConverter.convert2JMSMessage(message);

        JmsTextMessage jmsTextMessage = (JmsTextMessage) jmsBaseMessage;
        JmsTextMessage jmsTextMessageBack = (JmsTextMessage) jmsBaseMessageBack;

        Assert.assertEquals(jmsTextMessage.getText(), jmsTextMessageBack.getText());
        Assert.assertEquals(jmsTextMessage.getJMSDestination().toString(), jmsTextMessageBack.getJMSDestination().toString());
        Assert.assertEquals(jmsTextMessage.getJMSMessageID(), jmsTextMessageBack.getJMSMessageID());
        Assert.assertEquals(jmsTextMessage.getJMSRedelivered(), jmsTextMessageBack.getJMSRedelivered());
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MsgConvertUtil.JMS_MSGMODEL), jmsTextMessageBack.getHeaders().get(MsgConvertUtil.JMS_MSGMODEL));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MsgConvertUtil.MSG_TOPIC), jmsTextMessageBack.getHeaders().get(MsgConvertUtil.MSG_TOPIC));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MsgConvertUtil.MSG_TYPE), jmsTextMessageBack.getHeaders().get(MsgConvertUtil.MSG_TYPE));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MessageConst.PROPERTY_TAGS), jmsTextMessageBack.getHeaders().get(MessageConst.PROPERTY_TAGS));
        Assert.assertEquals(jmsTextMessage.getHeaders().get(MessageConst.PROPERTY_KEYS), jmsTextMessageBack.getHeaders().get(MessageConst.PROPERTY_KEYS));

    }
}
