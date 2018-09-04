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

package org.apache.rocketmq.iot.storage.message.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.storage.message.MessageStore;

public class InMemoryMessageStore implements MessageStore {

    private Map<String, List<Message>> topic2messageQueue;
    private Map<String, Message> id2message;

    @Override public Message get(String id) {
        return id2message.get(id);
    }

    @Override public String put(Message message) {
        String topic = message.getTopic();
        if (topic == null) {
            return null;
        }
        if (!topic2messageQueue.containsKey(topic)) {
            topic2messageQueue.put(topic, new LinkedList<>());
        }
        
        String messageId = generateId(message);
        message.setId(messageId);
        
        topic2messageQueue.get(topic).add(message);
        id2message.put(messageId, message);
            
        return messageId;
    }

    private String generateId(Message message) {
        return message.getTopic() + "-" + message.getClient() + "-" + System.currentTimeMillis();
    }

    @Override public void prepare(String messageId, List<String> clientIds, int qos) {

    }

    @Override public void ack(Message message, MqttClient client) {

    }

    @Override public void ack(Message message, List<MqttClient> clients) {

    }

    @Override public void expire(String id) {

    }

    @Override public void start() {

    }

    @Override public List<Message> getOfflineMessages(MqttClient client) {
        return null;
    }

    @Override public void shutdown() {

    }
}
