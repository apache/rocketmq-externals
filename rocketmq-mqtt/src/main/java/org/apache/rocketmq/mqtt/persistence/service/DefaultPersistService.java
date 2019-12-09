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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.persistence.redis.RedisPool;
import org.apache.rocketmq.mqtt.persistence.redis.RedisService;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.utils.SerializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.groupingBy;

public class DefaultPersistService implements PersistService {
    private static final Logger log = LoggerFactory.getLogger(DefaultPersistService.class);
    private RedisService redisService;

    public DefaultPersistService() {
    }

    @Override public void init(MqttBridgeController mqttBridgeController) {
        redisService = new RedisService(new RedisPool(mqttBridgeController));
    }

    @Override public boolean addConnection(MQTTSession client) {
        // add mqtt client
        if (!updateOrAddClient(client))
            return false;

        return true;
    }

    @Override public boolean updateOrAddClient(MQTTSession client) {
        MQTTSession mqttSession = (MQTTSession) client;
        // update mqtt client
        if (redisService.set(mqttSession.getClientId() + MqttConstant.PERSIST_CLIENT_SUFFIX, SerializationUtil.serialize(client)).equals("OK"))
            return true;
        return false;
    }

    @Override
    public boolean addRootTopic2MqttBridge(Set<String> rootTopicsBefore, Set<String> rootTopicsAfter,
        String ipAddress) {
        rootTopicsAfter.removeAll(rootTopicsBefore);
        // update clientId set which is collected by root topic
        for (String rootTopic : rootTopicsAfter) {
            redisService.addSetMember(rootTopic + MqttConstant.PERSIST_BRIDGE_SUFFIX, ipAddress);
        }
        return true;
    }

    @Override
    public boolean deleteRootTopic2MqttBridge(Set<String> rootTopicsBefore, Set<String> rootTopicsAfter,
        String ipAddress) {
        rootTopicsBefore.removeAll(rootTopicsAfter);
        // update clientId set which is collected by root topic
        for (String rootTopic : rootTopicsBefore) {
            redisService.deleteSetMember(rootTopic + MqttConstant.PERSIST_BRIDGE_SUFFIX, ipAddress);
        }
        return true;
    }

    @Override public Set<String> getMqttBridgeByRootTopic(String rootTopic) {
        return redisService.getAllMember(rootTopic + MqttConstant.PERSIST_BRIDGE_SUFFIX);
    }

    @Override public boolean deleteClient(MQTTSession client) {

        String clientId = ((MQTTSession) client).getClientId();
        // delete client from clentIds set of rootTopic
/*        ConcurrentHashMap<String, MqttSubscriptionData> mqttSubscriptionDataTable = ((MQTTSession) client).getMqttSubscriptionDataTable();
        Set<String> rootTopics = null;
        if (mqttSubscriptionDataTable.size() > 0) {
            rootTopics = mqttSubscriptionDataTable.keySet().stream().map(t -> MqttUtil.getRootTopic(t)).collect(Collectors.toSet());
            for (String rootTopic : rootTopics) {
                redisService.deleteSetMember(rootTopic + MqttConstant.PERSIST_CLIENT_SUFFIX, clientId);
            }
        }*/
        //delete client entity
        if (redisService.del(clientId + MqttConstant.PERSIST_CLIENT_SUFFIX) == 0 && redisService.get(clientId + MqttConstant.PERSIST_CLIENT_SUFFIX) != null) {
/*            if (mqttSubscriptionDataTable.size() > 0) {
                for (String rootTopic : rootTopics) {
                    redisService.addSetMember(rootTopic + MqttConstant.PERSIST_CLIENT_SUFFIX, clientId);
                }
            }*/
            return false;
        }
        return true;
    }

    @Override public Map<String, Set<String>> getMqttAddress2Clients(String topic) {
        // step1: get clientIds which subscribe the topic
        Set<String> clientIds = redisService.getAllMember(MqttUtil.getRootTopic(topic) + MqttConstant.PERSIST_CLIENT_SUFFIX);
        if (clientIds != null && clientIds.size() == 0) {
            return new HashMap<>();
        }
        Map<String, Set<String>> map = null;
        try {
            Set<MQTTSession> clients = clientIds.stream().map(c -> (MQTTSession) SerializationUtil.deserialize(redisService.get(c + MqttConstant.PERSIST_CLIENT_SUFFIX), MQTTSession.class))
                .filter(c -> c.isConnected())
                .filter(c -> MqttUtil.isContain(c.getMqttSubscriptionDataTable().keySet(), topic))
                .collect(Collectors.toSet());
            // step2: group by mqtt bridge
            map = clients.stream().collect(groupingBy(MQTTSession::getMqttBridgeAddr, Collectors.mapping(MQTTSession::getClientId, Collectors.toSet())));
        } catch (Exception e) {
            log.error("Exception was thrown when quering Client by clientId. e={}", e.getMessage());
        }
        return (map != null ? map : new HashMap<>());
    }

    @Override public MQTTSession getClientByClientId(String clientId) {
        return (MQTTSession) SerializationUtil.deserialize(redisService.get(clientId + MqttConstant.PERSIST_CLIENT_SUFFIX), MQTTSession.class);
    }

    @Override public boolean addOrUpdateTopic2RetainMessage(String topic, byte[] bytes) {

        // store topic
        redisService.addSetMember(MqttUtil.getRootTopic(topic) + MqttConstant.PERSIST_TOPIC_SUFFIX, topic);
        if (!redisService.set(topic + MqttConstant.PERSIST_RET_SUFFIX, SerializationUtil.serialize(bytes)).equals("OK")) {
            redisService.deleteSetMember(MqttUtil.getRootTopic(topic) + MqttConstant.PERSIST_TOPIC_SUFFIX, topic);
            return false;
        }
        return true;
    }

    @Override public byte[] getRetainMessageByTopic(String topic) {
        return (byte[]) SerializationUtil.deserialize(redisService.get(topic + MqttConstant.PERSIST_RET_SUFFIX), byte[].class);
    }

    @Override public Set<String> getTopicsByRootTopic(String rootTopic) {
        return redisService.getAllMember(rootTopic + MqttConstant.PERSIST_TOPIC_SUFFIX);
    }

    @Override public boolean deleteRetainMessageByTopic(String topic) {
        redisService.deleteSetMember(MqttUtil.getRootTopic(topic) + MqttConstant.PERSIST_TOPIC_SUFFIX, topic);
        if (redisService.del(topic + MqttConstant.PERSIST_RET_SUFFIX) == 0 && redisService.get(topic + MqttConstant.PERSIST_RET_SUFFIX) != null) {
            redisService.addSetMember(MqttUtil.getRootTopic(topic) + MqttConstant.PERSIST_TOPIC_SUFFIX, topic);
            return false;
        }
        return true;
    }

    @Override public boolean updateConsumeOffset(String key, long offset) {
        //key=brokerName^rootTopic^clientId
        //value=offset
        if (redisService.set(key, String.valueOf(offset)).equals("OK")) {
            return true;
        }
        return false;
    }

    @Override public long queryConsumeOffset(String key) {
        //key=brokerName^rootTopic^clientId
        String value = redisService.get(key);
        return value == null ? -1 : Long.valueOf(value);
    }

    @Override public boolean deleteConsumeOffset(String key) {
        if (redisService.del(key) == 0 && redisService.get(key) != null) {
            log.error("Delete consumeOffset failed. key={}", key);
            return false;
        }
        return true;
    }

}
