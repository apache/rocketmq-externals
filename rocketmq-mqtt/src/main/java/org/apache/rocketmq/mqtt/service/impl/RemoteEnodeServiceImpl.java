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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.client.common.ThreadLocalIndex;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.header.GetMaxOffsetResponseHeader;
import org.apache.rocketmq.common.protocol.header.GetMinOffsetResponseHeader;
import org.apache.rocketmq.common.protocol.header.QueryConsumerOffsetRequestHeader;
import org.apache.rocketmq.common.protocol.header.QueryConsumerOffsetResponseHeader;
import org.apache.rocketmq.common.protocol.header.SearchOffsetResponseHeader;
import org.apache.rocketmq.common.protocol.header.UpdateConsumerOffsetRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.mqtt.MqttBridgeController;
import org.apache.rocketmq.mqtt.constant.MqttConstant;
import org.apache.rocketmq.mqtt.service.EnodeService;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.remoting.protocol.RemotingSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteEnodeServiceImpl implements EnodeService {
    private static final Logger log = LoggerFactory.getLogger(RemoteEnodeServiceImpl.class);

    private MqttBridgeController mqttBridgeController;

    private final ConcurrentMap<String/* Broker Name */, HashMap<Long/* brokerId */, String/* address */>> enodeTable =
        new ConcurrentHashMap<>();

    private volatile ThreadLocalIndex sendWhichBroker = new ThreadLocalIndex();

    public RemoteEnodeServiceImpl(MqttBridgeController mqttBridgeController) {
        this.mqttBridgeController = mqttBridgeController;
    }

    @Override
    public void sendHeartbeat(RemotingCommand remotingCommand) {
        for (Map.Entry<String, HashMap<Long, String>> entry : enodeTable.entrySet()) {
            String enodeAddr = entry.getValue().get(MixAll.MASTER_ID);
            if (enodeAddr != null) {
                try {
                    this.mqttBridgeController.getMqttRemotingClient().invokeSync(enodeAddr, remotingCommand, MqttConstant.DEFAULT_TIMEOUT_MILLS);
                } catch (Exception ex) {
                    log.warn("Send heart beat failed:{}, ex:{}", enodeAddr, ex);
                }
            }
        }
    }

    @Override
    public CompletableFuture<RemotingCommand> pullMessage(final String enodeName,
        final RemotingCommand request) {

        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        try {
            String enodeAddress = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
            this.mqttBridgeController.getMqttRemotingClient().invokeAsync(enodeAddress, request, MqttConstant.CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND, new InvokeCallback() {
                @Override
                public void operationComplete(ResponseFuture responseFuture) {
                    RemotingCommand response = responseFuture.getResponseCommand();
                    if (response != null) {
                        future.complete(response);
                    } else {
                        if (!responseFuture.isSendRequestOK()) {
                            log.error("Pull message error in async callback: {}", responseFuture.getCause());
                        } else if (responseFuture.isTimeout()) {
                            log.warn("Pull message timeout!");
                        } else {
                            log.error("Unknown pull message error occurred: {}", responseFuture.getCause());
                        }
                    }
                }
            });
        } catch (Exception ex) {
            log.error("Pull message async error: {}", ex);
            future.completeExceptionally(ex);
        }
        return future;
    }

    @Override public RemotingCommand pullMessageSync(String enodeName, RemotingCommand request) {
        try {
            String enodeAddress = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
            RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(enodeAddress, request, MqttConstant.CONSUMER_TIMEOUT_MILLIS_WHEN_SUSPEND);
            return response;
        } catch (Exception ex) {
            log.error("Pull message sync error: {}", ex);
        }
        return null;
    }

    @Override
    public CompletableFuture<RemotingCommand> sendMessage(String enodeAddress, RemotingCommand request) {
        CompletableFuture<RemotingCommand> future = new CompletableFuture<>();
        try {
            this.mqttBridgeController.getMqttRemotingClient().invokeAsync(enodeAddress, request, MqttConstant.DEFAULT_TIMEOUT_MILLS, new InvokeCallback() {
                @Override public void operationComplete(ResponseFuture responseFuture) {
                    future.complete(responseFuture.getResponseCommand());
                }
            });
        } catch (Exception ex) {
            log.error("Send message async error:{}", ex.getMessage());
            future.completeExceptionally(ex);
        }
        return future;
    }

    @Override
    public void sendMessageAsync(List<BrokerData> brokerDatas, RemotingCommand request,
        final int retryTimesWhenSendFailed, final AtomicInteger times,
        final CompletableFuture<RemotingCommand> future) {
        if (brokerDatas.size() == 0) {
            future.completeExceptionally(new Exception("No available broker was found, send message failed. "));
            return;
        }
        int brokerDataPos = selectOneBrokerPos(brokerDatas);
        BrokerData brokerData = brokerDatas.get(brokerDataPos);
        try {
            String enodeAddress = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(brokerData.getBrokerName(), false);
            if (enodeAddress == null) {
                brokerDatas.remove(brokerDataPos);
                sendMessageAsync(brokerDatas, request, retryTimesWhenSendFailed, times, future);
            }
            this.mqttBridgeController.getMqttRemotingClient().invokeAsync(enodeAddress, request, MqttConstant.DEFAULT_TIMEOUT_MILLS, new InvokeCallback() {
                @Override public void operationComplete(ResponseFuture responseFuture) {
                    if (responseFuture.getResponseCommand() != null) {
                        RemotingCommand remotingCommand = responseFuture.getResponseCommand();
                        remotingCommand.addExtField(MqttConstant.ENODE_NAME, brokerData.getBrokerName());
                        future.complete(responseFuture.getResponseCommand());
                    } else {
                        if (!responseFuture.isSendRequestOK()) {
                            log.error("send request failed, cause is: {}", responseFuture.getCause());
                            onExceptionImpl(brokerDatas, request, retryTimesWhenSendFailed, times, future, true);
                        } else if (responseFuture.isTimeout()) {
                            log.error("wait response timeout " + responseFuture.getTimeoutMillis() + "ms" + ", cause is: {}",
                                responseFuture.getCause());
                            onExceptionImpl(brokerDatas, request, retryTimesWhenSendFailed, times, future, true);
                        } else {
                            log.error("unknow reseaon, cause is: {}", responseFuture.getCause());
                            onExceptionImpl(brokerDatas, request, retryTimesWhenSendFailed, times, future, true);
                        }
                    }
                }
            });
        } catch (Exception ex) {
            brokerDatas.remove(brokerDataPos);
            onExceptionImpl(brokerDatas, request, retryTimesWhenSendFailed, times, future, false);
        }
    }

    private void onExceptionImpl(List<BrokerData> brokerDatas, RemotingCommand request,
        final int retryTimesWhenSendFailed, final AtomicInteger times,
        final CompletableFuture<RemotingCommand> future, boolean retryTimesInc) {

        int curTime = retryTimesInc ? times.incrementAndGet() : 0;

        if (curTime < retryTimesWhenSendFailed) {

            sendMessageAsync(brokerDatas, request, retryTimesWhenSendFailed, times, future);

        } else {
            future.completeExceptionally(new Exception("Retry to send message to broker failed, reaches max retryTimesWhenSendFailed"));
        }
    }

    private int selectOneBrokerPos(List<BrokerData> brokerDatas) {
        int index = this.sendWhichBroker.getAndIncrement();
        int pos = Math.abs(index) % brokerDatas.size();
        if (pos < 0)
            pos = 0;
        return pos;
    }

    @Override
    public void updateEnodeAddress(ClusterInfo clusterInfo, String clusterName) {
        if (clusterInfo != null) {
            HashMap<String, Set<String>> enodeAddress = clusterInfo.getClusterAddrTable();
            if (enodeAddress != null && enodeAddress.get(clusterName) != null) {
                Set<String> enodeNames = enodeAddress.get(clusterName);
                for (String enodeName : enodeNames) {
                    enodeTable.put(enodeName, clusterInfo.getBrokerAddrTable().get(enodeName).getBrokerAddrs());
                }
            }
        }
    }

    @Override
    public boolean persistSubscriptionGroupConfig(
        SubscriptionGroupConfig subscriptionGroupConfig) {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_SUBSCRIPTIONGROUP, null);
        boolean persist = false;
        for (Map.Entry<String, HashMap<Long, String>> entry : enodeTable.entrySet()) {
            byte[] body = RemotingSerializable.encode(subscriptionGroupConfig);
            request.setBody(body);
            String enodeAddress = entry.getValue().get(MixAll.MASTER_ID);
            try {
                RemotingCommand response = this.mqttBridgeController.getMqttRemotingClient().invokeSync(enodeAddress,
                    request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
                if (response != null && response.getCode() == ResponseCode.SUCCESS) {
                    persist = true;
                } else {
                    persist = false;
                }
                log.info("Persist to broker address: {} result: {}", enodeAddress, persist);
            } catch (Exception ex) {
                log.warn("Persist Subscription to Enode {} error", enodeAddress);
                persist = false;
            }
        }
        return persist;
    }

    @Override
    public void persistOffset(String enodeName, String groupName, String topic,
        int queueId, long offset) {
        try {
            String address = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
            UpdateConsumerOffsetRequestHeader requestHeader = new UpdateConsumerOffsetRequestHeader();
            requestHeader.setTopic(topic);
            requestHeader.setConsumerGroup(groupName);
            requestHeader.setQueueId(queueId);
            requestHeader.setCommitOffset(offset);
            RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_CONSUMER_OFFSET, requestHeader);
            this.mqttBridgeController.getMqttRemotingClient().invokeOneway(address, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        } catch (Exception ex) {
            log.error("Persist offset to Enode error!");
        }
    }

    @Override
    public long getMinOffsetInQueue(String enodeName, String topic, int queueId,
        RemotingCommand request) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException, RemotingCommandException {
        String addr = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        RemotingCommand remotingCommand = this.mqttBridgeController.getMqttRemotingClient().invokeSync(addr, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        GetMinOffsetResponseHeader responseHeader =
            (GetMinOffsetResponseHeader) remotingCommand.decodeCommandCustomHeader(GetMinOffsetResponseHeader.class);
        return responseHeader.getOffset();

    }

    @Override
    public long queryOffset(String enodeName, String consumerGroup, String topic,
        int queueId) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException, RemotingCommandException {
        QueryConsumerOffsetRequestHeader requestHeader = new QueryConsumerOffsetRequestHeader();
        requestHeader.setTopic(topic);
        requestHeader.setConsumerGroup(consumerGroup);
        requestHeader.setQueueId(queueId);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.QUERY_CONSUMER_OFFSET, requestHeader);
        String addr = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        RemotingCommand remotingCommand = this.mqttBridgeController.getMqttRemotingClient().invokeSync(addr, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        QueryConsumerOffsetResponseHeader responseHeader =
            (QueryConsumerOffsetResponseHeader) remotingCommand.decodeCommandCustomHeader(QueryConsumerOffsetResponseHeader.class);
        return responseHeader.getOffset();
    }

    @Override
    public long getMaxOffsetInQueue(String enodeName, String topic,
        int queueId,
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, RemotingCommandException {
        String address = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        RemotingCommand remotingCommand = this.mqttBridgeController.getMqttRemotingClient().invokeSync(address,
            request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        GetMaxOffsetResponseHeader responseHeader =
            (GetMaxOffsetResponseHeader) remotingCommand.decodeCommandCustomHeader(GetMaxOffsetResponseHeader.class);
        return responseHeader.getOffset();
    }

    @Override
    public long getOffsetByTimestamp(String enodeName, String topic, int queueId,
        long timestamp,
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, RemotingCommandException {
        String address = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        RemotingCommand remotingCommand = this.mqttBridgeController.getMqttRemotingClient().invokeSync(address, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
        SearchOffsetResponseHeader responseHeader =
            (SearchOffsetResponseHeader) remotingCommand.decodeCommandCustomHeader(SearchOffsetResponseHeader.class);
        return responseHeader.getOffset();
    }

    @Override
    public RemotingCommand creatRetryTopic(String enodeName,
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        String address = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        return this.mqttBridgeController.getMqttRemotingClient().invokeSync(address, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
    }

    @Override
    public RemotingCommand lockBatchMQ(
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return transferToEnode(request);
    }

    @Override
    public RemotingCommand unlockBatchMQ(
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return transferToEnode(request);
    }

    private RemotingCommand transferToEnode(
        final RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        String enodeName = request.getExtFields().get(MqttConstant.ENODE_NAME);
        String address = this.mqttBridgeController.getNnodeService().getAddressByEnodeName(enodeName, false);
        return this.mqttBridgeController.getMqttRemotingClient().invokeSync(address, request, MqttConstant.DEFAULT_TIMEOUT_MILLS);
    }

    @Override
    public RemotingCommand requestMQTTInfoSync(
        final RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.transferToEnode(request);
    }

    @Override public CompletableFuture<RemotingCommand> requestMQTTInfoAsync(
        RemotingCommand request) throws
        InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException {
        return this.sendMessage(request.getExtFields().get(MqttConstant.ENODE_NAME), request);
    }
}
