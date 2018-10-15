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

package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl;

import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import java.util.List;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.storage.message.MessageStore;

public class MqttPubackMessageHandler implements MessageHandler {

    private MessageStore messageStore;

    public MqttPubackMessageHandler(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    /**
     * handle the PUBACK message from the client
     * <ol>
     *     <li>remove the message from the published in-flight messages</li>
     *     <li>ack the message in the MessageStore</li>
     * </ol>
     * @param
     * @return
     */
    @Override public void handleMessage(Message message) {
        MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) message.getPayload();
        // TODO: remove the message from the published in-flight message
        messageStore.ack(message, (List<MqttClient>) message.getClient());
    }
}
