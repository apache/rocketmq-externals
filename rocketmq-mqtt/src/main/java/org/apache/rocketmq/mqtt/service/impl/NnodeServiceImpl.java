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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.namesrv.TopAddressing;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.mqtt.protocol.MqttBridgeTableInfo;
import org.apache.rocketmq.mqtt.protocol.header.UnregisterMqttRequestHeader;
import org.apache.rocketmq.common.protocol.header.namesrv.GetRouteInfoRequestHeader;
import org.apache.rocketmq.mqtt.protocol.header.RegisterMqttRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.common.MqttBridgeConfig;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.service.NnodeService;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.remoting.protocol.RemotingSysResponseCode.SUCCESS;

public class NnodeServiceImpl implements NnodeService {
    private static final Logger log = LoggerFactory.getLogger(NnodeServiceImpl.class);
    private final TopAddressing topAddressing = new TopAddressing(MixAll.getWSAddr());
    private String nameSrvAddr = null;
    private MqttBridgeController mqttBridgeController;
    private ConcurrentHashMap<String /*Topic*/, TopicRouteData> topicRouteDataMap = new ConcurrentHashMap<>(1000);
    private ClusterInfo clusterInfo;

    public NnodeServiceImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    public String getMqttNodeAddress() {
        return this.mqttBridgeController.getMqttBridgeConfig().getMqttBridgeIP() + ":" + this.mqttBridgeController.getMultiNettyServerConfig().getMqttListenPort();
    }

    @Override
    public void registerMqttNode(MqttBridgeConfig mqttBridgeConfig) throws Exception {
        List<String> nnodeAddressList = this.mqttBridgeController.getMqttRemotingClient().getNameServerAddressList();
        RegisterMqttRequestHeader requestHeader = new RegisterMqttRequestHeader();
        requestHeader.setMqttBridgeIP(mqttBridgeConfig.getMqttBridgeIP());
        requestHeader.setListenPort(this.mqttBridgeController.getMultiNettyServerConfig().getListenPort());
        requestHeader.setMqttListenPort(this.mqttBridgeController.getMultiNettyServerConfig().getMqttListenPort());
        requestHeader.setMqttBridgeName(mqttBridgeConfig.getMqttBridgeName());
        requestHeader.setClusterName(this.mqttBridgeController.getMqttBridgeConfig().getClusterName());
        RemotingCommand remotingCommand = RemotingCommand.createRequestCommand(RequestCode.REGISTER_MQTT, requestHeader);
        if (nnodeAddressList != null && nnodeAddressList.size() > 0) {
            for (String nodeAddress : nnodeAddressList) {
                try {
                    this.mqttBridgeController.getMqttRemotingClient().invokeSync(nodeAddress, remotingCommand, MqttConstant.HEARTBEAT_TIME_OUT);
                } catch (Exception ex) {
                    log.warn("Register Mqtt Bridge to Nnode addr: {} error, ex:{} ", nodeAddress, ex);
                }
            }
        } else {
            log.warn("Nnode server list is null");
            throw new RemotingSendRequestException("Nnode server list is null");
        }
    }

    @Override public void unregisterMqttNode(MqttBridgeConfig mqttBridgeConfig) {
        List<String> nnodeAddressList = this.mqttBridgeController.getMqttRemotingClient().getNameServerAddressList();
        UnregisterMqttRequestHeader requestHeader = new UnregisterMqttRequestHeader();
        requestHeader.setMqttBridgeName(mqttBridgeConfig.getMqttBridgeName());
        RemotingCommand remotingCommand = RemotingCommand.createRequestCommand(RequestCode.UNREGISTER_MQTT, requestHeader);
        if (nnodeAddressList != null && nnodeAddressList.size() > 0) {
            for (String nodeAddress : nnodeAddressList) {
                try {
                    this.mqttBridgeController.getMqttRemotingClient().invokeSync(nodeAddress, remotingCommand, MqttConstant.HEARTBEAT_TIME_OUT);
                    log.info("Unregister Mqtt Bridge OK, Nnode addr: {}", nodeAddress);
                } catch (Exception ex) {
                    log.warn("Unregister Mqtt Bridge to Nnode addr: {} error, ex:{} ", nodeAddress, ex);
                }
            }
        }
    }

    @Override
    public void updateTopicRouteDataByTopic() {
        Set<String> topSet = topicRouteDataMap.keySet();
        for (String topic : topSet) {
            try {
                TopicRouteData topicRouteData = getTopicRouteDataByTopicFromNnode(topic, false);
                if (topicRouteData == null) {
                    topicRouteDataMap.remove(topic);
                    continue;
                }
                topicRouteDataMap.put(topic, topicRouteData);
            } catch (Exception ex) {
                log.error("Update topic {} error: {}", topic, ex);
            }
        }
    }

