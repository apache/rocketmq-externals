package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPingreqMessageHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttPingreqMessageHandlerTest {

    private ClientManager clientManager;
    private Client client;
    private ChannelHandlerContext ctx;
    private MqttPingreqMessageHandler handler;

    @Before
    public void setup() {
        clientManager = Mockito.mock(ClientManager.class);
        ctx = Mockito.mock(ChannelHandlerContext.class);
        client = Mockito.spy(new MqttClient());
        client.setConnected(true);
        handler = new MqttPingreqMessageHandler();

        Mockito.when(
            client.getCtx()
        ).thenReturn(
            ctx
        );
    }

    @After
    public void teardown() {
    }

    @Test
    public void testHandleMessage() {
        Message message = new Message();
        message.setClient(client);
        message.setPayload(getMqttPingreqMessage());

        handler.handleMessage(message);

        Mockito.verify(ctx).writeAndFlush(Mockito.any(MqttMessage.class));
    }

    private MqttMessage getMqttPingreqMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.PINGREQ,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        return new MqttMessage(
            fixedHeader,
            null,
            null
        );
    }
}
