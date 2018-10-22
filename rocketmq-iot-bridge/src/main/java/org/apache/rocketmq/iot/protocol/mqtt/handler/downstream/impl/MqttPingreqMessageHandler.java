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

import io.netty.handler.codec.mqtt.MqttMessage;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.common.util.MessageUtil;

public class MqttPingreqMessageHandler implements MessageHandler {

    /**
     * handle the PINGREQ message from client
     * <ol>
     * <li>check client exists</li>
     * <li>check client is connected</li>
     * <li>generate the PINGRESP message</li>
     * <li>send the PINGRESP message to the client</li>
     * </ol>
     *
     * @param message
     * @return
     */
    @Override public void handleMessage(Message message) {
        Client client = message.getClient();
        if (client == null || !client.isConnected()) {
            return ;
        }
        MqttMessage pingreqMessage = (MqttMessage) message.getPayload();
        MqttMessage pingrespMessage = MessageUtil.getMqttPingrespMessage(pingreqMessage);
        client.getCtx().writeAndFlush(pingrespMessage);
    }
}
