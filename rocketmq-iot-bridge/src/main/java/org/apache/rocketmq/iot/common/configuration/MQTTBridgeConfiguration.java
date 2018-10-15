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

import java.util.Properties;

public class MQTTBridgeConfiguration {

    private Properties properties;

    public static final String MQTT_PORT_PROPERTY = "iot.mqtt.port";
    public static final String DEFAULT_MQTT_PORT = "1883";
    public static final String MQTT_HOST_PROPERTY = "iot.mqtt.host";
    public static final String DEFAULT_MQTT_HOST = "127.0.0.1";
    public static final String MQTT_SOCKET_BACKLOG = "iot.mqtt.socket.backlog";
    public static final String DEFAULT_SOCKET_BACKLOG = "1024";
    public static final Integer DEFAULT_THREAD_NUM_OF_BOSS_GROUP = 1;
    public static final Integer DEFAULT_THREAD_NUM_OF_WORKER_GROUP = 32;

    public static Integer port() {
        return Integer.valueOf(System.getProperty(MQTT_PORT_PROPERTY, DEFAULT_MQTT_PORT));
    }

    public static String host() {
        return System.getProperty(MQTT_HOST_PROPERTY, DEFAULT_MQTT_HOST);
    }

    public static Integer socketBacklog() {
        return Integer.valueOf(System.getProperty(MQTT_SOCKET_BACKLOG, DEFAULT_SOCKET_BACKLOG));
    }

    public static Integer threadNumOfBossGroup() {
        return DEFAULT_THREAD_NUM_OF_BOSS_GROUP;
    }

    public static Integer threadNumOfWorkerGroup() {
        return DEFAULT_THREAD_NUM_OF_WORKER_GROUP;
    }
}
