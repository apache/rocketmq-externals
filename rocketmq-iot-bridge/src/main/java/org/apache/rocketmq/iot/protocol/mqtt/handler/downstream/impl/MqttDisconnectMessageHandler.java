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

import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;

public class MqttDisconnectMessageHandler implements MessageHandler {

    private ClientManager clientManager;

    public MqttDisconnectMessageHandler(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * handle the DISCONNECT message from the client
     * <ol>
     *     <li>discard the Will Message and Will Topic</li>
     *     <li>remove the client from the ClientManager</li>
     *     <li>disconnect the connection</li>
     * </ol>
     * @param message
     * @return
     */
    @Override public void handleMessage(Message message) {
        // TODO discard the Will Message and Will Topic
        clientManager.remove(message.getClient().getCtx().channel());
        message.getClient().getCtx().disconnect();
    }
}
