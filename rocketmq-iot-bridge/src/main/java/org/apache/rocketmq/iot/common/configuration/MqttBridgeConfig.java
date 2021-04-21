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

package org.apache.rocketmq.iot.common.configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BRIDGE_PASSWORD;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BRIDGE_PASSWORD_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BRIDGE_USERNAME;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BRIDGE_USERNAME_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BROKER_HOST;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BROKER_HOST_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BROKER_PORT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_BROKER_PORT_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_HTTP_CLUSTER_HOST_LIST;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_HTTP_CLUSTER_HOST_LIST_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_HTTP_PORT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_HTTP_PORT_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_ACCESSKEY;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_ACCESSKEY_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_CONSUMER_GROUP;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_CONSUMER_GROUP_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_CONSUMER_PULL_NUMS;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_CONSUMER_PULL_NUMS_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_NAMESRVADDR;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_NAMESRVADDR_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_PRODUCER_GROUP;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_PRODUCER_GROUP_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_SECRETKEY;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_ROCKETMQ_SECRETKEY_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_BOSS_GROUP_THREAD_NUM;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_BOSS_GROUP_THREAD_NUM_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_SOCKET_BACKLOG_SIZE;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_SOCKET_BACKLOG_SIZE_DEFAULT;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_WORKER_GROUP_THREAD_NUM;
import static org.apache.rocketmq.iot.common.configuration.MqttBridgeConfigKey.MQTT_SERVER_WORKER_GROUP_THREAD_NUM_DEFAULT;
import static org.apache.rocketmq.iot.common.constant.MqttConstant.HTTP_ADDRESS_SEPARATOR;

public class MqttBridgeConfig {
    private Properties properties;

    private String bridgeUsername;
    private String bridgePassword;

    private String brokerHost;
    private int brokerPort;
    private int bossGroupThreadNum;
    private int workerGroupThreadNum;
    private int socketBacklogSize;

    private String rmqAccessKey;
    private String rmqSecretKey;
    private String rmqNamesrvAddr;
    private String rmqProductGroup;
    private String rmqConsumerGroup;
    private int rmqConsumerPullNums;

    private int httpPort;
    private List<String> httpClusterHostList;

    public MqttBridgeConfig() {
        initSystemConfig();
    }

    public MqttBridgeConfig(Properties properties) {
        this.properties = properties;
        this.initPropertiesConfig();
    }

    public void initSystemConfig() {
        this.bridgeUsername = System.getProperty(MQTT_BRIDGE_USERNAME, MQTT_BRIDGE_USERNAME_DEFAULT);
        this.bridgePassword = System.getProperty(MQTT_BRIDGE_PASSWORD, MQTT_BRIDGE_PASSWORD_DEFAULT);

        this.brokerHost = System.getProperty(MQTT_BROKER_HOST, MQTT_BROKER_HOST_DEFAULT);
        this.brokerPort = Integer.parseInt(System.getProperty(MQTT_BROKER_PORT, MQTT_BROKER_PORT_DEFAULT));

        this.bossGroupThreadNum = Integer.parseInt(System.getProperty(MQTT_SERVER_BOSS_GROUP_THREAD_NUM,
            MQTT_SERVER_BOSS_GROUP_THREAD_NUM_DEFAULT));
        this.workerGroupThreadNum = Integer.parseInt(System.getProperty(MQTT_SERVER_WORKER_GROUP_THREAD_NUM,
            MQTT_SERVER_WORKER_GROUP_THREAD_NUM_DEFAULT));
        this.socketBacklogSize = Integer.parseInt(System.getProperty(MQTT_SERVER_SOCKET_BACKLOG_SIZE,
            MQTT_SERVER_SOCKET_BACKLOG_SIZE_DEFAULT));

        this.rmqAccessKey = System.getProperty(MQTT_ROCKETMQ_ACCESSKEY, MQTT_ROCKETMQ_ACCESSKEY_DEFAULT);
        this.rmqSecretKey = System.getProperty(MQTT_ROCKETMQ_SECRETKEY, MQTT_ROCKETMQ_SECRETKEY_DEFAULT);

        this.rmqNamesrvAddr = System.getProperty(MQTT_ROCKETMQ_NAMESRVADDR, MQTT_ROCKETMQ_NAMESRVADDR_DEFAULT);
        this.rmqProductGroup = System.getProperty(MQTT_ROCKETMQ_PRODUCER_GROUP, MQTT_ROCKETMQ_PRODUCER_GROUP_DEFAULT);
        this.rmqConsumerGroup = System.getProperty(MQTT_ROCKETMQ_CONSUMER_GROUP, MQTT_ROCKETMQ_CONSUMER_GROUP_DEFAULT);
        this.rmqConsumerPullNums = Integer.parseInt(System.getProperty(MQTT_ROCKETMQ_CONSUMER_PULL_NUMS,
            MQTT_ROCKETMQ_CONSUMER_PULL_NUMS_DEFAULT));

        this.httpPort = Integer.parseInt(System.getProperty(MQTT_HTTP_PORT, MQTT_HTTP_PORT_DEFAULT));
        this.httpClusterHostList = Arrays.asList(System.getProperty(MQTT_HTTP_CLUSTER_HOST_LIST, MQTT_HTTP_CLUSTER_HOST_LIST_DEFAULT)
            .split(HTTP_ADDRESS_SEPARATOR));
    }

