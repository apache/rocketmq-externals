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
import org.apache.rocketmq.jms.RocketMQSession;
import org.apache.rocketmq.jms.exception.DuplicateSubscriptionException;
import org.apache.rocketmq.jms.integration.support.ConditionMatcher;
import org.apache.rocketmq.jms.integration.support.TimeLimitAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class UnsharedDurableConsumeTest {

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    /**
     * Test each message will be deliver to only one consumer if these consumers are in unshared durable subscription.
     *
     * <p>Test step:
     * 1. Create a unshared durable consumer(consumerA) via the first connection(connectionA)
     * 2. Create a unshared durable consumer(consumerB) via another connection(connectionB)
     * 3. Result:
     * a. The creating consumerB should throw a JMSException as consumerA and consumberB have the same subscription
     * b. All messages should be received by consumerA
     *
     * @throws Exception
     * @see {@link RocketMQSession}
     */
    @Test
    public void testEachMessageOnlyConsumeByOneConsumer() throws Exception {
        final String rmqTopicName = "coffee" + UUID.randomUUID().toString();
        rocketMQAdmin.createTopic(rmqTopicName, 2);

        ConnectionFactory factory = new RocketMQConnectionFactory(Constant.NAME_SERVER_ADDRESS, Constant.CLIENT_ID);
        Connection connectionA = null, connectionB = null;
        final String subscriptionName = "MySubscription";
        final List<Message> receivedA = new ArrayList();

        try {
            // consumerA
            connectionA = factory.createConnection();
            Session sessionA = connectionA.createSession();
            connectionA.start();
            Topic topic = sessionA.createTopic(rmqTopicName);
            MessageConsumer consumerA = sessionA.createDurableConsumer(topic, subscriptionName);
            consumerA.setMessageListener(new MessageListener() {
                @Override public void onMessage(Message message) {
                    receivedA.add(message);
                }
            });

            Thread.sleep(1000 * 2);

            // consumerB
            try {
                connectionB = factory.createConnection();
                Session sessionB = connectionB.createSession();
                sessionB.createDurableConsumer(topic, subscriptionName);
                assertFalse("Doesn't get the expected " + DuplicateSubscriptionException.class.getSimpleName(), true);
            }
            catch (DuplicateSubscriptionException e) {
                assertTrue(true);
            }

            connectionA.start();

            //producer
            TextMessage message = sessionA.createTextMessage("a");
            MessageProducer producer = sessionA.createProducer(topic);
            for (int i = 0; i < 10; i++) {
                producer.send(message);
            }

            TimeLimitAssert.doAssert(new ConditionMatcher() {
                @Override public boolean match() {
                    return receivedA.size() == 10;
                }
            }, 5);
        }
        finally {
            connectionA.close();
            connectionB.close();
            rocketMQAdmin.deleteTopic(rmqTopicName);
        }
    }

}
