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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import org.apache.rocketmq.jms.JmsTestListener;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;
import org.springframework.jms.core.ProducerCallback;
import org.testng.Assert;
import org.testng.annotations.Test;

public class JmsConsumeIT extends JmsProduceIT {


    @Test
    public void testConsume() throws Exception {
        final Topic topic = (Topic) consumeContext.getBean("baseTopic");
        JmsTestListener messageListener = (JmsTestListener) consumeContext.getBean("messageListener");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        messageListener.setLatch(countDownLatch);
        messageListener.setExpectd(30);
        consumeContext.start();

        for (int i = 0; i < 30; i++) {
            jmsTemplate.execute(topic, new ProducerCallback() {
                @Override
                public Object doInJms(Session session, MessageProducer producer) throws JMSException {
                    JmsTextMessage message = (JmsTextMessage) session.createTextMessage("hello world,kafka, haha");
                    producer.send(topic, message);
                    Assert.assertNotNull(message.getJMSMessageID());
                    return message;
                }
            });
        }
        if (countDownLatch.await(30, TimeUnit.SECONDS)) {
            Thread.sleep(2000);
        }
        Assert.assertEquals(30, messageListener.getConsumedNum());
        consumeContext.close();
    }
}
