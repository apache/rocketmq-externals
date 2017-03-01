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
public class ConsumeAsynchronousTest {

    @Autowired
    private RocketMQAdmin rocketMQAdmin;

    @Test
    public void testConsumeAsynchronous() throws Exception {
        final String rmqTopicName = "coffee-async" + UUID.randomUUID().toString();
        rocketMQAdmin.createTopic(rmqTopicName);

        ConnectionFactory factory = new RocketMQConnectionFactory(Constant.NAME_SERVER_ADDRESS, Constant.CLIENT_ID);
        Connection connection = factory.createConnection();
        Session session = connection.createSession();
        Topic topic = session.createTopic(rmqTopicName);

        try {
            //producer
            TextMessage message = session.createTextMessage("mocha coffee,please");
            MessageProducer producer = session.createProducer(topic);
            producer.send(message);

            //consumer
            final List<Message> received = new ArrayList();
            MessageConsumer consumer = session.createDurableConsumer(topic, "consumer");
            consumer.setMessageListener(new MessageListener() {
                @Override public void onMessage(Message message) {
                    received.add(message);
                }
            });

            connection.start();

            TimeLimitAssert.doAssert(new ConditionMatcher() {
                @Override public boolean match() {
                    return received.size() == 1;
                }
            }, 5);
        }
        finally {
            connection.close();
            rocketMQAdmin.deleteTopic(rmqTopicName);
        }

    }
}
