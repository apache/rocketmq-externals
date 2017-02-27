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

package org.apache.rocketmq.jms.hook;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import org.apache.rocketmq.jms.RocketMQProducer;
import org.apache.rocketmq.jms.RocketMQTopic;
import org.apache.rocketmq.jms.exception.UnsupportDeliveryModelException;
import org.apache.rocketmq.jms.msg.JMSTextMessage;
import org.apache.rocketmq.jms.msg.enums.JMSHeaderEnum;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class SendMessageHookTest {

    @Test(expected = UnsupportDeliveryModelException.class)
    public void testValidate() throws Exception {
        final JMSTextMessage message = new JMSTextMessage("text");
        final RocketMQTopic destination = new RocketMQTopic("destination");
        final int deliveryMode = DeliveryMode.NON_PERSISTENT;
        final int priority = 4;
        final long timeToLive = 1000 * 100L;

        SendMessageHook hook = new SendMessageHook();
        hook.before(message, destination, deliveryMode, priority, timeToLive);
    }

    @Test
    public void testSetHeader() throws Exception {
        RocketMQProducer producer = new RocketMQProducer();
        producer.setDeliveryDelay(0L);

        final JMSTextMessage message = new JMSTextMessage("text");
        final Destination destination = new RocketMQTopic("destination");
        final int deliveryMode = DeliveryMode.PERSISTENT;
        final int priority = 5;
        long timeToLive = JMSHeaderEnum.JMS_TIME_TO_LIVE_DEFAULT_VALUE;
        SendMessageHook hook = new SendMessageHook(producer);
        hook.before(message, destination, deliveryMode, priority, timeToLive);

        assertThat(message.getJMSDestination(), is(destination));
        assertThat(message.getJMSDeliveryMode(), is(JMSHeaderEnum.JMS_DELIVERY_MODE_DEFAULT_VALUE));
        assertThat(message.getJMSExpiration(), is(0L));
        assertThat(message.getJMSDeliveryTime(), notNullValue());
        assertThat(message.getJMSPriority(), is(5));
        assertThat(message.getJMSMessageID(), notNullValue());
        assertThat(message.getJMSTimestamp(), notNullValue());
    }

    /**
     * Disable ID,timestamp, and set expired time
     *
     * @throws Exception
     */
    @Test
    public void testSetHeader2() throws Exception {
        RocketMQProducer producer = new RocketMQProducer();
        producer.setDisableMessageID(true);
        producer.setDisableMessageTimestamp(true);

        final JMSTextMessage message = new JMSTextMessage("text");
        final Destination destination = new RocketMQTopic("destination");
        final int deliveryMode = DeliveryMode.PERSISTENT;
        final int priority = 5;
        final long timeToLive = 1000 * 100L;
        SendMessageHook hook = new SendMessageHook(producer);
        hook.before(message, destination, deliveryMode, priority, timeToLive);

        assertThat(message.getJMSMessageID(), nullValue());
        assertThat(message.getJMSTimestamp(), is(0L));
        assertThat(message.getJMSExpiration(), not(is(0L)));
    }
}