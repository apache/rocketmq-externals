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
package org.apache.rocketmq.mqtt.processor;

import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.mqtt.client.MqttClientManager;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.mqtt.common.RemotingChannel;

/**
 * Common Iot remoting command processor
 */
public interface MqttRequestProcessor {
    MqttMessage processRequest(RemotingChannel remotingChannel, MqttMessage request)
        throws Exception;

    /**
     * 根据topic，找到当前节点订阅了该topic的客户端
     * （因为转发消息时也转发了客户端ID，该方法没有用到，但暂时保留）
     * @param topic
     * @param mqttClientManager
     * @return
     */
    default Set<MQTTSession> findCurrentNodeClientsTobePublish(String topic, MqttClientManager mqttClientManager) {
        //find those clients publishing the message to
        MqttClientManagerImpl iotClientManagerImpl = (MqttClientManagerImpl) mqttClientManager;
        ConcurrentHashMap<String, Set<MQTTSession>> topic2Clients = iotClientManagerImpl.getTopic2Clients();
        Set<MQTTSession> clientsTobePush = new HashSet<>();
        if (topic2Clients.containsKey(MqttUtil.getRootTopic(topic))) {
            Set<MQTTSession> clients = topic2Clients.get(MqttUtil.getRootTopic(topic));
            for (MQTTSession client : clients) {
                if (client.isConnected()) {
                    Enumeration<String> keys = client.getMqttSubscriptionDataTable().keys();
                    while (keys.hasMoreElements()) {
                        String topicFilter = keys.nextElement();
                        if (MqttUtil.isMatch(topicFilter, topic)) {
                            clientsTobePush.add(client);
                        }
                    }
                }
            }
        }
        return clientsTobePush;
    }

    default MqttMessage doResponse(MqttFixedHeader fixedHeader, MqttPublishVariableHeader variableHeader) {
        if (fixedHeader.qosLevel().value() > 0) {
            if (fixedHeader.qosLevel().equals(MqttQoS.AT_LEAST_ONCE)) {
                return new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 2), MqttMessageIdVariableHeader.from(variableHeader.packetId()));
            } else if (fixedHeader.qosLevel().equals(MqttQoS.EXACTLY_ONCE)) {
                //PUBREC/PUBREL/PUBCOMP
            }
        }
        return null;
    }

    boolean rejectRequest();
}
