package org.apache.rocketmq.iot.protocol.mqtt.handler.downstream;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.MqttClient;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageHandler;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public abstract class AbstractMqttMessageHandlerTest {

    protected ClientManager clientManager;
    protected SubscriptionStore subscriptionStore;
    protected EmbeddedChannel embeddedChannel;
    protected Message message;
    protected MockHandler mockHandler;
    protected MessageHandler messageHandler;
    protected MqttClient client;

    class MockHandler extends SimpleChannelInboundHandler<Message> {

        private MessageHandler handler;

        MockHandler(MessageHandler handler) {
            this.handler = handler;
        }

        @Override protected void channelRead0(ChannelHandlerContext context, Message message) throws Exception {
            handler.handleMessage(message);
        }
    }

    /**
     * setup the message which will be handled by the mockHandler
     * <ol>
     *     <li>set message Type</li>
     *     <li>set message Payload</li>
     * </ol>
     */
    public abstract void setupMessage();

    /**
     * check the conditions after handle the message
     */
    public abstract void assertConditions();

    /**
     * mock the behaviors of the stubs
     */
    public abstract void mock();

    /**
     * init the message handler
     */
    protected abstract void initMessageHandler();

    @Test
    public void testHandleMessage() {
        embeddedChannel.writeInbound(message);
        assertConditions();
    }

    @Before
    public void setup() {
        subscriptionStore = Mockito.mock(SubscriptionStore.class);
        clientManager = Mockito.mock(ClientManager.class);
        client = Mockito.spy(new MqttClient());

        initMessageHandler();
        mockHandler = new MockHandler(messageHandler);
        embeddedChannel = new EmbeddedChannel(mockHandler);
        initMessage();
        mock();

        Mockito.when(client.getCtx()).thenReturn(
            embeddedChannel.pipeline().context(mockHandler)
        );
    }

    @After
    public void teardown() {

    }

    private void initMessage() {
        message = new Message();
        setupMessage();
        message.setClient(client);
    }
}
