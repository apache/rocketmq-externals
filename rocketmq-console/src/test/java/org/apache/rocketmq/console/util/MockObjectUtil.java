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
package org.apache.rocketmq.console.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.trace.TraceConstants;
import org.apache.rocketmq.client.trace.TraceType;
import org.apache.rocketmq.common.DataVersion;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.BrokerStatsItem;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ConsumeStatus;
import org.apache.rocketmq.common.protocol.body.ConsumerConnection;
import org.apache.rocketmq.common.protocol.body.ConsumerRunningInfo;
import org.apache.rocketmq.common.protocol.body.ProcessQueueInfo;
import org.apache.rocketmq.common.protocol.body.SubscriptionGroupWrapper;
import org.apache.rocketmq.common.protocol.body.TopicConfigSerializeWrapper;
import org.apache.rocketmq.common.protocol.heartbeat.ConsumeType;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.common.protocol.heartbeat.SubscriptionData;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.remoting.protocol.LanguageCode;

import static org.apache.rocketmq.common.protocol.heartbeat.ConsumeType.CONSUME_ACTIVELY;

public class MockObjectUtil {

    public static ConsumeStats createConsumeStats() {
        ConsumeStats stats = new ConsumeStats();
        HashMap<MessageQueue, OffsetWrapper> offsetTable = new HashMap<MessageQueue, OffsetWrapper>();
        OffsetWrapper wrapper = new OffsetWrapper();
        wrapper.setBrokerOffset(10);
        wrapper.setConsumerOffset(7);
        wrapper.setLastTimestamp(System.currentTimeMillis());
        offsetTable.put(new MessageQueue("topic_test", "broker-a", 1), wrapper);
        offsetTable.put(new MessageQueue("topic_test", "broker-a", 2), wrapper);
        stats.setOffsetTable(offsetTable);
        return stats;
    }

    public static ClusterInfo createClusterInfo() {
        ClusterInfo clusterInfo = new ClusterInfo();
        HashMap<String, Set<String>> clusterAddrTable = new HashMap<>(3);
        Set<String> brokerNameSet = new HashSet<>(3);
        brokerNameSet.add("broker-a");
        clusterAddrTable.put("DefaultCluster", brokerNameSet);
        clusterInfo.setClusterAddrTable(clusterAddrTable);
        HashMap<String, BrokerData> brokerAddrTable = new HashMap<>(3);
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        HashMap<Long, String> brokerAddrs = new HashMap<>(2);
        brokerAddrs.put(MixAll.MASTER_ID, "127.0.0.1:10911");
        brokerData.setBrokerAddrs(brokerAddrs);
        brokerAddrTable.put("broker-a", brokerData);
        clusterInfo.setBrokerAddrTable(brokerAddrTable);
        return clusterInfo;
    }

    public static TopicStatsTable createTopicStatsTable() {
        TopicStatsTable topicStatsTable = new TopicStatsTable();
        HashMap<MessageQueue, TopicOffset> offsetTable = new HashMap<>();
        MessageQueue queue = new MessageQueue("topic_test", "broker-a", 2);
        TopicOffset offset = new TopicOffset();
        offset.setMinOffset(0);
        offset.setMaxOffset(100);
        offset.setLastUpdateTimestamp(System.currentTimeMillis());
        offsetTable.put(queue, offset);
        topicStatsTable.setOffsetTable(offsetTable);
        return topicStatsTable;
    }

    public static TopicRouteData createTopicRouteData() {
        TopicRouteData topicRouteData = new TopicRouteData();

        topicRouteData.setFilterServerTable(new HashMap<>());
        List<BrokerData> brokerDataList = new ArrayList<>();
        BrokerData brokerData = new BrokerData();
        brokerData.setBrokerName("broker-a");
        brokerData.setCluster("DefaultCluster");
        HashMap<Long, String> brokerAddrs = new HashMap<>();
        brokerAddrs.put(0L, "127.0.0.1:10911");
        brokerData.setBrokerAddrs(brokerAddrs);
        brokerDataList.add(brokerData);
        topicRouteData.setBrokerDatas(brokerDataList);

        List<QueueData> queueDataList = new ArrayList<>();
        QueueData queueData = new QueueData();
        queueData.setBrokerName("broker-a");
        queueData.setPerm(6);
        queueData.setReadQueueNums(4);
        queueData.setWriteQueueNums(4);
        queueData.setTopicSysFlag(0);
        queueDataList.add(queueData);
        topicRouteData.setQueueDatas(queueDataList);
        return topicRouteData;
    }