    public void initPropertiesConfig() {
        this.bridgeUsername = properties.getProperty(MQTT_BRIDGE_USERNAME, MQTT_BRIDGE_USERNAME_DEFAULT);
        this.bridgePassword = properties.getProperty(MQTT_BRIDGE_PASSWORD, MQTT_BRIDGE_PASSWORD_DEFAULT);

        this.brokerHost = properties.getProperty(MQTT_BROKER_HOST, MQTT_BROKER_HOST_DEFAULT);
        this.brokerPort = Integer.parseInt(properties.getProperty(MQTT_BROKER_PORT, MQTT_BROKER_PORT_DEFAULT));

        this.bossGroupThreadNum = Integer.parseInt(properties.getProperty(MQTT_SERVER_BOSS_GROUP_THREAD_NUM,
            MQTT_SERVER_BOSS_GROUP_THREAD_NUM_DEFAULT));
        this.workerGroupThreadNum = Integer.parseInt(properties.getProperty(MQTT_SERVER_WORKER_GROUP_THREAD_NUM,
            MQTT_SERVER_WORKER_GROUP_THREAD_NUM_DEFAULT));
        this.socketBacklogSize = Integer.parseInt(properties.getProperty(MQTT_SERVER_SOCKET_BACKLOG_SIZE,
            MQTT_SERVER_SOCKET_BACKLOG_SIZE_DEFAULT));

        this.rmqAccessKey = properties.getProperty(MQTT_ROCKETMQ_ACCESSKEY, MQTT_ROCKETMQ_ACCESSKEY_DEFAULT);
        this.rmqSecretKey = properties.getProperty(MQTT_ROCKETMQ_SECRETKEY, MQTT_ROCKETMQ_SECRETKEY_DEFAULT);

        this.rmqNamesrvAddr = properties.getProperty(MQTT_ROCKETMQ_NAMESRVADDR, MQTT_ROCKETMQ_NAMESRVADDR_DEFAULT);
        this.rmqProductGroup = properties.getProperty(MQTT_ROCKETMQ_PRODUCER_GROUP, MQTT_ROCKETMQ_PRODUCER_GROUP_DEFAULT);
        this.rmqConsumerGroup = properties.getProperty(MQTT_ROCKETMQ_CONSUMER_GROUP, MQTT_ROCKETMQ_CONSUMER_GROUP_DEFAULT);
        this.rmqConsumerPullNums = Integer.parseInt(properties.getProperty(MQTT_ROCKETMQ_CONSUMER_PULL_NUMS,
            MQTT_ROCKETMQ_CONSUMER_PULL_NUMS_DEFAULT));

        this.httpPort = Integer.parseInt(properties.getProperty(MQTT_HTTP_PORT, MQTT_HTTP_PORT_DEFAULT));
        this.httpClusterHostList = Arrays.asList(properties.getProperty(MQTT_HTTP_CLUSTER_HOST_LIST, MQTT_HTTP_CLUSTER_HOST_LIST_DEFAULT)
            .split(HTTP_ADDRESS_SEPARATOR));
    }

    public String getBridgeUsername() {
        return bridgeUsername;
    }

    public String getBridgePassword() {
        return bridgePassword;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
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

    public int getHttpPort() {
        return httpPort;
    }

    public List<String> getHttpClusterHostList() {
        return httpClusterHostList;
    }

    @Override public String toString() {
        return "MqttBridgeConfig{" +
            "bridgeUsername='" + bridgeUsername + '\'' +
            ", bridgePassword='" + bridgePassword + '\'' +
            ", brokerHost='" + brokerHost + '\'' +
            ", brokerPort=" + brokerPort +
            ", bossGroupThreadNum=" + bossGroupThreadNum +
            ", workerGroupThreadNum=" + workerGroupThreadNum +
            ", socketBacklogSize=" + socketBacklogSize +
            ", rmqAccessKey='" + rmqAccessKey + '\'' +
            ", rmqSecretKey='" + rmqSecretKey + '\'' +
            ", rmqNamesrvAddr='" + rmqNamesrvAddr + '\'' +
            ", rmqProductGroup='" + rmqProductGroup + '\'' +
            ", rmqConsumerGroup='" + rmqConsumerGroup + '\'' +
            ", rmqConsumerPullNums=" + rmqConsumerPullNums +
            ", httpPort=" + httpPort +
            ", httpClusterHostList=" + httpClusterHostList +
            '}';
    }
}