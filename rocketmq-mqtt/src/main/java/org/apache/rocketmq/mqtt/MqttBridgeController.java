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

import io.netty.handler.codec.mqtt.MqttMessageType;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.mqtt.client.MqttClientHousekeepingService;
import org.apache.rocketmq.mqtt.client.MqttClientManager;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.mqtt.interceptor.InterceptorGroup;
import org.apache.rocketmq.mqtt.persistence.PersistServiceFactory;
import org.apache.rocketmq.mqtt.persistence.service.PersistService;
import org.apache.rocketmq.mqtt.processor.*;
import org.apache.rocketmq.mqtt.service.*;
import org.apache.rocketmq.mqtt.service.impl.*;
import org.apache.rocketmq.mqtt.utils.ThreadUtils;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.RemotingServer;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.apache.rocketmq.remoting.netty.NettyRemotingClient;
import org.apache.rocketmq.remoting.netty.NettyRemotingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MqttBridgeController {

    private static final Logger log = LoggerFactory.getLogger(MqttBridgeController.class);
    private final MultiNettyServerConfig multiNettyServerConfig;
    private MqttBridgeConfig mqttBridgeConfig;
    private NettyClientConfig mqttClientConfig;
    private RemotingClient mqttRemotingClient;
    private MqttRemotingServer mqttRemotingServer;
    private MqttRemotingServer innerMqttRemotingServer;
    private RemotingServer remotingServer;
    private ExecutorService handleMqttConnectDisConnectMessageExecutor;
    private ExecutorService handleMqttPublishMessageExecutor;
    private ExecutorService handleMqttPubackMessageExecutor;
    private ExecutorService handleMqttSubUnsubMessageExecutor;
    private ExecutorService handleMqttPingreqMessageExecutor;
    private EnodeService enodeService;
    private NnodeService nnodeService;
    private BridgeService bridgeService;
    private ScheduledService scheduledService;
    private PersistService persistService;
    private WillMessageService willMessageService;
    private MqttClientManager mqttClientManager;
    private MqttClientHousekeepingService mqttClientHousekeepingService;
    private MqttConnectMessageProcessor mqttConnectMessageProcessor;
    private MqttPublishMessageProcessor mqttPublishMessageProcessor;
    private MqttPubackMessageProcessor mqttPubackMessageProcessor;
    private MqttSubscribeMessageProcessor mqttSubscribeMessageProcessor;
    private MqttUnsubscribeMessageProcessor mqttUnsubscribeMessageProcessor;
    private MqttDisconnectMessageProcessor mqttDisconnectMessageProcessor;
    private MqttPingreqMessageProcessor mqttPingreqMessageProcessor;
    private InnerMqttMessageProcessor innerMqttMessageProcessor;
    private InterceptorGroup remotingServerInterceptorGroup;
    private ExecutorService remotingCommandRequestExecutor;
    private ExecutorService qos0MessagePushExecutor;
    private ExecutorService qos1MessagePushExecutor;
    private ExecutorService transferMessageExecutor;
    private ExecutorService retainMessageAndWillMessagePushExecutor;

    public MqttBridgeController(MqttBridgeConfig mqttBridgeConfig) {
        this.multiNettyServerConfig = mqttBridgeConfig.getMultiNettyServerConfig();
        this.mqttClientConfig = mqttBridgeConfig.getMqttClientConfig();
        this.mqttBridgeConfig = mqttBridgeConfig;
        this.enodeService = new RemoteEnodeServiceImpl(this);
        this.nnodeService = new NnodeServiceImpl(this);
        this.bridgeService = new BridgeServiceImpl(this);
        this.scheduledService = new ScheduledServiceImpl(this);
        this.mqttRemotingClient = new NettyRemotingClient(this.mqttClientConfig);
        this.mqttRemotingServer = MqttServerFactory.getInstance().createMqttRemotingServer();
        this.remotingServer = new NettyRemotingServer(this.multiNettyServerConfig, null);

        this.handleMqttConnectDisConnectMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getHandleMqttConnectDisconnectMessagePoolSize(),
            mqttBridgeConfig.getHandleMqttConnectDisconnectMessagePoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "handleMqttConnectDisconnectMessageThread",
            false);
        this.handleMqttPublishMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getHandleMqttPublishMessageMinPoolSize(),
            mqttBridgeConfig.getHandleMqttPublishMessageMaxPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "handleMqttPublishMessageThread",
            false);
        this.handleMqttPubackMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getHandleMqttPubackMessageMinPoolSize(),
            mqttBridgeConfig.getHandleMqttPubackMessageMaxPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "handleMqttPubackMessageThread",
            false);
        this.handleMqttSubUnsubMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getHandleMqttSubUnsubMessagePoolSize(),
            mqttBridgeConfig.getHandleMqttSubUnsubMessagePoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "handleMqttSubUnsubMessageThread",
            false);
        this.handleMqttPingreqMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getHandleMqttPingreqPoolSize(),
            mqttBridgeConfig.getHandleMqttPingreqPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "handleMqttPingreqMessageThread",
            false);
        this.qos0MessagePushExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getPushMqttMessageMinPoolSize(),
            mqttBridgeConfig.getPushMqttMessageMaxPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "qos0MessagePushThread",
            false);
        this.qos1MessagePushExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getPushMqttMessageMinPoolSize(),
            mqttBridgeConfig.getPushMqttMessageMaxPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "qos1MessagePushThread",
            false);
        this.transferMessageExecutor = ThreadUtils.newThreadPoolExecutor(
            mqttBridgeConfig.getPushMqttMessageMinPoolSize(),
            mqttBridgeConfig.getPushMqttMessageMaxPoolSize(),
            3000,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(mqttBridgeConfig.getHandleMqttThreadPoolQueueCapacity()),
            "transferMessageThreads",
            false);

        this.remotingCommandRequestExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryImpl("remotingCommandRequestThread"));

