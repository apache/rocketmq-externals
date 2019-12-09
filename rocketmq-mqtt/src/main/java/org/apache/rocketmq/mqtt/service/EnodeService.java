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
package org.apache.rocketmq.mqtt.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.header.PullMessageRequestHeader;
import org.apache.rocketmq.common.protocol.header.SendMessageRequestHeaderV2;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;

public interface EnodeService {
    /**
     * Send Heartbeat data to enode server to keep alive
     *
     * @param remotingCommand Heartbeat request
     */
    void sendHeartbeat(RemotingCommand remotingCommand);

    /**
     * Send message to enode.
     *
     * @param enodeAddress Enode server address
     * @param request {@link SendMessageRequestHeaderV2} Send message request header
     * @return Send message response future
     */
    CompletableFuture<RemotingCommand> sendMessage(final String enodeAddress, final RemotingCommand request);

    /**
     * Pull message from enode server asynchronously.
     *
     * @param enodeName Enode server name
     * @param request {@link PullMessageRequestHeader} Pull message request header
     * @return Pull message Response future
     */
    CompletableFuture<RemotingCommand> pullMessage(final String enodeName, final RemotingCommand request);

    /**
     * Pull message from enode server synchronously.
     *
     * @param enodeName Enode server name
     * @param request {@link PullMessageRequestHeader} Pull message request header
     * @return RemotingCommand
     */
    RemotingCommand pullMessageSync(final String enodeName, final RemotingCommand request);

    /**
     * Create retry topic in enode server.
     *
     * @param enodeName Enode server name
     * @param request {@link RemotingCommand }  with @see Cra Create
     * @return
     * @throws InterruptedException
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     * @throws RemotingConnectException
     */
    RemotingCommand creatRetryTopic(String enodeName,
        RemotingCommand request) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

    /**
     * Update Enode address from name server
     *
     * @param clusterName Cluster name, keep insistence with enode cluster
     * @throws InterruptedException
     * @throws RemotingTimeoutException
     * @throws RemotingSendRequestException
     * @throws RemotingConnectException
     * @throws MQBrokerException
     */
    void updateEnodeAddress(ClusterInfo clusterInfo, String clusterName);

    /**
     * Persist subscription config data to storage server, maybe file or to enode, in this situation, will persist to
     * enode
     *
     * @param subscriptionGroupConfig {@link SubscriptionGroupConfig} Group subscription config
     * @return
     */
    boolean persistSubscriptionGroupConfig(SubscriptionGroupConfig subscriptionGroupConfig);

    /**
     * Persist offset information of consumer group to storage server.
     *
     * @param enodeName Which enode server.
     * @param groupName Consumer group name.
     * @param topic Related topic.
     * @param queueId QueueId of related topic.
     * @param offset Current offset of target queue of subscribed topic.
     */
    void persistOffset(String enodeName, String groupName, String topic, int queueId, long offset);

    long queryOffset(String enodeName, String consumerGroup,
        String topic, int queueId) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException, RemotingCommandException;

    long getMaxOffsetInQueue(String enodeName, String topic,
        int queueId, RemotingCommand request) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException, RemotingCommandException;

    long getMinOffsetInQueue(String enodeName, String topic,
        int queueId, RemotingCommand request) throws InterruptedException, RemotingTimeoutException,
        RemotingSendRequestException, RemotingConnectException, RemotingCommandException;

    long getOffsetByTimestamp(String enodeName,
        String topic, int queueId,
        long timestamp,
        RemotingCommand request) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException,
        RemotingConnectException, RemotingCommandException;

    RemotingCommand lockBatchMQ(
        final RemotingCommand remotingCommand) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

    RemotingCommand unlockBatchMQ(
        final RemotingCommand remotingCommand) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

    RemotingCommand requestMQTTInfoSync(final RemotingCommand request)
        throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

    CompletableFuture<RemotingCommand> requestMQTTInfoAsync(final RemotingCommand request)
        throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException;

    void sendMessageAsync(List<BrokerData> brokerDatas, RemotingCommand request,
        final int retryTimesWhenSendFailed, final AtomicInteger times,
        final CompletableFuture<RemotingCommand> future);
}
