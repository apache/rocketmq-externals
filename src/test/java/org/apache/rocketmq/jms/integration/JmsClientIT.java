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

package org.apache.rocketmq.jms.integration;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.jms.JmsTestListener;
import org.apache.rocketmq.jms.JmsTestUtil;
import org.apache.rocketmq.jms.domain.CommonConstant;
import org.apache.rocketmq.jms.domain.JmsBaseConnectionFactory;
import org.junit.Assert;
import org.junit.Test;

import static org.apache.rocketmq.jms.JmsTestUtil.getRMQPushConsumerExt;

public class JmsClientIT extends IntegrationTestBase {

    @Test
    public void testConfigInURI() throws Exception {
        JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
            URI(String.format("rocketmq://xxx?%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
            CommonConstant.PRODUCERID, producerId,
            CommonConstant.CONSUMERID, consumerId,
            CommonConstant.NAMESERVER, nameServer,
            CommonConstant.CONSUME_THREAD_NUMS, consumeThreadNums,
            CommonConstant.SEND_TIMEOUT_MILLIS, 10*1000,
            CommonConstant.INSTANCE_NAME, "JMS_TEST")));

        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
        try {
            Destination destination = session.createTopic(topic + ":" + messageType);
            session.createConsumer(destination);
            session.createProducer(destination);

            DefaultMQPushConsumer rmqPushConsumer = (DefaultMQPushConsumer) getRMQPushConsumerExt(consumerId).getConsumer();
            Assert.assertNotNull(rmqPushConsumer);
            Assert.assertEquals(consumerId, rmqPushConsumer.getConsumerGroup());
            Assert.assertEquals("JMS_TEST", rmqPushConsumer.getInstanceName());
            Assert.assertEquals(consumeThreadNums, rmqPushConsumer.getConsumeThreadMax());
            Assert.assertEquals(consumeThreadNums, rmqPushConsumer.getConsumeThreadMin());
            Assert.assertEquals(nameServer, rmqPushConsumer.getNamesrvAddr());

            DefaultMQProducer mqProducer = (DefaultMQProducer) JmsTestUtil.getMQProducer(producerId);
            Assert.assertNotNull(mqProducer);
            Assert.assertEquals(producerId, mqProducer.getProducerGroup());
            Assert.assertEquals("JMS_TEST", mqProducer.getInstanceName());
            Assert.assertEquals(10 * 1000, mqProducer.getSendMsgTimeout());
            Assert.assertEquals(nameServer, mqProducer.getNamesrvAddr());

            Thread.sleep(2000);
        }
        finally {
            connection.close();
        }

    }


    private Connection createConnection(String producerGroup, String consumerGroup) throws Exception {
        JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
            URI(String.format("rocketmq://xxx?%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
            CommonConstant.PRODUCERID, producerGroup,
            CommonConstant.CONSUMERID, consumerGroup,
            CommonConstant.NAMESERVER, nameServer,
            CommonConstant.CONSUME_THREAD_NUMS, consumeThreadNums,
            CommonConstant.SEND_TIMEOUT_MILLIS, 10*1000,
            CommonConstant.INSTANCE_NAME, "JMS_TEST")));
        return  connectionFactory.createConnection();
    }

    @Test
    public void testProducerAndConsume_TwoConsumer() throws Exception {

        Connection connection = createConnection(producerId, consumerId);
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destinationA = session.createTopic("TopicA");
        Destination destinationB = session.createTopic("TopicB");
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        JmsTestListener listenerA = new JmsTestListener(10,countDownLatch);
        JmsTestListener listenerB = new JmsTestListener(10, countDownLatch);

        try {
            //two consumers
            MessageConsumer messageConsumerA = session.createConsumer(destinationA);
            messageConsumerA.setMessageListener(listenerA);
            MessageConsumer messageConsumerB = session.createConsumer(destinationB);
            messageConsumerB.setMessageListener(listenerB);
            //producer
            MessageProducer messageProducer = session.createProducer(destinationA);
            connection.start();

            for (int i = 0; i < 10; i++) {
                TextMessage message = session.createTextMessage(text + i);
                Assert.assertNull(message.getJMSMessageID());
                messageProducer.send(message);
                Assert.assertNotNull(message.getJMSMessageID());
            }
            for (int i = 0; i < 10; i++) {
                TextMessage message = session.createTextMessage(text + i);
                Assert.assertNull(message.getJMSMessageID());
                messageProducer.send(destinationB, message);
                Assert.assertNotNull(message.getJMSMessageID());
            }

            if (countDownLatch.await(30, TimeUnit.SECONDS)) {
                Thread.sleep(2000);
            }
            Assert.assertEquals(10, listenerA.getConsumedNum());
            Assert.assertEquals(10, listenerB.getConsumedNum());
        }
        finally {
            //Close the connection
            connection.close();
        }

    }

    @Test
    public void testProducerAndConsume_TagFilter() throws Exception {
        Connection connection = createConnection(producerId, consumerId);
        Connection anotherConnection = createConnection(producerId, consumerId +"other");
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session anotherSession = anotherConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        Destination destinationA = session.createTopic("topic:tagA");
        Destination destinationB = session.createTopic("topic:tagB");
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        JmsTestListener listenerForTagA =  new JmsTestListener(10, countDownLatch);
        JmsTestListener listenerForAll = new JmsTestListener(40, countDownLatch);
        try {
            session.createConsumer(destinationA).setMessageListener(listenerForTagA);
            anotherSession.createConsumer(session.createTopic("topic")).setMessageListener(listenerForAll);
            //producer
            MessageProducer messageProducer = session.createProducer(destinationA);
            connection.start();
            anotherConnection.start();

            for (int i = 0; i < 20; i++) {
                TextMessage message = session.createTextMessage(text + i);
                Assert.assertNull(message.getJMSMessageID());
                messageProducer.send(message);
                Assert.assertNotNull(message.getJMSMessageID());
            }
            for (int i = 0; i < 20; i++) {
                TextMessage message = session.createTextMessage(text + i);
                Assert.assertNull(message.getJMSMessageID());
                messageProducer.send(destinationB, message);
                Assert.assertNotNull(message.getJMSMessageID());
            }

            if (countDownLatch.await(30, TimeUnit.SECONDS)) {
                Thread.sleep(2000);
            }
            Assert.assertEquals(20, listenerForTagA.getConsumedNum());
            Assert.assertEquals(40, listenerForAll.getConsumedNum());
        }
        finally {
            //Close the connection
            connection.close();
            anotherConnection.close();
        }

    }

}