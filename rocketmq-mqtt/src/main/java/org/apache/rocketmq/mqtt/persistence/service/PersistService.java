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

package org.apache.rocketmq.mqtt.persistence.service;

import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;

import java.util.Map;
import java.util.Set;

public interface PersistService {
    /**
     * Init Persist Service
     *
     * @param mqttBridgeController MQTT-Bridge controller
     */
    void init(MqttBridgeController mqttBridgeController);

    boolean addConnection(MQTTSession client);

    boolean updateOrAddClient(MQTTSession client);

    boolean addRootTopic2MqttBridge(Set<String> rootTopicsBefore, Set<String> rootTopicsAfter, String ipAddress);

    boolean deleteRootTopic2MqttBridge(Set<String> rootTopicsBefore, Set<String> rootTopicsAfter, String ipAddress);

    Set<String> getMqttBridgeByRootTopic(String rootTopic);

    boolean deleteClient(MQTTSession client);

    Map<String, Set<String>> getMqttAddress2Clients(String topic);

    MQTTSession getClientByClientId(String clientId);

    boolean addOrUpdateTopic2RetainMessage(String topic, byte[] bytes);

    byte[] getRetainMessageByTopic(String topic);

    Set<String> getTopicsByRootTopic(String rootTopic);

    boolean deleteRetainMessageByTopic(String topic);

    boolean updateConsumeOffset(String key, long offset);

    long queryConsumeOffset(String key);

    boolean deleteConsumeOffset(String key);
}
