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
package org.apache.rocketmq.console.service.client;

import com.google.common.base.Throwables;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.MQAdminImpl;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.RollbackStats;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageClientIDSetter;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.RequestCode;
import org.apache.rocketmq.common.protocol.ResponseCode;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import org.apache.rocketmq.common.protocol.body.ConsumeStatsList;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.KVTable;
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.common.protocol.body.QueueTimeSpan;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.util.JsonUtil;
import org.apache.rocketmq.remoting.RemotingClient;
import org.apache.rocketmq.remoting.exception.RemotingCommandException;
import org.apache.rocketmq.remoting.exception.RemotingConnectException;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.remoting.exception.RemotingSendRequestException;
import org.apache.rocketmq.remoting.exception.RemotingTimeoutException;
import org.apache.rocketmq.remoting.protocol.RemotingCommand;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.joor.Reflect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static org.apache.rocketmq.remoting.protocol.RemotingSerializable.decode;

@Service
public class MQAdminExtImpl implements MQAdminExt {
    private Logger logger = LoggerFactory.getLogger(MQAdminExtImpl.class);

    public MQAdminExtImpl() {
    }

    @Override
    public void updateBrokerConfig(String brokerAddr, Properties properties)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        UnsupportedEncodingException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateBrokerConfig(brokerAddr, properties);
    }

    @Override
    public void createAndUpdateTopicConfig(String addr, TopicConfig config)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateTopicConfig(addr, config);
    }

    @Override
    public void createAndUpdateSubscriptionGroupConfig(String addr, SubscriptionGroupConfig config)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateSubscriptionGroupConfig(addr, config);
    }

    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                SubscriptionGroupWrapper subscriptionGroupWrapper = decode(response.getBody(), SubscriptionGroupWrapper.class);
                return subscriptionGroupWrapper.getSubscriptionGroupTable().get(group);
            }
            default:
                throw Throwables.propagate(new MQBrokerException(response.getCode(), response.getRemark()));
        }
    }

    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                TopicConfigSerializeWrapper topicConfigSerializeWrapper = decode(response.getBody(), TopicConfigSerializeWrapper.class);
                return topicConfigSerializeWrapper.getTopicConfigTable().get(topic);
            }
            default:
                throw Throwables.propagate(new MQBrokerException(response.getCode(), response.getRemark()));
        }
    }

    @Override
    public TopicStatsTable examineTopicStats(String topic)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineTopicStats(topic);
    }

    @Override
    public TopicList fetchAllTopicList() throws RemotingException, MQClientException, InterruptedException {
        TopicList topicList = MQAdminInstance.threadLocalMQAdminExt().fetchAllTopicList();
        logger.debug("op=look={}", JsonUtil.obj2String(topicList.getTopicList()));
        return topicList;
    }

    @Override
    public KVTable fetchBrokerRuntimeStats(String brokerAddr)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchBrokerRuntimeStats(brokerAddr);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumeStats(consumerGroup);
    }

    @Override
    public ConsumeStats examineConsumeStats(String consumerGroup, String topic)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumeStats(consumerGroup, topic);
    }

    @Override
    public ClusterInfo examineBrokerClusterInfo()
        throws InterruptedException, MQBrokerException, RemotingTimeoutException, RemotingSendRequestException,
        RemotingConnectException {
        return MQAdminInstance.threadLocalMQAdminExt().examineBrokerClusterInfo();
    }

    @Override
    public TopicRouteData examineTopicRouteInfo(String topic)
        throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().examineTopicRouteInfo(topic);
    }

    @Override
    public ConsumerConnection examineConsumerConnectionInfo(String consumerGroup)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().examineConsumerConnectionInfo(consumerGroup);
    }

    @Override
    public ProducerConnection examineProducerConnectionInfo(String producerGroup, String topic)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().examineProducerConnectionInfo(producerGroup, topic);
    }

    @Override
    public List<String> getNameServerAddressList() {
        return MQAdminInstance.threadLocalMQAdminExt().getNameServerAddressList();
    }

    @Override
    public int wipeWritePermOfBroker(String namesrvAddr, String brokerName)
        throws RemotingCommandException, RemotingConnectException, RemotingSendRequestException,
        RemotingTimeoutException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().wipeWritePermOfBroker(namesrvAddr, brokerName);
    }

    @Override
    public void putKVConfig(String namespace, String key, String value) {
        MQAdminInstance.threadLocalMQAdminExt().putKVConfig(namespace, key, value);
    }

    @Override
    public String getKVConfig(String namespace, String key)
        throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getKVConfig(namespace, key);
    }

    @Override
    public KVTable getKVListByNamespace(String namespace)
        throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getKVListByNamespace(namespace);
    }

    @Override
    public void deleteTopicInBroker(Set<String> addrs, String topic)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        logger.info("addrs={} topic={}", JsonUtil.obj2String(addrs), topic);
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInBroker(addrs, topic);
    }

    @Override
    public void deleteTopicInNameServer(Set<String> addrs, String topic)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteTopicInNameServer(addrs, topic);
    }

    @Override
    public void deleteSubscriptionGroup(String addr, String groupName)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteSubscriptionGroup(addr, groupName);
    }

    @Override
    public void createAndUpdateKvConfig(String namespace, String key, String value)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createAndUpdateKvConfig(namespace, key, value);
    }

    @Override
    public void deleteKvConfig(String namespace, String key)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().deleteKvConfig(namespace, key);
    }

    @Override
    public List<RollbackStats> resetOffsetByTimestampOld(String consumerGroup, String topic, long timestamp,
        boolean force) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().resetOffsetByTimestampOld(consumerGroup, topic, timestamp, force);
    }

    @Override
    public Map<MessageQueue, Long> resetOffsetByTimestamp(String topic, String group, long timestamp,
        boolean isForce) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().resetOffsetByTimestamp(topic, group, timestamp, isForce);
    }

    @Override
    public void resetOffsetNew(String consumerGroup, String topic, long timestamp)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().resetOffsetNew(consumerGroup, topic, timestamp);
    }

    @Override
    public Map<String, Map<MessageQueue, Long>> getConsumeStatus(String topic, String group,
        String clientAddr) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().getConsumeStatus(topic, group, clientAddr);
    }

    @Override
    public void createOrUpdateOrderConf(String key, String value, boolean isCluster)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createOrUpdateOrderConf(key, value, isCluster);
    }

    @Override
    public GroupList queryTopicConsumeByWho(String topic)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException,
        InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().queryTopicConsumeByWho(topic);
    }

    @Override
    public boolean cleanExpiredConsumerQueue(String cluster)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException,
        InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanExpiredConsumerQueue(cluster);
    }

    @Override
    public boolean cleanExpiredConsumerQueueByAddr(String addr)
        throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException,
        InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanExpiredConsumerQueueByAddr(addr);
    }

    @Override
    public ConsumerRunningInfo getConsumerRunningInfo(String consumerGroup, String clientId, boolean jstack)
        throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getConsumerRunningInfo(consumerGroup, clientId, jstack);
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId,
        String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().consumeMessageDirectly(consumerGroup, clientId, msgId);
    }

    @Override
    public List<MessageTrack> messageTrackDetail(MessageExt msg)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().messageTrackDetail(msg);
    }

    @Override
    public void cloneGroupOffset(String srcGroup, String destGroup, String topic, boolean isOffline)
        throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().cloneGroupOffset(srcGroup, destGroup, topic, isOffline);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum) throws MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createTopic(key, newTopic, queueNum);
    }

    @Override
    public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag)
        throws MQClientException {
        MQAdminInstance.threadLocalMQAdminExt().createTopic(key, newTopic, queueNum, topicSysFlag);
    }

    @Override
    public long searchOffset(MessageQueue mq, long timestamp) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().searchOffset(mq, timestamp);
    }

    @Override
    public long maxOffset(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().maxOffset(mq);
    }

    @Override
    public long minOffset(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().minOffset(mq);
    }

    @Override
    public long earliestMsgStoreTime(MessageQueue mq) throws MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().earliestMsgStoreTime(mq);
    }

    @Override
    public MessageExt viewMessage(String msgId)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().viewMessage(msgId);
    }

    @Override
    public QueryResult queryMessage(String topic, String key, int maxNum, long begin, long end)
        throws MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().queryMessage(topic, key, maxNum, begin, end);
    }

    @Override
    @Deprecated
    public void start() throws MQClientException {
        throw new IllegalStateException("thisMethod is deprecated.use org.apache.rocketmq.console.aspect.admin.MQAdminAspect instead of this");
    }

    @Override
    @Deprecated
    public void shutdown() {
        throw new IllegalStateException("thisMethod is deprecated.use org.apache.rocketmq.console.aspect.admin.MQAdminAspect instead of this");
    }

    // below is 3.2.6->3.5.8 updated

    @Override
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic,
        String group) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return MQAdminInstance.threadLocalMQAdminExt().queryConsumeTimeSpan(topic, group);
    }

    //MessageClientIDSetter.getNearlyTimeFromID has bug,so we subtract half a day
    //next version we will remove it
    //https://issues.apache.org/jira/browse/ROCKETMQ-111
    //https://github.com/apache/incubator-rocketmq/pull/69
    @Override
    public MessageExt viewMessage(String topic,
        String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        logger.info("MessageClientIDSetter.getNearlyTimeFromID(msgId)={} msgId={}", MessageClientIDSetter.getNearlyTimeFromID(msgId), msgId);
        try {
            return viewMessage(msgId);
        }
        catch (Exception e) {
        }
        MQAdminImpl mqAdminImpl = MQAdminInstance.threadLocalMqClientInstance().getMQAdminImpl();
        QueryResult qr = Reflect.on(mqAdminImpl).call("queryMessage", topic, msgId, 32,
            MessageClientIDSetter.getNearlyTimeFromID(msgId).getTime() - 1000 * 60 * 60 * 13L, Long.MAX_VALUE, true).get();
        if (qr != null && qr.getMessageList() != null && qr.getMessageList().size() > 0) {
            return qr.getMessageList().get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic,
        String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().consumeMessageDirectly(consumerGroup, clientId, topic, msgId);
    }

    @Override
    public Properties getBrokerConfig(
        String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getBrokerConfig(brokerAddr);
    }

    @Override
    public TopicList fetchTopicsByCLuster(
        String clusterName) throws RemotingException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchTopicsByCLuster(clusterName);
    }

    @Override
    public boolean cleanUnusedTopic(
        String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopic(cluster);
    }

    @Override
    public boolean cleanUnusedTopicByAddr(
        String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopicByAddr(addr);
    }

    @Override
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName,
        String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().viewBrokerStatsData(brokerAddr, statsName, statsKey);
    }

    @Override
    public Set<String> getClusterList(
        String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getClusterList(topic);
    }

    @Override
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder,
        long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override
    public Set<String> getTopicClusterList(
        String topic) throws InterruptedException, MQBrokerException, MQClientException, RemotingException {
        return MQAdminInstance.threadLocalMQAdminExt().getTopicClusterList(topic);
    }

    @Override
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr,
        long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getAllSubscriptionGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr,
        long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getAllTopicGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq,
        long offset) throws RemotingException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateConsumeOffset(brokerAddr, consumeGroup, mq, offset);
    }

    // 4.0.0 added
    @Override public void updateNameServerConfig(Properties properties,
        List<String> list) throws InterruptedException, RemotingConnectException, UnsupportedEncodingException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, MQBrokerException {

    }

    @Override public Map<String, Properties> getNameServerConfig(
        List<String> list) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQClientException, UnsupportedEncodingException {
        return null;
    }
}
