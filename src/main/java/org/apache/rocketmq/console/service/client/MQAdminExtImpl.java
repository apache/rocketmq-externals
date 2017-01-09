package org.apache.rocketmq.console.service.client;

import com.alibaba.rocketmq.client.QueryResult;
import com.alibaba.rocketmq.client.exception.MQBrokerException;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.TopicConfig;
import com.alibaba.rocketmq.common.admin.ConsumeStats;
import com.alibaba.rocketmq.common.admin.RollbackStats;
import com.alibaba.rocketmq.common.admin.TopicStatsTable;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.RequestCode;
import com.alibaba.rocketmq.common.protocol.ResponseCode;
import com.alibaba.rocketmq.common.protocol.body.BrokerStatsData;
import com.alibaba.rocketmq.common.protocol.body.ClusterInfo;
import com.alibaba.rocketmq.common.protocol.body.ConsumeMessageDirectlyResult;
import com.alibaba.rocketmq.common.protocol.body.ConsumeStatsList;
import com.alibaba.rocketmq.common.protocol.body.ConsumerConnection;
import com.alibaba.rocketmq.common.protocol.body.ConsumerRunningInfo;
import com.alibaba.rocketmq.common.protocol.body.GroupList;
import com.alibaba.rocketmq.common.protocol.body.KVTable;
import com.alibaba.rocketmq.common.protocol.body.ProducerConnection;
import com.alibaba.rocketmq.common.protocol.body.QueueTimeSpan;
import com.alibaba.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import com.alibaba.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import com.alibaba.rocketmq.common.protocol.body.TopicList;
import com.alibaba.rocketmq.common.protocol.route.TopicRouteData;
import com.alibaba.rocketmq.common.subscription.SubscriptionGroupConfig;
import com.alibaba.rocketmq.remoting.RemotingClient;
import com.alibaba.rocketmq.remoting.exception.RemotingCommandException;
import com.alibaba.rocketmq.remoting.exception.RemotingConnectException;
import com.alibaba.rocketmq.remoting.exception.RemotingException;
import com.alibaba.rocketmq.remoting.exception.RemotingSendRequestException;
import com.alibaba.rocketmq.remoting.exception.RemotingTimeoutException;
import com.alibaba.rocketmq.remoting.protocol.RemotingCommand;
import com.alibaba.rocketmq.tools.admin.MQAdminExt;
import com.alibaba.rocketmq.tools.admin.api.MessageTrack;
import org.apache.rocketmq.console.util.JsonUtil;
import com.google.common.base.Throwables;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.alibaba.rocketmq.remoting.protocol.RemotingSerializable.decode;

