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

    public void initMqttConnectOptions() {
        this.mqttConnectOptions = new MqttConnectOptions();
        this.mqttConnectOptions.setCleanSession(true);
        this.mqttConnectOptions.setUserName(canaryConfig.getMqttUserName());
        this.mqttConnectOptions.setPassword(canaryConfig.getMqttPassword().toCharArray());
    }

    public void subscribeMessages() {
        try {
            if (subscriberMqttClient == null) {
                MemoryPersistence persistence = new MemoryPersistence();
                subscriberMqttClient = new MqttClient(mqttServerURI, subscriberClientId, persistence);
                subscriberMqttClient.connect(mqttConnectOptions);
                subscriberMqttClient.subscribe(canaryConfig.getMqttTopic(), canaryConfig.getMqttQos());
                subscriberMqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable throwable) {
                        logger.error("subscribe mqtt messages connection lost, connect status:{}, throwable:{}",
                            subscriberMqttClient.isConnected(), throwable);
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
                });
            }
            if (!subscriberMqttClient.isConnected()) {
                subscriberMqttClient.connect(mqttConnectOptions);
            }
        } catch (MqttException e) {
            subscriberAvai.addFail();
            totalAvai.addFail();
            logger.error("subscriber client {} subscribe mqtt messages failed.", subscriberClientId, e);
        }
    }

    public void publishMessages() {
        try {
            if (publisherMqttClient == null) {
                MemoryPersistence persistence = new MemoryPersistence();
                publisherMqttClient = new MqttClient(mqttServerURI, publisherClientId, persistence);
                publisherMqttClient.connect(mqttConnectOptions);
                publisherMqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable throwable) {
                        logger.error("publish mqtt messages connection lost, connect status:{}, throwable:{}",
                            publisherMqttClient.isConnected(), throwable);
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
                            publisherAvai.addFail();
                            totalAvai.addFail();
                        }
                    }
                });
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

    public void pushAvailabilityData() {
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

    public double getETELatency() {
        if (latencyList.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (Double latency : latencyList) {
            sum += latency;
        }
        return sum / latencyList.size();
    }

    public void pushDelayData() {
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
                publisherMqttClient = null;
            }
        } catch (MqttException e) {
            logger.error("disconnect canary publisher client failed.", e);
        }
    }
}
