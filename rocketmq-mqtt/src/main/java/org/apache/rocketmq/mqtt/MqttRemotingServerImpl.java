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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.common.MqttChannelEventListener;
import org.apache.rocketmq.mqtt.common.NettyChannelImpl;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.mqtt.processor.MqttRequestProcessor;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.common.Pair;
import org.apache.rocketmq.remoting.common.TlsMode;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.mqtt.interceptor.InterceptorGroup;
import org.apache.rocketmq.remoting.netty.*;
import org.apache.rocketmq.mqtt.utils.JvmUtils;
import org.apache.rocketmq.mqtt.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttRemotingServerImpl extends MqttRemotingServerAbstract implements MqttRemotingServer {

    private static final Logger log = LoggerFactory.getLogger(MqttRemotingServerImpl.class);
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup eventLoopGroupSelector;
    private EventLoopGroup eventLoopGroupBoss;
    private MultiNettyServerConfig mqttServerConfig;

    private ExecutorService publicExecutor;
    private MqttChannelEventListener mqttChannelEventListener;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private Class<? extends ServerSocketChannel> socketChannelClass;

    private int port = 1883;
    private InterceptorGroup interceptorGroup;

    private static final String HANDSHAKE_HANDLER_NAME = "handshakeHandler";
    private static final String TLS_HANDLER_NAME = "sslHandler";
    private static final String FILE_REGION_ENCODER_NAME = "fileRegionEncoder";

    public MqttRemotingServerImpl() {
        super();
    }

    public MqttRemotingServerImpl(final MultiNettyServerConfig mqttServerConfig) {
        this(mqttServerConfig, null);
    }

    public MqttRemotingServerImpl(final MultiNettyServerConfig mqttServerConfig,
                                  final MqttChannelEventListener channelEventListener) {
        init(mqttServerConfig, channelEventListener);
    }

    @Override
    public MqttRemotingServer init(MultiNettyServerConfig mqttServerConfig,
        MqttChannelEventListener mqttChannelEventListener) {
        this.mqttServerConfig = mqttServerConfig;
        super.init(mqttServerConfig.getMqttServerOnewaySemaphoreValue());
        this.serverBootstrap = new ServerBootstrap();
        this.mqttChannelEventListener = mqttChannelEventListener;

        int publicThreadNums = mqttServerConfig.getServerCallbackExecutorThreads();
        if (publicThreadNums <= 0) {
            publicThreadNums = 4;
        }
        this.publicExecutor = ThreadUtils.newFixedThreadPool(
            publicThreadNums,
            10000, "MqttRemoting-PublicExecutor", true);
        if (JvmUtils.isUseEpoll() && this.mqttServerConfig.isUseEpollNativeSelector()) {
            this.eventLoopGroupSelector = new EpollEventLoopGroup(
                mqttServerConfig.getServerSelectorThreads(),
                ThreadUtils.newGenericThreadFactory("MqttNettyEpollIoThreads",
                    mqttServerConfig.getServerSelectorThreads()));
            this.eventLoopGroupBoss = new EpollEventLoopGroup(1, ThreadUtils.newGenericThreadFactory("MqttNettyBossThreads"));
            this.socketChannelClass = EpollServerSocketChannel.class;
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(1, ThreadUtils.newGenericThreadFactory("MqttNettyBossThreads"));
            this.eventLoopGroupSelector = new NioEventLoopGroup(
                mqttServerConfig.getServerSelectorThreads(),
                ThreadUtils.newGenericThreadFactory("MqttNettyNioIoThreads",
                    mqttServerConfig.getServerSelectorThreads()));
            this.socketChannelClass = NioServerSocketChannel.class;
        }
        this.port = mqttServerConfig.getMqttListenPort();
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
            mqttServerConfig.getServerWorkerThreads(),
            ThreadUtils.newGenericThreadFactory("MqttNettyWorkerThreads",
                mqttServerConfig.getServerWorkerThreads()));
        loadSslContext();
        return this;
    }

    public void loadSslContext() {
        TlsMode tlsMode = TlsSystemConfig.tlsMode;
        log.info("Server is running in TLS {} mode", tlsMode.getName());
        if (tlsMode != TlsMode.DISABLED) {
            try {
                sslContext = TlsHelper.buildSslContext(false);
                log.info("SSLContext created for server");
            } catch (CertificateException e) {
                log.error("Failed to create SSLContext for server", e);
            } catch (IOException e) {
                log.error("Failed to create SSLContext for server", e);
            }
        }
    }

    @Override
    public void start() {
        super.start();
        ServerBootstrap childHandler =
            this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                .channel(socketChannelClass)
                .option(ChannelOption.SO_BACKLOG, 102400)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF,
                    mqttServerConfig.getServerSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF,
                    mqttServerConfig.getServerSocketRcvBufSize())
                .localAddress(new InetSocketAddress(this.port))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                            .addLast(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME,
                                new HandshakeHandler(TlsSystemConfig.tlsMode))
                            .addLast(defaultEventExecutorGroup,
                                new MqttDecoder(mqttServerConfig.getMaxBytesInMqttMessage()),
                                MqttEncoder.INSTANCE,
                                new IdleStateHandler(mqttServerConfig.getMqttConnectionChannelReaderIdleSeconds(), mqttServerConfig.getMqttConnectionChannelWriterIdleSeconds(), mqttServerConfig.getMqttServerChannelMaxIdleTimeSeconds()),
                                new NettyConnectManageHandler(),
                                new IotServerHandler(false)

                            );
                    }
                });

        if (mqttServerConfig.isServerPooledByteBufAllocatorEnable()) {
            childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        }

        try {
            ChannelFuture sync = this.serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            this.port = addr.getPort();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException",
                e1);
        }
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            if (this.eventLoopGroupBoss != null) {
                this.eventLoopGroupBoss.shutdownGracefully();
            }
            if (this.eventLoopGroupSelector != null) {
                this.eventLoopGroupSelector.shutdownGracefully();
            }
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingServer shutdown exception, ", e);
        }
        if (this.publicExecutor != null) {
            try {
                this.publicExecutor.shutdown();
            } catch (Exception e) {
                log.error("NettyRemotingServer shutdown exception, ", e);
            }
        }
    }

    @Override public void registerRPCHook(RPCHook rpcHook) {

    }

    @Override
    public void registerProcessor(MqttMessageType mqttMessageType, MqttRequestProcessor processor,
        ExecutorService executor) {
        executor = executor == null ? this.publicExecutor : executor;
        registerIotProcessor(mqttMessageType, processor, executor);
    }

    @Override
    public void registerDefaultProcessor(MqttRequestProcessor processor, ExecutorService executor) {
        this.defaultRequestProcessor = new Pair<>(processor, executor);
    }

    @Override
    public int localListenPort() {
        return this.port;
    }

    @Override
    public Pair<MqttRequestProcessor, ExecutorService> getProcessorPair(MqttMessageType messageType) {
        return processorTable.get(messageType);
    }

    @Override
    public void invokeOneway(RemotingChannel remotingChannel, MqttMessage request,
        long timeoutMillis) throws InterruptedException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        this.invokeOnewayImpl(((NettyChannelImpl) remotingChannel).getChannel(), request,
            timeoutMillis);
    }

    @Override
    public MqttChannelEventListener getMqttChannelEventListener() {
        return this.mqttChannelEventListener;
    }

    @Override
    public InterceptorGroup getInterceptorGroup() {
        return this.interceptorGroup;
    }

    @Override
    protected RemotingChannel getAndCreateChannel(String addr, long timeout)
        throws InterruptedException {
        return null;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return this.publicExecutor;
    }

    class HandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

        private final TlsMode tlsMode;

        private static final byte HANDSHAKE_MAGIC_CODE = 0x16;

        HandshakeHandler(TlsMode tlsMode) {
            this.tlsMode = tlsMode;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {

            // mark the current position so that we can peek the first byte to determine if the content is starting with
            // TLS handshake
            msg.markReaderIndex();

            byte b = msg.getByte(0);

            if (b == HANDSHAKE_MAGIC_CODE) {
                switch (tlsMode) {
                    case DISABLED:
                        ctx.close();
                        log.warn(
                            "Clients intend to establish a SSL connection while this server is running in SSL disabled mode");
                        break;
                    case PERMISSIVE:
                    case ENFORCING:
                        if (null != sslContext) {
                            ctx.pipeline()
                                .addAfter(defaultEventExecutorGroup, HANDSHAKE_HANDLER_NAME,
                                    TLS_HANDLER_NAME,
                                    sslContext.newHandler(ctx.channel().alloc()))
                                .addAfter(defaultEventExecutorGroup, TLS_HANDLER_NAME,
                                    FILE_REGION_ENCODER_NAME, new FileRegionEncoder());
                            log.info(
                                "Handlers prepended to channel pipeline to establish SSL connection");
                        } else {
                            ctx.close();
                            log.error(
                                "Trying to establish a SSL connection but sslContext is null");
                        }
                        break;

                    default:
                        log.warn("Unknown TLS mode");
                        break;
                }
            } else if (tlsMode == TlsMode.ENFORCING) {
                ctx.close();
                log.warn(
                    "Clients intend to establish an insecure connection while this server is running in SSL enforcing mode");
            }

            // reset the reader index so that handshake negotiation may proceed as normal.
            msg.resetReaderIndex();

            try {
                // Remove this handler
                ctx.pipeline().remove(this);
            } catch (NoSuchElementException e) {
                log.error("Error while removing HandshakeHandler", e);
            }

            // Hand over this message to the next .
            ctx.fireChannelRead(msg.retain());
        }
    }

    @Override
    public void push(RemotingChannel remotingChannel, MqttMessage request,
                     long timeoutMillis) throws InterruptedException,
        RemotingTooMuchRequestException, RemotingTimeoutException, RemotingSendRequestException {
        this.invokeOneway(remotingChannel, request, timeoutMillis);
    }
}
