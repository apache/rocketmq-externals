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

package org.apache.rocketmq.jms.integration.test;

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
import org.apache.rocketmq.jms.integration.source.AppConfig;
import org.apache.rocketmq.jms.integration.source.Constant;
import org.apache.rocketmq.jms.integration.source.RocketMQAdmin;
import org.apache.rocketmq.jms.integration.source.support.ConditionMatcher;
import org.apache.rocketmq.jms.integration.source.support.TimeLimitAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class SharedDurableConsumeTest {

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    /**
     * Test messages will be deliver to every consumer if these consumers are in shared durable subscription.
     *
     * <p>Test step:
     * 1. Create a share durable consumer(consumerA) via the first connection(connectionA)
     * 2. Create a share durable consumer(consumerB) via another connection(connectionB)
     * 3. The two consumer must subscribe the same topic with identical subscriptionName,
     * and they also have the same clientID.
     * 4. Send several(eg:10) messages to this topic
     * 5. Result: all messages should be received by both consumerA and consumerB
     *
     * @throws Exception
     */
    @Test
    public void testConsumeAllMessages() throws Exception {
        final String rmqTopicName = "coffee" + UUID.randomUUID().toString();
        rocketMQAdmin.createTopic(rmqTopicName);

        ConnectionFactory factory = new RocketMQConnectionFactory(Constant.NAME_SERVER_ADDRESS, Constant.CLIENT_ID);
        Connection connectionA = null, connectionB = null;
        final String subscriptionName = "MySubscription";
        final List<Message> receivedA = new ArrayList(), receivedB = new ArrayList();

        try {
            // consumerA
            connectionA = factory.createConnection();
            Session sessionA = connectionA.createSession();
            connectionA.start();
            Topic topic = sessionA.createTopic(rmqTopicName);
            MessageConsumer consumerA = sessionA.createSharedDurableConsumer(topic, subscriptionName);
            consumerA.setMessageListener(new MessageListener() {
                @Override public void onMessage(Message message) {
                    receivedA.add(message);
                }
            });

            // consumerB
            connectionB = factory.createConnection();
            Session sessionB = connectionB.createSession();
            MessageConsumer consumerB = sessionB.createSharedDurableConsumer(topic, subscriptionName);
            consumerB.setMessageListener(new MessageListener() {
                @Override public void onMessage(Message message) {
                    receivedB.add(message);
                }
            });
            connectionB.start();

            //producer
            TextMessage message = sessionA.createTextMessage("a");
            MessageProducer producer = sessionA.createProducer(topic);
            for (int i = 0; i < 10; i++) {
                producer.send(message);
            }

            Thread.sleep(1000 * 2);

            TimeLimitAssert.doAssert(new ConditionMatcher() {
                @Override public boolean match() {
                    return receivedA.size()==10 && receivedB.size()==10;
                }
            },5);
        }
        finally {
            connectionA.close();
            connectionB.close();
            rocketMQAdmin.deleteTopic(rmqTopicName);
        }
    }

}
