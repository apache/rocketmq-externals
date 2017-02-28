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

package org.apache.rocketmq.jms.msg.convert;

import java.util.Map;
import javax.jms.JMSException;
import javax.jms.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.destination.RocketMQTopic;
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;
import org.apache.rocketmq.jms.msg.JMSBytesMessage;
import org.apache.rocketmq.jms.msg.JMSMapMessage;
import org.apache.rocketmq.jms.msg.JMSObjectMessage;
import org.apache.rocketmq.jms.msg.JMSTextMessage;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;
import org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum;
import org.apache.rocketmq.jms.msg.enums.JMSPropertiesEnum;
import org.apache.rocketmq.jms.msg.serialize.MapSerialize;
import org.apache.rocketmq.jms.msg.serialize.ObjectSerialize;
import org.apache.rocketmq.jms.msg.serialize.StringSerialize;

import static java.lang.String.format;
import static org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum.MSG_MODEL_NAME;

public class RMQ2JMSMessageConvert {

    public static Message convert(MessageExt rmqMsg) throws JMSException {
        if (rmqMsg == null) {
            throw new IllegalArgumentException("RocketMQ message could not be null");
        }
        if (rmqMsg.getBody() == null) {
            throw new IllegalArgumentException("RocketMQ message body could not be null");
        }

        AbstractJMSMessage jmsMsg = newAbstractJMSMessage(rmqMsg.getUserProperty(MSG_MODEL_NAME), rmqMsg.getBody());

        setHeader(rmqMsg, jmsMsg);

        setProperties(rmqMsg, jmsMsg);

        return jmsMsg;
    }

    private static AbstractJMSMessage newAbstractJMSMessage(String msgModel, byte[] body) throws JMSException {
        AbstractJMSMessage message;
        switch (JMSMessageModelEnum.valueOf(msgModel)) {
            case BYTE:
                return new JMSBytesMessage(body);
            case MAP:
                message = new JMSMapMessage(MapSerialize.instance().deserialize(body));
                break;
            case OBJECT:
                message = new JMSObjectMessage(ObjectSerialize.instance().deserialize(body));
                break;
            case STRING:
                message = new JMSTextMessage(StringSerialize.instance().deserialize(body));
                break;
            default:
                throw new JMSException(format("The type[%s] is not supported", msgModel));
        }

        return message;
    }

    private static void setHeader(MessageExt rmqMsg, AbstractJMSMessage jmsMsg) {
        jmsMsg.setJMSMessageID(rmqMsg.getUserProperty(JMSHeaderEnum.JMSMessageID.name()));
        jmsMsg.setJMSTimestamp(rmqMsg.getBornTimestamp());
        jmsMsg.setJMSExpiration(Long.valueOf(rmqMsg.getUserProperty(JMSHeaderEnum.JMSExpiration.name())));
        jmsMsg.setJMSRedelivered(rmqMsg.getReconsumeTimes() > 0 ? true : false);
        //todo: what about Queue?
        jmsMsg.setJMSDestination(new RocketMQTopic(rmqMsg.getTopic()));
    }

    private static void setProperties(MessageExt rmqMsg, AbstractJMSMessage jmsMsg) {
        jmsMsg.setIntProperty(JMSPropertiesEnum.JMSXDeliveryCount.name(), rmqMsg.getReconsumeTimes() + 1);

        Map<String, String> propertiesMap = rmqMsg.getProperties();
        if (propertiesMap != null) {
            for (String properName : propertiesMap.keySet()) {
                String properValue = propertiesMap.get(properName);
                jmsMsg.setStringProperty(properName, properValue);
            }
        }
    }
}
