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

import java.util.Properties;

public class CanaryConfig {
    private int canaryIntervalMillis;

    private String clientSubscriberId;
    private String clientPublisherId;
    private int publishTimes;
    private String mqttTopic;
    private int mqttQos;

    private String mqttServerURI;
    private String mqttUserName;
    private String mqttPassword;
    private String connectNumberURI;

    private String falconUrl;
    private int falconIntervalMillis;
    private String endpoint;
    private String clusterName;

    private String metricAvailability;
    private String metricConnect;
    private String metricLatency;

    public CanaryConfig(Properties properties) {
        // canary
        this.canaryIntervalMillis = Integer.parseInt(properties.getProperty(CanaryConfigKey.CANARY_INTERVAL_MILLIS,
            CanaryConfigKey.CANARY_INTERVAL_MILLIS_DEFAULT));

        // canary mqtt client
        this.clientSubscriberId = properties.getProperty(CanaryConfigKey.CANARY_CLIENT_SUBSCRIBER,
            CanaryConfigKey.CANARY_CLIENT_SUBSCRIBER_DEFAULT);
        this.clientPublisherId = properties.getProperty(CanaryConfigKey.CANARY_CLIENT_PUBLISHER,
            CanaryConfigKey.CANARY_CLIENT_PUBLISHER_DEFAULT);
        this.publishTimes = Integer.parseInt(properties.getProperty(CanaryConfigKey.CANARY_CLIENT_PUBLISH_TIMES,
            CanaryConfigKey.CANARY_CLIENT_PUBLISH_TIMES_DEFAULT));
        this.mqttTopic = properties.getProperty(CanaryConfigKey.CANARY_CLIENT_MQTT_TOPIC,
            CanaryConfigKey.CANARY_CLIENT_MQTT_TOPIC_DEFAULT);
        this.mqttQos = Integer.parseInt(properties.getProperty(CanaryConfigKey.CANARY_CLIENT_MQTT_QOS,
            CanaryConfigKey.CANARY_CLIENT_MQTT_QOS_DEFAULT));

        // mqtt server
        this.mqttServerURI = properties.getProperty(CanaryConfigKey.MQTT_SERVER_URI, CanaryConfigKey.MQTT_SERVER_URI_DEFAULT);
        this.mqttUserName = properties.getProperty(CanaryConfigKey.MQTT_USER_NAME, CanaryConfigKey.MQTT_USER_NAME_DEFAULT);
        this.mqttPassword = properties.getProperty(CanaryConfigKey.MQTT_PASSWORD, CanaryConfigKey.MQTT_PASSWORD_DEFAULT);
        this.connectNumberURI = properties.getProperty(CanaryConfigKey.MQTT_CONNECT_NUMBER_URI, CanaryConfigKey.MQTT_CONNECT_NUMBER_URI_DEFAULT);

        // falcon
        this.falconUrl = properties.getProperty(CanaryConfigKey.FALCON_URL, CanaryConfigKey.FALCON_URL_DEFAULT);
        this.falconIntervalMillis = Integer.parseInt(properties.getProperty(CanaryConfigKey.FALCON_INTERVAL_MILLIS,
            CanaryConfigKey.FALCON_INTERVAL_MILLIS_DEFAULT));
        this.endpoint = properties.getProperty(CanaryConfigKey.FALCON_ENDPOINT, CanaryConfigKey.FALCON_ENDPOINT_DEFAULT);
        this.clusterName = properties.getProperty(CanaryConfigKey.FALCON_METRIC_CLUSTER_NAME,
            CanaryConfigKey.FALCON_METRIC_CLUSTER_NAME_DEFAULT);
        this.metricAvailability = properties.getProperty(CanaryConfigKey.FALCON_METRIC_AVAILABILITY,
            CanaryConfigKey.FALCON_METRIC_AVAILABILITY_DEFAULT);
        this.metricConnect = properties.getProperty(CanaryConfigKey.FALCON_METRIC_CONNECT,
            CanaryConfigKey.FALCON_METRIC_CONNECT_DEFAULT);
        this.metricLatency = properties.getProperty(CanaryConfigKey.FALCON_METRIC_LATENCY,
            CanaryConfigKey.FALCON_METRIC_LATENCY_DEFAULT);
    }

    public int getCanaryIntervalMillis() {
        return canaryIntervalMillis;
    }

    public String getClientSubscriberId() {
        return clientSubscriberId;
    }

    public String getClientPublisherId() {
        return clientPublisherId;
    }

    public int getPublishTimes() {
        return publishTimes;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public int getMqttQos() {
        return mqttQos;
    }

    public String getMqttServerURI() {
        return mqttServerURI;
    }

    public String getMqttUserName() {
        return mqttUserName;
    }

    public String getMqttPassword() {
        return mqttPassword;
    }

    public String getConnectNumberURI() {
        return connectNumberURI;
    }

    public String getFalconUrl() {
        return falconUrl;
    }

    public int getFalconIntervalMillis() {
        return falconIntervalMillis;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getMetricAvailability() {
        return metricAvailability;
    }

    public String getMetricConnect() {
        return metricConnect;
    }

    public String getMetricLatency() {
        return metricLatency;
    }

    @Override public String toString() {
        return "CanaryConfig{" +
            "canaryIntervalMillis=" + canaryIntervalMillis +
            ", clientSubscriberId='" + clientSubscriberId + '\'' +
            ", clientPublisherId='" + clientPublisherId + '\'' +
            ", publishTimes=" + publishTimes +
            ", mqttTopic='" + mqttTopic + '\'' +
            ", mqttQos=" + mqttQos +
            ", mqttServerURI='" + mqttServerURI + '\'' +
            ", mqttUserName='" + mqttUserName + '\'' +
            ", mqttPassword='" + mqttPassword + '\'' +
            ", connectNumberURI='" + connectNumberURI + '\'' +
            ", falconUrl='" + falconUrl + '\'' +
            ", falconIntervalMillis=" + falconIntervalMillis +
            ", endpoint='" + endpoint + '\'' +
            ", clusterName='" + clusterName + '\'' +
            ", metricAvailability='" + metricAvailability + '\'' +
            ", metricConnect='" + metricConnect + '\'' +
            ", metricLatency='" + metricLatency + '\'' +
            '}';
    }
}
