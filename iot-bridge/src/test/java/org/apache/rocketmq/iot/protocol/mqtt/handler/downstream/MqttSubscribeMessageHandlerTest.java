package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttSubscribeMessageHandler;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MqttSubscribeMessageHandlerTest extends AbstractMqttMessageHandlerTest {

    private MqttSubscribeMessage getMqttSubscribeMessage() {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(
            MqttMessageType.SUBSCRIBE,
            false,
            MqttQoS.AT_MOST_ONCE,
            false,
            0
        );
        MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from(1);
        List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        subscriptions.add(new MqttTopicSubscription("topic1", MqttQoS.AT_MOST_ONCE));
        subscriptions.add(new MqttTopicSubscription("topic2", MqttQoS.AT_LEAST_ONCE));
        MqttSubscribePayload payload = new MqttSubscribePayload(subscriptions);

        return new MqttSubscribeMessage(
            fixedHeader,
            variableHeader,
            payload
        );
    }

    @Override public void setupMessage() {
        message.setType(Message.Type.MQTT_SUBSCRIBE);
        message.setPayload(getMqttSubscribeMessage());
    }

    @Override public void assertConditions() {
        MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) this.message.getPayload();
        MqttSubAckMessage ackMessage = embeddedChannel.readOutbound();
        Assert.assertEquals(subscribeMessage.variableHeader().messageId(), ackMessage.variableHeader().messageId());
        Assert.assertEquals(subscribeMessage.payload().topicSubscriptions().size(), ackMessage.payload().grantedQoSLevels().size());
    }

    @Override public void mock() {
    }

    @Override protected void initMessageHandler() {
        messageHandler = new MqttSubscribeMessageHandler(subscriptionStore);
    }
}
