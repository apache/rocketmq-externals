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

import java.util.List;

public class TaskConfig {

    private String sourceCluster;
    private String storeTopic;
    private String sourceGroup;
    private String sourceRocketmq;
    private Integer dataType;
    private Long nextPosition;
    private String taskTopicList;
    private String taskGroupList;
    private String offsetSyncTopic;

    public String getSourceGroup() {
        return sourceGroup;
    }

    public void setSourceGroup(String sourceGroup) {
        this.sourceGroup = sourceGroup;
    }

    public String getStoreTopic() {
        return storeTopic;
    }

    public void setStoreTopic(String storeTopic) {
        this.storeTopic = storeTopic;
    }

    public String getSourceRocketmq() {
        return sourceRocketmq;
    }

    public void setSourceRocketmq(String sourceRocketmq) {
        this.sourceRocketmq = sourceRocketmq;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    public Long getNextPosition() {
        return nextPosition;
    }

    public void setNextPosition(Long nextPosition) {
        this.nextPosition = nextPosition;
    }

    public String getTaskTopicList() {
        return taskTopicList;
    }

    public void setTaskGroupList(String taskGroupList) {
        this.taskGroupList = taskGroupList;
    }

    public String getTaskGroupList() {
        return this.taskGroupList;
    }

    public void setTaskTopicList(String taskTopicList) {
        this.taskTopicList = taskTopicList;
    }

    public void setSourceCluster(String sourceCluster) {
        this.sourceCluster = sourceCluster;
    }

    public String getSourceCluster() {
        return this.sourceCluster;
    }

    public void setOffsetSyncTopic(String offsetSyncTopic) {
        this.offsetSyncTopic = offsetSyncTopic;
    }

    public String getOffsetSyncTopic() {
        return offsetSyncTopic;
    }
}
