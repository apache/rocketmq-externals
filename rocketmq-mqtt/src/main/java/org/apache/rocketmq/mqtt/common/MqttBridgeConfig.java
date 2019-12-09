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
package org.apache.rocketmq.mqtt.common;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.annotation.ImportantField;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.persistence.redis.RedisConfig;
import org.apache.rocketmq.remoting.common.RemotingUtil;
import org.apache.rocketmq.mqtt.config.MultiNettyServerConfig;
import org.apache.rocketmq.remoting.netty.NettyClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqttBridgeConfig {

    private static final Logger log = LoggerFactory.getLogger(MqttBridgeConfig.class);

    @ImportantField
    private String namesrvAddr = System.getProperty(MixAll.NAMESRV_ADDR_PROPERTY, System.getenv(MixAll.NAMESRV_ADDR_ENV));

    private String rocketmqHome = System.getProperty(MixAll.ROCKETMQ_HOME_PROPERTY, System.getenv(MixAll.ROCKETMQ_HOME_ENV));

    @ImportantField
    private String mqttBridgeIP = RemotingUtil.getLocalAddress();

    /**
     * broker clusterName when use broker as persist backend
     */
    private String clusterName = "defaultCluster";

    private NettyClientConfig mqttClientConfig;

    private MultiNettyServerConfig multiNettyServerConfig;

    private RedisConfig redisConfig;

    private int maxInflight = 10000;
    /**
     * Internal transfer messages' num in flight
     */
    private int internalMaxInFlight = 10;

    @ImportantField
    private String mqttBridgeName = localHostName();

    private int handleMqttThreadPoolQueueCapacity = 200000;

    private int handleMqttConnectDisconnectMessagePoolSize = 1;

    private int handleMqttPubackMessageMinPoolSize = 10;

    private int handleMqttPubackMessageMaxPoolSize = Runtime.getRuntime().availableProcessors() + 1;

    private int handleMqttSubUnsubMessagePoolSize = 1;

    private int handleMqttPingreqPoolSize = 1;

    private int handleMqttPublishMessageMinPoolSize = 10;

    private int handleMqttPublishMessageMaxPoolSize = Runtime.getRuntime().availableProcessors() + 1;

    private int pushMqttMessageMinPoolSize = 10;

    private int pushMqttMessageMaxPoolSize = Runtime.getRuntime().availableProcessors() + 1;

    private int pushMqttMessageThreadPoolQueueCapacity = 200000;

    /**
     * Acl feature switch
     */
    @ImportantField
    private boolean aclEnable = false;

    @ImportantField
    private boolean enableMeasurement = false;

    /**
     * 定时采集当前的连接数和订阅数
     */
    @ImportantField
    private long measureInterval = 60 * 1000;

    private long houseKeepingInterval = 10 * 1000;

    private long persistOffsetInterval = 10 * 1000;

    private long scanAckTimeoutInterval = 1000;

    private long mqttHeartBeatInterval = 30 * 1000;

    @ImportantField
    private boolean fetchNamesrvAddrByAddressServer = false;

    /**
     * This configurable item defines interval of mqtt node registration to nameserver. Allowing values are between 10,
     * 000 and 60, 000 milliseconds.
     */
    private int registerNameServerPeriod = 1000 * 30;

    private int updateEnodeClusterInfoPeriod = 1000 * 30;

    /**
     * 消息超时重发间隔
     */
    private int msgFlyTimeBeforeResend = 10000;

    @ImportantField
    private String persistServiceType;

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getRocketmqHome() {
        return rocketmqHome;
    }

    public void setRocketmqHome(String rocketmqHome) {
        this.rocketmqHome = rocketmqHome;
    }

    public String getMqttBridgeIP() {
        return mqttBridgeIP;
    }

    public void setMqttBridgeIP(String mqttBridgeIP) {
        this.mqttBridgeIP = mqttBridgeIP;
    }

    public boolean isAclEnable() {
        return aclEnable;
    }

    public void setAclEnable(boolean aclEnable) {
        this.aclEnable = aclEnable;
    }

    public MultiNettyServerConfig getMultiNettyServerConfig() {
        return multiNettyServerConfig;
    }

    public void setMultiNettyServerConfig(MultiNettyServerConfig multiNettyServerConfig) {
        this.multiNettyServerConfig = multiNettyServerConfig;
    }

    public NettyClientConfig getMqttClientConfig() {
        return mqttClientConfig;
    }

    public void setMqttClientConfig(NettyClientConfig mqttClientConfig) {
        this.mqttClientConfig = mqttClientConfig;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public int getHandleMqttThreadPoolQueueCapacity() {
        return handleMqttThreadPoolQueueCapacity;
    }

    public void setHandleMqttThreadPoolQueueCapacity(int handleMqttThreadPoolQueueCapacity) {
        this.handleMqttThreadPoolQueueCapacity = handleMqttThreadPoolQueueCapacity;
    }

    public int getHandleMqttConnectDisconnectMessagePoolSize() {
        return handleMqttConnectDisconnectMessagePoolSize;
    }

    public void setHandleMqttConnectDisconnectMessagePoolSize(int handleMqttConnectDisconnectMessagePoolSize) {
        this.handleMqttConnectDisconnectMessagePoolSize = handleMqttConnectDisconnectMessagePoolSize;
    }

    public int getHandleMqttPubackMessageMinPoolSize() {
        return handleMqttPubackMessageMinPoolSize;
    }

    public void setHandleMqttPubackMessageMinPoolSize(int handleMqttPubackMessageMinPoolSize) {
        this.handleMqttPubackMessageMinPoolSize = handleMqttPubackMessageMinPoolSize;
    }

    public int getHandleMqttPubackMessageMaxPoolSize() {
        return handleMqttPubackMessageMaxPoolSize;
    }

    public void setHandleMqttPubackMessageMaxPoolSize(int handleMqttPubackMessageMaxPoolSize) {
        this.handleMqttPubackMessageMaxPoolSize = handleMqttPubackMessageMaxPoolSize;
    }

    public int getHandleMqttSubUnsubMessagePoolSize() {
        return handleMqttSubUnsubMessagePoolSize;
    }

    public void setHandleMqttSubUnsubMessagePoolSize(int handleMqttSubUnsubMessagePoolSize) {
        this.handleMqttSubUnsubMessagePoolSize = handleMqttSubUnsubMessagePoolSize;
    }

    public int getHandleMqttPingreqPoolSize() {
        return handleMqttPingreqPoolSize;
    }

    public void setHandleMqttPingreqPoolSize(int handleMqttPingreqPoolSize) {
        this.handleMqttPingreqPoolSize = handleMqttPingreqPoolSize;
    }

    public int getHandleMqttPublishMessageMinPoolSize() {
        return handleMqttPublishMessageMinPoolSize;
    }

    public void setHandleMqttPublishMessageMinPoolSize(int handleMqttPublishMessageMinPoolSize) {
        this.handleMqttPublishMessageMinPoolSize = handleMqttPublishMessageMinPoolSize;
    }

    public int getHandleMqttPublishMessageMaxPoolSize() {
        return handleMqttPublishMessageMaxPoolSize;
    }

    public void setHandleMqttPublishMessageMaxPoolSize(int handleMqttPublishMessageMaxPoolSize) {
        this.handleMqttPublishMessageMaxPoolSize = handleMqttPublishMessageMaxPoolSize;
    }

    public int getPushMqttMessageMinPoolSize() {
        return pushMqttMessageMinPoolSize;
    }

    public void setPushMqttMessageMinPoolSize(int pushMqttMessageMinPoolSize) {
        this.pushMqttMessageMinPoolSize = pushMqttMessageMinPoolSize;
    }

    public int getPushMqttMessageMaxPoolSize() {
        return pushMqttMessageMaxPoolSize;
    }

    public void setPushMqttMessageMaxPoolSize(int pushMqttMessageMaxPoolSize) {
        this.pushMqttMessageMaxPoolSize = pushMqttMessageMaxPoolSize;
    }

    public int getPushMqttMessageThreadPoolQueueCapacity() {
        return pushMqttMessageThreadPoolQueueCapacity;
    }

    public void setPushMqttMessageThreadPoolQueueCapacity(int pushMqttMessageThreadPoolQueueCapacity) {
        this.pushMqttMessageThreadPoolQueueCapacity = pushMqttMessageThreadPoolQueueCapacity;
    }

    public long getHouseKeepingInterval() {
        return houseKeepingInterval;
    }

    public void setHouseKeepingInterval(long houseKeepingInterval) {
        this.houseKeepingInterval = houseKeepingInterval;
    }

    public long getPersistOffsetInterval() {
        return persistOffsetInterval;
    }

    public void setPersistOffsetInterval(long persistOffsetInterval) {
        this.persistOffsetInterval = persistOffsetInterval;
    }

    public long getScanAckTimeoutInterval() {
        return scanAckTimeoutInterval;
    }

    public void setScanAckTimeoutInterval(long scanAckTimeoutInterval) {
        this.scanAckTimeoutInterval = scanAckTimeoutInterval;
    }

    public long getMqttHeartBeatInterval() {
        return mqttHeartBeatInterval;
    }

    public void setMqttHeartBeatInterval(long mqttHeartBeatInterval) {
        this.mqttHeartBeatInterval = mqttHeartBeatInterval;
    }

    public boolean isFetchNamesrvAddrByAddressServer() {
        return fetchNamesrvAddrByAddressServer;
    }

    public void setFetchNamesrvAddrByAddressServer(boolean fetchNamesrvAddrByAddressServer) {
        this.fetchNamesrvAddrByAddressServer = fetchNamesrvAddrByAddressServer;
    }

    public int getRegisterNameServerPeriod() {
        return registerNameServerPeriod;
    }

    public void setRegisterNameServerPeriod(int registerNameServerPeriod) {
        this.registerNameServerPeriod = registerNameServerPeriod;
    }

    public int getUpdateEnodeClusterInfoPeriod() {
        return updateEnodeClusterInfoPeriod;
    }

    public void setUpdateEnodeClusterInfoPeriod(int updateEnodeClusterInfoPeriod) {
        this.updateEnodeClusterInfoPeriod = updateEnodeClusterInfoPeriod;
    }

    public int getMsgFlyTimeBeforeResend() {
        return msgFlyTimeBeforeResend;
    }

    public void setMsgFlyTimeBeforeResend(int msgFlyTimeBeforeResend) {
        this.msgFlyTimeBeforeResend = msgFlyTimeBeforeResend;
    }

    public static String localHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("Failed to obtain the host name", e);
        }

        return "DEFAULT_MQTTBRIDGE";
    }

    public String getMqttBridgeName() {
        return mqttBridgeName;
    }

    public void setMqttBridgeName(String mqttBridgeName) {
        this.mqttBridgeName = mqttBridgeName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getPersistServiceType() {
        return persistServiceType;
    }

    public void setPersistServiceType(String persistServiceType) {
        this.persistServiceType = persistServiceType;
    }

    public int getInternalMaxInFlight() {
        return internalMaxInFlight;
    }

    public void setInternalMaxInFlight(int internalMaxInFlight) {
        this.internalMaxInFlight = internalMaxInFlight;
    }

    public int getMaxInflight() {
        return maxInflight;
    }

    public void setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
    }

    public long getMeasureInterval() {
        return measureInterval;
    }

    public void setMeasureInterval(long measureInterval) {
        this.measureInterval = measureInterval;
    }
}
