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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.mqtt.protocol.MqttBridgeData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.common.RemotingChannel;
import org.apache.rocketmq.mqtt.common.TransferData;
import org.apache.rocketmq.mqtt.persistence.service.PersistService;
import org.apache.rocketmq.mqtt.utils.MqttUtil;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class MqttClientManagerImpl implements MqttClientManager {

    private static final Logger log = LoggerFactory.getLogger(MqttClientManagerImpl.class);

    public static final String IOT_GROUP = "IOT_GROUP";
    private final ConcurrentHashMap<RemotingChannel, MQTTSession> channelClientTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/*clientId*/, RemotingChannel> clientIdChannelTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/*root topic*/, Set<MQTTSession>> topic2Clients = new ConcurrentHashMap<>(
        1024);
    private final ConcurrentHashMap<String/*mqttBridge ip*/, MqttAsyncClient> mqttBridge2MqttClient = new ConcurrentHashMap<>();
    private Set<String> liveMqttBridgeIps;
    private final ConcurrentHashMap<String /*broker^rootTopic^clientId*/, TreeMap<Long/*queueOffset*/, Message>> processTable = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /*broker^rootTopic^clientId*/, Long[]> consumeOffsetTable = new ConcurrentHashMap<>();
    private final DelayQueue<InFlightPacket> inflightTimeouts = new DelayQueue<>();
    private final ScheduledExecutorService scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor(
            new ThreadFactoryImpl("ClientHousekeepingScheduledThread"));
    private final MqttBridgeController mqttBridgeController;
    private final PersistService persistService;

    public MqttClientManagerImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
        this.persistService = mqttBridgeController.getPersistService();
    }

    @Override public boolean register(MQTTSession client) {

        if (client != null) {
            RemotingChannel remotingChannel = client.getRemotingChannel();
            MQTTSession oldClient = channelClientTable.get(remotingChannel);
            if (oldClient == null) {
                channelClientTable.put(remotingChannel, client);
                clientIdChannelTable.put(client.getClientId(), remotingChannel);
                log.info("New client connected, client: {}", client.toString());
                oldClient = client;
            } else {
                if (!oldClient.getClientId().equals(client.getClientId())) {
                    log.error(
                        "[BUG] client channel exist, but clientId not equal. OLD: {} NEW: {} ",
                        oldClient.toString(),
                        client.toString());
                    channelClientTable.put(remotingChannel, client);
                    clientIdChannelTable.put(client.getClientId(), remotingChannel);
                }
            }
            oldClient.setLastUpdateTimestamp(System.currentTimeMillis());
            persistClient(client);
            onRegister(remotingChannel);
        }
        log.debug("Register client: lastUpdateTimestamp: {}", client.getLastUpdateTimestamp());
        return true;
    }

    @Override public void unRegister(RemotingChannel remotingChannel) {
        removeClient(remotingChannel);
        onUnregister(remotingChannel);
    }

    /**
     * 在Channel关闭前清理数据
     *
     * @param remotingChannel
     */
    @Override public void onClose(RemotingChannel remotingChannel) {
        //remove client after invoking onClosed method(client may be used in onClosed)
        onClosed(remotingChannel);
        removeClient(remotingChannel);
    }

    @Override public void onException(RemotingChannel remotingChannel) {

    }

    @Override public void onIdle(RemotingChannel remotingChannel) {

    }

    @Override public List<String> getAllClientId() {
        List<String> result = new ArrayList<String>();
        Iterator<Map.Entry<RemotingChannel, MQTTSession>> it = channelClientTable.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RemotingChannel, MQTTSession> entry = it.next();
            MQTTSession client = entry.getValue();
            result.add(client.getClientId());
        }
        return result;
    }

    @Override public MQTTSession getClient(RemotingChannel remotingChannel) {
        assert remotingChannel != null;
        return channelClientTable.get(remotingChannel);
    }

    @Override public RemotingChannel getChannel(String clientId) {
        return clientIdChannelTable.get(clientId);
    }

    @Override public void startScan(long interval) {
/*        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    scanExpiredChannel();
                } catch (Throwable e) {
                    log.error("Error occurred when scan not active client channels.", e);
                }
            }
        }, 1000 * 10, interval, TimeUnit.MILLISECONDS);*/
    }

    @Override public void shutdown() {
        Iterator<RemotingChannel> it = this.channelClientTable.keySet().iterator();
        while (it.hasNext()) {
            RemotingChannel channel = it.next();
            try {
                this.onClose(channel);
            } catch (Exception e) {
                log.error("[Shutdown invoked]Exception was thrown when closing connection. RemotingChannel={}", channel);
            }
        }

        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdown();
        }
    }

    public void onUnregister(RemotingChannel remotingChannel) {

    }

    public void onRegister(RemotingChannel remotingChannel) {

    }

    private void updateSessionAndMeasure(MQTTSession mqttSession) {
        MQTTSession client = mqttSession;
        if (client.isCleanSession()) {
            cleanSessionState(client);
        }
        Set<String> brokerNames = this.mqttBridgeController.getNnodeService().getEnodeNames(this.mqttBridgeController.getMqttBridgeConfig().getClusterName());
        Set<String> rootTopics = client.getMqttSubscriptionDataTable().keySet().stream().map(MqttUtil::getRootTopic).collect(Collectors.toSet());
        Set<String> rootTopicsBefore = new HashSet<>(this.topic2Clients.keySet());
        //清理topic2Clients/consumeOffsetTable/processTable中相关数据
        removeFromTables(brokerNames, rootTopics, client);
        //clear sessionQueue
        client.clearSessionQueue();
        Set<String> rootTopicsAfter = new HashSet<>(this.topic2Clients.keySet());
        //更新Redis:rootTopic---->Set<MqttBridge>
        this.mqttBridgeController.getPersistService().deleteRootTopic2MqttBridge(rootTopicsBefore, rootTopicsAfter, this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP());
    }

    public void addTopic2Client(MQTTSession client, String rootTopic) {
        if (this.topic2Clients.containsKey(rootTopic)) {
            this.topic2Clients.getOrDefault(rootTopic, new HashSet<>()).add(client);
        } else {
            HashSet<MQTTSession> clients = new HashSet<>();
            clients.add(client);
            Set<MQTTSession> old = this.topic2Clients.putIfAbsent(rootTopic, clients);
            if (old != null) {
                old.add(client);
            }
        }
    }

    public void transferMessage(String mqttBridgeAddress, TransferData transferData) throws MqttException {

        MqttAsyncClient client;
        if (mqttBridge2MqttClient.containsKey(mqttBridgeAddress)) {
            client = mqttBridge2MqttClient.get(mqttBridgeAddress);
        } else {
            client = this.createOrGetTransferMsgClient(mqttBridgeAddress);
        }
        org.eclipse.paho.client.mqttv3.MqttMessage message = new org.eclipse.paho.client.mqttv3.MqttMessage(transferData.encode());

        client.publish(transferData.getTopic(), message);

    }

    public synchronized MqttAsyncClient createOrGetTransferMsgClient(String mqttBridgeAddress) throws MqttException {

        if (mqttBridge2MqttClient.containsKey(mqttBridgeAddress)) {
            return mqttBridge2MqttClient.get(mqttBridgeAddress);
        }
        MqttAsyncClient client;
        MqttBridgeConfig mqttBridgeConfig = mqttBridgeController.getMqttBridgeConfig();
        String url = "tcp://" + mqttBridgeAddress + ":" + (this.mqttBridgeController.getMultiNettyServerConfig().getMqttListenPort() - 1);
        String clientId = new StringBuilder().append("GID_Internal@@@").append(mqttBridgeConfig.getMqttBridgeIP().replaceAll("\\.", "_")).append("-").append(mqttBridgeAddress.replaceAll("\\.", "_")).toString();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttAsyncClient(url, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setAutomaticReconnect(true);
        connOpts.setMaxReconnectDelay(2000);
        connOpts.setMaxInflight(mqttBridgeConfig.getInternalMaxInFlight());
        // set disconnectedBuffer
        DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
        disconnectedBufferOptions.setBufferEnabled(true);
        disconnectedBufferOptions.setBufferSize(1000 * 16);
        disconnectedBufferOptions.setDeleteOldestMessages(true);
        client.setBufferOpts(disconnectedBufferOptions);

        client.connect(connOpts);
        mqttBridge2MqttClient.putIfAbsent(mqttBridgeAddress, client);

        log.info("Create internal transfer-message client:{} ", clientId);
        return client;
    }

    public void put2processTable(String key, Message message) {
        if (!processTable.containsKey(key)) {
            TreeMap<Long, Message> treeMap = new TreeMap<>();
            processTable.putIfAbsent(key, treeMap);
        }
        synchronized (processTable) {
            if (processTable.containsKey(key)) {
                processTable.get(key).put(message.getQueueOffset(), message);
            }
        }
    }

    private void deleteTopic2Client(MQTTSession client, String rootTopic) {
        if (this.topic2Clients.containsKey(rootTopic)) {
            Set<MQTTSession> mqttSessions = this.topic2Clients.get(rootTopic);
            mqttSessions.remove(client);
            if (mqttSessions.size() == 0) {
                topic2Clients.remove(rootTopic);
            }
        }
    }

    private void onClosed(RemotingChannel remotingChannel) {
        //do the logic when connection is closed by any reason.
        //step1. Clean subscription data if cleanSession=1
        MQTTSession client = this.getClient(remotingChannel);
        if (client == null) {
            log.error("No client associated with the remotingChannel: {} was found. Maybe already cleaned.", remotingChannel.toString());
            return;
        }

        //Publish will message associated with current connection
        if (client.getWillMessage() != null && client.getWillMessage().getWillTopic() != null && client.getWillMessage().getBody() != null && client.getWillMessage().getBody().length > 0) {
            this.mqttBridgeController.getWillMessageService().sendWillMessage(client);
            client.setWillMessage(null);
        }
        client.setConnected(false);
        updateSessionAndMeasure(client);
        if (!client.isCleanSession()) {
            //持久化连接仅更新client
            this.mqttBridgeController.getPersistService().updateOrAddClient(client);
        }
    }

    public void removeClient(RemotingChannel remotingChannel) {
        MQTTSession prev = channelClientTable.remove(remotingChannel);
        if (prev != null) {
            clientIdChannelTable.remove(prev.getClientId());
            log.info("Unregister client: {} success", prev);
        } else {
            log.error("No client associated with the remotingChannel: {} was found. Maybe already cleaned.", remotingChannel.toString());
        }
    }

    public void deletePersistConsumeOffset(MQTTSession client) {
        Set<String> brokerNames = this.mqttBridgeController.getNnodeService().getEnodeNames(this.mqttBridgeController.getMqttBridgeConfig().getClusterName());
        Set<String> rootTopics = client.getMqttSubscriptionDataTable().keySet().stream().map(t -> MqttUtil.getRootTopic(t)).collect(Collectors.toSet());
        if (brokerNames.size() == 0 || rootTopics.size() == 0) {
            return;
        }
        for (String brokerName : brokerNames) {
            for (String rootTopic : rootTopics) {
                String key = new StringBuilder().append(brokerName).append(TOPIC_CLIENTID_SEPARATOR).append(rootTopic).append(TOPIC_CLIENTID_SEPARATOR).append(client.getClientId()).toString();
                this.consumeOffsetTable.remove(key);
                this.persistService.deleteConsumeOffset(key);
            }
        }
    }

    private void persistClient(MQTTSession client) {
        this.persistService.addConnection(client);
    }

    private void removeFromTables(Set<String> brokerNames, Set<String> rootTopics, MQTTSession client) {
        for (String rootTopic : rootTopics) {
            //清除内存中rootTopic2Client的关系
            deleteTopic2Client(client, rootTopic);
            for (String brokerName : brokerNames) {
                String key = new StringBuilder().append(brokerName).append(TOPIC_CLIENTID_SEPARATOR).append(rootTopic).append(TOPIC_CLIENTID_SEPARATOR).append(client.getClientId()).toString();
                //清理consumeOffset（如果是持久化连接则更新到Redis）
                Long[] remove = this.consumeOffsetTable.remove(key);
                if (remove != null && !client.isCleanSession()) {
                    this.mqttBridgeController.getPersistService().updateConsumeOffset(key, remove[0]);
                }
                //清理processTable
                processTable.remove(key);
            }
        }
    }

    private void scanExpiredChannel() {
        Iterator iter = channelClientTable.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            MQTTSession client = (MQTTSession) entry.getValue();
            long interval = System.currentTimeMillis() - client.getLastUpdateTimestamp();
            if (interval > Math.round(client.getHeartbeatInterval() * 1.5f)) {
//                this.onClosed(client.getRemotingChannel());
                iter.remove();
                this.clientIdChannelTable.remove(client.getClientId());
                client.getRemotingChannel().close();
                log.warn(
                    "SCAN: Remove expired channel from channelClientTable. channel={}",
                    RemotingHelper.parseSocketAddressAddr(client.getRemotingChannel().remoteAddress()));
            }
        }
    }

    public void cleanSessionState(MQTTSession client) {
        persistService.deleteClient(client);
    }

    public ConcurrentHashMap<String/*root topic*/, Set<MQTTSession>> getTopic2Clients() {
        return topic2Clients;
    }

    public ConcurrentHashMap<String, MqttAsyncClient> getMqttBridge2MqttClient() {
        return mqttBridge2MqttClient;
    }

    public Set<String> getLiveMqttBridgeIps() {
        if (this.liveMqttBridgeIps == null) {
            try {
                Set<Map.Entry<String, MqttBridgeData>> liveMqttBridgesEntryset = mqttBridgeController.getNnodeService().getMqttBridgeTableInfo().getMqttBridgeTable().entrySet();
                this.liveMqttBridgeIps = liveMqttBridgesEntryset.stream().map(e -> e.getValue().getMqttBridgeIP()).collect(Collectors.toSet());
            } catch (Exception e) {
                log.error("Exception thrown when getMqttBridgeTable:{}", e);
            }
        }
        return this.liveMqttBridgeIps;
    }

    public void setLiveMqttBridgeIps(Set<String> liveMqttBridgeIps) {
        this.liveMqttBridgeIps = liveMqttBridgeIps;
    }

    public ConcurrentHashMap<String, TreeMap<Long, Message>> getProcessTable() {
        return processTable;
    }

    public ConcurrentHashMap<String, Long[]> getConsumeOffsetTable() {
        return this.consumeOffsetTable;
    }

    public DelayQueue<InFlightPacket> getInflightTimeouts() {
        return inflightTimeouts;
    }

    public ConcurrentHashMap<RemotingChannel, MQTTSession> getChannelClientTable() {
        return channelClientTable;
    }

    public ConcurrentHashMap<String, RemotingChannel> getClientIdChannelTable() {
        return clientIdChannelTable;
    }
}
