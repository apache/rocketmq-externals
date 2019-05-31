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
package org.apache.rocketmq.exporter.task;

import com.google.common.base.Throwables;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.admin.TopicOffset;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.BrokerStatsData;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.body.GroupList;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.exporter.aspect.admin.annotation.MultiMQAdminCmdMethod;
import org.apache.rocketmq.exporter.config.RMQConfigure;
import org.apache.rocketmq.exporter.service.RMQMetricsService;
import org.apache.rocketmq.exporter.service.client.MQAdminExtImpl;
import org.apache.rocketmq.store.stats.BrokerStatsManager;
import org.apache.rocketmq.tools.admin.MQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MetricsCollectTask {

    @Resource
    private MQAdminExt mqAdminExt;
    @Resource
    private RMQConfigure rmqConfigure;

    @Resource
    private RMQMetricsService  metricsService;

    private final static Logger log = LoggerFactory.getLogger(MetricsCollectTask.class);

    @Scheduled(cron = "15 0/1 * * * ?")
    @MultiMQAdminCmdMethod(timeoutMillis = 5000)
    public void collectOffset() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        Date date = new Date();
        try {
            TopicList topicList = mqAdminExt.fetchAllTopicList();
            Set<String> topicSet = topicList.getTopicList();
            for (String topic : topicSet) {
                if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) || topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                    continue;
                }
                String clusterName = null;
                ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
                Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();
                for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
                    clusterName  = clusterEntry.getValue().getCluster();
                    break;
                }
                if (clusterName != null) {
                    HashMap<String,Long>    brokerOffsetMap = new HashMap<>();
                    TopicStatsTable topicStatus = mqAdminExt.examineTopicStats(topic);
                    Set<Map.Entry<MessageQueue, TopicOffset>> topicStatusEntries = topicStatus.getOffsetTable().entrySet();
                    for (Map.Entry<MessageQueue, TopicOffset> topicStatusEntry : topicStatusEntries) {
                        MessageQueue q      =   topicStatusEntry.getKey();
                        TopicOffset offset  =   topicStatusEntry.getValue();
                        if  (brokerOffsetMap.containsKey(q.getBrokerName())) {
                            brokerOffsetMap.put(q.getBrokerName(),brokerOffsetMap.get(q.getBrokerName()) + offset.getMaxOffset());
                        }
                        else {
                            brokerOffsetMap.put(q.getBrokerName(),offset.getMaxOffset());
                        }
                    }
                    Set<Map.Entry<String, Long>> brokerOffsetEntries = brokerOffsetMap.entrySet();
                    for (Map.Entry<String, Long> brokerOffsetEntry : brokerOffsetEntries) {
                        metricsService.getCollector().AddTopicOffsetMetric(clusterName,brokerOffsetEntry.getKey(), topic, brokerOffsetEntry.getValue());
                    }
                }

                HashMap<String,Long>    consumeOffsetMap = new HashMap<>();
                GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);
                if (groupList != null && !groupList.getGroupList().isEmpty()) {
                    for (String group : groupList.getGroupList()) {
                        try {
                            ConsumeStats consumeStatus = mqAdminExt.examineConsumeStats(group, topic);
                            Set<Map.Entry<MessageQueue, OffsetWrapper>> consumeStatusEntries = consumeStatus.getOffsetTable().entrySet();
                            for (Map.Entry<MessageQueue, OffsetWrapper> consumeStatusEntry : consumeStatusEntries) {
                                MessageQueue q = consumeStatusEntry.getKey();
                                OffsetWrapper offset = consumeStatusEntry.getValue();
                                if (consumeOffsetMap.containsKey(q.getBrokerName())) {
                                    consumeOffsetMap.put(q.getBrokerName(), consumeOffsetMap.get(q.getBrokerName()) + offset.getConsumerOffset());
                                } else {
                                    consumeOffsetMap.put(q.getBrokerName(), offset.getConsumerOffset());
                                }
                            }
                        } catch (Exception e) {
                            log.info("ignore this consumer", e.getMessage());
                        }
                        Set<Map.Entry<String, Long>> consumeOffsetEntries = consumeOffsetMap.entrySet();
                        for (Map.Entry<String, Long> consumeOffsetEntry : consumeOffsetEntries) {
                            metricsService.getCollector().AddGroupOffsetMetric(clusterName,consumeOffsetEntry.getKey(), topic, group, consumeOffsetEntry.getValue());
                        }
                        consumeOffsetMap.clear();
                    }
                }
            }
        } catch (Exception e) {
            log.info("error is " + e.getMessage());
        }
    }

    @Scheduled(cron = "15 0/1 * * * ?")
    @MultiMQAdminCmdMethod(timeoutMillis = 5000)
    public void collectTopic() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        Date date = new Date();
        try {
            TopicList topicList = mqAdminExt.fetchAllTopicList();
            Set<String> topicSet = topicList.getTopicList();
            for (String topic : topicSet) {
                if (topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX) || topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) {
                    continue;
                }
                TopicRouteData topicRouteData = mqAdminExt.examineTopicRouteInfo(topic);
                GroupList groupList = mqAdminExt.queryTopicConsumeByWho(topic);

                for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                    String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (masterAddr != null) {
                        try {
                            BrokerStatsData bsd = null;
                            try {
                                bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.TOPIC_PUT_NUMS, topic);
                                metricsService.getCollector().AddTopicPutNumsMetric(bd.getCluster(), bd.getBrokerName(), topic, bsd.getStatsMinute().getTps());
                            }
                            catch (Exception e) {
                                log.info("error is " + e.getMessage());
                            }
                            try {
                                bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.TOPIC_PUT_SIZE, topic);
                                metricsService.getCollector().AddTopicPutSizeMetric(bd.getCluster(), bd.getBrokerName(), topic, bsd.getStatsMinute().getTps());
                            }
                            catch (Exception e) {
                                log.info("error is " + e.getMessage());
                            }
                        } catch (Exception e) {
                            log.info("error is " + e.getMessage());
                        }
                    }
                }
                if (groupList != null && !groupList.getGroupList().isEmpty()) {
                    for (String group : groupList.getGroupList()) {
                        for (BrokerData bd : topicRouteData.getBrokerDatas()) {
                            String masterAddr = bd.getBrokerAddrs().get(MixAll.MASTER_ID);
                            if (masterAddr != null) {
                                try {
                                    String statsKey = String.format("%s@%s", topic, group);
                                    BrokerStatsData bsd = null;
                                    try {
                                        bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_NUMS, statsKey);
                                        metricsService.getCollector().AddGroupGetNumsMetric(bd.getCluster(), bd.getBrokerName(), topic, group, bsd.getStatsMinute().getTps());
                                    } catch (Exception e) {
                                        log.info("error is " + e.getMessage());
                                    }
                                    try {
                                        bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_SIZE, statsKey);
                                        metricsService.getCollector().AddGroupGetSizeMetric(bd.getCluster(), bd.getBrokerName(), topic, group, bsd.getStatsMinute().getTps());
                                    } catch (Exception e) {
                                        log.info("error is " + e.getMessage());
                                    }
                                    try {

                                        bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.SNDBCK_PUT_NUMS, statsKey);
                                        metricsService.getCollector().AddsendBackNumsMetric(bd.getCluster(), bd.getBrokerName(), topic, group, bsd.getStatsMinute().getTps());
                                    } catch (Exception e) {
                                        log.info("error is " + e.getMessage());
                                    }
                                    try {
                                        collectLatencyMetrcisInner(topic, group, masterAddr, bd);
                                    } catch (Exception e) {
                                        log.info("error is " + e.getMessage());
                                    }
                                } catch (Exception e) {
                                    log.info("error is " + e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }

    @Scheduled(cron = "15 0/1 * * * ?")
    @MultiMQAdminCmdMethod(timeoutMillis = 5000)
    public void collectBroker() {
        if (!rmqConfigure.isEnableCollect()) {
            return;
        }
        try {
            Date date = new Date();
            ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
            Set<Map.Entry<String, BrokerData>> clusterEntries = clusterInfo.getBrokerAddrTable().entrySet();
            for (Map.Entry<String, BrokerData> clusterEntry : clusterEntries) {
                String masterAddr = clusterEntry.getValue().getBrokerAddrs().get(MixAll.MASTER_ID);
                if (masterAddr != null) {
                    try {
                        BrokerStatsData bsd = null;
                        try {
                            bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.BROKER_PUT_NUMS,clusterEntry.getValue().getCluster());
                            metricsService.getCollector().AddBrokerPutNumsMetric(clusterEntry.getValue().getCluster(), clusterEntry.getValue().getBrokerName(), bsd.getStatsMinute().getTps());
                        }
                        catch (Exception e) {
                            log.info("error is " + e.getMessage());
                        }
                        try {
                            bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.BROKER_GET_NUMS, clusterEntry.getValue().getCluster());
                            metricsService.getCollector().AddBrokerGetNumsMetric(clusterEntry.getValue().getCluster(), clusterEntry.getValue().getBrokerName(), bsd.getStatsMinute().getTps());
                        }
                        catch (Exception e) {
                            log.info("error is " + e.getMessage());
                        }
                    } catch (Exception e) {
                        log.info("error is " + e.getMessage());
                    }
                }
            }
        }
        catch (Exception err) {
            throw Throwables.propagate(err);
        }
    }
    private void collectLatencyMetrcisInner(String topic,String group,String masterAddr, BrokerData bd) throws Exception {
        long maxLagTime = 0;
        String statsKey;
        BrokerStatsData bsd = null;
        ConsumeStats consumeStatus = mqAdminExt.examineConsumeStats(group, topic);
        Set<Map.Entry<MessageQueue, OffsetWrapper>> consumeStatusEntries = consumeStatus.getOffsetTable().entrySet();
        for (Map.Entry<MessageQueue, OffsetWrapper> consumeStatusEntry : consumeStatusEntries) {
            MessageQueue q = consumeStatusEntry.getKey();
            OffsetWrapper offset = consumeStatusEntry.getValue();
            int queueId = q.getQueueId();
            statsKey = String.format("%d@%s@%s", queueId, topic, group);
            try {
                bsd = mqAdminExt.viewBrokerStatsData(masterAddr, BrokerStatsManager.GROUP_GET_LATENCY, statsKey);
                metricsService.getCollector().AddGroupGetLatencyMetric(bd.getCluster(), bd.getBrokerName(), topic, group, String.format("%d", queueId), bsd.getStatsMinute().getTps());
            } catch (Exception e) {
                log.info("error is " + e.getMessage());
            }
            MQAdminExtImpl mqAdminImpl = (MQAdminExtImpl) mqAdminExt;
            PullResult consumePullResult = mqAdminImpl.queryMsgByOffset(q, offset.getConsumerOffset());
            long lagTime = 0;
            if (consumePullResult != null && consumePullResult.getPullStatus() == PullStatus.FOUND) {
                lagTime = System.currentTimeMillis() - consumePullResult.getMsgFoundList().get(0).getStoreTimestamp();
                if (offset.getBrokerOffset() == offset.getConsumerOffset()) {
                    lagTime = 0;
                }
            } else if (consumePullResult.getPullStatus() == PullStatus.NO_MATCHED_MSG) {
                lagTime = 0;
            } else if (consumePullResult.getPullStatus() == PullStatus.OFFSET_ILLEGAL) {
                PullResult pullResult = mqAdminImpl.queryMsgByOffset(q, consumePullResult.getMinOffset());
                if (pullResult != null && pullResult.getPullStatus() == PullStatus.FOUND) {
                    lagTime = System.currentTimeMillis() - consumePullResult.getMsgFoundList().get(0).getStoreTimestamp();
                }
            } else {
                lagTime = 0;
            }
            if (lagTime > maxLagTime) {
                maxLagTime = lagTime;
            }
        }
        metricsService.getCollector().AddGroupGetLatencyByStoreTimeMetric(bd.getCluster(), bd.getBrokerName(), topic, group, maxLagTime);
    }
}
