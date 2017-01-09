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

package org.apache.rocketmq.jms.client;

import java.net.URI;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.rocketmq.jms.domain.JmsBaseConnectionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsProducerTest {
    private static String topic = "jixiang-test-2";
    private static String topic1 = "jixiang-test1";
    private static String messageType = "TagA";
    private static String appId = "ons-test";
    private static String producerId = "PID-jixiang-test-1";
    private static String consumerId = "CID-jixiang-test-1";
    private static String accessKey = "BfqbMqEc4gYksKue";
    private static String secretKey = "zBQILPqFG8q08vbdeXtHks4H5D0cWW";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Normal sender
     *
     * @throws Exception
     */
    @Test
    public void sendMessageTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("ons://xxx?producerId=" + producerId + "&accessKey=" + accessKey + "&secretKey=" + secretKey));
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageProducer messageProducer = session.createProducer(destination);
                String text = "中文，Hello world!";

                connection.start();
                TextMessage message = session.createTextMessage(text);

                messageProducer.send(message);
                System.out.println("send message success! msgId:" + message.getJMSMessageID());
                Assert.assertNotNull(message.getJMSMessageID());
            }
            finally {
                //do the close work
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("send message error", e);
        }
    }

    /**
     * Normal sender
     *
     * @throws Exception
     */
    @Test
    public void sendMultiMessageTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("ons://xxx?producerId=" + producerId + "&accessKey=" + accessKey + "&secretKey=" + secretKey));
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(topic + ":" + messageType);
                Destination destination1 = session.createTopic(topic1 + ":" + messageType);
                MessageProducer messageProducer = session.createProducer(destination);
                String text = "中文，Hello world!";

                connection.start();
                TextMessage message = session.createTextMessage(text);

                //Send message to destination
                messageProducer.send(message);

                //Send Message to destination1
                TextMessage message1 = session.createTextMessage(text);
                messageProducer.send(destination1, message1);

                System.out.println("send message success! msgId:" + message.getJMSMessageID());
                Assert.assertNotNull(message.getJMSMessageID());
            }
            finally {
                //do the close work
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("send message error", e);
        }
    }

    /**
     * @throws Exception
     */
    @Test
    public void loopSendMessageTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("ons://xxx?producerId=" + producerId + "&accessKey=" + accessKey + "&secretKey=" + secretKey));
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageProducer messageProducer = session.createProducer(destination);
                String text = "中文，Hello world!";

                connection.start();
                TextMessage message = session.createTextMessage(text);

                int i = 0;
                while (i++ < 1000) {
                    messageProducer.send(message);
                }
                Assert.assertEquals(i, 1001);
            }
            finally {
                //do the close work
                connection.close();
            }

        }
        catch (Exception e) {
            logger.error("send message error", e);
        }
    }

    /**
     * Use set method to init connectionFactory
     *
     * @throws Exception
     */
    @Test
    public void newConnectionFactoryTest() throws Exception {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory();
            //test-target
            connectionFactory.setConnectionUri(new URI("ons://xxx?producerId=" + producerId));
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(topic + ":" + messageType);
                MessageProducer messageProducer = session.createProducer(destination);
                String text = "中文，Hello world!";

                connection.start();
                TextMessage message = session.createTextMessage(text);

                messageProducer.send(message);
                System.out.println("send message success! msgId:" + message.getJMSMessageID());
                Assert.assertNotNull(message.getJMSMessageID());
            }
            finally {
                //do the close work
                connection.close();
            }
        }
        catch (Exception e) {
            logger.error("send message error", e);
        }
    }

}
