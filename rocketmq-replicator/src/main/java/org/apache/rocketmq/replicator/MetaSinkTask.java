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

package org.apache.rocketmq.replicator;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.common.QueueMetaData;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.common.protocol.route.BrokerData;
import org.apache.rocketmq.replicator.common.Utils;
import org.apache.rocketmq.replicator.config.ConfigUtil;
import org.apache.rocketmq.replicator.config.TaskConfig;
import org.apache.rocketmq.replicator.schema.FieldName;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaSinkTask extends SinkTask {

    private Logger log = LoggerFactory.getLogger(MetaSinkTask.class);
    private final String taskId;
    private final TaskConfig taskConfig;
    private DefaultMQAdminExt targetMQAdmin;
    private volatile boolean started = false;
    private final Map<String, String> brokerRoute;
    private ScheduledExecutorService executor;
    private long refreshInterval = 10 * 1000;

    public MetaSinkTask() {
        this.taskConfig = new TaskConfig();
        this.brokerRoute = new HashMap<>();
        this.taskId = Utils.createTaskId(Thread.currentThread().getName());
        this.executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("MetaSinkTask-SinkWatcher-%d").daemon(true).build());
    }

    @Override
    public void put(Collection<SinkDataEntry> sinkDataEntries) {
        for (SinkDataEntry dataEntry : sinkDataEntries) {
            if (this.started) {
                Schema schema = dataEntry.getSchema();
                Object[] payload = dataEntry.getPayload();

                String brokerName = (String) getPayloadValue(FieldName.BROKER_NAME.getKey(), schema, payload);
                String topic = (String) getPayloadValue(FieldName.TOPIC.getKey(), schema, payload);
                Integer queueId = (Integer) getPayloadValue(FieldName.QUEUE_ID.getKey(), schema, payload);
                Long offset = (Long) getPayloadValue(FieldName.OFFSET.getKey(), schema, payload);
                String consumeGroup = (String) getPayloadValue(FieldName.CONSUME_GROUP.getKey(), schema, payload);
                String brokerAddr = this.brokerRoute.get(brokerName);
                MessageQueue queue = new MessageQueue(topic, brokerName, queueId);

                try {
                    this.targetMQAdmin.updateConsumeOffset(brokerAddr, consumeGroup, queue, offset);
                } catch (Exception e) {
                    log.error("RocketMQ replicator mata task put error, brokerAddr={}, queue={}, offset={}", brokerAddr, queue, offset, e);
                    throw new RuntimeException(e);
                }
            }
        }

        if (!this.started) {
            log.error("RocketMQ replicator mata sink task is not started.");
            throw new RuntimeException("RocketMQ replicator mata sink task is not started.");
        }
    }

    private Object getPayloadValue(String fieldName, Schema schema, Object[] payload) {
        Field field = schema.getField(fieldName);
        if (field == null && payload.length > field.getIndex()) {
            return null;
        }

        return payload[field.getIndex()];
    }

    @Override
    public void commit(Map<QueueMetaData, Long> map) {
    }

    @Override
    public void start(KeyValue config) {
        ConfigUtil.load(config, this.taskConfig);
        try {
            this.targetMQAdmin = Utils.startTargetMQAdminTool(this.taskConfig);
        } catch (MQClientException e) {
            log.error("RocketMQ mata sink task start error. taskId={}, taskConfig={}", this.taskId, this.taskConfig, e);
            throw new RuntimeException(String.format("RocketMQ MetaSinkTask start error. taskId=%s", this.taskId));
        }
        buildBrokerRoute();
        this.executor.scheduleAtFixedRate(this::buildBrokerRoute, refreshInterval, refreshInterval, TimeUnit.MILLISECONDS);
        this.started = true;
        log.info("RocketMQ mata sink task started");
    }

    @Override
    public void stop() {
        if (this.started) {
            this.started = false;
            this.executor.shutdown();
            this.targetMQAdmin.shutdown();
            log.info("RocketMQ mata sink task stop. taskId={}", this.taskId);
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    private void buildBrokerRoute() {
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = this.targetMQAdmin.examineBrokerClusterInfo();
        } catch (Exception e) {
            log.error("RocketMQ Replicator build broker route error for `examineBrokerClusterInfo`", e);
        }

        if (clusterInfo == null) {
            return;
        }

        Map<String, BrokerData> brokerDataTable = clusterInfo.getBrokerAddrTable();
        if (brokerDataTable == null) {
            return;
        }

        for (BrokerData brokerData : brokerDataTable.values()) {
            this.brokerRoute.put(brokerData.getBrokerName(), brokerData.selectBrokerAddr());
        }
    }
}
