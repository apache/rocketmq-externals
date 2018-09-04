package org.apache.rocketmq.iot.protocol.mqtt.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.rocketmq.iot.common.data.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MqttIdleHandlerTest {

    private ChannelHandlerContext ctx;
    private Channel channel;
    private MqttIdleHandler idleHandler;
    private AtomicReference<MqttIdleHandler.State> state;
    private EventLoop loop;

    @Before
    public void setup() throws IllegalAccessException {
        idleHandler = new MqttIdleHandler(1);
        ctx = Mockito.mock(ChannelHandlerContext.class);
//        channel = Mockito.spy(new EmbeddedChannel());
//        channel.attr(ChannelConfiguration.CHANNEL_IDLE_TIME_ATTRIBUTE_KEY).set(5);
        channel = Mockito.mock(Channel.class);
        state = (AtomicReference<MqttIdleHandler.State>) FieldUtils.getField(MqttIdleHandler.class, "state", true).get(idleHandler);
        loop = new DefaultEventLoop();

        Mockito.when(ctx.channel()).thenReturn(channel);
        Mockito.when(ctx.executor()).thenReturn(loop);
        Mockito.when(ctx.fireChannelRead(Mockito.any())).then(new Answer() {

            @Override public Object answer(InvocationOnMock mock) throws Throwable {
                return null;
            }
        });
        Mockito.when(ctx.channel().isOpen()).thenReturn(true);
        Mockito.when(ctx.fireUserEventTriggered(Mockito.any(IdleStateEvent.class))).thenAnswer(new Answer<Object>() {
            @Override public Object answer(InvocationOnMock mock) throws Throwable {
                loop.shutdownGracefully();
                return null;
            }
        });
    }

    @After
    public void teardown() {
    }

    @Test
    public void test() throws Exception {
        Message message = new Message();
        idleHandler.channelActive(ctx);
        idleHandler.channelRead(ctx, message);
        idleHandler.channelReadComplete(ctx);
        Thread.sleep(2000); // sleep for 2s
        Mockito.verify(ctx).fireUserEventTriggered(Mockito.any(IdleStateEvent.class));
    }

}
