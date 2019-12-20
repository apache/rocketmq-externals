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

public class TaskDivideConfig {

    private String sourceNamesrvAddr;

    private String srcCluster;

    private String storeTopic;

    private String srcRecordConverter;

    private int dataType;

    private int taskParallelism;

    public TaskDivideConfig(String sourceNamesrvAddr, String srcCluster, String storeTopic, String srcRecordConverter,
                            int dataType, int taskParallelism) {
        this.sourceNamesrvAddr = sourceNamesrvAddr;
        this.srcCluster = srcCluster;
        this.storeTopic = storeTopic;
        this.srcRecordConverter = srcRecordConverter;
        this.dataType = dataType;
        this.taskParallelism = taskParallelism;
    }

    public String getSourceNamesrvAddr() {
        return sourceNamesrvAddr;
    }

    public void setSourceNamesrvAddr(String sourceNamesrvAddr) {
        this.sourceNamesrvAddr = sourceNamesrvAddr;
    }

    public String getStoreTopic() {
        return storeTopic;
    }

    public void setStoreTopic(String storeTopic) {
        this.storeTopic = storeTopic;
    }

    public String getSrcRecordConverter() {
        return srcRecordConverter;
    }

    public void setSrcRecordConverter(String srcRecordConverter) {
        this.srcRecordConverter = srcRecordConverter;
    }

    public int getDataType() {
        return dataType;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public int getTaskParallelism() {
        return taskParallelism;
    }

    public void setTaskParallelism(int taskParallelism) {
        this.taskParallelism = taskParallelism;
    }
}
