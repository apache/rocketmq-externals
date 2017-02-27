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
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;

import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSExpiration;
import static org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum.JMSMessageID;
import static org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum.MSG_MODEL_NAME;
import static org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum.toMsgModelEnum;

public class JMS2RMQMessageConvert {

    public static MessageExt convert(AbstractJMSMessage jmsMsg) throws Exception {
        MessageExt rmqMsg = new MessageExt();

        handleHeader(jmsMsg, rmqMsg);

        handleBody(jmsMsg, rmqMsg);

        handleProperties(jmsMsg, rmqMsg);

        return rmqMsg;
    }

    private static void handleHeader(AbstractJMSMessage jmsMsg, MessageExt rmqMsg) {
        rmqMsg.setTopic(jmsMsg.getJMSDestination().toString());
        rmqMsg.putUserProperty(JMSMessageID.name(), jmsMsg.getJMSMessageID());
        rmqMsg.setBornTimestamp(jmsMsg.getJMSTimestamp());
        rmqMsg.putUserProperty(JMSExpiration.name(), String.valueOf(jmsMsg.getJMSExpiration()));
        rmqMsg.setKeys(jmsMsg.getJMSMessageID());
    }

    private static void handleProperties(AbstractJMSMessage jmsMsg, MessageExt rmqMsg) {
        Map<String, Object> userProps = jmsMsg.getProperties();
        for (Map.Entry<String, Object> entry : userProps.entrySet()) {
            rmqMsg.putUserProperty(entry.getKey(), entry.getValue().toString());
        }
    }

    private static void handleBody(AbstractJMSMessage jmsMsg, MessageExt rmqMsg) throws JMSException {
        rmqMsg.putUserProperty(MSG_MODEL_NAME, toMsgModelEnum(jmsMsg).name());
        rmqMsg.setBody(jmsMsg.getBody());
    }
}
