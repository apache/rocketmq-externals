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

package org.apache.rocketmq.connect.runtime.service;

import io.netty.channel.ChannelHandlerContext;
import io.openmessaging.connector.api.exception.ConnectException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.header.NotifyConsumerIdsChangedRequestHeader;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.apache.rocketmq.connect.runtime.config.ConnectConfig;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.netty.NettyRequestProcessor;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterManagementServiceImpl implements ClusterManagementService {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    private Set<WorkerStatusListener> workerStatusListeners;

    /**
     * Configs of current worker.
     */
    private final ConnectConfig connectConfig;

    /**
     * Used for worker discovery
     */
    private DefaultMQPullConsumer defaultMQPullConsumer;

    public ClusterManagementServiceImpl(ConnectConfig connectConfig) {
        this.connectConfig = connectConfig;
        this.workerStatusListeners = new HashSet<>();
        this.defaultMQPullConsumer = new DefaultMQPullConsumer(connectConfig.getConnectClusterId());
        this.defaultMQPullConsumer.setNamesrvAddr(connectConfig.getNamesrvAddr());
    }

    @Override
    public void start() {
        try {
            this.defaultMQPullConsumer.start();
        } catch (MQClientException ex) {
            log.error("Start RocketMQ consumer for cluster management service error");
            throw new ConnectException(-1, "Start RocketMQ consumer for cluster management service error");
        }
        WorkerChangeListener workerChangeListener = new WorkerChangeListener();

        this.defaultMQPullConsumer.getDefaultMQPullConsumerImpl()
            .getRebalanceImpl()
            .getmQClientFactory()
            .getMQClientAPIImpl()
            .getRemotingClient()
            .registerProcessor(RequestCode.NOTIFY_CONSUMER_IDS_CHANGED, workerChangeListener,
                null);
    }

    @Override
    public void stop() {
        this.defaultMQPullConsumer.shutdown();
    }

    @Override
    public boolean hasClusterStoreTopic() {
        return this.defaultMQPullConsumer.getDefaultMQPullConsumerImpl()
            .getRebalanceImpl()
            .getmQClientFactory()
            .updateTopicRouteInfoFromNameServer(connectConfig.getClusterStoreTopic());
    }

    @Override
    public List<String> getAllAliveWorkers() {
        return this.defaultMQPullConsumer.getDefaultMQPullConsumerImpl()
            .getRebalanceImpl()
            .getmQClientFactory()
            .findConsumerIdList(connectConfig.getClusterStoreTopic(), connectConfig.getConnectClusterId());
    }

    @Override
    public String getCurrentWorker() {
        return this.defaultMQPullConsumer.getDefaultMQPullConsumerImpl().getRebalanceImpl().getmQClientFactory().getClientId();
    }

    @Override
    public void registerListener(WorkerStatusListener listener) {
        this.workerStatusListeners.add(listener);
    }

    public class WorkerChangeListener implements NettyRequestProcessor {

        @Override
        public RemotingCommand processRequest(ChannelHandlerContext ctx, RemotingCommand request) throws Exception {
            switch (request.getCode()) {
                case RequestCode.NOTIFY_CONSUMER_IDS_CHANGED:
                    return this.workerChanged(ctx, request);
                default:
                    break;
            }
            return null;
        }

        public RemotingCommand workerChanged(ChannelHandlerContext ctx,
            RemotingCommand request) {
            try {
                final NotifyConsumerIdsChangedRequestHeader requestHeader =
                    (NotifyConsumerIdsChangedRequestHeader) request.decodeCommandCustomHeader(NotifyConsumerIdsChangedRequestHeader.class);
                log.info("Receive broker's notification[{}], the consumer group for connect: {} changed,  rebalance immediately",
                    RemotingHelper.parseChannelRemoteAddr(ctx.channel()),
                    requestHeader.getConsumerGroup());
                for (WorkerStatusListener workerChangeListener : workerStatusListeners) {
                    workerChangeListener.onWorkerChange();
                }
            } catch (Exception e) {
                log.error("NotifyConsumerIdsChanged for connect exception", RemotingHelper.exceptionSimpleDesc(e));
            }
            return null;
        }

        @Override public boolean rejectRequest() {
            return false;
        }
    }
}
