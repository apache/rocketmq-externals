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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.event.DisconnectChannelEvent;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class MqttConnectionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(MqttConnectionHandler.class);

    private ClientManager clientManager;
    private SubscriptionStore subscriptionStore;

    public MqttConnectionHandler(ClientManager clientManager, SubscriptionStore subscriptionStore) {
        this.clientManager = clientManager;
        this.subscriptionStore = subscriptionStore;
    }

    @Override public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (IdleState.ALL_IDLE.equals(idleStateEvent.state())) {
                doDisconnect(ctx.channel());
            }
        } else if (evt instanceof DisconnectChannelEvent) {
            DisconnectChannelEvent disconnectChannelEvent = (DisconnectChannelEvent) evt;
            doDisconnect(disconnectChannelEvent.getChannel());
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Client client = clientManager.get(ctx.channel());
        String clientId = client != null ? client.getId() : "null";
        doDisconnect(ctx.channel());
        log.debug("clientId:{} netty exception caught from {}", clientId, ctx.channel(), cause);
    }

    /**
     * disconnect the channel, save the Subscription of the client which the channel belongs to if CleanSession
     * if set to <b>false</b>, otherwise discard them
     *
     * @param channel
     */
    private void doDisconnect(Channel channel) {
        if (channel == null) {
            return;
        }
        MqttClient client = (MqttClient) clientManager.get(channel);
        if (client != null) {
            if (client.isCleanSession()) {
                subscriptionStore.getTopicFilters(client.getId()).forEach(filter -> {
                    subscriptionStore.getTopics(filter).forEach(topic -> {
                        subscriptionStore.remove(topic, client);
                    });
                });
                clientManager.remove(channel);
            } else {
                // TODO support Sticky Session
            }
        }
        channel.close();
    }
}
