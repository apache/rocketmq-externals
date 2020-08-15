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