//        this.qos1MessagePushOrderedExecutor = OrderedExecutor.newBuilder().name("qos1MessageThreads").numThreads(mqttBridgeConfig.getPushMqttMessageMaxPoolSize()).build();
//        this.transferMessageOrderedExecutor = OrderedExecutor.newBuilder().name("transferMessageThreads").numThreads(mqttBridgeConfig.getPushMqttMessageMaxPoolSize()).build();

        this.retainMessageAndWillMessagePushExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("retainMessageAndWillMessagePushThreads"));
        if (this.mqttBridgeConfig.getNamesrvAddr() != null) {
            this.nnodeService.updateNnodeAddressList(this.mqttBridgeConfig.getNamesrvAddr());
            log.info("Set user specified name server address: {}",
                this.mqttBridgeConfig.getNamesrvAddr());
        }

        this.mqttConnectMessageProcessor = new MqttConnectMessageProcessor(this);
        this.mqttPublishMessageProcessor = new MqttPublishMessageProcessor(this);
        this.mqttPubackMessageProcessor = new MqttPubackMessageProcessor(this);
        this.mqttSubscribeMessageProcessor = new MqttSubscribeMessageProcessor(this);
        this.mqttUnsubscribeMessageProcessor = new MqttUnsubscribeMessageProcessor(this);
        this.mqttDisconnectMessageProcessor = new MqttDisconnectMessageProcessor(this);
        this.mqttPingreqMessageProcessor = new MqttPingreqMessageProcessor(this);
        this.innerMqttMessageProcessor = new InnerMqttMessageProcessor(this);
        this.persistService = PersistServiceFactory.getInstance().createPersistService(mqttBridgeConfig.getPersistServiceType());
        if (this.persistService != null) {
            persistService.init(this);
        }
        this.mqttClientManager = new MqttClientManagerImpl(this);
        this.mqttClientHousekeepingService = new MqttClientHousekeepingService(mqttClientManager);
        this.willMessageService = new WillMessageServiceImpl(this);
    }

    public MqttBridgeConfig getMqttBridgeConfig() {
        return mqttBridgeConfig;
    }

    public boolean initialize() throws CloneNotSupportedException {
        this.remotingServerInterceptorGroup = new InterceptorGroup();
        if (this.mqttRemotingServer != null) {
            this.mqttRemotingServer.init(this.multiNettyServerConfig, this.mqttClientHousekeepingService);
        }
        this.innerMqttRemotingServer = MqttServerFactory.getInstance().createMqttRemotingServer();
        MultiNettyServerConfig innerMqttServerConfig = (MultiNettyServerConfig) this.multiNettyServerConfig.clone();
        innerMqttServerConfig.setMqttListenPort(multiNettyServerConfig.getMqttListenPort() - 1);
        if (this.innerMqttRemotingServer != null) {
            this.innerMqttRemotingServer.init(innerMqttServerConfig, this.mqttClientHousekeepingService);
        }
        registerProcessor();
        return true;
    }

    private void registerProcessor() {
        if (mqttRemotingServer != null) {
            this.mqttRemotingServer.registerProcessor(MqttMessageType.CONNECT, mqttConnectMessageProcessor, this.handleMqttConnectDisConnectMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.PUBLISH, mqttPublishMessageProcessor, this.handleMqttPublishMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.PUBACK, mqttPubackMessageProcessor, this.handleMqttPubackMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.SUBSCRIBE, mqttSubscribeMessageProcessor, this.handleMqttSubUnsubMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.UNSUBSCRIBE, mqttUnsubscribeMessageProcessor, this.handleMqttSubUnsubMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.DISCONNECT, mqttDisconnectMessageProcessor, this.handleMqttConnectDisConnectMessageExecutor);
            this.mqttRemotingServer.registerProcessor(MqttMessageType.PINGREQ, mqttPingreqMessageProcessor, this.handleMqttPingreqMessageExecutor);
        }
        if (innerMqttRemotingServer != null) {
            this.innerMqttRemotingServer.registerProcessor(MqttMessageType.CONNECT, mqttConnectMessageProcessor, this.handleMqttConnectDisConnectMessageExecutor);
            this.innerMqttRemotingServer.registerProcessor(MqttMessageType.PUBLISH, innerMqttMessageProcessor, this.handleMqttPublishMessageExecutor);
            this.innerMqttRemotingServer.registerProcessor(MqttMessageType.PINGREQ, mqttPingreqMessageProcessor, this.handleMqttPingreqMessageExecutor);
        }
        if (remotingServer != null) {
            RemotingCommandRequestProcessor remotingCommandRequestProcessor = new RemotingCommandRequestProcessor(this);
            this.remotingServer.registerProcessor(RequestCode.CLOSE_MQTTCLIENT_CONNECTION, remotingCommandRequestProcessor, remotingCommandRequestExecutor);
        }
    }

    public void start() {
        if (mqttRemotingServer != null) {
            this.mqttRemotingServer.start();
        }
        if (innerMqttRemotingServer != null) {
            this.innerMqttRemotingServer.start();
        }
        if (remotingServer != null) {
            this.remotingServer.start();
        }
        if (mqttRemotingClient != null) {
            this.mqttRemotingClient.start();
        }
        this.scheduledService.startScheduleTask();
        this.mqttClientHousekeepingService.start(this.mqttBridgeConfig.getHouseKeepingInterval());
    }

    public void shutdown() {
        this.nnodeService.unregisterMqttNode(mqttBridgeConfig);
        if (this.mqttClientHousekeepingService != null) {
            this.mqttClientHousekeepingService.shutdown();
        }
        if (this.mqttRemotingServer != null) {
            this.mqttRemotingServer.shutdown();
        }
        if (this.innerMqttRemotingServer != null) {
            this.innerMqttRemotingServer.shutdown();
        }
        if (this.mqttRemotingClient != null) {
            this.mqttRemotingClient.shutdown();
        }
        if (this.remotingServer != null) {
            this.remotingServer.shutdown();
        }
        if (this.scheduledService != null) {
            this.scheduledService.shutdown();
        }
        if (this.handleMqttConnectDisConnectMessageExecutor != null) {
            this.handleMqttConnectDisConnectMessageExecutor.shutdown();
        }
        if (this.handleMqttPublishMessageExecutor != null) {
            this.handleMqttPublishMessageExecutor.shutdown();
        }
        if (this.handleMqttPubackMessageExecutor != null) {
            this.handleMqttPubackMessageExecutor.shutdown();
        }
        if (this.handleMqttSubUnsubMessageExecutor != null) {
            this.handleMqttSubUnsubMessageExecutor.shutdown();
        }
        if (this.handleMqttPingreqMessageExecutor != null) {
            this.handleMqttPingreqMessageExecutor.shutdown();
        }
        if (this.qos0MessagePushExecutor != null) {
            this.qos0MessagePushExecutor.shutdown();
        }
//        if (this.qos1MessagePushOrderedExecutor != null) {
//            this.qos1MessagePushOrderedExecutor.shutdown();
//        }
//        if (this.transferMessageOrderedExecutor != null) {
//            this.transferMessageOrderedExecutor.shutdown();
//        }
        if (this.qos1MessagePushExecutor != null) {
            this.qos1MessagePushExecutor.shutdown();
        }
        if (this.transferMessageExecutor != null) {
            this.transferMessageExecutor.shutdown();
        }
        if (this.retainMessageAndWillMessagePushExecutor != null) {
            this.retainMessageAndWillMessagePushExecutor.shutdown();
        }
        if (this.remotingCommandRequestExecutor != null) {
            this.remotingCommandRequestExecutor.shutdown();
        }
    }

    public RemotingServer getRemotingServer() {
        return remotingServer;
    }

    public void setRemotingServer(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    public EnodeService getEnodeService() {
        return enodeService;
    }

    public NnodeService getNnodeService() {
        return nnodeService;
    }

    public BridgeService getBridgeService() {
        return bridgeService;
    }

    public void setBridgeService(BridgeService bridgeService) {
        this.bridgeService = bridgeService;
    }

    public PersistService getPersistService() {
        return persistService;
    }

    public RemotingClient getMqttRemotingClient() {
        return mqttRemotingClient;
    }

    public MqttRemotingServer getMqttRemotingServer() {
        return mqttRemotingServer;
    }

    public MqttRemotingServer getInnerMqttRemotingServer() {
        return innerMqttRemotingServer;
    }

    public void setInnerMqttRemotingServer(MqttRemotingServer innerMqttRemotingServer) {
        this.innerMqttRemotingServer = innerMqttRemotingServer;
    }

    public InterceptorGroup getRemotingServerInterceptorGroup() {
        return remotingServerInterceptorGroup;
    }

    public void setRemotingServerInterceptorGroup(
        InterceptorGroup remotingServerInterceptorGroup) {
        this.remotingServerInterceptorGroup = remotingServerInterceptorGroup;
    }

    public MqttClientManager getMqttClientManager() {
        return mqttClientManager;
    }

    public void setMqttClientManager(MqttClientManager mqttClientManager) {
        this.mqttClientManager = mqttClientManager;
    }

    public ExecutorService getQos0MessagePushExecutor() {
        return qos0MessagePushExecutor;
    }

    public void setQos0MessagePushExecutor(ExecutorService qos0MessagePushExecutor) {
        this.qos0MessagePushExecutor = qos0MessagePushExecutor;
    }

    public ExecutorService getQos1MessagePushExecutor() {
        return qos1MessagePushExecutor;
    }

    public void setQos1MessagePushExecutor(ExecutorService qos1MessagePushExecutor) {
        this.qos1MessagePushExecutor = qos1MessagePushExecutor;
    }

    public ExecutorService getTransferMessageExecutor() {
        return transferMessageExecutor;
    }

    public void setTransferMessageExecutor(ExecutorService transferMessageExecutor) {
        this.transferMessageExecutor = transferMessageExecutor;
    }

    public ExecutorService getRetainMessageAndWillMessagePushExecutor() {
        return retainMessageAndWillMessagePushExecutor;
    }

    public void setRetainMessageAndWillMessagePushExecutor(
        ExecutorService retainMessageAndWillMessagePushExecutor) {
        this.retainMessageAndWillMessagePushExecutor = retainMessageAndWillMessagePushExecutor;
    }

    public WillMessageService getWillMessageService() {
        return willMessageService;
    }

    public MultiNettyServerConfig getMultiNettyServerConfig() {
        return multiNettyServerConfig;
    }

    public MqttPublishMessageProcessor getMqttPublishMessageProcessor() {
        return mqttPublishMessageProcessor;
    }
}
