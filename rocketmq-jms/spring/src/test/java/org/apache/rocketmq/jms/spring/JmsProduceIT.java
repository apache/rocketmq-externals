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

package org.apache.rocketmq.jms.spring;

import com.google.common.collect.Lists;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.rocketmq.jms.domain.message.JmsBytesMessage;
import org.apache.rocketmq.jms.domain.message.JmsObjectMessage;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.ProducerCallback;
import org.testng.Assert;
import org.testng.annotations.Test;


public class JmsProduceIT extends SpringTestBase {

    protected JmsTemplate jmsTemplate = (JmsTemplate) produceContext.getBean("jmsTemplate");

    private Topic destination = (Topic) produceContext.getBean("destination");

    @Test
    public void simpleSendTest() throws Exception {
        //Send text message
        jmsTemplate.execute(destination, new ProducerCallback() {
            @Override
            public Object doInJms(Session session, MessageProducer producer) throws JMSException {
                JmsTextMessage message = (JmsTextMessage) session.createTextMessage("hello world,kafka");
                producer.send(destination, message);
                Assert.assertNotNull(message.getJMSMessageID());
                return message;
            }
        });

        //Send object message
        jmsTemplate.execute(destination, new ProducerCallback() {
            @Override
            public Object doInJms(Session session, MessageProducer producer) throws JMSException {
                JmsObjectMessage message = (JmsObjectMessage) session.createObjectMessage(Lists.newArrayList(1, 2, 3));
                producer.send(destination, message);
                Assert.assertNotNull(message.getJMSMessageID());
                return message;
            }
        });

        //Send byte message
        jmsTemplate.execute(destination, new ProducerCallback() {
            @Override
            public Object doInJms(Session session, MessageProducer producer) throws JMSException {
                byte[] ts = "Von,Test".getBytes();
                JmsBytesMessage message = (JmsBytesMessage) session.createBytesMessage();
                message.writeBytes(ts);
                producer.send(destination, message);
                Assert.assertNotNull(message.getJMSMessageID());
                return message;
            }
        });
    }


    @Test(threadPoolSize = 2, invocationCount = 20)
    public void multiSenderTest() throws Exception {
        jmsTemplate.execute(destination, new ProducerCallback() {
            @Override
            public Object doInJms(Session session, MessageProducer producer) throws JMSException {
                byte[] ts = "Von,Multi thread sender test".getBytes();
                JmsBytesMessage message = (JmsBytesMessage) session.createBytesMessage();
                message.writeBytes(ts);
                producer.send(destination, message);
                Assert.assertNotNull(message.getJMSMessageID());
                return message;
            }
        });
    }
}
