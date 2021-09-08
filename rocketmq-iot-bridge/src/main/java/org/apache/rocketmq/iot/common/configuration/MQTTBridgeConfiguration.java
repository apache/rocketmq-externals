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

public class MQTTBridgeConfiguration {
    /**
     * iot mqtt bridge broker server configuration
     */
    public static final String MQTT_BROKER_HOST = "iot.mqtt.broker.host";
    public static final String MQTT_BROKER_HOST_DEFAULT = "127.0.0.1";

    public static final String MQTT_BROKER_PORT = "iot.mqtt.broker.port";
    public static final String MQTT_BROKER_PORT_DEFAULT = "1883";


    public static final String MQTT_HTTP_PORT = "iot.mqtt.http.port";
    public static final String MQTT_HTTP_PORT_DEFAULT = "8080";

    public static final String MQTT_HTTP_CLUSTER_HOST_LIST = "iot.mqtt.http.cluster.host.list";
    public static final String MQTT_HTTP_CLUSTER_HOST_LIST_DEFAULT = "127.0.0.1:8080";

    public static final String MQTT_SERVER_BOSS_GROUP_THREAD_NUM = "iot.mqtt.server.boss.group.thread.num";
    public static final String MQTT_SERVER_BOSS_GROUP_THREAD_NUM_DEFAULT = "1";

    public static final String MQTT_SERVER_WORKER_GROUP_THREAD_NUM = "iot.mqtt.server.worker.group.thread.num";
    public static final String MQTT_SERVER_WORKER_GROUP_THREAD_NUM_DEFAULT = "32";

    public static final String MQTT_SERVER_SOCKET_BACKLOG_SIZE = "iot.mqtt.server.socket.backlog.size";
    public static final String MQTT_SERVER_SOCKET_BACKLOG_SIZE_DEFAULT = "1024";

    public static final String MQTT_BROKER_HEARTBEAT_ALLIDLETIME = "iot.mqtt.server.heartbeat.allidletime";
    public static final String MQTT_BROKER_HEARTBEAT_ALLIDLETIME_DEFAULT = "120";

    /**
     * iot mqtt bridge broker store configuration
     */

    public static final String MQTT_ROCKETMQ_STORE_ENABLED = "iot.mqtt.server.rocketmq.store.enabled";
    public static final String MQTT_ROCKETMQ_STORE_ENABLED_DEFAULT = "true";

    public static final String MQTT_ROCKETMQ_ACCESSKEY = "iot.mqtt.server.rocketmq.accessKey";
    public static final String MQTT_ROCKETMQ_ACCESSKEY_DEFAULT = "";

    public static final String MQTT_ROCKETMQ_SECRETKEY = "iot.mqtt.server.rocketmq.secretkey";
    public static final String MQTT_ROCKETMQ_SECRETKEY_DEFAULT = "";

    public static final String MQTT_ROCKETMQ_NAMESRVADDR = "iot.mqtt.server.rocketmq.namesrvaddr";
    public static final String MQTT_ROCKETMQ_NAMESRVADDR_DEFAULT = "";

    public static final String MQTT_ROCKETMQ_PRODUCER_GROUP = "iot.mqtt.server.rocketmq.producer.group";
    public static final String MQTT_ROCKETMQ_PRODUCER_GROUP_DEFAULT = "mqtt_producer_group";

    public static final String MQTT_ROCKETMQ_CONSUMER_GROUP = "iot.mqtt.server.rocketmq.consumer.group";
    public static final String MQTT_ROCKETMQ_CONSUMER_GROUP_DEFAULT = "mqtt_consumer_group";

    public static final String MQTT_ROKECTMQ_CONSUMER_PULL_NUMS = "iot.mqtt.server.rocketmq.consumer.pull.nums";
    public static final String MQTT_ROKECTMQ_CONSUMER_PULL_NUMS_DEFAULT = "32";
}
