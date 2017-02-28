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

import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.destination.RocketMQTopic;
import org.apache.rocketmq.jms.msg.AbstractJMSMessage;
import org.apache.rocketmq.jms.msg.JMSTextMessage;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;
import org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum;
import org.junit.Test;

import static org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum.MSG_MODEL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JMS2RMQMessageConvertTest {

    @Test
    public void testConvert() throws Exception {
        AbstractJMSMessage jmsMessage = new JMSTextMessage("text");

        // given
        jmsMessage.setJMSDestination(new RocketMQTopic("topic"));
        jmsMessage.setJMSMessageID("ID:XXX");
        jmsMessage.setJMSTimestamp(1488273583542L);
        jmsMessage.setJMSExpiration(0L);

        jmsMessage.setStringProperty("MyProperty", "MyValue");

        // when
        MessageExt rmqMessage = JMS2RMQMessageConvert.convert(jmsMessage);

        // then
        assertThat(rmqMessage.getTopic(), is("topic"));
        assertThat(rmqMessage.getUserProperty(JMSHeaderEnum.JMSMessageID.name()), is("ID:XXX"));
        assertThat(rmqMessage.getBornTimestamp(), is(1488273583542L));
        assertThat(rmqMessage.getUserProperty(JMSHeaderEnum.JMSExpiration.name()), is("0"));
        assertThat(rmqMessage.getKeys(), is("ID:XXX"));

        assertThat(rmqMessage.getUserProperty("MyProperty"), is("MyValue"));
        assertThat(rmqMessage.getUserProperty(MSG_MODEL_NAME), is(JMSMessageModelEnum.STRING.name()));
        assertThat(new String(rmqMessage.getBody()), is("text"));
    }
}