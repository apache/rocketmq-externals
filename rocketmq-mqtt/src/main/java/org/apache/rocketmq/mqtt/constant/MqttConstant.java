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

package org.apache.rocketmq.mqtt.constant;

public class MqttConstant {
    public static final int MAX_SUPPORTED_QOS = 1;
    public static final String SUBSCRIPTION_FLAG_PLUS = "+";
    public static final String SUBSCRIPTION_FLAG_SHARP = "#";
    public static final String SUBSCRIPTION_SEPARATOR = "/";
    public static final String IP_LISTENPORT_SEPARATOR = ":";
    public static final String TOPIC_CLIENTID_SEPARATOR = "^";
    public static final long DEFAULT_TIMEOUT_MILLS = 6000L;
    public static final long CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND = 1000 * 30;
    public static final long HEARTBEAT_TIME_OUT = 3000;
    public static final int KEEP_ALIVE_INTERVAL_DEFAULT = 60;
    public static final int MAX_PROCESS_TIME_LIMIT = 2000;
    public static final String PROPERTY_MQTT_QOS = "PROPERTY_MQTT_QOS";
    public static final String DEFAULT_PROTOCOL = "mqtt";
    public static final String ENODE_NAME = "enodeName";
    public static final String PERSIST_BRIDGE_SUFFIX = "-bri";
    public static final String PERSIST_CLIENT_SUFFIX = "-cli";
    public static final String PERSIST_TOPIC_SUFFIX = "-top";
    public static final String PERSIST_RET_SUFFIX = "-ret";
}