    private TopicRouteData getTopicRouteDataByTopicFromNnode(String topic,
        boolean allowTopicNotExist) throws MQClientException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        GetRouteInfoRequestHeader requestHeader = new GetRouteInfoRequestHeader();
        requestHeader.setTopic(topic);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ROUTEINTO_BY_TOPIC, requestHeader);
        RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(null, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        log.info("GetTopicRouteInfoFromNameServer response: " + response);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.TOPIC_NOT_EXIST: {
                if (allowTopicNotExist && !topic.equals(MixAll.AUTO_CREATE_TOPIC_KEY_TOPIC)) {
                    log.warn("Topic [{}] RouteInfo is not exist value", topic);
                }
                break;
            }
            case ResponseCode.SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return TopicRouteData.decode(body, TopicRouteData.class);
                }
            }
            default:
                break;
        }

        throw new MQClientException(response.getCode(), response.getRemark());
    }

    @Override
    public TopicRouteData getTopicRouteDataByTopic(
        String topic,
        boolean allowTopicNotExist) throws MQClientException, InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        if (topic == null || "".equals(topic)) {
            return null;
        }

        TopicRouteData topicRouteData = topicRouteDataMap.get(topic);
        if (topicRouteData == null) {
            topicRouteData = getTopicRouteDataByTopicFromNnode(topic, allowTopicNotExist);
            if (topicRouteData != null) {
                topicRouteDataMap.put(topic, topicRouteData);
            }
        }
        return topicRouteData;
    }

    @Override
    public MqttBridgeTableInfo getMqttBridgeTableInfo() throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_MQTTBRIDGETABLEINFO, null);

        RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(null, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        assert response != null;
        switch (response.getCode()) {
            case SUCCESS: {
                byte[] body = response.getBody();
                if (body != null) {
                    return MqttBridgeTableInfo.decode(body, MqttBridgeTableInfo.class);
                }
            }
            default:
                break;
        }
        throw new MQClientException(response.getCode(), response.getRemark());
    }

    @Override
    public void updateNnodeAddressList(final String addrs) {
        List<String> list = new ArrayList<String>();
        String[] addrArray = addrs.split(";");
        for (String addr : addrArray) {
            list.add(addr);
        }
        this.mqttBridgeController.getMqttRemotingClient().updateNameServerAddressList(list);
    }

    @Override
    public String fetchNnodeAdress() {
        try {
            String addrs = this.topAddressing.fetchNSAddr();
            if (addrs != null) {
                if (!addrs.equals(this.nameSrvAddr)) {
                    log.info("Nnode server address changed, old: {} new: {}", this.nameSrvAddr, addrs);
                    this.updateNnodeAddressList(addrs);
                    this.nameSrvAddr = addrs;
                    return nameSrvAddr;
                }
            }
        } catch (Exception e) {
            log.error("FetchNnodeServerAddr Exception", e);
        }
        return nameSrvAddr;
    }

    @Override
    public void updateEnodeClusterInfo() throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException {
        synchronized (this) {
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_BROKER_CLUSTER_INFO, null);
            RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(null, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
            switch (response.getCode()) {
                case ResponseCode.SUCCESS: {
                    this.clusterInfo = ClusterInfo.decode(response.getBody(), ClusterInfo.class);
                    this.mqttBridgeController.getEnodeService().updateEnodeAddress(clusterInfo, this.mqttBridgeController.getMqttBridgeConfig().getClusterName());
                    break;
                }
                default:
                    log.error("Update Cluster info error: {}", response);
                    break;
            }
        }
    }

    @Override
    public Set<String> getEnodeNames(String clusterName) {
        if (this.clusterInfo == null || clusterInfo.getBrokerAddrTable().size() == 0) {
            try {
                updateEnodeClusterInfo();
            } catch (Exception ex) {
                log.error("Update Cluster info error:{}", ex);
            }
        }
        return this.clusterInfo.getClusterAddrTable().get(clusterName);
    }

    @Override
    public String getAddressByEnodeName(String enodeName,
        boolean isUseSlave) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException {
        if (this.clusterInfo == null) {
            updateEnodeClusterInfo();
        }
        if (this.clusterInfo != null) {
            BrokerData brokerData = this.clusterInfo.getBrokerAddrTable().get(enodeName);
            if (brokerData != null) {
                return brokerData.getBrokerAddrs().get(MixAll.MASTER_ID);
            }
        }
        return null;
    }

    @Override
    public ClusterInfo getEnodeClusterInfo() throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        if (clusterInfo == null) {
            updateEnodeClusterInfo();
        }
        return clusterInfo;
    }
}
