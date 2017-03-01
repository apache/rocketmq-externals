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
public class SharedDurableConsumeTest {

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    @Test
    public void test() throws Exception {
        final String rmqTopicName = "coffee-syn" + UUID.randomUUID().toString();
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

            connectionA.start();
            connectionB.start();

            //producer
            TextMessage message = sessionA.createTextMessage("a");
            MessageProducer producer = sessionA.createProducer(topic);
            for (int i = 0; i < 10; i++) {
                producer.send(message);
            }

            Thread.sleep(1000 * 5);

            assertThat(receivedA.size(), is(10));
            assertThat(receivedB.size(), is(10));
        }
        finally {
            connectionA.close();
            connectionB.close();
            rocketMQAdmin.deleteTopic(rmqTopicName);
        }
    }

}