/**
 * Created by tangjie
 * 2016/11/18
 * 这个类相当于是DefaultMQAdminExt的一个装饰者Decorator
 * MQAdminInstance.threadLocalMQAdminExt()获取DefaultMQAdminExt的一个实例（通过MQAdminAspect管理里连接/断开）
 * 这样连接的管理对使用方透明
 */
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

    /*brokerMasterAddr就可以了 写是写向主的 从会从主的进行同步 这个个人认为更应该传brokerName ConsumerService进行封装*/
    @Override
    public SubscriptionGroupConfig examineSubscriptionGroupConfig(String addr, String group) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_SUBSCRIPTIONGROUP_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception err) {
            throw Throwables.propagate(err);
        }
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                SubscriptionGroupWrapper subscriptionGroupWrapper = SubscriptionGroupWrapper.decode(response.getBody(), SubscriptionGroupWrapper.class);
                return subscriptionGroupWrapper.getSubscriptionGroupTable().get(group);
            }
            default:
                throw Throwables.propagate(new MQBrokerException(response.getCode(), response.getRemark()));
        }
    }

    /*brokerMasterAddr就可以了 写是写向主的 从会从主的进行同步 这个个人认为更应该传brokerName 我们在TopicService进行封装*/
    @Override
    public TopicConfig examineTopicConfig(String addr, String topic) {
        RemotingClient remotingClient = MQAdminInstance.threadLocalRemotingClient();
        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.GET_ALL_TOPIC_CONFIG, null);
        RemotingCommand response = null;
        try {
            response = remotingClient.invokeSync(addr, request, 3000);
        } catch (Exception err) {
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
        logger.info("op=look={}", JsonUtil.obj2String(topicList.getTopicList()));
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
        logger.info("I am test");
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
        logger.info("addrs={} topic={}",JsonUtil.obj2String(addrs),topic);
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

    @Override// todo 如果是新客户端发的消息 那么你用这个查 是查不到的 你得用MessageExt viewMessage(String topic, String msgId)
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
    public List<QueueTimeSpan> queryConsumeTimeSpan(String topic, String group) throws InterruptedException, MQBrokerException, RemotingException, MQClientException {
        return  MQAdminInstance.threadLocalMQAdminExt().queryConsumeTimeSpan(topic, group);
    }


    @Override //todo 这个方法似乎有问题
    public MessageExt viewMessage(String topic, String msgId) throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        return  MQAdminInstance.threadLocalMQAdminExt().viewMessage(topic, msgId);
    }


    @Override
    public ConsumeMessageDirectlyResult consumeMessageDirectly(String consumerGroup, String clientId, String topic, String msgId) throws RemotingException, MQClientException, InterruptedException, MQBrokerException {
        return  MQAdminInstance.threadLocalMQAdminExt().consumeMessageDirectly(consumerGroup,clientId,topic,msgId);
    }


    @Override
    public Properties getBrokerConfig(String brokerAddr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, UnsupportedEncodingException, InterruptedException, MQBrokerException {
        return  MQAdminInstance.threadLocalMQAdminExt().getBrokerConfig(brokerAddr);
    }


    @Override //可以根据集群细分topic了
    public TopicList fetchTopicsByCLuster(String clusterName) throws RemotingException, MQClientException, InterruptedException {
        return  MQAdminInstance.threadLocalMQAdminExt().fetchTopicsByCLuster(clusterName);
    }
    @Override
    public boolean cleanUnusedTopic(String cluster) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return  MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopic(cluster);
    }

    @Override
    public boolean cleanUnusedTopicByAddr(String addr) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().cleanUnusedTopicByAddr(addr);
    }
    @Override //查看集群状况 分时间段 tps
    public BrokerStatsData viewBrokerStatsData(String brokerAddr, String statsName, String statsKey) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().viewBrokerStatsData(brokerAddr, statsName, statsKey);
    }

    @Override //主题被哪些集群消费
    public Set<String> getClusterList(String topic) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().getClusterList(topic);
    }

    @Override  //查消费状态
    public ConsumeStatsList fetchConsumeStatsInBroker(String brokerAddr, boolean isOrder, long timeoutMillis) throws RemotingConnectException, RemotingSendRequestException, RemotingTimeoutException, MQClientException, InterruptedException {
        return MQAdminInstance.threadLocalMQAdminExt().fetchConsumeStatsInBroker(brokerAddr, isOrder, timeoutMillis);
    }

    @Override  //和getClusterList什么不同？
    public Set<String> getTopicClusterList(String topic) throws InterruptedException, MQBrokerException, MQClientException, RemotingException {
        return MQAdminInstance.threadLocalMQAdminExt().getTopicClusterList(topic);
    }

    @Override //todo 可以使用这个更加细化组管理
    public SubscriptionGroupWrapper getAllSubscriptionGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return  MQAdminInstance.threadLocalMQAdminExt().getAllSubscriptionGroup(brokerAddr,timeoutMillis);
    }

    @Override //todo use
    public TopicConfigSerializeWrapper getAllTopicGroup(String brokerAddr, long timeoutMillis) throws InterruptedException, RemotingTimeoutException, RemotingSendRequestException, RemotingConnectException, MQBrokerException {
        return MQAdminInstance.threadLocalMQAdminExt().getAllTopicGroup(brokerAddr, timeoutMillis);
    }

    @Override
    public void updateConsumeOffset(String brokerAddr, String consumeGroup, MessageQueue mq, long offset) throws RemotingException, InterruptedException, MQBrokerException {
        MQAdminInstance.threadLocalMQAdminExt().updateConsumeOffset(brokerAddr, consumeGroup, mq, offset);
    }
}
