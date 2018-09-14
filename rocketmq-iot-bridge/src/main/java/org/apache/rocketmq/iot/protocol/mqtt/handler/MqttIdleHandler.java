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

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.rocketmq.iot.common.configuration.ChannelConfiguration;

public class MqttIdleHandler extends ChannelDuplexHandler {

    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private volatile long lastReadTimeNanos;

    private volatile long lastWriteTimeNanos;

    private volatile long allIdleTimeNanos = 0;

    private volatile ScheduledFuture<?> allIdleTimeoutFuture;

    boolean firstAllIdleEvent = true;

    public enum State {
        NONE,
        INITIALIZED,
        DESTROYED,
        READING
    };

    private volatile AtomicReference<State> state = new AtomicReference<>();

    public MqttIdleHandler() {
        this(0);
    }
    public MqttIdleHandler(long allIdleTimeNanos) {
        this.allIdleTimeNanos = allIdleTimeNanos;
        state.set(State.NONE);
    }

    private final ChannelFutureListener writeListener = new ChannelFutureListener() {
        @Override public void operationComplete(ChannelFuture future) throws Exception {
            lastWriteTimeNanos = System.nanoTime();
            firstAllIdleEvent = true;
        }
    };

    @Override public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            initialize(ctx);
        }
    }

    @Override public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception {
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (allIdleTimeNanos > 0) {
            state.set(State.READING);
            firstAllIdleEvent = true;
        }
        ctx.fireChannelRead(msg);
    }

    @Override public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (state.get() == State.NONE) {
            Integer idleTime = ctx.channel().attr(ChannelConfiguration.CHANNEL_IDLE_TIME_ATTRIBUTE_KEY).get();
            reDoInit(ctx, idleTime);
        }
        state.set(State.INITIALIZED);
        ctx.fireChannelReadComplete();
    }

    @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (allIdleTimeNanos > 0) {
            ChannelPromise unvoidPromise = promise.unvoid();
            unvoidPromise.addListener(writeListener);
            ctx.write(msg, unvoidPromise);
        } else {
            ctx.write(msg, promise);
        }
    }

    private void initialize(ChannelHandlerContext ctx) {
        if (state.get() != State.NONE) {
            return;
        }
        state.set(State.INITIALIZED);

        EventExecutor loop = ctx.executor();

        lastReadTimeNanos = lastWriteTimeNanos = System.nanoTime();
        if (allIdleTimeNanos > 0) {
            loop.schedule(new AllIdleTimeoutTask(ctx), allIdleTimeNanos, TimeUnit.SECONDS);
        }
    }

    private void destroy() {
        state.set(State.DESTROYED);
        if (allIdleTimeoutFuture != null) {
            allIdleTimeoutFuture.cancel(false);
            allIdleTimeoutFuture = null;
        }
    }

    private void reDoInit(ChannelHandlerContext ctx, Integer idleTime) {
        state.set(State.INITIALIZED);
        destroy();
        allIdleTimeNanos = Math.max(TimeUnit.SECONDS.toNanos(idleTime), MIN_TIMEOUT_NANOS);
        state.set(State.NONE);
        initialize(ctx);
    }

    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent event) throws Exception {
        ctx.fireUserEventTriggered(event);
    }

    private final class AllIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;

        public AllIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }
            long nextDelay = allIdleTimeNanos;
            if (state.get() != State.READING) {
                nextDelay -= System.nanoTime() - Math.max(lastReadTimeNanos, lastWriteTimeNanos);
            }
            if (nextDelay < 0) {
                /**
                 * Both reader and writer are idle,
                 * set a new timeout and notify the callback
                 */
                allIdleTimeoutFuture = ctx.executor().schedule(
                    this,
                    allIdleTimeNanos,
                    TimeUnit.NANOSECONDS
                );

                try {
                    IdleStateEvent event;
                    if (firstAllIdleEvent) {
                        firstAllIdleEvent = false;
                        event = IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT;
                    } else {
                        event = IdleStateEvent.ALL_IDLE_STATE_EVENT;
                    }
                    channelIdle(ctx, event);
                } catch (Throwable throwable) {
                    ctx.fireExceptionCaught(throwable);
                }
            } else {
                allIdleTimeoutFuture = ctx.executor().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}