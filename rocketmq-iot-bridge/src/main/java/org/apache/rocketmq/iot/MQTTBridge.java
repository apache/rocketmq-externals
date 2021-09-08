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

package org.apache.rocketmq.iot;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.apache.rocketmq.iot.common.config.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageDispatcher;
import org.apache.rocketmq.iot.connection.client.impl.ClientManagerImpl;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MqttConnectionHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MqttIdleHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttConnectMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttDisconnectMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttMessageForwarder;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPingreqMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPublishMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttSubscribeMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttUnsubscribeMessagHandler;
import org.apache.rocketmq.iot.rest.HttpRestHandler;
import org.apache.rocketmq.iot.rest.HttpRestHandlerImp;
import org.apache.rocketmq.iot.storage.message.MessageStore;
import org.apache.rocketmq.iot.storage.rocketmq.PublishProducer;
import org.apache.rocketmq.iot.storage.rocketmq.RocketMQPublishProducer;
import org.apache.rocketmq.iot.storage.rocketmq.RocketMQSubscribeConsumer;
import org.apache.rocketmq.iot.storage.rocketmq.SubscribeConsumer;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.iot.storage.subscription.impl.InMemorySubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MQTTBridge {
    private Logger logger = LoggerFactory.getLogger(MQTTBridge.class);

    private MqttBridgeConfig bridgeConfig;

    private ServerBootstrap serverBootstrap;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;

    private MessageDispatcher messageDispatcher;
    private SubscriptionStore subscriptionStore;
    private ClientManager clientManager;
    private MqttConnectionHandler connectionHandler;
    private HttpRestHandler httpRestHandler;
    private MessageStore messageStore;
    private PublishProducer publishProducer;
    private SubscribeConsumer subscribeConsumer;

    public MQTTBridge() {
        init();
    }

    private void  init() {
        this.bridgeConfig = new MqttBridgeConfig();

        subscriptionStore = new InMemorySubscriptionStore();
        if (bridgeConfig.isEnableRocketMQStore()) {
            this.publishProducer = new RocketMQPublishProducer(bridgeConfig);
            this.subscribeConsumer = new RocketMQSubscribeConsumer(bridgeConfig, subscriptionStore);
        }

        clientManager = new ClientManagerImpl();
        messageDispatcher = new MessageDispatcher(clientManager);
        connectionHandler = new MqttConnectionHandler(clientManager, subscriptionStore, subscribeConsumer);
        registerMessageHandlers();
        initHttpRest();
        bossGroup = new NioEventLoopGroup(bridgeConfig.getBossGroupThreadNum());
        workerGroup = new NioEventLoopGroup(bridgeConfig.getWorkerGroupThreadNum());
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .localAddress(bridgeConfig.getBrokerPort())
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, bridgeConfig.getSocketBacklogSize())
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(new IdleStateHandler(0, 0, bridgeConfig.getHeartbeatAllidleTime(), TimeUnit.SECONDS));
                    pipeline.addLast("mqtt-decoder", new MqttDecoder());
                    pipeline.addLast("mqtt-encoder", MqttEncoder.INSTANCE);
                    pipeline.addLast("channel-idle-handler", new MqttIdleHandler());
                    pipeline.addLast("message-dispatcher", messageDispatcher);
                    pipeline.addLast("connection-manager", connectionHandler);
                }
            });

    }

    private void registerMessageHandlers() {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, new MqttConnectMessageHandler(clientManager));
        messageDispatcher.registerHandler(Message.Type.MQTT_DISCONNECT, new MqttDisconnectMessageHandler(clientManager));
        if (bridgeConfig.isEnableRocketMQStore()) {
            messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, new MqttPublishMessageHandler(messageStore, publishProducer));
            // TODO: mqtt cluster inner forwarder, need management of offset and client
        } else {
            messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, new MqttMessageForwarder(subscriptionStore));
        }
        // TODO qos 1/2 PUBLISH
        // TODO qos 1: PUBACK
        // TODO qos 2: PUBREC
        // TODO qos 2: PUBREL
        // TODO qos 2: PUBCOMP
        messageDispatcher.registerHandler(Message.Type.MQTT_PINGREQ, new MqttPingreqMessageHandler());
        messageDispatcher.registerHandler(Message.Type.MQTT_SUBSCRIBE, new MqttSubscribeMessageHandler(subscriptionStore, subscribeConsumer));
        messageDispatcher.registerHandler(Message.Type.MQTT_UNSUBSCRIBE, new MqttUnsubscribeMessagHandler(subscriptionStore, subscribeConsumer));
    }

    private void initHttpRest() {
        this.httpRestHandler = new HttpRestHandlerImp(bridgeConfig, clientManager, subscriptionStore);
        logger.info("init httpRest handler.");
    }

    public void start() {
        logger.info("start the MQTTServer with config " + bridgeConfig);
        try {
            if (bridgeConfig.isEnableRocketMQStore()) {
                publishProducer.start();
                subscribeConsumer.start();
            }
            httpRestHandler.start();
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("fail to start the MQTTServer." + e);
        } finally {
            logger.info("shutdown the MQTTServer");
            shutdown();
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        if (bridgeConfig.isEnableRocketMQStore()) {
            publishProducer.shutdown();
            subscribeConsumer.shutdown();
        }
        httpRestHandler.shutdown();
    }

    public static void main(String [] args) {
        MQTTBridge server = new MQTTBridge();
        server.start();
    }
}