    public static SubscriptionGroupWrapper createSubscriptionGroupWrapper() {
        SubscriptionGroupWrapper wrapper = new SubscriptionGroupWrapper();
        SubscriptionGroupConfig config = new SubscriptionGroupConfig();
        config.setGroupName("group_test");
        ConcurrentMap<String, SubscriptionGroupConfig> subscriptionGroupTable = new ConcurrentHashMap(2);
        subscriptionGroupTable.put("group_test", config);
        wrapper.setSubscriptionGroupTable(subscriptionGroupTable);
        wrapper.setDataVersion(new DataVersion());
        return wrapper;
    }

    public static TopicConfigSerializeWrapper createTopicConfigWrapper() {
        TopicConfigSerializeWrapper wrapper = new TopicConfigSerializeWrapper();
        TopicConfig config = new TopicConfig();
        config.setTopicName("topic_test");
        ConcurrentMap<String, TopicConfig> topicConfigTable = new ConcurrentHashMap(2);
        topicConfigTable.put("topic_test", config);
        wrapper.setTopicConfigTable(topicConfigTable);
        wrapper.setDataVersion(new DataVersion());
        return wrapper;
    }

    public static ConsumerConnection createConsumerConnection() {
        ConsumerConnection consumerConnection = new ConsumerConnection();
        HashSet<Connection> connections = new HashSet<Connection>();
        Connection conn = new Connection();
        conn.setClientAddr("127.0.0.1");
        conn.setClientId("clientId");
        conn.setVersion(LanguageCode.JAVA.getCode());
        connections.add(conn);

        ConcurrentHashMap<String/* Topic */, SubscriptionData> subscriptionTable = new ConcurrentHashMap<String, SubscriptionData>();
        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionTable.put("topic_test", subscriptionData);

        ConsumeType consumeType = ConsumeType.CONSUME_ACTIVELY;
        MessageModel messageModel = MessageModel.CLUSTERING;
        ConsumeFromWhere consumeFromWhere = ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET;

        consumerConnection.setConnectionSet(connections);
        consumerConnection.setSubscriptionTable(subscriptionTable);
        consumerConnection.setConsumeType(consumeType);
        consumerConnection.setMessageModel(messageModel);
        consumerConnection.setConsumeFromWhere(consumeFromWhere);
        return consumerConnection;
    }

    public static ConsumerRunningInfo createConsumerRunningInfo() {
        ConsumerRunningInfo consumerRunningInfo = new ConsumerRunningInfo();
        consumerRunningInfo.setJstack("test");

        TreeMap<MessageQueue, ProcessQueueInfo> mqTable = new TreeMap<MessageQueue, ProcessQueueInfo>();
        MessageQueue messageQueue = new MessageQueue("topic_test", "broker-a", 1);
        mqTable.put(messageQueue, new ProcessQueueInfo());
        consumerRunningInfo.setMqTable(mqTable);

        TreeMap<String, ConsumeStatus> statusTable = new TreeMap<String, ConsumeStatus>();
        statusTable.put("topic_test", new ConsumeStatus());
        consumerRunningInfo.setStatusTable(statusTable);

        TreeSet<SubscriptionData> subscriptionSet = new TreeSet<SubscriptionData>();
        subscriptionSet.add(new SubscriptionData());
        consumerRunningInfo.setSubscriptionSet(subscriptionSet);

        Properties properties = new Properties();
        properties.put(ConsumerRunningInfo.PROP_CONSUME_TYPE, CONSUME_ACTIVELY.name());
        properties.put(ConsumerRunningInfo.PROP_CONSUMER_START_TIMESTAMP, Long.toString(System.currentTimeMillis()));
        consumerRunningInfo.setProperties(properties);
        return consumerRunningInfo;
    }

    public static MessageExt createMessageExt() {
        MessageExt messageExt = new MessageExt();
        messageExt.setBrokerName("broker-a");
        messageExt.setQueueId(0);
        messageExt.setStoreSize(205);
        messageExt.setQueueOffset(1L);
        messageExt.setKeys("KeyA");
        messageExt.setMsgId("0A9A003F00002A9F0000000000000319");
        messageExt.setTopic("topic_test");
        messageExt.setBody("this is message ext body".getBytes());
        messageExt.setStoreHost(new InetSocketAddress("127.0.0.1", 8899));
        messageExt.setBornHost(new InetSocketAddress("127.0.0.1", 7788));
        messageExt.setBornTimestamp(System.currentTimeMillis());
        messageExt.setStoreTimestamp(System.currentTimeMillis());
        messageExt.setCommitLogOffset(793);
        messageExt.setReconsumeTimes(0);
        return messageExt;
    }

