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
package org.apache.rocketmq.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.common.MqttChannelEventListener;
import org.apache.rocketmq.mqtt.common.NettyChannelImpl;
import org.apache.rocketmq.mqtt.common.RequestTask;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.processor.MqttRequestProcessor;
import org.apache.rocketmq.remoting.ChannelEventListener;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.remoting.common.Pair;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.common.SemaphoreReleaseOnlyOnce;
import org.apache.rocketmq.remoting.common.ServiceThread;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.mqtt.interceptor.InterceptorGroup;
import org.apache.rocketmq.mqtt.interceptor.InterceptorInvoker;
import org.apache.rocketmq.remoting.netty.NettyEvent;
import org.apache.rocketmq.remoting.netty.NettyLogger;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.mqtt.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MqttRemotingAbstract {

    private static final Logger log = LoggerFactory.getLogger(MqttRemotingAbstract.class);

    /**
     * Semaphore to limit maximum number of on-going one-way requests, which protects system memory footprint.
     */
    protected Semaphore semaphoreOneway;

    /**
     * This container holds all processors per request code, aka, for each incoming request, we may look up the
     * responding processor in this map to handle the request.
     */
    protected final HashMap<MqttMessageType, Pair<MqttRequestProcessor, ExecutorService>> processorTable =
        new HashMap<>(64);

    /**
     * Executor to feed netty events to user defined {@link ChannelEventListener}.
     */
    protected NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();

    /**
     * The default request processor to use in case there is no exact match in {@link #processorTable} per request
     * code.
     */
    protected Pair<MqttRequestProcessor, ExecutorService> defaultRequestProcessor;

    /**
     * Used for async execute task for aysncInvokeMethod
     */
    private ExecutorService asyncExecuteService = ThreadUtils.newFixedThreadPool(5, 10000, "asyncExecute", false);

    /**
     * SSL context via which to create {@link SslHandler}.
     */
    protected volatile SslContext sslContext;

    static {
        NettyLogger.initNettyLogger();
    }

    public MqttRemotingAbstract() {
    }

    /**
     * Constructor, specifying capacity of one-way and asynchronous semaphores.
     *
     * @param permitsOneway Number of permits for one-way requests.
     */
    public MqttRemotingAbstract(final int permitsOneway) {
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
    }

    public void init(final int permitsOneway) {
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
    }

    /**
     * Custom channel event listener.
     *
     * @return custom channel event listener if defined; null otherwise.
     */
    public abstract MqttChannelEventListener getMqttChannelEventListener();

    /**
     * Put a netty event to the executor.
     *
     * @param event Netty event instance.
     */
    public void putNettyEvent(final NettyEvent event) {
        this.nettyEventExecutor.putNettyEvent(event);
    }

    /**
     * Entry of incoming command processing.
     *
     * @param ctx Channel handler context.
     * @param command incoming remoting command.
     */
    public void processMessageReceived(ChannelHandlerContext ctx, MqttMessage command) {
        if (command != null) {
            final RemotingChannel remotingChannel = new NettyChannelImpl(ctx);
            processRequestCommand(remotingChannel, command);
        }
    }

    /**
     * Process incoming request command issued by remote peer.
     *
     * @param remotingChannel channel handler context.
     * @param cmd request command.
     */
    public void processRequestCommand(final RemotingChannel remotingChannel, final MqttMessage cmd) {
        final ChannelHandlerContext ctx = ((NettyChannelImpl) remotingChannel).getChannelHandlerContext();
        final Pair<MqttRequestProcessor, ExecutorService> matched = this.processorTable.get(cmd.fixedHeader().messageType());
        final Pair<MqttRequestProcessor, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;
        final InterceptorGroup interceptorGroup = MqttRemotingAbstract.this.getInterceptorGroup();
        if (pair != null) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    try {
                        InterceptorInvoker.invokeBeforeRequest(interceptorGroup, remotingChannel, cmd);
                        final MqttMessage response = pair.getObject1().processRequest(remotingChannel, cmd);
                        InterceptorInvoker.invokeAfterRequest(interceptorGroup, remotingChannel, cmd, response);

                        if (response != null) {
                            try {
                                ctx.writeAndFlush(response);
                            } catch (Throwable e) {
                                log.error("process request over, but response failed", e);
                                log.error(cmd.toString());
                                log.error(response.toString());
                            }
                        }

                    } catch (Throwable throwable) {
                        log.error("Process request exception", throwable);
                        log.error(cmd.toString());
                        InterceptorInvoker.invokeOnException(interceptorGroup, remotingChannel, cmd, throwable, null);
                    }
                }
            };

            if (pair.getObject1().rejectRequest()) {
                log.error("[REJECTREQUEST]system busy, start flow control for a while");
                throw new MqttRuntimeException("[REJECTREQUEST]system busy, start flow control for a while");
            }

            try {
                final RequestTask requestTask = new RequestTask(run, ctx.channel(), cmd);
                pair.getObject2().submit(requestTask);
            } catch (RejectedExecutionException e) {
                if ((System.currentTimeMillis() % 10000) == 0) {
                    log.warn(RemotingHelper.parseChannelRemoteAddr(ctx.channel())
                        + ", too many requests and system thread pool busy, RejectedExecutionException "
                        + pair.getObject2().toString()
                        + " MqttMessageType: " + cmd.fixedHeader().messageType());
                }
                log.error("pair.getObject1()={}, pair.getObject2()={}", pair.getObject1().toString(), pair.getObject2().toString());
                log.error("[OVERLOAD]system busy, start flow control for a while");
                throw new MqttRuntimeException("[REJECTREQUEST]system busy, start flow control for a while");
            }
        } else {
            log.error("MqttMessageType {} not supported", cmd.fixedHeader().messageType());
            throw new MqttRuntimeException("[REJECTREQUEST]system busy, start flow control for a while");
        }
    }

    /**
     * Execute callback in callback executor. If callback executor is null, run directly in current thread
     */
    private void executeInvokeCallback(final ResponseFuture responseFuture) {
        boolean runInThisThread = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            responseFuture.executeInvokeCallback();
                        } catch (Throwable e) {
                            log.warn("execute callback in executor exception, and callback throw", e);
                        } finally {
                            responseFuture.release();
                        }
                    }
                });
            } catch (Exception e) {
                runInThisThread = true;
                log.warn("execute callback in executor exception, maybe executor busy", e);
            }
        } else {
            runInThisThread = true;
        }

        if (runInThisThread) {
            try {
                responseFuture.executeInvokeCallback();
            } catch (Throwable e) {
                log.warn("executeInvokeCallback Exception", e);
            } finally {
                responseFuture.release();
            }
        }
    }

    /**
     * Custom interceptor hook.
     *
     * @return Interceptor hooks if specified; null otherwise.
     */
    public abstract InterceptorGroup getInterceptorGroup();

    /**
     * This method specifies thread pool to use while invoking callback methods.
     *
     * @return Dedicated thread pool instance if specified; or null if the callback is supposed to be executed in the
     * netty event-loop thread.
     */
    public abstract ExecutorService getCallbackExecutor();

    public void start() {
        if (getMqttChannelEventListener() != null) {
            nettyEventExecutor.start();
        }

    }

    public void shutdown() {
        if (this.nettyEventExecutor != null) {
            this.nettyEventExecutor.shutdown();
        }
    }

    abstract protected RemotingChannel getAndCreateChannel(final String addr, long timeout) throws InterruptedException;

    public void invokeOnewayWithInterceptor(final RemotingChannel remotingChannel, final MqttMessage request,
        final long timeoutMillis)
        throws
        InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        Channel channel = null;
        InterceptorGroup interceptorGroup = this.getInterceptorGroup();
        InterceptorInvoker.invokeBeforeRequest(interceptorGroup, remotingChannel, request);
        if (remotingChannel instanceof NettyChannelImpl) {
            channel = ((NettyChannelImpl) remotingChannel).getChannel();
        }
        try {
            invokeOnewayImpl(channel, request, timeoutMillis);
        } catch (InterruptedException | RemotingTooMuchRequestException | RemotingTimeoutException | RemotingSendRequestException ex) {
            InterceptorInvoker.invokeOnException(interceptorGroup, remotingChannel, request, ex, null);
            throw ex;
        }
    }

    public void invokeOnewayImpl(final Channel channel, final MqttMessage request, final long timeoutMillis)
        throws
        InterruptedException, RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        boolean acquired = this.semaphoreOneway.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS);
        if (acquired) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(this.semaphoreOneway);
            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        once.release();
                        if (!f.isSuccess()) {
                            log.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                        }
                    }
                });
            } catch (Exception e) {
                once.release();
                log.warn("write send a request command to channel <" + channel.remoteAddress() + "> failed.");
                throw new RemotingSendRequestException(RemotingHelper.parseChannelRemoteAddr(channel), e);
            }
        } else {
            if (timeoutMillis <= 0) {
                throw new RemotingTooMuchRequestException("invokeOnewayImpl invoke too fast");
            } else {
                String info = String.format(
                    "invokeOnewayImpl tryAcquire semaphore timeout, %dms, waiting thread nums: %d semaphoreAsyncValue: %d",
                    timeoutMillis,
                    this.semaphoreOneway.getQueueLength(),
                    this.semaphoreOneway.availablePermits()
                );
                log.warn(info);
                throw new RemotingTimeoutException(info);
            }
        }
    }

    class NettyEventExecutor extends ServiceThread {
        private final LinkedBlockingQueue<NettyEvent> eventQueue = new LinkedBlockingQueue<NettyEvent>();
        private final int maxSize = 200000;

        public void putNettyEvent(final NettyEvent event) {
            if (this.eventQueue.size() <= maxSize) {
                this.eventQueue.add(event);
            } else {
                log.warn("event queue size[{}] enough, so drop this event {}", this.eventQueue.size(), event.toString());
            }
        }

        @Override
        public void run() {
            log.info(this.getServiceName() + " service started");

            final MqttChannelEventListener listener = MqttRemotingAbstract.this.getMqttChannelEventListener();

            while (!this.isStopped()) {
                try {
                    NettyEvent event = this.eventQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (event != null && listener != null) {
                        switch (event.getType()) {
                            case IDLE:
                                listener.onChannelIdle(event.getRemoteAddr(), new NettyChannelImpl(event.getChannel()));
                                break;
                            case CLOSE:
                                listener.onChannelClose(event.getRemoteAddr(), new NettyChannelImpl(event.getChannel()));
                                break;
                            case CONNECT:
                                listener.onChannelConnect(event.getRemoteAddr(), new NettyChannelImpl(event.getChannel()));
                                break;
                            case EXCEPTION:
                                listener.onChannelException(event.getRemoteAddr(), new NettyChannelImpl(event.getChannel()));
                                break;
                            default:
                                break;

                        }
                    }
                } catch (Exception e) {
                    log.warn(this.getServiceName() + " service has exception. ", e);
                }
            }

            log.info(this.getServiceName() + " service end");
        }

        @Override
        public String getServiceName() {
            return NettyEventExecutor.class.getSimpleName();
        }
    }

    public void registerIotProcessor(MqttMessageType messageType, MqttRequestProcessor processor,
        ExecutorService executor) {
        Pair<MqttRequestProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(messageType, pair);
    }

    public class IotServerHandler extends SimpleChannelInboundHandler<MqttMessage> {
        public IotServerHandler(boolean autoRelease) {
            super(autoRelease);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg) throws Exception {
            processMessageReceived(ctx, msg);
        }
    }

}
