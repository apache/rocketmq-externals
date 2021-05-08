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

package org.apache.rocketmq.iot.canary.config;

public class CanaryConfigKey {
    /**
     * iot mqtt canary interval millis
     */
    public static final String CANARY_INTERVAL_MILLIS = "iot.mqtt.canary.interval.millis";
    public static final String CANARY_INTERVAL_MILLIS_DEFAULT = "60000";

    /**
     * iot mqtt canary client subscriber
     */
    public static final String CANARY_CLIENT_SUBSCRIBER = "iot.mqtt.canary.client.subscriber";
    public static final String CANARY_CLIENT_SUBSCRIBER_DEFAULT = "client_subscriber_01";

    /**
     * iot mqtt canary client publisher
     */
    public static final String CANARY_CLIENT_PUBLISHER = "iot.mqtt.canary.client.publisher";
    public static final String CANARY_CLIENT_PUBLISHER_DEFAULT = "client_publisher_01";

    /**
     * iot mqtt canary client publish times
     */
    public static final String CANARY_CLIENT_PUBLISH_TIMES = "iot.mqtt.canary.client.publish.times";
    public static final String CANARY_CLIENT_PUBLISH_TIMES_DEFAULT = "20";

    /**
     * iot mqtt canary client mqtt topic
     */
    public static final String CANARY_CLIENT_MQTT_TOPIC = "iot.mqtt.canary.client.mqtt.topic";
    public static final String CANARY_CLIENT_MQTT_TOPIC_DEFAULT = "mqtt_canary/topic_01";

    /**
     * iot mqtt canary client mqtt qos
     */
    public static final String CANARY_CLIENT_MQTT_QOS = "iot.mqtt.canary.client.mqtt.qos";
    public static final String CANARY_CLIENT_MQTT_QOS_DEFAULT = "0";

    /**
     * iot mqtt server URI
     */
    public static final String MQTT_SERVER_URI = "iot.mqtt.server.uri";
    public static final String MQTT_SERVER_URI_DEFAULT = "tcp://127.0.0.1:1883";

    /**
     * iot mqtt username
     */
    public static final String MQTT_USER_NAME = "iot.mqtt.user.name";
    public static final String MQTT_USER_NAME_DEFAULT = "mqtt";

    /**
     * iot mqtt password
     */
    public static final String MQTT_PASSWORD = "iot.mqtt.password";
    public static final String MQTT_PASSWORD_DEFAULT = "123456";

    /**
     * iot mqtt connect number uri
     */
    public static final String MQTT_CONNECT_NUMBER_URI = "iot.mqtt.connect.number.uri";
    public static final String MQTT_CONNECT_NUMBER_URI_DEFAULT = "http://127.0.0.1:8081/mqtt/connection/num?mode=cluster";

    /**
     * falcon url
     */
    public static final String FALCON_URL = "iot.mqtt.falcon.url";
    public static final String FALCON_URL_DEFAULT = "http://127.0.0.1:1988/v1/push";

    /**
     * falcon falcon intervalMillis
     */
    public static final String FALCON_INTERVAL_MILLIS = "iot.mqtt.falcon.interval.millis";
    public static final String FALCON_INTERVAL_MILLIS_DEFAULT = "60000";

    /**
     * falcon falcon endpoint
     */
    public static final String FALCON_ENDPOINT = "iot.mqtt.falcon.endpoint";
    public static final String FALCON_ENDPOINT_DEFAULT = "rmq-mqtt.monitor";

    /**
     * falcon falcon metric availability
     */
    public static final String FALCON_METRIC_AVAILABILITY = "iot.mqtt.falcon.metric.availability";
    public static final String FALCON_METRIC_AVAILABILITY_DEFAULT = "availability";

    /**
     * falcon falcon metric connect
     */
    public static final String FALCON_METRIC_CONNECT = "iot.mqtt.falcon.metric.connect";
    public static final String FALCON_METRIC_CONNECT_DEFAULT = "clusterConnect";

    /**
     * falcon falcon metric latency
     */
    public static final String FALCON_METRIC_LATENCY = "iot.mqtt.falcon.metric.latency";
    public static final String FALCON_METRIC_LATENCY_DEFAULT = "latency";

    /**
     * falcon falcon metric clusterName
     */
    public static final String FALCON_METRIC_CLUSTER_NAME = "iot.mqtt.falcon.cluster.name";
    public static final String FALCON_METRIC_CLUSTER_NAME_DEFAULT = "tjwqtst-common-rmq-mqtt";
}
