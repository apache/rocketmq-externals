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
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.data.Message;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.connection.client.impl.ClientManagerImpl;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MessageDispatcher;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MqttConnectionHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.MqttIdleHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttConnectMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttDisconnectMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPingreqMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttPublishMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttSubscribeMessageHandler;
import org.apache.rocketmq.iot.protocol.mqtt.handler.downstream.impl.MqttUnsubscribeMessagHandler;
import org.apache.rocketmq.iot.storage.message.MessageStore;
import org.apache.rocketmq.iot.storage.rocketmq.PublishProducer;
import org.apache.rocketmq.iot.storage.rocketmq.RocketMQPublishProducer;
import org.apache.rocketmq.iot.storage.rocketmq.RocketMQSubscribeConsumer;
import org.apache.rocketmq.iot.storage.rocketmq.SubscribeConsumer;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.apache.rocketmq.iot.storage.subscription.impl.InMemorySubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQTTBridge {
    private Logger logger = LoggerFactory.getLogger(MQTTBridge.class);

    private MqttBridgeConfig bridgeConfig;

    private MessageStore messageStore;
    private SubscriptionStore subscriptionStore;
    private PublishProducer publishProducer;
    private SubscribeConsumer subscribeConsumer;

    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;

    private ClientManager clientManager;
    private MessageDispatcher messageDispatcher;
    private MqttConnectionHandler connectionHandler;

    public MQTTBridge() throws MQClientException {
        this.bridgeConfig = new MqttBridgeConfig();
        initStoreService();
        initMqttHandler();
        initServer();
        logger.info("Mqtt bridge config:" + bridgeConfig);
    }

    private void initStoreService() throws MQClientException {
        this.subscriptionStore = new InMemorySubscriptionStore();
        this.publishProducer = new RocketMQPublishProducer(bridgeConfig);
        this.subscribeConsumer = new RocketMQSubscribeConsumer(bridgeConfig, subscriptionStore);
        logger.info("init subscription store and rocketMQ service.");
    }

    private void initMqttHandler() {
        this.clientManager = new ClientManagerImpl();
        this.messageDispatcher = new MessageDispatcher(clientManager);
        this.connectionHandler = new MqttConnectionHandler(clientManager, subscriptionStore);
        registerMessageHandlers();
        logger.info("init client manager and mqtt handler.");
    }

    private void registerMessageHandlers() {
        messageDispatcher.registerHandler(Message.Type.MQTT_CONNECT, new MqttConnectMessageHandler(clientManager));
        messageDispatcher.registerHandler(Message.Type.MQTT_DISCONNECT, new MqttDisconnectMessageHandler(clientManager));
        // TODO: mqtt cluster inner forwarder
        // messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, new MqttMessageForwarder(subscriptionStore));
        messageDispatcher.registerHandler(Message.Type.MQTT_PUBLISH, new MqttPublishMessageHandler(messageStore, publishProducer));
        // TODO qos 1/2 PUBLISH
        // TODO qos 1: PUBACK
        // TODO qos 2: PUBREC
        // TODO qos 2: PUBREL
        // TODO qos 2: PUBCOMP
        messageDispatcher.registerHandler(Message.Type.MQTT_PINGREQ, new MqttPingreqMessageHandler());
        messageDispatcher.registerHandler(Message.Type.MQTT_SUBSCRIBE, new MqttSubscribeMessageHandler(subscriptionStore, subscribeConsumer));
        messageDispatcher.registerHandler(Message.Type.MQTT_UNSUBSCRIBE, new MqttUnsubscribeMessagHandler(subscriptionStore, subscribeConsumer));
    }

    private void initServer() {
        this.bossGroup = new NioEventLoopGroup(bridgeConfig.getBossGroupThreadNum());
        this.workerGroup = new NioEventLoopGroup(bridgeConfig.getWorkerGroupThreadNum());
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .localAddress(bridgeConfig.getBrokerPort())
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.SO_BACKLOG, bridgeConfig.getSocketBacklogSize())
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    //pipeline.addLast(new IdleStateHandler(5, 0, 5, TimeUnit.SECONDS));
                    pipeline.addLast("connection-manager", connectionHandler);
                    pipeline.addLast("mqtt-decoder", new MqttDecoder());
                    pipeline.addLast("mqtt-encoder", MqttEncoder.INSTANCE);
                    pipeline.addLast("channel-idle-handler", new MqttIdleHandler());
                    pipeline.addLast("message-dispatcher", messageDispatcher);
                }
            });
    }

    public void start() {
        try {
            publishProducer.start();
            ChannelFuture channelFuture = serverBootstrap.bind().sync();
            channelFuture.channel().closeFuture().sync();
            logger.info("start the MQTTServer success.");
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
        publishProducer.shutdown();
    }

    public static void main(String[] args) throws Exception {
        MQTTBridge server = new MQTTBridge();
        server.start();
    }
}
