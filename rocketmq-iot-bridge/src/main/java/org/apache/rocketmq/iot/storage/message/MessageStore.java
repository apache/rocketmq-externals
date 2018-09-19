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

package org.apache.rocketmq.iot.storage.message;

import java.util.List;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;

public interface MessageStore {

    /**
     * Get message from the topic by <code>id</code>, the method will return
     * <b>null</b> if the topic doesn't exist or the message doesn't exist
     * @param id identifier of the message
     * @return
     */
    Message get(String id);

    /**
     * Put message to MessageStore
     * @param message the message to put to the Message Store
     * @return identidier of saved message
     */
    String put(Message message);

    /**
     * Prepare to push message to the clients by marking the receiving id list of the clients
     * @param messageId the identifier of the message to be prepared
     * @param clientIds the identifier list of the clients which will receive the message
     * @param qos the QoS level of the message to be prepared
     */
    void prepare(String messageId, List<String> clientIds, int qos);

    /**
     * Acknowledge the message for the specific client
     * @param message the message to be acknowledged
     * @param client the client which acknowledge the message
     */
    void ack(Message message, MqttClient client);

    /**
     * Acknowledge the message for the specific list of clients
     * @param message the message to be acknowledged
     * @param clients the list of clients which acknowledge the message
     */
    void ack(Message message, List<MqttClient> clients);

    /**
     * Expire the message with specific identifier
     * @param id identifier of the message to be expired
     */
    void expire(String id);

    /**
     * Start the MessageStore
     */
    void start();

    /**
     * Get offline messages of the client
     * @param client
     * @return the offline messages of the client
     */
    List<Message> getOfflineMessages(MqttClient client);

    /**
     * Shutdown the MessageStore
     */
    void shutdown();
}
