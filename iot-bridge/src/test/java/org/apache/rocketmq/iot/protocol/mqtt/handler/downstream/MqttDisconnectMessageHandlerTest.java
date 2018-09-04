package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.awt.color.CMMException;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.Client;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttDisconnectMessageHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttDisconnectMessageHandlerTest extends AbstractMqttMessageHandlerTest {

    @Override public void setupMessage() {
        message.setType(Message.Type.MQTT_DISCONNECT);
        message.setPayload(getMqttDisconnectMessage());
    }

    @Override public void assertConditions() {
        Mockito.verify(clientManager).remove(Mockito.any(Channel.class));
        Assert.assertFalse(embeddedChannel.isOpen());
    }

    @Override public void mock() {

    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttDisconnectMessageHandler(clientManager);
    }

    private MqttMessage getMqttDisconnectMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.DISCONNECT,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        return new MqttMessage(fixedHeader, null, null);
    }

}
