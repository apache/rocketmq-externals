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
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.rocketmq.jms.domain.JmsBaseConnectionFactory;
import org.apache.rocketmq.jms.integration.IntegrationTestBase;
import org.junit.Assert;
import org.junit.Test;

public class JmsClientTest extends IntegrationTestBase {

    /**
     * Normal test: send and receive a message
     *
     * @throws Exception
     */
    @Test
    public void onsJmsClientTest() throws Exception {
        //common
        JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
            URI("rocketmq://xxx?producerId=" + producerId + "&consumerId=" + consumerId + "&nameServer=" + nameServer));
        Connection connection = connectionFactory.createConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createTopic(topic + ":" + messageType);

        try {
            //producer
            MessageProducer messageProducer = session.createProducer(destination);

            TextMessage message = session.createTextMessage(this.text);
            messageProducer.send(message);
            Assert.assertNotNull(message.getJMSMessageID());

            //consumer
            MessageConsumer messageConsumer = session.createConsumer(destination);
            messageConsumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    try {
                        Assert.assertNotNull(message);
                        Assert.assertNotNull(message.getJMSMessageID());
                    }
                    catch (Exception e) {
                        throw new RuntimeException("Handle message error!");
                    }
                }
            });
            connection.start();

            //wait 5 seconds for message to arrive
            Thread.sleep(5000);
        }
        finally {
            //Close the connection
            connection.close();
        }

    }


}