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

package org.apache.rocketmq.connect.hudi.config;

import io.openmessaging.KeyValue;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SinkConnectConfig extends HudiConnectConfig {
    private Set<String> whiteList;
    private String srcNamesrvs;
    private String srcCluster;
    private long refreshInterval;
    private Map<String, Set<MessageQueue>> topicRouteMap;
    public int taskParallelism;
    private String taskDivideStrategy;

    public SinkConnectConfig(){
    }

    public void validate(KeyValue config) {
        buildWhiteList(config);
        this.tablePath = config.getString(HudiConnectConfig.CONN_HUDI_TABLE_PATH);
        this.tableName = config.getString(HudiConnectConfig.CONN_HUDI_TABLE_NAME);
        this.insertShuffleParallelism = config.getInt(HudiConnectConfig.CONN_HUDI_INSERT_SHUFFLE_PARALLELISM);
        this.deleteParallelism = config.getInt(HudiConnectConfig.CONN_HUDI_DELETE_PARALLELISM);
        this.upsertShuffleParallelism = config.getInt(HudiConnectConfig.CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM);
        this.setSrcRecordConverter(config.getString(HudiConnectConfig.CONN_SOURCE_RECORD_CONVERTER));
        this.setTopicNames(config.getString(HudiConnectConfig.CONN_TOPIC_NAMES));
        this.setSchemaPath(config.getString(HudiConnectConfig.CONN_SCHEMA_PATH));

        this.srcNamesrvs = config.getString(HudiConnectConfig.CONN_SOURCE_RMQ);
        this.srcCluster = config.getString(HudiConnectConfig.CONN_SOURCE_CLUSTER);
        this.refreshInterval = config.getLong(HudiConnectConfig.REFRESH_INTERVAL, 3);

    }

    private void buildWhiteList(KeyValue config) {
        this.whiteList = new HashSet<>();
        String whiteListStr = config.getString(HudiConnectConfig.CONN_TOPIC_NAMES, "");
        String[] wl = whiteListStr.trim().split(",");
        if (wl.length <= 0)
            throw new IllegalArgumentException("White list must be not empty.");
        else {
            this.whiteList.clear();
            for (String t : wl) {
                this.whiteList.add(t.trim());
            }
        }
    }


    public Set<String> getWhiteList() {
        return whiteList;
    }

    public void setWhiteList(Set<String> whiteList) {
        this.whiteList = whiteList;
    }

    public String getSrcNamesrvs() {
        return this.srcNamesrvs;
    }

    public String getSrcCluster() {
        return this.srcCluster;
    }

    public long getRefreshInterval() {
        return this.refreshInterval;
    }

    public Map<String, Set<MessageQueue>> getTopicRouteMap() {
        return topicRouteMap;
    }

    public void setTopicRouteMap(Map<String, Set<MessageQueue>> topicRouteMap) {
        this.topicRouteMap = topicRouteMap;
    }

    public Set<String> getWhiteTopics() {
        return getWhiteList();
    }

    public int getTaskParallelism() {
        return taskParallelism;
    }

    public void setTaskParallelism(int taskParallelism) {
        this.taskParallelism = taskParallelism;
    }

    public String getTaskDivideStrategy() {
        return taskDivideStrategy;
    }

    public void setTaskDivideStrategy(String taskDivideStrategy) {
        this.taskDivideStrategy = taskDivideStrategy;
    }

    @Override
    public String toString() {
        return "SinkConnectConfig{" +
                "whiteList=" + whiteList +
                ", srcNamesrvs='" + srcNamesrvs + '\'' +
                ", srcCluster='" + srcCluster + '\'' +
                ", refreshInterval=" + refreshInterval +
                ", topicRouteMap=" + topicRouteMap +
                ", tableType='" + tableType + '\'' +
                ", tablePath='" + tablePath + '\'' +
                ", tableName='" + tableName + '\'' +
                ", insertShuffleParallelism=" + insertShuffleParallelism +
                ", upsertShuffleParallelism=" + upsertShuffleParallelism +
                ", deleteParallelism=" + deleteParallelism +
                ", indexType='" + indexType + '\'' +
                ", schemaPath='" + schemaPath + '\'' +
                ", schema=" + schema +
                '}';
    }
}
