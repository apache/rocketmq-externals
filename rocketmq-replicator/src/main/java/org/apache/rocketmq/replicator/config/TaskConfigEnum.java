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
package org.apache.rocketmq.replicator.config;

public enum TaskConfigEnum {

    TASK_ID("taskId"),
    TASK_SOURCE_GROUP("sourceGroup"),
    TASK_SOURCE_ROCKETMQ("sourceRocketmq"),
    TASK_SOURCE_CLUSTER("sourceCluster"),
    TASK_OFFSET_SYNC_TOPIC("offsetSyncTopic"),
    TASK_SOURCE_TOPIC("sourceTopic"),
    TASK_STORE_ROCKETMQ("storeTopic"),
    TASK_DATA_TYPE("dataType"),
    TASK_BROKER_NAME("brokerName"),
    TASK_QUEUE_ID("queueId"),
    TASK_NEXT_POSITION("nextPosition"),
    TASK_TOPIC_INFO("taskTopicList"),
    TASK_GROUP_INFO("taskGroupList"),
    TASK_SOURCE_RECORD_CONVERTER("source-record-converter");

    private String key;

    TaskConfigEnum(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
