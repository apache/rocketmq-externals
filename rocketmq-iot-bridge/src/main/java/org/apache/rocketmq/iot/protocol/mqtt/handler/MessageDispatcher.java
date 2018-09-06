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

package org.apache.rocketmq.iot.protocol.mqtt.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.common.util.MessageUtil;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;

@ChannelHandler.Sharable
public class MessageDispatcher extends SimpleChannelInboundHandler {

    private Map<Message.Type, MessageHandler> type2handler = new HashMap<>();

    private ClientManager clientManager;

    public MessageDispatcher(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    public void registerHandler(Message.Type type, MessageHandler handler) {
        type2handler.put(type, handler);
    }

    private void dispatch(Message message) {
        Message.Type type = message.getType();
        if (!type2handler.containsKey(type)) {
            return;
        }
        type2handler.get(type).handleMessage(message);
    }

    @Override protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof MqttMessage)) {
            return;
        }
        Client client = clientManager.get(ctx.channel());
        if (client == null) {
            client = new MqttClient();
            client.setCtx(ctx);
            clientManager.put(ctx.channel(), client);
        }
        MqttMessage mqttMessage = (MqttMessage) msg;
        Message message = MessageUtil.getMessage(mqttMessage);
        message.setClient(client);
        dispatch(message);
    }
}
