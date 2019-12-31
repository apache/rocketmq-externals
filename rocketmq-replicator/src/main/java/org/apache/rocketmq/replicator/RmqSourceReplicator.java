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
import org.apache.rocketmq.replicator.config.RmqConnectorConfig;
import org.apache.rocketmq.replicator.config.TaskDivideConfig;
import org.apache.rocketmq.replicator.config.TaskTopicInfo;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RmqSourceReplicator extends SourceConnector {

    private static final Logger log = LoggerFactory.getLogger(RmqSourceReplicator.class);

    private boolean syncDLQ = false;

    private boolean syncRETRY = false;

    private RmqConnectorConfig replicatorConfig;

    private Map<String, Set<TaskTopicInfo>> topicRouteMap;

    private volatile boolean configValid = false;

    private DefaultMQAdminExt srcMQAdminExt;
    private DefaultMQAdminExt targetMQAdminExt;

    private volatile boolean adminStarted;

    private ScheduledExecutorService executor;

    public RmqSourceReplicator() {
        topicRouteMap = new HashMap<>();
        replicatorConfig = new RmqConnectorConfig();
        executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("RmqSourceReplicator-SourceWatcher-%d").daemon(true).build());
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.srcMQAdminExt.setNamesrvAddr(this.replicatorConfig.getSrcNamesrvs());
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getSrcNamesrvs()));

        this.targetMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.targetMQAdminExt.setNamesrvAddr(this.replicatorConfig.getTargetNamesrvs());
        this.targetMQAdminExt.setAdminExtGroup(Utils.createGroupName(ConstDefine.REPLICATOR_ADMIN_PREFIX));
        this.targetMQAdminExt.setInstanceName(Utils.createInstanceName(this.replicatorConfig.getTargetNamesrvs()));

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

        try {
            this.replicatorConfig.validate(config);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        this.configValid = true;
        return "";
    }

    @Override
    public void start() {
        startMQAdminTools();
        buildRoute();
        startListner();
    }

    public void startListner() {
        executor.scheduleAtFixedRate(new Runnable() {

            boolean first = true;
            Map<String, Set<TaskTopicInfo>> origin = null;


            @Override public void run() {

                buildRoute();
                if (first) {
                    origin = new HashMap<>(topicRouteMap);
                    first = false;
                }
                if (!compare(origin, topicRouteMap)) {
                    context.requestTaskReconfiguration();
                    origin = new HashMap<>(topicRouteMap);
                }
            }
        }, replicatorConfig.getRefreshInterval(), replicatorConfig.getRefreshInterval(), TimeUnit.SECONDS);
    }

    public boolean compare(Map<String, Set<TaskTopicInfo>> origin, Map<String, Set<TaskTopicInfo>> updated) {
        if (origin.size() != updated.size()) {
            return false;
        }
        for (Map.Entry<String, Set<TaskTopicInfo>> entry : origin.entrySet()) {
            if (!updated.containsKey(entry.getKey())) {
                return false;
            }
            Set<TaskTopicInfo> originTasks = entry.getValue();
            Set<TaskTopicInfo> updateTasks = updated.get(entry.getKey());
            if (originTasks.size() != updateTasks.size()) {
                return false;
            }

            if (!originTasks.containsAll(updateTasks)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void stop() {
        executor.shutdown();
        this.srcMQAdminExt.shutdown();
        this.targetMQAdminExt.shutdown();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
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
            this.replicatorConfig.getSrcNamesrvs(),
            this.replicatorConfig.getSrcCluster(),
            this.replicatorConfig.getStoreTopic(),
            this.replicatorConfig.getConverter(),
            DataType.COMMON_MESSAGE.ordinal(),
            this.replicatorConfig.getTaskParallelism()
        );
        return this.replicatorConfig.getTaskDivideStrategy().divide(this.topicRouteMap, tdc);
    }

    public void buildRoute() {
        List<Pattern> patterns = new ArrayList<Pattern>();
        String srcCluster = this.replicatorConfig.getSrcCluster();
        try {
            Set<String> targetTopicSet = fetchTargetTopics();
            for (String topic : this.replicatorConfig.getWhiteList()) {
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
                            if (!targetTopicSet.contains(targetTopic)) {
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
                                topicRouteMap.put(topic, new HashSet<>(16));
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
        }
    }

    public Map<String, Set<TaskTopicInfo>> getTopicRouteMap() {
        return this.topicRouteMap;
    }

    public Set<String> fetchTargetTopics() throws RemotingException, MQClientException, InterruptedException {
        String targetCluster = this.replicatorConfig.getTargetCluster();
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
        String srcCluster = this.replicatorConfig.getSrcCluster();
        String targetCluster = this.replicatorConfig.getTargetCluster();

        List<BrokerData> brokerList = Utils.examineBrokerData(this.srcMQAdminExt, srcTopic, srcCluster);
        if (brokerList.size() == 0) {
            throw new IllegalStateException(String.format("no broker found for srcTopic: %s srcCluster: %s", srcTopic, srcCluster));
        }

        final TopicRouteData topicRouteData = this.srcMQAdminExt.examineTopicRouteInfo(srcTopic);
        final TopicConfig topicConfig = new TopicConfig();
        final List<QueueData> queueDatas = topicRouteData.getQueueDatas();
        QueueData queueData = queueDatas.get(0);
        topicConfig.setPerm(queueData.getPerm());
        topicConfig.setReadQueueNums(queueData.getReadQueueNums());
        topicConfig.setWriteQueueNums(queueData.getWriteQueueNums());
        topicConfig.setTopicSysFlag(queueData.getTopicSynFlag());
        topicConfig.setTopicName(targetTopic);
        Utils.createTopic(this.targetMQAdminExt, topicConfig, targetCluster);
    }

    public String generateTargetTopic(String topic) {
        String fmt = this.replicatorConfig.getRenamePattern();
        if (StringUtils.isNotEmpty(fmt)) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("topic", topic);
            return StrSubstitutor.replace(fmt, params);
        }
        return topic;
    }
}

