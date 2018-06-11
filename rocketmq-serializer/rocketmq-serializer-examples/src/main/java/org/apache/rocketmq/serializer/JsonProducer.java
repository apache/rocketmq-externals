/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.serializer;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.serializer.json.RocketMQJsonSerializer;

public class JsonProducer {
    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer("producer-group-json");
        producer.setNamesrvAddr("localhost:9876");
        producer.start();

        // creating serializer for message body serializing
        RocketMQSerializer serializer = new RocketMQJsonSerializer<User>();

        for (int i = 0; i < 100; i++) {
            User user = new User();
            user.setName("tom");
            user.setAge(i);

            // creating message from user data.
            Message message = Messages.newMessage("topic-json", user, serializer);
            SendResult result = producer.send(message);
            System.out.print(result.getSendStatus() + " " + i + "\n");

            Thread.sleep(1000);
        }
    }
}

