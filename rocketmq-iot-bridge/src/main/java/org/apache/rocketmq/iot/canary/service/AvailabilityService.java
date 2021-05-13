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

package org.apache.rocketmq.iot.canary.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.iot.canary.config.CanaryConfig;
import org.apache.rocketmq.iot.canary.util.FalconOutput;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvailabilityService implements FalconDataService {
    private Logger logger = LoggerFactory.getLogger(AvailabilityService.class);

    private CanaryConfig canaryConfig;
    private FalconOutput falconOutput;
    private String mqttServerURI;
    private MqttConnectOptions mqttConnectOptions;
    private MqttClient subscriberMqttClient;
    private MqttClient publisherMqttClient;
    private String subscriberClientId;
    private String publisherClientId;
    private AvailabilityResult subscriberAvai;
    private AvailabilityResult publisherAvai;
    private AvailabilityResult totalAvai;
    private List<Double> latencyList;

    public AvailabilityService(CanaryConfig canaryConfig) {
        this.canaryConfig = canaryConfig;
        this.falconOutput = new FalconOutput(canaryConfig);
        this.mqttServerURI = canaryConfig.getMqttServerURI();
        this.subscriberClientId = canaryConfig.getClientSubscriberId();
        this.publisherClientId = canaryConfig.getClientPublisherId();
        this.subscriberAvai = new AvailabilityResult();
        this.publisherAvai = new AvailabilityResult();
        this.totalAvai = new AvailabilityResult();
        this.latencyList = new ArrayList<>();
        initMqttConnectOptions();
    }

    private void initMqttConnectOptions() {
        this.mqttConnectOptions = new MqttConnectOptions();
        this.mqttConnectOptions.setCleanSession(true);
        this.mqttConnectOptions.setUserName(canaryConfig.getMqttUserName());
        this.mqttConnectOptions.setPassword(canaryConfig.getMqttPassword().toCharArray());
    }

    private void subscribeMessages() {
        try {
            if (subscriberMqttClient == null) {
                MemoryPersistence persistence = new MemoryPersistence();
                subscriberMqttClient = new MqttClient(mqttServerURI, subscriberClientId, persistence);
                subscriberMqttClient.connect(mqttConnectOptions);
                subscriberMqttClient.subscribe(canaryConfig.getMqttTopic(), canaryConfig.getMqttQos());
                SubscribeCallback callback = new SubscribeCallback(subscriberMqttClient, mqttConnectOptions,
                    canaryConfig.getMqttTopic(), canaryConfig.getMqttQos());
                subscriberMqttClient.setCallback(callback);
            }
            if (!subscriberMqttClient.isConnected()) {
                subscriberMqttClient.connect(mqttConnectOptions);
                subscriberMqttClient.subscribe(canaryConfig.getMqttTopic(), canaryConfig.getMqttQos());
            }
        } catch (MqttException e) {
            subscriberAvai.addFail();
            totalAvai.addFail();
            logger.error("subscriber client {} subscribe mqtt messages failed.", subscriberClientId, e);
        }
    }

    private class SubscribeCallback implements MqttCallback {
        private MqttClient mqttClient;
        private MqttConnectOptions connectOptions;
        private String topic;
        private int qos;

        public SubscribeCallback(MqttClient mqttClient, MqttConnectOptions connectOptions, String topic, int qos) {
            this.mqttClient = mqttClient;
            this.connectOptions = connectOptions;
            this.topic = topic;
            this.qos = qos;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            logger.error("subscribe mqtt messages connection lost, the connection will be tried again." +
                "connect status:{}, throwable:{}", subscriberMqttClient.isConnected(), throwable);
            try {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    mqttClient.connect(connectOptions);
                    mqttClient.subscribe(topic, qos);
                }
            } catch (MqttException e) {
                logger.error("publish mqtt client try connect again failed.");
            }
            subscriberAvai.addFail();
            totalAvai.addFail();
        }

        @Override
        public void messageArrived(String s, MqttMessage message) throws Exception {
            subscriberAvai.addSuccess();
            totalAvai.addSuccess();
            double delay = (System.nanoTime() - Long.parseLong(message.toString())) * 1.0 / 1000 / 1000;
            latencyList.add(delay);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private void publishMessages() {
        try {
            if (publisherMqttClient == null) {
                MemoryPersistence persistence = new MemoryPersistence();
                publisherMqttClient = new MqttClient(mqttServerURI, publisherClientId, persistence);
                publisherMqttClient.connect(mqttConnectOptions);
                PublishCallback callback = new PublishCallback(publisherMqttClient, mqttConnectOptions);
                publisherMqttClient.setCallback(callback);
            }
            if (!publisherMqttClient.isConnected()) {
                publisherMqttClient.connect(mqttConnectOptions);
            }

            for (int i = 0; i < canaryConfig.getPublishTimes(); i++) {
                MqttMessage message = new MqttMessage();
                message.setPayload(String.valueOf(System.nanoTime()).getBytes());
                publisherMqttClient.publish(canaryConfig.getMqttTopic(), message);
            }
        } catch (MqttException e) {
            publisherAvai.addFail();
            totalAvai.addFail();
            logger.error("publisher client {} publish mqtt messages failed.", publisherClientId, e);
        }
    }

    private class PublishCallback implements MqttCallback {
        private MqttClient mqttClient;
        private MqttConnectOptions connectOptions;

        public PublishCallback(MqttClient mqttClient, MqttConnectOptions connectOptions) {
            this.mqttClient = mqttClient;
            this.connectOptions = connectOptions;
        }

        @Override
        public void connectionLost(Throwable throwable) {
            logger.error("publish mqtt messages connection lost, the connection will be tried again." +
                "connect status:{}, throwable:{}", mqttClient.isConnected(), throwable);
            try {
                if (mqttClient != null && !mqttClient.isConnected()) {
                    mqttClient.connect(connectOptions);
                }
            } catch (MqttException e) {
                logger.error("publish mqtt client try connect again failed.");
            }
            publisherAvai.addFail();
            totalAvai.addFail();
        }

        @Override
        public void messageArrived(String s, MqttMessage message) throws Exception {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            if (token.isComplete()) {
                publisherAvai.addSuccess();
                totalAvai.addSuccess();
            } else {
                logger.error("publish mqtt messages failed, token isn't complete.");
                publisherAvai.addFail();
                totalAvai.addFail();
            }
        }
    }

    private void pushAvailabilityData() {
        String metric = canaryConfig.getMetricAvailability();

        // publisher
        double publisherRatio = publisherAvai.getRatio() * 100;
        falconOutput.pushFalconData(publisherRatio, "publisher", metric);
        logger.info("publisher availability ratio:" + publisherRatio);

        // subscriber
        double subscriberRatio = subscriberAvai.getRatio() * 100;
        falconOutput.pushFalconData(subscriberRatio, "subscriber", metric);
        logger.info("subscriber availability ratio:" + subscriberRatio);

        // total
        double totalRatio = totalAvai.getRatio() * 100;
        falconOutput.pushFalconData(totalRatio, "total", metric);
        logger.info("total availability ratio:" + totalRatio);
    }

    private double getETELatency() {
        if (latencyList.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double latency : latencyList) {
            sum += latency;
        }
        return sum / latencyList.size();
    }

    private void pushDelayData() {
        double eteLatency = getETELatency();
        falconOutput.pushFalconData(eteLatency, "ETE-Latency", canaryConfig.getMetricLatency());
        latencyList.clear();
        logger.info("ETE-Latency:" + eteLatency);
    }

    @Override public void report() {
        subscribeMessages();
        publishMessages();
        pushAvailabilityData();
        pushDelayData();
    }

    @Override public void shutdown() {
        try {
            if (publisherMqttClient != null && publisherMqttClient.isConnected()) {
                publisherMqttClient.disconnect();
            }
            publisherMqttClient = null;
        } catch (MqttException e) {
            logger.error("disconnect canary publisher client failed.", e);
        }
    }
}
