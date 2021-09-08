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

package org.apache.rocketmq.connect.hudi.connector;


import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.sink.SinkConnector;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.common.protocol.route.QueueData;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.connect.hudi.config.HudiConnectConfig;
import org.apache.rocketmq.connect.hudi.config.SinkConnectConfig;
import org.apache.rocketmq.connect.hudi.config.CloneUtils;
import org.apache.rocketmq.connect.hudi.config.Utils;
import org.apache.rocketmq.connect.hudi.strategy.ITaskDivideStrategy;
import org.apache.rocketmq.connect.hudi.strategy.TaskDivideStrategyFactory;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


public class HudiSinkConnector extends SinkConnector {
    private static final Logger log = LoggerFactory.getLogger(HudiSinkConnector.class);
    private volatile boolean configValid = false;
    private ScheduledExecutorService executor;
    private HashMap<String, Set<MessageQueue>> topicRouteMap;

    private DefaultMQAdminExt srcMQAdminExt;
    private SinkConnectConfig sinkConnectConfig;

    private volatile boolean adminStarted;

    private ScheduledFuture<?> listenerHandle;
    public static final String HUDI_CONNECTOR_ADMIN_PREFIX = "HUDI-CONNECTOR-ADMIN";
    public static final String PREFIX = "hudi";

    public HudiSinkConnector() {
        topicRouteMap = new HashMap<>();
        sinkConnectConfig = new SinkConnectConfig();
        executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("HudiFSinkConnector-SinkWatcher-%d").daemon(true).build());
    }

    private synchronized void startMQAdminTools() {
        if (!configValid || adminStarted) {
            return;
        }
        RPCHook rpcHook = null;
        this.srcMQAdminExt = new DefaultMQAdminExt(rpcHook);
        this.srcMQAdminExt.setNamesrvAddr(this.sinkConnectConfig.getSrcNamesrvs());
        this.srcMQAdminExt.setAdminExtGroup(Utils.createGroupName(HUDI_CONNECTOR_ADMIN_PREFIX));
        this.srcMQAdminExt.setInstanceName(Utils.createInstanceName(this.sinkConnectConfig.getSrcNamesrvs()));

        try {
            log.info("Trying to start srcMQAdminExt");
            this.srcMQAdminExt.start();
            log.info("RocketMQ srcMQAdminExt started");

        } catch (MQClientException e) {
            log.error("Hudi Sink Task start failed for `srcMQAdminExt` exception.", e);
        }

        adminStarted = true;
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {
        for (String requestKey : HudiConnectConfig.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        try {
            this.sinkConnectConfig.validate(config);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        this.configValid = true;

        return "";
    }

    @Override
    public void start() {
        startMQAdminTools();
        startListener();
    }

    public void startListener() {
        listenerHandle = executor.scheduleAtFixedRate(new Runnable() {
            boolean first = true;
            HashMap<String, Set<MessageQueue>> origin = null;

            @Override
            public void run() {
                buildRoute();
                if (first) {
                    origin = CloneUtils.clone(topicRouteMap);
                    first = false;
                }
                if (!compare(origin, topicRouteMap)) {
                    context.requestTaskReconfiguration();
                    origin = CloneUtils.clone(topicRouteMap);
                }
            }
        }, sinkConnectConfig.getRefreshInterval(), sinkConnectConfig.getRefreshInterval(), TimeUnit.SECONDS);
    }

    public boolean compare(Map<String, Set<MessageQueue>> origin, Map<String, Set<MessageQueue>> updated) {
        if (origin.size() != updated.size()) {
            return false;
        }
        for (Map.Entry<String, Set<MessageQueue>> entry : origin.entrySet()) {
            if (!updated.containsKey(entry.getKey())) {
                return false;
            }
            Set<MessageQueue> originTasks = entry.getValue();
            Set<MessageQueue> updateTasks = updated.get(entry.getKey());
            if (originTasks.size() != updateTasks.size()) {
                return false;
            }

            if (!originTasks.containsAll(updateTasks)) {
                return false;
            }
        }

        return true;
    }

    public void buildRoute() {
        String srcCluster = this.sinkConnectConfig.getSrcCluster();
        try {
            for (String topic : this.sinkConnectConfig.getWhiteList()) {

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
                            MessageQueue taskTopicInfo = new MessageQueue(topic, qd.getBrokerName(), i);
                            topicRouteMap.get(topic).add(taskTopicInfo);
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


    /**
     * We need to reason why we don't call srcMQAdminExt.shutdown() here, and why
     * it can be applied to srcMQAdminExt
     */
    @Override
    public void stop() {
        listenerHandle.cancel(true);
        srcMQAdminExt.shutdown();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return HudiSinkTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        log.info("List.start");
        if (!configValid) {
            return new ArrayList<KeyValue>();
        }
        startMQAdminTools();
        buildRoute();
        DefaultKeyValue defaultKeyValue = new DefaultKeyValue();
        defaultKeyValue.put(HudiConnectConfig.CONN_HUDI_TABLE_PATH, sinkConnectConfig.getTablePath());
        defaultKeyValue.put(HudiConnectConfig.CONN_HUDI_TABLE_NAME, sinkConnectConfig.getTableName());
        defaultKeyValue.put(HudiConnectConfig.CONN_HUDI_INSERT_SHUFFLE_PARALLELISM, sinkConnectConfig.getInsertShuffleParallelism());
        defaultKeyValue.put(HudiConnectConfig.CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM, sinkConnectConfig.getUpsertShuffleParallelism());
        defaultKeyValue.put(HudiConnectConfig.CONN_HUDI_DELETE_PARALLELISM, sinkConnectConfig.getDeleteParallelism());
        defaultKeyValue.put(HudiConnectConfig.CONN_SOURCE_RECORD_CONVERTER, sinkConnectConfig.getSrcRecordConverter());
        defaultKeyValue.put(HudiConnectConfig.CONN_TOPIC_NAMES, sinkConnectConfig.getTopicNames());
        defaultKeyValue.put(HudiConnectConfig.CONN_SCHEMA_PATH, sinkConnectConfig.getSchemaPath());
        defaultKeyValue.put(HudiConnectConfig.CONN_TASK_PARALLELISM, sinkConnectConfig.getTaskParallelism());
        defaultKeyValue.put(HudiConnectConfig.CONN_TASK_DIVIDE_STRATEGY, sinkConnectConfig.getTaskDivideStrategy());
        defaultKeyValue.put(HudiConnectConfig.CONN_WHITE_LIST, JSONObject.toJSONString(sinkConnectConfig.getWhiteList()));
        defaultKeyValue.put(HudiConnectConfig.CONN_SCHEMA_PATH, sinkConnectConfig.getSchemaPath());
        defaultKeyValue.put(HudiConnectConfig.CONN_TOPIC_ROUTE_INFO, JSONObject.toJSONString(sinkConnectConfig.getTopicRouteMap()));
        log.info("taskConfig : " + defaultKeyValue + ", sinkConnectConfig : " + sinkConnectConfig);
        ITaskDivideStrategy strategy = TaskDivideStrategyFactory.getInstance();
        List<KeyValue> taskConfigs = strategy.divide(defaultKeyValue);
        return taskConfigs;
    }
}
