/*
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
package org.apache.rocketmq.replicator.config;

import io.openmessaging.KeyValue;
import java.util.HashSet;
import java.util.Set;
import org.apache.rocketmq.replicator.strategy.DivideStrategyEnum;
import org.apache.rocketmq.replicator.strategy.DivideTaskByQueue;
import org.apache.rocketmq.replicator.strategy.DivideTaskByTopic;
import org.apache.rocketmq.replicator.strategy.TaskDivideStrategy;

public class RmqConnectorConfig {

    private int taskParallelism;
    private Set<String> whiteList;
    private String srcNamesrvs;
    private String targetNamesrvs;
    private String srcCluster;
    private String targetCluster;
    private TaskDivideStrategy taskDivideStrategy;
    private String storeTopic;
    private String converter;
    private long refreshInterval;
    private String renamePattern;
    private String offsetSyncTopic;

    public RmqConnectorConfig() {
    }

    public void validate(KeyValue config) {
        this.taskParallelism = config.getInt(ConfigDefine.CONN_TASK_PARALLELISM, 1);

        int strategy = config.getInt(ConfigDefine.CONN_TASK_DIVIDE_STRATEGY, DivideStrategyEnum.BY_QUEUE.ordinal());
        if (strategy == DivideStrategyEnum.BY_QUEUE.ordinal()) {
            this.taskDivideStrategy = new DivideTaskByQueue();
        } else {
            this.taskDivideStrategy = new DivideTaskByTopic();
        }

        buildWhiteList(config);

        srcNamesrvs = config.getString(ConfigDefine.CONN_SOURCE_RMQ);
        srcCluster = config.getString(ConfigDefine.CONN_SOURCE_CLUSTER);
        targetNamesrvs = config.getString(ConfigDefine.CONN_TARGET_RMQ);
        targetCluster = config.getString(ConfigDefine.CONN_TARGET_CLUSTER);

        storeTopic = config.getString(ConfigDefine.CONN_STORE_TOPIC);
        converter = config.getString(ConfigDefine.CONN_SOURCE_RECORD_CONVERTER);
        refreshInterval = config.getLong(ConfigDefine.REFRESH_INTERVAL, 3);
        renamePattern = config.getString(ConfigDefine.CONN_TOPIC_RENAME_FMT);
        offsetSyncTopic = config.getString(ConfigDefine.OFFSET_SYNC_TOPIC);
    }

    private void buildWhiteList(KeyValue config) {
        this.whiteList = new HashSet<String>();
        String whileListStr = config.getString(ConfigDefine.CONN_WHITE_LIST, "");
        String[] wl = whileListStr.trim().split(",");
        if (wl.length <= 0)
            throw new IllegalArgumentException("White list must be not empty.");
        else {
            this.whiteList.clear();
            for (String t : wl) {
                this.whiteList.add(t.trim());
            }
        }
    }

    public int getTaskParallelism() {
        return this.taskParallelism;
    }

    public Set<String> getWhiteList() {
        return this.whiteList;
    }

    public String getSrcNamesrvs() {
        return this.srcNamesrvs;
    }

    public String getTargetNamesrvs() {
        return this.targetNamesrvs;
    }

    public String getSrcCluster() {
        return this.srcCluster;
    }

    public String getTargetCluster() {
        return this.targetCluster;
    }

    public TaskDivideStrategy getTaskDivideStrategy() {
        return this.taskDivideStrategy;
    }

    public String getStoreTopic() {
        return this.storeTopic;
    }

    public String getConverter() {
        return this.converter;
    }

    public long getRefreshInterval() {
        return this.refreshInterval;
    }

    public String getRenamePattern() {
        return this.renamePattern;
    }

    public String getOffsetSyncTopic() {
        return this.offsetSyncTopic;
    }
}
