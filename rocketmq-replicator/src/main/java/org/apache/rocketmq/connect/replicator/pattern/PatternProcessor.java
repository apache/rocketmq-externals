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

package org.apache.rocketmq.connect.replicator.pattern;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQClientAPIImpl;
import org.apache.rocketmq.client.impl.MQClientManager;
import org.apache.rocketmq.client.impl.factory.MQClientInstance;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerOffsetSerializeWrapper;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.header.GetConsumerConnectionListRequestHeader;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.connect.replicator.Config;
import org.apache.rocketmq.connect.replicator.Replicator;
import org.apache.rocketmq.remoting.InvokeCallback;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.exception.RemotingTooMuchRequestException;
import org.apache.rocketmq.remoting.netty.ResponseFuture;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatternProcessor {

    private static final Logger log = LoggerFactory.getLogger(PatternProcessor.class);

    private static int POOL_SIZE = Runtime.getRuntime().availableProcessors();

    private Replicator replicator;

    private Config config;

    private MQClientInstance mqClientInstance;

    private MQClientAPIImpl clientAPIImpl;

    private RemotingClient remotingClient;

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5,
        new ThreadFactoryImpl("BrokerControllerScheduledThread"));

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(POOL_SIZE, POOL_SIZE, 5, TimeUnit.MINUTES,
        new LinkedBlockingQueue<Runnable>());

    public PatternProcessor(Replicator replicator) {
        this.replicator = replicator;
        this.config = replicator.getConfig();
    }

    public void start() throws Exception {
        this.mqClientInstance = MQClientManager.getInstance().getAndCreateMQClientInstance(new ClientConfig(), null);
        this.mqClientInstance.getMQClientAPIImpl().updateNameServerAddressList(config.getNameServerAddress());
        this.clientAPIImpl = this.mqClientInstance.getMQClientAPIImpl();
        this.remotingClient = this.clientAPIImpl.getRemotingClient();
        scheduledThreadPoolExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                PatternProcessor.this.execute();
            }
        }, config.getSyncInterval(), TimeUnit.SECONDS);
    }

    public void stop() throws Exception {
        scheduledThreadPoolExecutor.shutdown();
        threadPoolExecutor.shutdown();
    }

    public void execute() {
        try {
            ClusterInfo clusterInfo = this.getBrokerClusterInfo();
            HashMap<String, BrokerData> brokerAddrTable = clusterInfo.getBrokerAddrTable();
            Iterator<Entry<String, BrokerData>> it = brokerAddrTable.entrySet().iterator();
            while (it.hasNext()) {
                for (final String addr : it.next().getValue().getBrokerAddrs().values()) {
                    threadPoolExecutor.execute(new Runnable() {
                        public void run() {
                            PatternProcessor.this.getBrokerInfo(addr);
                        }
                    });
                }
            }

        } catch (RemotingException | MQClientException | InterruptedException | MQBrokerException e) {
            log.error("replicator task start failed.", e);
        }
    }

    public void getBrokerInfo(String addr) {
        try {
            BrokerInfo brokerInfo = new BrokerInfo();
            brokerInfo.setBrokerConfig(this.getBrokerConfig(addr));
            brokerInfo.setTopicConfig(this.getAllTopicConfig(addr));
            brokerInfo.setDeleayOffset(this.getAllDelayOffset(addr));
            if (config.isSyncConsumerOffset()) {
                brokerInfo.setConsumerOffsetSerializeWrapper(this.getAllConsumerOffset(addr));
            }
            if (config.isSyncSubscription()) {
                brokerInfo.setSubscriptionGroupWrapper(this.getAllSubscriptionGroup(addr));
            }
            if (config.isSyncConsumerConnection()) {
                brokerInfo.setConsumerConnections(config.isSyncConsumerOffset()
                    ? this.getAllConsumerConnectionToConsumerOffset(addr, brokerInfo.getConsumerOffsetSerializeWrapper())
                    : this.getAllConsumerConnectionToTopicConfig(addr, brokerInfo.getTopicConfig()));
            }
            this.replicator.commit(brokerInfo, false);

        } catch (UnsupportedEncodingException | RemotingException | InterruptedException | MQBrokerException e) {
            log.error("getBrokerInfo task start failed. broker address" + addr, e);
        }
    }

    private ClusterInfo getBrokerClusterInfo()
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return clientAPIImpl.getBrokerClusterInfo(config.getTimeoutMillis());
    }

    private Properties getBrokerConfig(String addr) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return clientAPIImpl.getBrokerConfig(addr, config.getTimeoutMillis());
    }

    private TopicConfigSerializeWrapper getAllTopicConfig(String addr)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return clientAPIImpl.getAllTopicConfig(addr, config.getTimeoutMillis());
    }

    private List<ConsumerConnection> getAllConsumerConnectionToConsumerOffset(String addr,
        ConsumerOffsetSerializeWrapper consumerOffsetSerializeWrapper) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        List<ConsumerConnection> consumerConnections = new ArrayList<>();
        for (String key : consumerOffsetSerializeWrapper.getOffsetTable().keySet()) {
            ConsumerConnection consumerConnection = getConsumerConnectionList(addr,
                key.substring(key.indexOf((int) '@')), config.getTimeoutMillis());
            consumerConnections.add(consumerConnection);
        }
        return consumerConnections;
    }

    private List<ConsumerConnection> getAllConsumerConnectionToConsumerOffsetAsyn(String addr,
        ConsumerOffsetSerializeWrapper consumerOffsetSerializeWrapper) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, InterruptedException, MQBrokerException {
        List<ConsumerConnection> consumerConnections = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(consumerOffsetSerializeWrapper.getOffsetTable().size());
        ConsumerConnectionInvokeCallback callback = new ConsumerConnectionInvokeCallback(countDownLatch, consumerConnections);
        for (String key : consumerOffsetSerializeWrapper.getOffsetTable().keySet()) {
            getConsumerConnectionListAsync(addr, key.substring(key.indexOf((int) '@')+1), config.getTimeoutMillis(), callback, countDownLatch);
        }
        countDownLatch.await();
        return consumerConnections;
    }

    private List<ConsumerConnection> getAllConsumerConnectionToTopicConfig(String addr, TopicConfigSerializeWrapper topicConfig)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        InterruptedException, MQBrokerException {
        List<ConsumerConnection> consumerConnections = new ArrayList<>();
        for (String topic : topicConfig.getTopicConfigTable().keySet()) {
            GroupList groupList = this.clientAPIImpl.queryTopicConsumeByWho(addr, topic, config.getTimeoutMillis());
            for (String groupName : groupList.getGroupList()) {
                ConsumerConnection consumerConnection = getConsumerConnectionList(addr, groupName,
                    config.getTimeoutMillis());
                consumerConnections.add(consumerConnection);
            }
        }
        return consumerConnections;
    }

    private SubscriptionGroupWrapper getAllSubscriptionGroup(String addr) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG,
            null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return SubscriptionGroupWrapper.decode(response.getBody(), SubscriptionGroupWrapper.class);
            }
            default:
                break;
        }
        return null;
    }

    private ConsumerOffsetSerializeWrapper getAllConsumerOffset(String addr) throws RemotingConnectException,
        RemotingSendRequestException, RemotingTimeoutException, InterruptedException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_CONSUMER_OFFSET, null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ConsumerOffsetSerializeWrapper.decode(response.getBody(), ConsumerOffsetSerializeWrapper.class);
            }
            default:
                break;
        }
        return null;
    }

    private String getAllDelayOffset(String addr) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, UnsupportedEncodingException {
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_DELAY_OFFSET, null);
        RemotingCommand response = this.remotingClient.invokeSync(addr, request, 3000);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return new String(response.getBody(), MixAll.DEFAULT_CHARSET);
            }
            default:
                break;
        }
        return null;
    }

    private ConsumerConnection getConsumerConnectionList(final String addr, final String consumerGroup,
        final long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, MQBrokerException {
        GetConsumerConnectionListRequestHeader requestHeader = new GetConsumerConnectionListRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST,
            requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(addr, request, timeoutMillis);
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return ConsumerConnection.decode(response.getBody(), ConsumerConnection.class);
            }
            default:
                break;
        }

        throw new MQBrokerException(response.getCode(), response.getRemark());
    }

    private void getConsumerConnectionListAsync(final String addr, final String consumerGroup,
        final long timeoutMillis, InvokeCallback invokeCallback,
        CountDownLatch countDownLatch) throws RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, MQBrokerException {
        GetConsumerConnectionListRequestHeader requestHeader = new GetConsumerConnectionListRequestHeader();
        requestHeader.setConsumerGroup(consumerGroup);

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_CONSUMER_CONNECTION_LIST,
            requestHeader);
        try {
            this.remotingClient.invokeAsync(addr, request, timeoutMillis, invokeCallback);
        } catch (RemotingTooMuchRequestException e) {
            countDownLatch.countDown();
            log.error(e.getMessage(), e);
        }

    }

    private class ConsumerConnectionInvokeCallback implements InvokeCallback {

        List<ConsumerConnection> consumerConnections;
        CountDownLatch countDownLatch;

        public ConsumerConnectionInvokeCallback(CountDownLatch countDownLatch,
            List<ConsumerConnection> consumerConnections) {
            this.consumerConnections = consumerConnections;
            this.countDownLatch = countDownLatch;
        }

        public void operationComplete(ResponseFuture responseFuture) {
            countDownLatch.countDown();
            RemotingCommand response = responseFuture.getResponseCommand();
            switch (response.getCode()) {
                case ResponseCode.SUCCESS:
                    consumerConnections.add(ConsumerConnection.decode(response.getBody(), ConsumerConnection.class));
                default:
                    log.error(response.getRemark());
            }
        }

    }
}
