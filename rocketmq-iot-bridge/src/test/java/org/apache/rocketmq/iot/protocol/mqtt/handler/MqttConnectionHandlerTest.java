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
