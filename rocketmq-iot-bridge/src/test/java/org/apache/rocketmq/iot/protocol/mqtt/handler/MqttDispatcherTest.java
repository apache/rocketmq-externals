package org.apache.rocketmq.iot.protocol.mqtt.handler;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.common.util.MessageUtil;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttDispatcherTest {

    private MessageDispatcher messageDispatcher;
    private ClientManager clientManager;
    private MessageHandler mockedConnectMessageHandler;
    private MessageHandler mockedDisconnectMessageHandler;


    @Before
    public void setup() {
        clientManager = Mockito.mock(ClientManager.class);
        messageDispatcher = new MessageDispatcher(clientManager);
        mockedConnectMessageHandler = Mockito.mock(MessageHandler.class);
        mockedDisconnectMessageHandler = Mockito.mock(MessageHandler.class);
    }

    @After public void teardown() {

    }

    @Test
    public void testRegisterHandler() throws IllegalAccessException {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mockedConnectMessageHandler);
        messageDispatcher.registerHandler(Message.Type.MQTT_DISCONNECT, mockedDisconnectMessageHandler);

        Map<Message.Type, MessageHandler> type2handler = (Map<Message.Type, MessageHandler>) FieldUtils.getField(MessageDispatcher.class, "type2handler", true).get(messageDispatcher);

        Assert.assertEquals(mockedConnectMessageHandler, type2handler.get(Message.Type.MQTT_CONNECT));
        Assert.assertEquals(mockedDisconnectMessageHandler, type2handler.get(Message.Type.MQTT_DISCONNECT));
    }

    public void testChanelRead0 () {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, mockedConnectMessageHandler);

        MqttConnectMessage mockedConnectMessage = Mockito.mock(MqttConnectMessage.class);

        Message mockedMessage = Mockito.spy(new Message());

        Mockito.when(MessageUtil.getMessage(mockedConnectMessage)).thenReturn(mockedMessage);

        EmbeddedChannel embeddedChannel = new EmbeddedChannel(messageDispatcher);

        embeddedChannel.writeInbound(mockedConnectMessage);

        Mockito.verify(mockedConnectMessageHandler).handleMessage(mockedMessage);

    }
}
