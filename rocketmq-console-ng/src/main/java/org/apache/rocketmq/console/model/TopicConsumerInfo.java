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
package org.apache.rocketmq.console.model;

import com.google.common.collect.Lists;

import java.util.List;

public class TopicConsumerInfo {
    private String topic;
    private long diffTotal;
    private long lastTimestamp;
    private List<QueueStatInfo> queueStatInfoList = Lists.newArrayList();

    public TopicConsumerInfo(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public long getDiffTotal() {
        return diffTotal;
    }

    public void setDiffTotal(long diffTotal) {
        this.diffTotal = diffTotal;
    }

    public List<QueueStatInfo> getQueueStatInfoList() {
        return queueStatInfoList;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public void appendQueueStatInfo(QueueStatInfo queueStatInfo) {
        queueStatInfoList.add(queueStatInfo);
        diffTotal = diffTotal + (queueStatInfo.getBrokerOffset() - queueStatInfo.getConsumerOffset());
        lastTimestamp = Math.max(lastTimestamp, queueStatInfo.getLastTimestamp());
    }
}
