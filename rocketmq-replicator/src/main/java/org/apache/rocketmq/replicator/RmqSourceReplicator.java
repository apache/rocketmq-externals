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
package org.apache.rocketmq.replicator;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.apache.rocketmq.replicator.common.ConstDefine;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigDefine;
import org.apache.rocketmq.replicator.config.DataType;
import org.apache.rocketmq.replicator.config.TaskDivideConfig;
import org.apache.rocketmq.replicator.config.TaskTopicInfo;
import org.apache.rocketmq.replicator.strategy.DivideStrategyEnum;
import org.apache.rocketmq.replicator.strategy.DivideTaskByQueue;
import org.apache.rocketmq.replicator.strategy.DivideTaskByTopic;
import org.apache.rocketmq.replicator.strategy.TaskDivideStrategy;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RmqSourceReplicator extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceReplicator.class);

    private boolean syncDLQ = false;

    private boolean syncRETRY = false;

    private KeyValue replicatorConfig;

    private Map<String, List<TaskTopicInfo>> topicRouteMap;

    private TaskDivideStrategy taskDivideStrategy;

    private Set<String> whiteList;

    private volatile boolean configValid = false;

    private int taskParallelism = 1;

    private DefaultMQAdminExt srcMQAdminExt;
    private DefaultMQAdminExt targetMQAdminExt;

    private volatile boolean adminStarted;

    private ScheduledExecutorService executor;

    public RmqSourceReplicator() {
        topicRouteMap = new HashMap<String, List<TaskTopicInfo>>();
        whiteList = new HashSet<String>();
        executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("RmqSourceReplicator-SourceWatcher-%d").daemon(true).build());
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.srcMQAdminExt.setNamesrvAddr(this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ));
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ)));

        this.targetMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.targetMQAdminExt.setNamesrvAddr(this.replicatorConfig.getString(ConfigDefine.CONN_TARGET_RMQ));
        this.targetMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.targetMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getString(ConfigDefine.CONN_TARGET_RMQ)));

        try {
            this.srcMQAdminExt.start();
            log.info("RocketMQ srcMQAdminExt started");

            this.targetMQAdminExt.start();
            log.info("RocketMQ targetMQAdminExt started");
        } catch (MQClientException e) {
            log.error("Replicator start failed for `srcMQAdminExt` exception.", e);
        }

        adminStarted = true;
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {

        // Check the need key.
        for (String requestKey : ConfigDefine.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }

        // Check the whitelist, whitelist is required.
        String whileListStr = config.getString(ConfigDefine.CONN_WHITE_LIST);
        String[] wl = whileListStr.trim().split(",");
        if (wl.length <= 0)
            return "White list must be not empty.";
        else {
            for (String t : wl) {
                this.whiteList.add(t.trim());
            }
        }

        if (config.containsKey(ConfigDefine.CONN_TASK_DIVIDE_STRATEGY) &&
            config.getInt(ConfigDefine.CONN_TASK_DIVIDE_STRATEGY) == DivideStrategyEnum.BY_QUEUE.ordinal()) {
            this.taskDivideStrategy = new DivideTaskByQueue();
        } else {
            this.taskDivideStrategy = new DivideTaskByTopic();
        }

        if (config.containsKey(ConfigDefine.CONN_TASK_PARALLELISM)) {
            this.taskParallelism = config.getInt(ConfigDefine.CONN_TASK_PARALLELISM);
        }

        this.replicatorConfig = config;
        this.configValid = true;
        return "";
    }

    @Override
    public void start() {
        startMQAdminTools();
        startListner();
    }

    public void startListner() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override public void run() {
                Map<String, List<TaskTopicInfo>> origin = topicRouteMap;
                topicRouteMap = new HashMap<String, List<TaskTopicInfo>>();

                buildRoute();

                if (!compare(origin, topicRouteMap)) {
                    context.requestTaskReconfiguration();
                }
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    public boolean compare(Map<String, List<TaskTopicInfo>> origin, Map<String, List<TaskTopicInfo>> updated) {
        if (origin.size() != updated.size()) {
            return false;
        }
        for (Map.Entry<String, List<TaskTopicInfo>> entry : origin.entrySet()) {
            if (!updated.containsKey(entry.getKey())) {
                return false;
            }
            List<TaskTopicInfo> originTasks = entry.getValue();
            List<TaskTopicInfo> updateTasks = updated.get(entry.getKey());
            if (originTasks.size() != updateTasks.size()) {
                return false;
            }

            if (!originTasks.containsAll(updateTasks)) {
                return false;
            }
        }

        return true;
    }

    public void stop() {
    }

    public void pause() {

    }

    public void resume() {

    }

    public Class<? extends Task> taskClass() {

        return RmqSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        if (!configValid) {
            return new ArrayList<KeyValue>();
        }

        startMQAdminTools();

        buildRoute();

        TaskDivideConfig tdc = new TaskDivideConfig(
            this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RMQ),
            this.replicatorConfig.getString(ConfigDefine.CONN_STORE_TOPIC),
            this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_RECORD_CONVERTER),
            DataType.COMMON_MESSAGE.ordinal(),
            this.taskParallelism
        );
        return this.taskDivideStrategy.divide(this.topicRouteMap, tdc);
    }

    public void buildRoute() {
        List<Pattern> patterns = new ArrayList<Pattern>();
        String srcCluster = this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_CLUSTER);
        try {
            Set<String> targetTopicSet = fetchTargetTopics();
            for (String topic : this.whiteList) {
                Pattern pattern = Pattern.compile(topic);
                patterns.add(pattern);
            }

            TopicList topics = srcMQAdminExt.fetchAllTopicList();
            for (String topic : topics.getTopicList()) {
                if ((syncRETRY && topic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) ||
                    (syncDLQ && topic.startsWith(MixAll.DLQ_GROUP_TOPIC_PREFIX)) ||
                    !topic.equals(ConfigDefine.CONN_STORE_TOPIC)) {

                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(topic);
                        if (matcher.matches()) {
                            String targetTopic = generateTargetTopic(topic);
                            if (!targetTopicSet.contains(topic)) {
                                ensureTargetTopic(topic, targetTopic);
                            }

                            // different from BrokerData with cluster field, which can ensure the brokerData is from expected cluster.
                            // QueueData use brokerName as unique info on cluster of rocketmq. so when we want to get QueueData of
                            // expected cluster, we should get brokerNames of expected cluster, and then filter queueDatas.
                            List<BrokerData> brokerList = Utils.examineBrokerData(this.srcMQAdminExt, topic, srcCluster);
                            Set<String> brokerNameSet = new HashSet<String>();
                            for (BrokerData b : brokerList) {
                                brokerNameSet.add(b.getBrokerName());
                            }

                            TopicRouteData topicRouteData = srcMQAdminExt.examineTopicRouteInfo(topic);
                            if (!topicRouteMap.containsKey(topic)) {
                                topicRouteMap.put(topic, new ArrayList<TaskTopicInfo>());
                            }
                            for (QueueData qd : topicRouteData.getQueueDatas()) {
                                if (brokerNameSet.contains(qd.getBrokerName())) {
                                    for (int i = 0; i < qd.getReadQueueNums(); i++) {
                                        TaskTopicInfo taskTopicInfo = new TaskTopicInfo(topic, qd.getBrokerName(), i, targetTopic);
                                        topicRouteMap.get(topic).add(taskTopicInfo);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Fetch topic list error.", e);
        } finally {
            srcMQAdminExt.shutdown();
        }
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    public Map<String, List<TaskTopicInfo>> getTopicRouteMap() {
        return this.topicRouteMap;
    }

    public Set<String> fetchTargetTopics() throws RemotingException, MQClientException, InterruptedException {
        String targetCluster = this.replicatorConfig.getString(ConfigDefine.CONN_TARGET_CLUSTER);
        TopicList targetTopics = this.targetMQAdminExt.fetchTopicsByCLuster(targetCluster);
        return targetTopics.getTopicList();
    }

    /**
     * ensure target topic eixst. if target topic does not exist, ensureTopic will create target topic on target
     * cluster, with same TopicConfig but using target topic name. any exception will be caught and then throw
     * IllegalStateException.
     *
     * @param srcTopic
     * @param targetTopic
     * @throws RemotingException
     * @throws MQClientException
     * @throws InterruptedException
     */
    public void ensureTargetTopic(String srcTopic,
        String targetTopic) throws RemotingException, MQClientException, InterruptedException {
        String srcCluster = this.replicatorConfig.getString(ConfigDefine.CONN_SOURCE_CLUSTER);
        String targetCluster = this.replicatorConfig.getString(ConfigDefine.CONN_TARGET_CLUSTER);

        List<BrokerData> brokerList = Utils.examineBrokerData(this.srcMQAdminExt, srcTopic, srcCluster);
        if (brokerList.size() == 0) {
            throw new IllegalStateException(String.format("no broker found for srcTopic: %s srcCluster: %s", srcTopic, srcCluster));
        }

        String brokerAddr = brokerList.get(0).selectBrokerAddr();
        TopicConfig topicConfig = this.srcMQAdminExt.examineTopicConfig(brokerAddr, srcTopic);
        topicConfig.setTopicName(targetTopic);
        Utils.createTopic(this.targetMQAdminExt, topicConfig, targetCluster);

        throw new IllegalStateException("");
    }

    public String generateTargetTopic(String topic) {
        String fmt = this.replicatorConfig.getString(ConfigDefine.CONN_TOPIC_RENAME_FMT);
        if (StringUtils.isNotEmpty(fmt)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("topic", topic);
            return StrSubstitutor.replace(fmt, params);
        }
        return topic;
    }
}

