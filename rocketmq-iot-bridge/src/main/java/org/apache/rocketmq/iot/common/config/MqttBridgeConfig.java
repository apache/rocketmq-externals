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

package org.apache.rocketmq.iot.common.config;

import org.apache.rocketmq.iot.protocol.mqtt.constant.MqttConstant;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.rocketmq.iot.common.configuration.MQTTBridgeConfiguration.*;

public class MqttBridgeConfig {
    private Properties properties;

    private String brokerHost;
    private int brokerPort;
    private int httpPort;
    private List<String> httpClusterHostList;
    private int bossGroupThreadNum;
    private int workerGroupThreadNum;
    private int socketBacklogSize;
    private long heartbeatAllidleTime;

    private boolean enableRocketMQStore;
    private String rmqNamesrvAddr;
    private String rmqProductGroup;
    private String rmqConsumerGroup;
    private int rmqConsumerPullNums;
    private String rmqAccessKey;
    private String rmqSecretKey;

    public MqttBridgeConfig() {
        initConfig();
    }

    public MqttBridgeConfig(Properties properties) {
        this.properties = properties;
    }

    public void initConfig() {
        this.brokerHost = System.getProperty(MQTT_BROKER_HOST, MQTT_BROKER_HOST_DEFAULT);
        this.brokerPort = Integer.parseInt(System.getProperty(MQTT_BROKER_PORT, MQTT_BROKER_PORT_DEFAULT));
        this.httpPort = Integer.parseInt(System.getProperty(MQTT_HTTP_PORT, MQTT_HTTP_PORT_DEFAULT));
        this.httpClusterHostList = Arrays.asList(System.getProperty(MQTT_HTTP_CLUSTER_HOST_LIST, MQTT_HTTP_CLUSTER_HOST_LIST_DEFAULT)
                .split(MqttConstant.HTTP_ADDRESS_SEPARATOR));

        this.bossGroupThreadNum = Integer.parseInt(System.getProperty(MQTT_SERVER_BOSS_GROUP_THREAD_NUM,
            MQTT_SERVER_BOSS_GROUP_THREAD_NUM_DEFAULT));
        this.workerGroupThreadNum = Integer.parseInt(System.getProperty(MQTT_SERVER_WORKER_GROUP_THREAD_NUM,
            MQTT_SERVER_WORKER_GROUP_THREAD_NUM_DEFAULT));
        this.socketBacklogSize = Integer.parseInt(System.getProperty(MQTT_SERVER_SOCKET_BACKLOG_SIZE,
            MQTT_SERVER_SOCKET_BACKLOG_SIZE_DEFAULT));
        this.heartbeatAllidleTime = Long.parseLong(System.getProperty(MQTT_BROKER_HEARTBEAT_ALLIDLETIME,
                MQTT_BROKER_HEARTBEAT_ALLIDLETIME_DEFAULT));

        this.enableRocketMQStore =  Boolean.parseBoolean(System.getProperty(MQTT_ROCKETMQ_STORE_ENABLED, MQTT_ROCKETMQ_STORE_ENABLED_DEFAULT));
        if (enableRocketMQStore) {
            this.rmqNamesrvAddr = System.getProperty(MQTT_ROCKETMQ_NAMESRVADDR, MQTT_ROCKETMQ_NAMESRVADDR_DEFAULT);
            this.rmqProductGroup = System.getProperty(MQTT_ROCKETMQ_PRODUCER_GROUP, MQTT_ROCKETMQ_PRODUCER_GROUP_DEFAULT);
            this.rmqConsumerGroup = System.getProperty(MQTT_ROCKETMQ_CONSUMER_GROUP, MQTT_ROCKETMQ_CONSUMER_GROUP_DEFAULT);
            this.rmqConsumerPullNums = Integer.parseInt(System.getProperty(MQTT_ROKECTMQ_CONSUMER_PULL_NUMS,
                    MQTT_ROKECTMQ_CONSUMER_PULL_NUMS_DEFAULT));

            this.rmqAccessKey = System.getProperty(MQTT_ROCKETMQ_ACCESSKEY, MQTT_ROCKETMQ_ACCESSKEY_DEFAULT);
            this.rmqSecretKey = System.getProperty(MQTT_ROCKETMQ_SECRETKEY, MQTT_ROCKETMQ_SECRETKEY_DEFAULT);
        }

    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public List<String> getHttpClusterHostList() {
        return httpClusterHostList;
    }

    public int getBossGroupThreadNum() {
        return bossGroupThreadNum;
    }

    public int getWorkerGroupThreadNum() {
        return workerGroupThreadNum;
    }

    public int getSocketBacklogSize() {
        return socketBacklogSize;
    }

    public long getHeartbeatAllidleTime() {
        return heartbeatAllidleTime;
    }

    public boolean isEnableRocketMQStore() {
        return enableRocketMQStore;
    }

    public String getRmqAccessKey() {
        return rmqAccessKey;
    }

    public String getRmqSecretKey() {
        return rmqSecretKey;
    }

    public String getRmqNamesrvAddr() {
        return rmqNamesrvAddr;
    }

    public String getRmqProductGroup() {
        return rmqProductGroup;
    }

    public String getRmqConsumerGroup() {
        return rmqConsumerGroup;
    }

    public int getRmqConsumerPullNums() {
        return rmqConsumerPullNums;
    }

    @Override public String toString() {
        return "MqttBridgeConfig{" +
            "brokerHost='" + brokerHost + '\'' +
            ", brokerPort=" + brokerPort +
            ", httpPort=" + httpPort +
            ", httpClusterHostList='" + httpClusterHostList + '\'' +
            ", bossGroupThreadNum=" + bossGroupThreadNum +
            ", workerGroupThreadNum=" + workerGroupThreadNum +
            ", socketBacklogSize=" + socketBacklogSize +
            ", heartbeatAllidleTime=" + heartbeatAllidleTime +
            ", enableRocketMQStore=" + enableRocketMQStore +
            ", rmqNamesrvAddr='" + rmqNamesrvAddr + '\'' +
            ", rmqProductGroup='" + rmqProductGroup + '\'' +
            ", rmqConsumerGroup='" + rmqConsumerGroup + '\'' +
            ", rmqConsumerPullNums='" + rmqConsumerPullNums + '\'' +
            ", rmqAccessKey='" + rmqAccessKey + '\'' +
            ", rmqSecretKey='" + rmqSecretKey + '\'' +
            '}';
    }
}