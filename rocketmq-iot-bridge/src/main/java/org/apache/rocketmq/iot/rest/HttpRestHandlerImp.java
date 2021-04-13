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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.iot.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.netty.channel.Channel;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.iot.common.configuration.MqttBridgeConfig;
import org.apache.rocketmq.iot.common.util.HttpAPIClient;
import org.apache.rocketmq.iot.common.util.MqttUtil;
import org.apache.rocketmq.iot.connection.client.ClientManager;
import org.apache.rocketmq.iot.protocol.mqtt.data.Subscription;
import org.apache.rocketmq.iot.rest.common.Connection;
import org.apache.rocketmq.iot.rest.common.ConnectionInfo;
import org.apache.rocketmq.iot.rest.common.ContextResponse;
import org.apache.rocketmq.iot.storage.subscription.SubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRestHandlerImp implements HttpRestHandler {
    private static final Logger logger = LoggerFactory.getLogger(HttpRestHandlerImp.class);

    private static Gson gson;
    private MqttBridgeConfig bridgeConfig;
    private ClientManager clientManager;
    private String brokerHost;
    private List<String> clusterHostList;
    private SubscriptionStore subscriptionStore;
    private Javalin javalin;

    public HttpRestHandlerImp(MqttBridgeConfig bridgeConfig, ClientManager clientManager,
        SubscriptionStore subscriptionStore) {
        this.gson = new GsonBuilder().create();
        this.bridgeConfig = bridgeConfig;
        this.clientManager = clientManager;
        this.brokerHost = bridgeConfig.getBrokerHost();
        this.clusterHostList = bridgeConfig.getHttpClusterHostList();
        this.subscriptionStore = subscriptionStore;
    }

    @Override public void start() {
        this.javalin = Javalin.create().start(bridgeConfig.getHttpPort());
        this.javalin.get("/mqtt/connection/num", this::getConnectionNum);
        this.javalin.get("/mqtt/connection/query", this::queryConnection);
    }

    private void getConnectionNum(Context context) {
        String mode = context.queryParam("mode");
        ContextResponse response = new ContextResponse<>();
        Connection localConnection = localConnectionNum();
        if (mode.equals("node")) {
            response.setData(localConnection);
        } else {
            ConnectionInfo connectionInfo = new ConnectionInfo();
            connectionInfo.addConnection(localConnection);
            int totalNum = localConnection.getNum();
            for (String address : clusterHostList) {
                if (!address.contains(brokerHost)) {
                    String url = "http://" + address + "/mqtt/connection/num?mode=node";
                    try {
                        String result = HttpAPIClient.executeHttpGet(url);
                        if (result != null && !result.isEmpty() && result.contains("status")) {
                            ContextResponse<Connection> contextResponse = gson.fromJson(result,
                                new TypeToken<ContextResponse<Connection>>() {
                                }.getType());
                            if (contextResponse.getStatus() == 200) {
                                Connection connection = contextResponse.getData();
                                connectionInfo.addConnection(connection);
                                totalNum = totalNum + connection.getNum();
                            }
                        }
                    } catch (Exception e) {
                        logger.error("request http broker connection failed, url:{}:", url, e);
                    }
                }
            }
            connectionInfo.setTotalNum(totalNum);
            response.setData(connectionInfo);
        }
        context.result(gson.toJson(response).toString());
    }

    private void queryConnection(Context context) {
        ContextResponse<ConnectionInfo> response = new ContextResponse();
        String mode = context.queryParam("mode");
        String key = context.queryParam("key");
        String value = context.queryParam("value");
        if (mode == null || mode.isEmpty() || key == null || key.isEmpty() || value == null || value.isEmpty()) {
            response.setStatus(-1);
        } else if (key.equals("clientId")) {
            response = queryConnectionByClientId(mode, value);
        } else if (key.equals("mqttTopic")) {
            response = queryConnectionByMqttTopic(mode, value);
        }
        context.result(gson.toJson(response).toString());
    }

    private ContextResponse queryConnectionByClientId(String mode, String clientId) {
        ContextResponse response = new ContextResponse<>();
        ConnectionInfo connectionInfo = localConnectionByClientId(clientId);
        if (!mode.equals("node")) {
            for (String clusterAddress : clusterHostList) {
                if (!clusterAddress.contains(brokerHost)) {
                    String url = "http://" + clusterAddress + "/mqtt/connection/query?mode=node&key=clientId&value=" + clientId;
                    requestOtherNodeConnection(connectionInfo, url);
                }
            }
        }
        response.setData(connectionInfo);
        connectionInfo.setTotalNum(connectionInfo.size());
        return response;
    }

    private ContextResponse queryConnectionByMqttTopic(String mode, String mqttTopic) {
        ContextResponse response = new ContextResponse<>();
        ConnectionInfo connectionInfo = localConnectionByMqttTopic(mqttTopic);
        if (!mode.equals("node")) {
            for (String clusterAddress : clusterHostList) {
                if (!clusterAddress.contains(brokerHost)) {
                    String url = "http://" + clusterAddress + "/mqtt/connection/query?mode=node&key=mqttTopic&value=" + mqttTopic;
                    requestOtherNodeConnection(connectionInfo, url);
                }
            }
        }
        response.setData(connectionInfo);
        connectionInfo.setTotalNum(connectionInfo.size());
        return response;
    }

    private Connection getConnection(String mqttTopic, Subscription subscription) {
        Connection connection = new Connection();
        Channel channel = subscription.getClient().getCtx().channel();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.remoteAddress();
        connection.setHostName(inetSocketAddress.getHostName());
        connection.setIp(inetSocketAddress.getAddress().getHostAddress());
        connection.setPort(inetSocketAddress.getPort());
        connection.setNum(1);
        connection.setRmqTopic(MqttUtil.getMqttRootTopic(mqttTopic));
        connection.setMqttTopic(mqttTopic);
        connection.setClientId(subscription.getClient().getId());
        return connection;
    }

    private void requestOtherNodeConnection(ConnectionInfo connectionInfo, String url) {
        try {
            String result = HttpAPIClient.executeHttpGet(url);
            if (result != null && !result.isEmpty() && result.contains("status")) {
                ContextResponse<ConnectionInfo> contextResponse = gson.fromJson(result,
                    new TypeToken<ContextResponse<ConnectionInfo>>() {
                    }.getType());
                if (contextResponse.getStatus() == 200) {
                    ConnectionInfo nodeConnection = contextResponse.getData();
                    connectionInfo.addConnectionList(nodeConnection.getConnectionList());
                }
            }
        } catch (Exception e) {
            logger.error("request http broker, query connection by clientId failed, url:{}:", url, e);
        }
    }

    private Connection localConnectionNum() {
        Connection connection = new Connection();
        connection.setHostName(bridgeConfig.getBrokerHost());
        connection.setPort(bridgeConfig.getBrokerPort());
        connection.setNum(clientManager.size());
        return connection;
    }

    private ConnectionInfo localConnectionByClientId(String clientId) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        Set<String> topicSet = subscriptionStore.getTopicFilters(clientId);
        for (String mqttTopic : topicSet) {
            List<Subscription> subscriptionList = subscriptionStore.get(mqttTopic);
            for (Subscription subscription : subscriptionList) {
                String subscriptionId = subscription.getId();
                if (subscriptionId.equals(clientId)) {
                    Connection connection = getConnection(mqttTopic, subscription);
                    connectionInfo.addConnection(connection);
                }
            }
        }
        return connectionInfo;
    }

    private ConnectionInfo localConnectionByMqttTopic(String mqttTopic) {
        ConnectionInfo connectionInfo = new ConnectionInfo();
        List<Subscription> subscriptionList = subscriptionStore.get(mqttTopic);
        for (Subscription subscription : subscriptionList) {
            Connection connection = getConnection(mqttTopic, subscription);
            connectionInfo.addConnection(connection);
        }
        return connectionInfo;
    }

    @Override public void shutdown() {
        this.javalin.stop();
    }
}
