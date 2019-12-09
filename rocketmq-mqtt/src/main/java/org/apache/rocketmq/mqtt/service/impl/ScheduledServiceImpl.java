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
package org.apache.rocketmq.mqtt.service.impl;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.heartbeat.HeartbeatData;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.client.InFlightPacket;
import org.apache.rocketmq.mqtt.client.MQTTSession;
import org.apache.rocketmq.mqtt.client.MqttClientManagerImpl;
import org.apache.rocketmq.mqtt.common.Message;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.protocol.MqttBridgeData;
import org.apache.rocketmq.mqtt.service.ScheduledService;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.apache.rocketmq.mqtt.constant.MqttConstant.TOPIC_CLIENTID_SEPARATOR;

public class ScheduledServiceImpl implements ScheduledService {
    private static final Logger log = LoggerFactory.getLogger(ScheduledServiceImpl.class);

    private MqttBridgeController mqttBridgeController;

    private MqttBridgeConfig mqttBridgeConfig;

    private final RemotingCommand enodeHeartbeat;

    public ScheduledServiceImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
        this.mqttBridgeConfig = mqttBridgeController.getMqttBridgeConfig();
        enodeHeartbeat = RemotingCommand.createRequestCommand(RequestCode.HEART_BEAT, null);
        HeartbeatData heartbeatData = new HeartbeatData();
        heartbeatData.setClientID(mqttBridgeConfig.getMqttBridgeName());
        enodeHeartbeat.setBody(heartbeatData.encode());
    }

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "MqttBridgeScheduledThread");
        }
    });

    @Override
    public void startScheduleTask() {

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttBridgeController.getEnodeService().sendHeartbeat(enodeHeartbeat);
                } catch (Exception e) {
                    log.error("ScheduledTask sendHeartbeat to Enode exception", e);
                }
            }
        }, 0, this.mqttBridgeConfig.getMqttHeartBeatInterval(), TimeUnit.MILLISECONDS);

        if (mqttBridgeConfig.isFetchNamesrvAddrByAddressServer()) {
            this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        mqttBridgeController.getNnodeService().fetchNnodeAdress();
                    } catch (Throwable e) {
                        log.error("ScheduledTask fetchNameServerAddr exception", e);
                    }
                }
            }, 1000 * 10, 1000 * 60 * 2, TimeUnit.MILLISECONDS);
        }

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttBridgeController.getNnodeService().registerMqttNode(mqttBridgeConfig);
                } catch (Exception ex) {
                    log.warn("Register mqtt error", ex);
                }
            }
        }, 0, Math.max(10000, Math.min(mqttBridgeConfig.getRegisterNameServerPeriod(), 60000)), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttBridgeController.getNnodeService().updateEnodeClusterInfo();
                } catch (Exception ex) {
                    log.warn("Update broker addr error:{}", ex);
                }
            }
        }, 0, Math.max(10000, Math.min(mqttBridgeConfig.getUpdateEnodeClusterInfoPeriod(), 60000)), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    mqttBridgeController.getNnodeService().updateTopicRouteDataByTopic();
                } catch (Exception ex) {
                    log.warn("Update broker addr error:{}", ex);
                }
            }
        }, 0, Math.max(10000, Math.min(mqttBridgeConfig.getRegisterNameServerPeriod(), 60000)), TimeUnit.MILLISECONDS);

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
                    log.debug("channelClientMap size={}, clientIdChannelMap size={}", iotClientManager.getChannelClientTable().size(), iotClientManager.getClientIdChannelTable().size());
                    ConcurrentHashMap<String, Long[]> consumeOffsetTable = iotClientManager.getConsumeOffsetTable();
                    for (Map.Entry<String, Long[]> entry : consumeOffsetTable.entrySet()) {
                        String key = entry.getKey();
                        Long[] offset = entry.getValue();
                        String clientId = key.substring(key.lastIndexOf(TOPIC_CLIENTID_SEPARATOR) + 1);
                        if (iotClientManager.getChannel(clientId) != null) {
                            MQTTSession client = iotClientManager.getClient(iotClientManager.getChannel(clientId));
                            if (client != null && !client.isCleanSession() && client.isConnected()) {
                                mqttBridgeController.getPersistService().updateConsumeOffset(key, offset[0]);
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.error("Update consumeOffset error:{}", ex);
                }
//                log.info("Update consumeOffset success. consumeOffsetTable={}", consumeOffsetTable);
            }
        }, 0, mqttBridgeController.getMqttBridgeConfig().getPersistOffsetInterval(), TimeUnit.MILLISECONDS);

        //scan ackTimeout messages and resend
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
                    Collection<InFlightPacket> expired = new ArrayList<>();
                    iotClientManager.getInflightTimeouts().drainTo(expired);
                    for (InFlightPacket notAcked : expired) {
                        MQTTSession client = notAcked.getClient();
                        if (!client.isConnected()) {
                            log.info("The client is not online now, remove notAcked message. InFlightPacket={}, clientId={}", notAcked, client.getClientId());
                            iotClientManager.getInflightTimeouts().remove(notAcked);
                            continue;
                        }
                        if (notAcked.getResendTime() == 3) {
                            log.info("The resend time has reached max. Remove the message and close the connection. InFlightPacket={}, clientId={}", notAcked, client.getClientId());
                            iotClientManager.getInflightTimeouts().remove(notAcked);
                            client.setConnected(false);
//                            iotClientManager.onClose(client.getRemotingChannel());
                            client.getRemotingChannel().close();
                            continue;
                        }
                        if (client.getInflightWindow().containsKey(notAcked.getPacketId())) {
                            log.info("Begin resend notAcked message. InFlightPacket={}, clientId={}", notAcked, client.getClientId());
                            Message inFlightMessage = client.getInflightWindow().get(notAcked.getPacketId());
                            MqttPublishMessage mqttPublishMessage = new MqttPublishMessage(
                                    new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.valueOf(inFlightMessage.getPushQos()), false, 0),
                                    new MqttPublishVariableHeader(inFlightMessage.getTopicName(), notAcked.getPacketId()), Unpooled.copiedBuffer(inFlightMessage.getBody()));

                            notAcked.setStartTime(System.currentTimeMillis() + mqttBridgeController.getMqttBridgeConfig().getMsgFlyTimeBeforeResend());
                            notAcked.setResendTime(notAcked.getResendTime() + 1);
                            iotClientManager.getInflightTimeouts().add(notAcked);
                            client.pushMessage2Client(mqttPublishMessage);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Scan ack timeout error:{}", ex);
                }
            }
        }, 10000, mqttBridgeController.getMqttBridgeConfig().getScanAckTimeoutInterval(), TimeUnit.MILLISECONDS);

        // create internal transfer-messages clients
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                MqttClientManagerImpl iotClientManager = (MqttClientManagerImpl) mqttBridgeController.getMqttClientManager();
                ConcurrentHashMap<String, MqttAsyncClient> mqttBridge2MqttClents = iotClientManager.getMqttBridge2MqttClient();
                try {
                    Set<Map.Entry<String, MqttBridgeData>> liveMqttBridgesEntryset = mqttBridgeController.getNnodeService().getMqttBridgeTableInfo().getMqttBridgeTable().entrySet();
                    Set<String> liveMqttBridgeIPs = liveMqttBridgesEntryset.stream().map(e -> e.getValue().getMqttBridgeIP()).collect(Collectors.toSet());
                    iotClientManager.setLiveMqttBridgeIps(liveMqttBridgeIPs);
                    Iterator<Map.Entry<String, MqttAsyncClient>> mqttBridge2MqttClientsIt = mqttBridge2MqttClents.entrySet().iterator();
                    while (mqttBridge2MqttClientsIt.hasNext()) {
                        Map.Entry<String, MqttAsyncClient> entry = mqttBridge2MqttClientsIt.next();
                        if (liveMqttBridgeIPs.contains(entry.getKey())) {
                            continue;
                        } else {
                            String clientId = new StringBuilder().append(mqttBridgeConfig.getMqttBridgeIP()).
                                    append("->").append(entry.getKey()).toString();
                            try {
                                entry.getValue().disconnectForcibly();
                                entry.getValue().close(true);
                            } catch (MqttException e) {
                                log.error("Client [{}] disconnect or close forcibly failed, error: {}", clientId, e);
                                continue;
                            }
                            mqttBridge2MqttClientsIt.remove();
                            log.info("Delete client [{}] connected to dead mqttBridge", clientId);
                        }
                    }

                    Iterator<Map.Entry<String, MqttBridgeData>> liveMqttBridgesIterator = liveMqttBridgesEntryset.iterator();
                    while (liveMqttBridgesIterator.hasNext()) {
                        Map.Entry<String, MqttBridgeData> entry = liveMqttBridgesIterator.next();
                        if (mqttBridge2MqttClents.containsKey(entry.getValue().getMqttBridgeIP()) || entry.getValue().getMqttBridgeIP().equals(mqttBridgeConfig.getMqttBridgeIP())) {
                            continue;
                        } else {
                            iotClientManager.createOrGetTransferMsgClient(entry.getValue().getMqttBridgeIP());
                        }
                    }

                } catch (InterruptedException | RemotingTimeoutException | RemotingSendRequestException | RemotingConnectException | MQClientException e) {
                    log.error("Get MqttBridge Table Info failed. Error:{}", e.getMessage());
                } catch (MqttException e) {
                    log.error("Create internal transfer-messages client failed. Error: {}", e.getMessage());
                }

            }
        }, 10000, 120000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if (this.scheduledExecutorService != null) {
            this.scheduledExecutorService.shutdown();
        }
    }
}
