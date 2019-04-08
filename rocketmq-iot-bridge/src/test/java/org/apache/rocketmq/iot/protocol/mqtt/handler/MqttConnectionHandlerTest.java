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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.event.DisconnectChannelEvent;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttConnectionHandlerTest {
    private MqttConnectionHandler handler;
    private ClientManager clientManager;
    private SubscriptionStore subscriptionStore;
    private Client client;
    private Channel channel;
    private ChannelHandlerContext ctx;

    @Before
    public void setup() {
        clientManager = Mockito.mock(ClientManager.class);
        subscriptionStore = Mockito.mock(SubscriptionStore.class);
        handler = new MqttConnectionHandler(clientManager, subscriptionStore);
        client = Mockito.spy(new MqttClient());
        channel = Mockito.mock(Channel.class);
        ctx = Mockito.mock(ChannelHandlerContext.class);

        Mockito.when(
            clientManager.get(channel)
        ).thenReturn(client);

        Mockito.when(
            ctx.channel()
        ).thenReturn(channel);
    }

    @After
    public void teardown() {

    }

    @Test
    public void testHandleDisconnectChannelEvent() throws Exception {
        DisconnectChannelEvent event = new DisconnectChannelEvent(channel);
        handler.userEventTriggered(ctx, event);

        Mockito.verify(clientManager).remove(channel);
        Mockito.verify(channel).close();
    }

    @Test
    public void testHandleIdleStateEvent() throws Exception {
        IdleStateEvent idleStateEvent = IdleStateEvent.ALL_IDLE_STATE_EVENT;

        handler.userEventTriggered(ctx, idleStateEvent);
        Mockito.verify(clientManager, Mockito.times(1)).remove(channel);
        Mockito.verify(channel, Mockito.times(1)).close();
    }
}