    public static String createTraceData() {
        StringBuilder sb = new StringBuilder(100);
        // pub trace data
        sb.append(TraceType.Pub.name()).append(TraceConstants.CONTENT_SPLITOR)
            .append("1627568812564").append(TraceConstants.CONTENT_SPLITOR)
            .append("DefaultRegion").append(TraceConstants.CONTENT_SPLITOR)
            .append("PID_test").append(TraceConstants.CONTENT_SPLITOR)
            .append("topic_test").append(TraceConstants.CONTENT_SPLITOR)
            .append("0A9A003F00002A9F0000000000000319").append(TraceConstants.CONTENT_SPLITOR)
            .append("TagA").append(TraceConstants.CONTENT_SPLITOR)
            .append("KeyA").append(TraceConstants.CONTENT_SPLITOR)
            .append("127.0.0.1:10911").append(TraceConstants.CONTENT_SPLITOR)
            .append("16").append(TraceConstants.CONTENT_SPLITOR)
            .append("1224").append(TraceConstants.CONTENT_SPLITOR)
            .append("0").append(TraceConstants.CONTENT_SPLITOR)
            .append("0A9A003F00002A9F0000000000000000").append(TraceConstants.CONTENT_SPLITOR)
            .append("true").append(TraceConstants.FIELD_SPLITOR);
        // subBefore trace data
        sb.append(TraceType.SubBefore.name()).append(TraceConstants.CONTENT_SPLITOR)
            .append("1627569868519").append(TraceConstants.CONTENT_SPLITOR)
            .append("DefaultRegion").append(TraceConstants.CONTENT_SPLITOR)
            .append("group_test").append(TraceConstants.CONTENT_SPLITOR)
            .append("7F000001752818B4AAC2951341580000").append(TraceConstants.CONTENT_SPLITOR)
            .append("0A9A003F00002A9F0000000000000319").append(TraceConstants.CONTENT_SPLITOR)
            .append("0").append(TraceConstants.CONTENT_SPLITOR)
            .append("KeyA").append(TraceConstants.FIELD_SPLITOR);
        // subAfter trace data
        sb.append(TraceType.SubAfter.name()).append(TraceConstants.CONTENT_SPLITOR)
            .append("7F000001752818B4AAC2951341580000").append(TraceConstants.CONTENT_SPLITOR)
            .append("0A9A003F00002A9F0000000000000319").append(TraceConstants.CONTENT_SPLITOR)
            .append("200").append(TraceConstants.CONTENT_SPLITOR)
            .append("true").append(TraceConstants.CONTENT_SPLITOR)
            .append("KeyA").append(TraceConstants.CONTENT_SPLITOR)
            .append("0").append(TraceConstants.FIELD_SPLITOR);
        // endTransaction trace data
        sb.append(TraceType.EndTransaction.name()).append(TraceConstants.CONTENT_SPLITOR)
            .append("1627569868519").append(TraceConstants.CONTENT_SPLITOR)
            .append("DefaultRegion").append(TraceConstants.CONTENT_SPLITOR)
            .append("group_test").append(TraceConstants.CONTENT_SPLITOR)
            .append("topic_test").append(TraceConstants.CONTENT_SPLITOR)
            .append("0A9A003F00002A9F0000000000000319").append(TraceConstants.CONTENT_SPLITOR)
            .append("TagA").append(TraceConstants.CONTENT_SPLITOR)
            .append("KeyA").append(TraceConstants.CONTENT_SPLITOR)
            .append("127.0.0.1:10911").append(TraceConstants.CONTENT_SPLITOR)
            .append(2).append(TraceConstants.CONTENT_SPLITOR)
            .append("7F000001752818B4AAC2951341580000").append(TraceConstants.CONTENT_SPLITOR)
            .append(LocalTransactionState.COMMIT_MESSAGE).append(TraceConstants.CONTENT_SPLITOR)
            .append("true").append(TraceConstants.FIELD_SPLITOR);
        return sb.toString();
    }

    public static BrokerStatsData createBrokerStatsData() {
        BrokerStatsData brokerStatsData = new BrokerStatsData();
        BrokerStatsItem statsDay = new BrokerStatsItem();
        statsDay.setAvgpt(100.0);
        statsDay.setSum(10000L);
        statsDay.setTps(100.0);
        brokerStatsData.setStatsDay(statsDay);

        BrokerStatsItem statsHour = new BrokerStatsItem();
        statsHour.setAvgpt(10.0);
        statsHour.setSum(100L);
        statsHour.setTps(100.0);
        brokerStatsData.setStatsHour(statsHour);

        BrokerStatsItem statsMinute = new BrokerStatsItem();
        statsMinute.setAvgpt(10.0);
        statsMinute.setSum(100L);
        statsMinute.setTps(100.0);
        brokerStatsData.setStatsMinute(statsMinute);
        return brokerStatsData;
    }
}
