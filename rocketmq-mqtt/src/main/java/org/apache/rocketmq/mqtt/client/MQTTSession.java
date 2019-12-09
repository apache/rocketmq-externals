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
package org.apache.rocketmq.mqtt.client;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.mqtt.common.MqttSubscriptionData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.common.WillMessage;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.exception.MqttRuntimeException;
import org.apache.rocketmq.mqtt.persistence.service.PersistService;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.remoting.protocol.LanguageCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class MQTTSession{
    private static final Logger log = LoggerFactory.getLogger(MQTTSession.class);

    private final transient ReentrantLock lock = new ReentrantLock();
    private transient MqttBridgeController mqttBridgeController;
    private String ownerUserId;
    private String clientId;
    private boolean cleanSession;
    private boolean isConnected;
    private transient RemotingChannel remotingChannel;
    private int heartbeatInterval;
    private volatile long lastUpdateTimestamp;
    private int version;
    private LanguageCode language;
    private String accessKey;
    private String mqttBridgeAddr;
    private WillMessage willMessage;
    private ConcurrentHashMap<String, MqttSubscriptionData> mqttSubscriptionDataTable = new ConcurrentHashMap<>();

    private AtomicInteger inflightSlots;
    private final ConcurrentHashMap<Integer, Message> inflightWindow = new ConcurrentHashMap<>();
    private Hashtable<Integer, Integer> inUsePacketIds = new Hashtable();
    private final ConcurrentLinkedQueue<Message> sessionQueue = new ConcurrentLinkedQueue<>();
    private int nextPacketId = 0;

    public MQTTSession() {
    }

    public MQTTSession(String clientId, boolean isConnected, boolean cleanSession,
        RemotingChannel remotingChannel, long lastUpdateTimestamp, String accessKey,
        String mqttBridgeAddr, int heartbeatInterval, MqttBridgeController mqttBridgeController) {
        this.clientId = clientId;
        this.isConnected = isConnected;
        this.cleanSession = cleanSession;
        this.remotingChannel = remotingChannel;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
        this.accessKey = accessKey;
        this.mqttBridgeAddr = mqttBridgeAddr;
        this.heartbeatInterval = heartbeatInterval;
        this.mqttBridgeController = mqttBridgeController;
        this.inflightSlots = new AtomicInteger(mqttBridgeController.getMqttBridgeConfig().getMaxInflight());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MQTTSession)) {
            return false;
        }
        MQTTSession client = (MQTTSession) o;
        return Objects.equals(this.getClientId(), client.getClientId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getClientId());
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public RemotingChannel getRemotingChannel() {
        return remotingChannel;
    }

    public void setRemotingChannel(RemotingChannel remotingChannel) {
        this.remotingChannel = remotingChannel;
    }

    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
        this.lastUpdateTimestamp = lastUpdateTimestamp;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LanguageCode getLanguage() {
        return language;
    }

    public void setLanguage(LanguageCode language) {
        this.language = language;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getMqttBridgeAddr() {
        return mqttBridgeAddr;
    }

    public void setMqttBridgeAddr(String mqttBridgeAddr) {
        this.mqttBridgeAddr = mqttBridgeAddr;
    }

    public ConcurrentHashMap<String, MqttSubscriptionData> getMqttSubscriptionDataTable() {
        return mqttSubscriptionDataTable;
    }

    public void setMqttSubscriptionDataTable(
        ConcurrentHashMap<String, MqttSubscriptionData> mqttSubscriptionDataTable) {
        this.mqttSubscriptionDataTable = mqttSubscriptionDataTable;
    }

    public void pushMessageQos0(MqttFixedHeader fixedHeader, MqttPublishVariableHeader mqttPublishVariableHeader,
        byte[] body) {
        MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(fixedHeader, mqttPublishVariableHeader, Unpooled.copiedBuffer(body));
        pushMessage2Client(mqttPublishMessage);
    }

    public synchronized void pushMessageQos1(MqttFixedHeader fixedHeader, Message message) {
        MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
        MqttPublishVariableHeader variableHeader;
        String key = new StringBuilder().append(message.getBrokerName()).append(TOPIC_CLIENTID_SEPARATOR).append(MqttUtil.getRootTopic(message.getTopicName())).append(TOPIC_CLIENTID_SEPARATOR).append(this.clientId).toString();
        if (alreadyInFlight(key, message.getQueueOffset())) {
            log.info("The message is already inflight. key={}, queueOffset={}", key, message.getQueueOffset());
            return;
        }
        if (this.inflightSlots.get() <= 0) {
            log.info("Inflight window is full, put message to queue. ClientId={}", this.clientId);
            //如果inflight windows满的话，将message放入queue
            sessionQueue.add(message);
            log.info("Add msg into sessionQueue. size={}", sessionQueue.size());
            return;
        }
        log.debug("Inflightslots={}, InflightWindow size={}", this.inflightSlots.get(), this.inflightWindow.size());
        inflightSlots.decrementAndGet();
        iotClientManager.put2processTable(key, message);
        variableHeader = new MqttPublishVariableHeader(message.getTopicName(), getNextPacketId());
        inflightWindow.put(variableHeader.packetId(), message);
        iotClientManager.getInflightTimeouts().add(new InFlightPacket(this, variableHeader.packetId(), this.mqttBridgeController.getMqttBridgeConfig().getMsgFlyTimeBeforeResend()));
        log.debug("Send message -------------ClientId={}, topic={}, packetId={}, queueOffset={}, content={}", this.getClientId(), message.getTopicName(), variableHeader.packetId(),
            message.getQueueOffset(), new String(message.getBody()));
        pushMessage2Client(new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.copiedBuffer(message.getBody())));

    }

    public void pushRetainAndWillMessage(MqttFixedHeader fixedHeader, String topicName, byte[] body) {
        if (fixedHeader.qosLevel().equals(MqttQoS.AT_MOST_ONCE)) {
            MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topicName, 0);
            MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.copiedBuffer(body));
            pushMessage2Client(mqttPublishMessage);
        } else {
            if (inflightSlots.get() > 0) {
                inflightSlots.decrementAndGet();
                MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topicName, getNextPacketId());
                inflightWindow.put(variableHeader.packetId(), new Message(topicName, fixedHeader.qosLevel().value(), null, null, body));
                MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager();
                iotClientManager.getInflightTimeouts().add(new InFlightPacket(this, variableHeader.packetId(), this.mqttBridgeController.getMqttBridgeConfig().getMsgFlyTimeBeforeResend()));
                pushMessage2Client(new MqttPublishMessage(fixedHeader, variableHeader, Unpooled.copiedBuffer(body)));
            }
        }
    }

    public Message pubAckReceived(int ackPacketId) {
        Message remove = inflightWindow.remove(ackPacketId);
        if (remove == null) {
            log.info("Inflight message has already been removed from inflight window. packetId={}, clientId={}", ackPacketId, this.clientId);
            return null;
        }
        inflightSlots.incrementAndGet();
        ((MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager()).getInflightTimeouts().remove(new InFlightPacket(this, ackPacketId, 0));
        releasePacketId(ackPacketId);
        if (remove.getBrokerName() == null) {
            return remove;
        }
        ConcurrentHashMap<String, TreeMap<Long, Message>> processTable = ((MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager()).getProcessTable();
        ConcurrentHashMap<String, Long[]> consumeOffsetTable = ((MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager()).getConsumeOffsetTable();
        PersistService persistService = this.mqttBridgeController.getPersistService();
        String offsetKey = new StringBuilder().append(remove.getBrokerName()).append(TOPIC_CLIENTID_SEPARATOR).append(MqttUtil.getRootTopic(remove.getTopicName())).append(TOPIC_CLIENTID_SEPARATOR).append(this.clientId).toString();
        if (processTable.containsKey(offsetKey)) {
            TreeMap<Long, Message> treeMap = processTable.get(offsetKey);
            if (treeMap != null) {
                synchronized (this) {
                    handleOfflineMessagesOffset(consumeOffsetTable, offsetKey, persistService);
                    Long[] consumeOffset = consumeOffsetTable.get(offsetKey);
                    Message message = treeMap.remove(remove.getQueueOffset());
                    if (message != null) {
                        if (consumeOffset[1] < remove.getQueueOffset()) {
                            log.debug("currentMaxOffset changed. Before:{}, remove.queueOffset={}", consumeOffset[1], remove.getQueueOffset());
                            consumeOffset[1] = remove.getQueueOffset();
                            log.debug("currentMaxOffset changed. After:{}, remove.queueOffset={}", consumeOffset[1], remove.getQueueOffset());
                        }
                    }
                    consumeOffset[0] = treeMap.size() > 0 ? treeMap.firstKey() : consumeOffset[1] + 1;
                    log.debug("Message acked. QueueOffset={}, pakcetId={}, content={}, treeMap.firstKey={}, consumeOffset={}", remove.getQueueOffset(),
                        ackPacketId, new String(remove.getBody()), treeMap.size() > 0 ? treeMap.firstKey() : "empty", consumeOffset);
                }
            }
        }
        return remove;
    }

    public void handleOfflineMessagesOffset(ConcurrentHashMap<String, Long[]> offsetTable,
        String offsetKey, PersistService persistService) {
        if (!offsetTable.containsKey(offsetKey)) {
            Long[] offsets = new Long[2];
            offsets[0] = persistService.queryConsumeOffset(offsetKey);
            offsets[1] = offsets[0] - 1;
            Long[] prev = offsetTable.putIfAbsent(offsetKey, offsets);
            if (prev != null) {
                log.info("[handleOffsetMessagesOffset]. Item already exist. offsetKey={}, offset={}", offsetKey, offsetTable.get(offsetKey));
            }
        }
    }

    public void pushMessage2Client(MqttPublishMessage mqttPublishMessage) {
        try {
            RemotingChannel remotingChannel = this.getRemotingChannel();
            this.mqttBridgeController.getMqttRemotingServer().push(remotingChannel, mqttPublishMessage, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        } catch (Exception ex) {
            log.warn("Exception was thrown when pushing MQTT message. Topic: {}, clientId:{}, exception={}", mqttPublishMessage.variableHeader().topicName(), this.getClientId(), ex.getMessage());
        }
    }

    public void drainFromQueue() {
        if (this.sessionQueue.isEmpty() || this.inflightSlots.get() <= 0) {
            return;
        }
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        while (!sessionQueue.isEmpty() && this.inflightSlots.get() > 0) {
            Message msg = sessionQueue.poll();
            log.debug("drain message from sessionQueue to inflight window. message={}", msg.toString());
            if (msg != null) {
                pushMessageQos1(fixedHeader, msg);
            }
        }
    }

    public void clearSessionQueue() {
        this.sessionQueue.clear();
    }

    private boolean alreadyInFlight(String key, Long queueOffset) {
        ConcurrentHashMap<String, TreeMap<Long, Message>> processTable = ((MqttClientManagerImpl) this.mqttBridgeController.getMqttClientManager()).getProcessTable();
        if (processTable.containsKey(key)) {
            TreeMap<Long, Message> treeMap = processTable.get(key);
            if (treeMap != null && treeMap.get(queueOffset) != null) {
                return true;
            }
        }
        return false;
    }

    private synchronized void releasePacketId(int msgId) {
        this.inUsePacketIds.remove(new Integer(msgId));
    }

    private synchronized int getNextPacketId() {
        int startingMessageId = this.nextPacketId;
        int loopCount = 0;

        do {
            ++this.nextPacketId;
            if (this.nextPacketId > 65535) {
                this.nextPacketId = 1;
            }

            if (this.nextPacketId == startingMessageId) {
                ++loopCount;
                if (loopCount == 2) {
                    throw new MqttRuntimeException("Could not get available packetId.");
                }
            }
        }
        while (this.inUsePacketIds.containsKey(new Integer(this.nextPacketId)));

        Integer id = new Integer(this.nextPacketId);
        this.inUsePacketIds.put(id, id);
        return this.nextPacketId;
    }

    public AtomicInteger getInflightSlots() {
        return inflightSlots;
    }

    public Map<Integer, Message> getInflightWindow() {
        return inflightWindow;
    }

    public Hashtable getInUsePacketIds() {
        return inUsePacketIds;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public MqttBridgeController getMqttBridgeController() {
        return mqttBridgeController;
    }

    public void setMqttBridgeController(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    public WillMessage getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(WillMessage willMessage) {
        this.willMessage = willMessage;
    }

    @Override public String toString() {
        return "MQTTSession{" +
            "ownerUserId='" + ownerUserId + '\'' +
            ", clientId='" + clientId + '\'' +
            ", cleanSession=" + cleanSession +
            ", isConnected=" + isConnected +
            ", remotingChannel=" + remotingChannel +
            ", heartbeatInterval=" + heartbeatInterval +
            ", lastUpdateTimestamp=" + lastUpdateTimestamp +
            ", version=" + version +
            ", language=" + language +
            ", accessKey='" + accessKey + '\'' +
            ", mqttBridgeAddr='" + mqttBridgeAddr + '\'' +
            ", mqttSubscriptionDataTable=" + mqttSubscriptionDataTable +
            '}';
    }
}
