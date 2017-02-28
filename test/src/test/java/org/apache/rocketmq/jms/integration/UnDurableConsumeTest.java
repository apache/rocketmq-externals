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

package org.apache.rocketmq.jms.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.rocketmq.jms.RocketMQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UnDurableConsumeTest {

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    /**
     * Test messages that producer after consumer inactive will not be delivered to consumer when it start again.
     *
     * <p>Test step:
     * 1. Create a consumer and start the connection
     * 2. Create a producer and send a message(msgA) to the topic subscribed by previous consumer
     * 3. MsgA should be consumed successfully
     * 4. Close the consumer and stop the connection
     * 5. Producer sends a message(msgB) after the consumer closed
     * 6. Create another consumer which is a un-durable one, and start the connection
     * 7. Result: msgB should be consumed by the previous un-durable consumer
     *
     * @throws Exception
     */
    @Test
    public void testConsumeNotDurable() throws Exception {
        final String rmqTopicName = "coffee-syn" + UUID.randomUUID().toString();
        rocketMQAdmin.createTopic(rmqTopicName);

        ConnectionFactory factory = new RocketMQConnectionFactory(Constant.NAME_SERVER_ADDRESS, Constant.CLIENT_ID);
        Connection connection = factory.createConnection();
        Session session = connection.createSession();
        connection.start();
        Topic topic = session.createTopic(rmqTopicName);

        try {
            //consumer
            final List<Message> received = new ArrayList();
            final MessageListener msgListener = new MessageListener() {
                @Override public void onMessage(Message message) {
                    received.add(message);
                }
            };
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(msgListener);

            connection.start();

            //producer
            TextMessage message = session.createTextMessage("a");
            MessageProducer producer = session.createProducer(topic);
            producer.send(message);

            Thread.sleep(1000 * 2);

            assertThat(received.size(), is(1));
            received.clear();

            // close the consumer
            connection.stop();
            consumer.close();

            // send message
            TextMessage lostMessage = session.createTextMessage("b");
            producer.send(lostMessage);

            Thread.sleep(1000 * 2);

            // start the un-durable consumer again
            consumer = session.createConsumer(topic, "topic");
            consumer.setMessageListener(msgListener);
            connection.start();

            Thread.sleep(1000 * 3);

            assertThat(received.size(), is(0));

        }
        finally {
            connection.close();
            rocketMQAdmin.deleteTopic(rmqTopicName);
        }
    }

}
