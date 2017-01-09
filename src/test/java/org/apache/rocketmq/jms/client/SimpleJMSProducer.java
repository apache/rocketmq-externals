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
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.apache.rocketmq.jms.domain.JmsBaseConnectionFactory;

public class SimpleJMSProducer {
    private static String metaqTopic = "jixiang-test-2";
    private static String metaqType = "TagA";
    private static String metaqProducerId = "PID-jixiang-test-1";

    private static String notifyTopic = "JIXIANG_NOTIFY_TEST";
    private static String notifyType = "NM-JIXIANG-TEST-2";
    private static String notifyProducerId = "p-jixiang-test-0";

    private static String accessKey = "BfqbMqEc4gYksKue";
    private static String secretKey = "zBQILPqFG8q08vbdeXtHks4H5D0cWW";
    private static int sendMsgTimeoutMillis = 10000;

    public static void main(String[] args) {
        try {
            JmsBaseConnectionFactory connectionFactory = new JmsBaseConnectionFactory(new
                URI("ons://xxx?producerId=" + metaqProducerId + "&sendMsgTimeoutMillis=" + sendMsgTimeoutMillis
                + "&accessKey=" + accessKey + "&secretKey=" + secretKey));
            Connection connection = connectionFactory.createConnection();
            try {
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createTopic(metaqTopic + ":" + metaqType);
                MessageProducer messageProducer = session.createProducer(destination);

//                BytesMessage message = session.createBytesMessage();
//                message.writeBytes("hello test ".getBytes());
                ObjectMessage message = session.createObjectMessage();
                message.setObject(new ObjectMessageDemo("jixiang", 26));
//                message.setJMSDeliveryMode(2);

                messageProducer.send(message);
                System.out.println("send message success! msgId:" + message.getJMSMessageID());
            }
            finally {
                //do the close work
                connection.close();
            }
        }
        catch (JMSException ex) {
            ex.getLinkedException().printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
