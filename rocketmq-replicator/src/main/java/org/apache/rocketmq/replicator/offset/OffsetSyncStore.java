package org.apache.rocketmq.replicator.offset;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.common.admin.OffsetWrapper;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OffsetSyncStore {
    private static final Logger log = LoggerFactory.getLogger(OffsetSyncStore.class);

    private DefaultMQAdminExt srcMQAdminExt;
    private DefaultMQAdminExt targetMQAdminExt;
    private DefaultMQPullConsumer srcConsumer;
    private TaskConfig taskConfig;
    private final ScheduledExecutorService executor;
    private volatile boolean started = false;

    private final Map<String /*ConsumerGroup*/, Map<MessageQueue, OffsetSync>> offsetMapper;
    private final Map<String, String> brokerNameMapper;

    public OffsetSyncStore(TaskConfig taskConfig) {
        this.taskConfig = taskConfig;
        this.brokerNameMapper = new HashMap<>();
        this.offsetMapper = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("OffsetSyncStore-Watcher-%d").daemon(true).build());
    }

    public void start() {
        try {
            this.srcMQAdminExt = Utils.startSrcMQAdminTool(this.taskConfig);
            this.targetMQAdminExt = Utils.startTargetMQAdminTool(this.taskConfig);
            this.srcConsumer = Utils.startSrcMQPullConsumer(this.taskConfig);
            this.buildBrokerRoute();
            this.buildOffsetMapper();

            this.executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    buildBrokerRoute();
                }
            }, 60 * 1000, 60 * 1000, TimeUnit.SECONDS);

            this.executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    buildOffsetMapper();
                }
            }, 5 * 1000, 5 * 1000, TimeUnit.SECONDS);
            this.started = true;
        } catch (MQClientException e) {
            log.error("OffsetSyncStore start error.", e);
            this.shutdown();
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        if (started) {
            this.started = false;
            try {
                this.executor.shutdown();
            } catch (Exception e) {
                log.error("Executor shutdown error.", e);
            }

            try {
                this.srcConsumer.shutdown();
            } catch (Exception e) {
                log.error("Source consumer shutdown error.", e);
            }

            try {
                this.srcMQAdminExt.shutdown();
            } catch (Exception e) {
                log.error("Source mqAdmin shutdown error.", e);
            }

            try {
                this.targetMQAdminExt.shutdown();
            } catch (Exception e) {
                log.error("Target mqAdmin shutdown error.", e);
            }
        }
    }

    public long convertTargetOffset(MessageQueue mq, String consumerGroup, long srcOffset) {
        OffsetSync offsetSync = latestOffsetSync(mq, consumerGroup);
        if (offsetSync.getSrcOffset() > srcOffset) {
            return -1;
        }
        long delta = srcOffset - offsetSync.getSrcOffset();
        return offsetSync.getTargetOffset() + delta;
    }

    public String getTargetBrokerName(String brokerName) {
        return this.brokerNameMapper.get(brokerName);
    }

    public void buildBrokerRoute() {
        try {
            List<String> srcBrokerNames = fetchBrokerNames(srcMQAdminExt, taskConfig.getSourceCluster());
            if (srcBrokerNames == null) {
                return;
            }
            List<String> targetBrokerNames = fetchBrokerNames(targetMQAdminExt, taskConfig.getTargetCluster());
            if (targetBrokerNames == null) {
                return;
            }
            Collections.sort(srcBrokerNames);
            Collections.sort(targetBrokerNames);
            for (int i = 0; i < srcBrokerNames.size() && i < targetBrokerNames.size(); i++) {
                brokerNameMapper.put(srcBrokerNames.get(i), targetBrokerNames.get(i));
            }
        } catch (Exception e) {
            log.error("RocketMQ replicator build broker route error", e);
        }
    }

    public List<String> fetchBrokerNames(DefaultMQAdminExt mqAdminExt, String cluster) throws Exception {
        List<String> brokerNames = null;
        ClusterInfo clusterInfo = mqAdminExt.examineBrokerClusterInfo();
        if (clusterInfo != null && clusterInfo.getBrokerAddrTable() != null) {
            brokerNames = clusterInfo.getBrokerAddrTable().values().stream().filter(brokerData -> brokerData.getCluster().equals(cluster))
                .map(brokerData -> brokerData.getBrokerName()).collect(Collectors.toList());
        }
        return brokerNames;
    }

    private void buildOffsetMapper() {
        Set<OffsetSync> offsetSyncs = new HashSet<>();
        for (Map<MessageQueue, OffsetSync> offsetSyncMap : offsetMapper.values()) {
            offsetSyncs.addAll(offsetSyncMap.values());
        }

        for (OffsetSync offsetSync : offsetSyncs) {
            try {
                long sourceOffset = fetchConsumeOffset(srcMQAdminExt, offsetSync.getMq(), offsetSync.getGroup());
                MessageExt srcMsg = pullMessage(srcConsumer, offsetSync.getMq(), sourceOffset);
                if (srcMsg == null) {
                    return;
                }
                String targetMsgKey = Utils.getOffsetMsgId(srcMsg);
                String targetTopic = Utils.getTargetTopic(srcMsg.getTopic(), taskConfig.getRenamePattern());
                MessageExt targetMsg = queryMessage(targetMQAdminExt, targetTopic, targetMsgKey);
                if (targetMsg == null) {
                    return;
                }
                long targetOffset = targetMsg.getQueueOffset();
                log.info("Update Offset Mapper. sourceOffset = {} -> {}, targetOffset: {} -> {}",
                    offsetSync.getSrcOffset(), sourceOffset, offsetSync.getTargetOffset(), targetOffset);
                long deviation = (offsetSync.getSrcOffset() - offsetSync.getTargetOffset()) - (sourceOffset - targetOffset);
                if (deviation != 0) {
                    log.warn("Offsets are inconsistent. deviation: {}", deviation);
                }
                offsetSync.updateOffset(sourceOffset, targetOffset);
            } catch (Exception e) {
                log.error("RocketMQ replicator build offset mapper error, offsetSync={}", offsetSync, e);
            }
        }
    }

    private OffsetSync latestOffsetSync(MessageQueue queue, String consumerGroup) {
        if (!offsetMapper.containsKey(consumerGroup)) {
            synchronized (consumerGroup) {
                if (!offsetMapper.containsKey(consumerGroup)) {
                    offsetMapper.put(consumerGroup, new HashMap<>());
                }
            }
        }
        Map<MessageQueue, OffsetSync> offsetSyncMap = offsetMapper.get(consumerGroup);
        if (!offsetSyncMap.containsKey(queue)) {
            synchronized (queue) {
                if (!offsetSyncMap.containsKey(queue)) {
                    offsetSyncMap.put(queue, new OffsetSync(queue, consumerGroup, -1, -1));
                    offsetSyncMap = offsetMapper.get(consumerGroup);
                }
            }
        }
        return offsetSyncMap.get(queue);
    }

    public static long fetchConsumeOffset(DefaultMQAdminExt mqAdminExt, MessageQueue queue, String consumeGroup) {
        try {
            ConsumeStats consumeStats = mqAdminExt.examineConsumeStats(consumeGroup, queue.getTopic());
            if (consumeStats == null || consumeStats.getOffsetTable() == null) {
                return -1;
            }
            OffsetWrapper offsetWrapper = consumeStats.getOffsetTable().get(queue);
            if (offsetWrapper != null) {
                return offsetWrapper.getConsumerOffset();
            }
        } catch (Exception e) {
            log.error("Fetch consume offset error. queue: {}, consumeGroup: {}", queue, consumeGroup, e);
        }
        return -1;
    }

    public MessageExt pullMessage(DefaultMQPullConsumer consumer, MessageQueue queue, long offset) {
        try {

            PullResult pullResult = consumer.pull(queue, "*", offset, 1);
            switch (pullResult.getPullStatus()) {
                case FOUND:
                    List<MessageExt> msgs = pullResult.getMsgFoundList();
                    if (msgs.size() > 0) {
                        return msgs.get(0);
                    }
                default:
                    break;
            }
        } catch (Exception e) {
            log.error("Pull message error. queue: {}, offset: {}", queue, offset, e);
        }

        return null;
    }

    public MessageExt queryMessage(DefaultMQAdminExt mqAdminExt, String topic, String key) {
        try {
            QueryResult queryResult = mqAdminExt.queryMessage(topic, key, 1, 0, Long.MAX_VALUE);
            if (queryResult == null && queryResult.getMessageList() == null) {
                return null;
            }
            List<MessageExt> msgs = queryResult.getMessageList();
            if (msgs.size() > 0) {
                return msgs.get(0);
            }
        } catch (Exception e) {
            log.error("Query message error. topic: {}, key: {}", topic, key, e);
        }
        return null;
    }
}
