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

import javax.jms.Message;
import javax.jms.MessageListener;
import org.apache.rocketmq.jms.domain.message.JmsBytesMessage;
import org.apache.rocketmq.jms.domain.message.JmsTextMessage;

public class MessageListenerImpl implements MessageListener {
    public void onMessage(Message message) {
        try {
            if (message instanceof JmsTextMessage) {
                JmsTextMessage textMessage = (JmsTextMessage)message;
                String text = textMessage.getText();
                System.out.println("Received text: " + text + ",receive properties: "
                    + textMessage.getProperties());
                System.out.println("headers:" + textMessage.getHeaders());
            }
            else if (message instanceof JmsBytesMessage) {
                JmsBytesMessage bytesMessage = (JmsBytesMessage)message;
                System.out.println("Received byte array: " + bytesMessage.getData() + "to String : "
                    + new String(bytesMessage.getData()));
            }

        }
        catch (Exception e) {
            // If there are some errors while processing the message，throw
            // Exception here to reDeliver the message

        }
    }

}
