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
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import org.apache.rocketmq.jms.domain.JmsBaseConnectionFactory;
import org.apache.rocketmq.jms.integration.IntegrationTestBase;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsConsumerTest extends IntegrationTestBase {
    private static String topic1 = "jixiang-test1";
    private static int consumeThreadNums = 16;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates multiple consumers in one session
     *
     * @throws Exception
     */
    @Test
    public void multiConsumersTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("rocketmq://xxx?consumerId=" + consumerId + "&nameServer=" + nameServer));
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        try {
                            Assert.assertNotNull(message);
                            Assert.assertNotNull(message.getJMSMessageID());
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                Destination destination1 = session.createTopic(topic1 + ":" + messageType);
                //test-target
                MessageConsumer consumer1 = session.createConsumer(destination1);
                consumer1.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        try {
                            Assert.assertNotNull(message);
                            Assert.assertNotNull(message.getJMSMessageID());
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                connection.start();

                Thread.sleep(5000);
            }
            finally {
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("Consume message error", e);
        }
    }

    /**
     * Normal consumer
     *
     * @throws Exception
     */
    @Test
    public void normalConsumeTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("rocketmq://xxx?consumerId=" + consumerId + "&nameServer=" + nameServer));
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        try {
                            Assert.assertNotNull(message.getJMSMessageID());
                            System.out.println(message.getJMSMessageID());
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                connection.start();

                Thread.sleep(5000);
            }
            finally {
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("Consume Message error", e);
        }

    }

    /**
     * Set consumer thread number Use consumeThreadNums in URI
     *
     * @throws Exception
     */
    @Test
    public void setConsumeThreadNumsTest() throws Exception {
        try {
            //test-target
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("rocketmq://xxx?consumerId=" + consumerId + "&consumeThreadNums=" + consumeThreadNums
                + "&nameServer=" + nameServer));
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        try {
                            System.out.println("Receive message successfully. MsgId: "
                                + message.getJMSMessageID());
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                connection.start();

                Thread.sleep(5000);
            }
            finally {
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("Consume Message error", e);
        }

    }

    /**
     * Start a connection before creating a consumer.
     *
     * @throws Exception
     */
    @Test
    public void connectionStartTimeTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("rocketmq://xxx?consumerId=" + consumerId + "&nameServer=" + nameServer));
            Connection connection = connectionFactory.createConnection();
            //test-target
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            try {
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        try {
                            System.out.println("hello message");
                            Assert.assertNotNull(message);
                            Assert.assertNotNull(message.getJMSMessageID());
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                Thread.sleep(5000);
            }
            finally {
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("Consume message error", e);
        }
    }
}
