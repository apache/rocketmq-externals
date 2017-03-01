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

import javax.jms.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.jms.msg.JMSBytesMessage;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;
import org.apache.rocketmq.jms.msg.enums.JMSMessageModelEnum;
import org.apache.rocketmq.jms.msg.enums.JMSPropertiesEnum;
import org.apache.rocketmq.jms.support.JMSUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RMQ2JMSMessageConvertTest {

    @Test
    public void testConvert() throws Exception {
        MessageExt rmqMessage = new MessageExt();

        // given
        rmqMessage.setBody("body".getBytes());
        rmqMessage.putUserProperty(JMSMessageModelEnum.MSG_MODEL_NAME, JMSMessageModelEnum.BYTE.name());
        rmqMessage.putUserProperty(JMSHeaderEnum.JMSMessageID.name(), "ID:YYY");
        rmqMessage.setBornTimestamp(1488273585542L);
        rmqMessage.putUserProperty(JMSHeaderEnum.JMSExpiration.name(), "0");
        rmqMessage.setReconsumeTimes(2);
        rmqMessage.setTopic("topic");

        rmqMessage.putUserProperty(JMSPropertiesEnum.JMSXDeliveryCount.name(), "2");
        rmqMessage.putUserProperty(JMS2RMQMessageConvert.USER_PROPERTY_PREFIX + "MyProperty", "MyValue");

        // when
        Message jmsMessage = RMQ2JMSMessageConvert.convert(rmqMessage);

        // then
        assertThat(JMSBytesMessage.class.isInstance(jmsMessage), is(true));
        assertThat(jmsMessage.getJMSMessageID(), is("ID:YYY"));
        assertThat(jmsMessage.getJMSTimestamp(), is(1488273585542L));
        assertThat(jmsMessage.getJMSExpiration(), is(0L));
        assertThat(jmsMessage.getJMSRedelivered(), is(true));
        assertThat(JMSUtils.getDestinationName(jmsMessage.getJMSDestination()), is("topic"));

        assertThat(jmsMessage.getStringProperty("MyProperty"), is("MyValue"));
        assertThat(jmsMessage.getIntProperty(JMSPropertiesEnum.JMSXDeliveryCount.name()), is(3));
    }

}