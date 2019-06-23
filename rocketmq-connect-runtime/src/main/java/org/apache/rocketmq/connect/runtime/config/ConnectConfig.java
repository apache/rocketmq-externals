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

package org.apache.rocketmq.connect.runtime.config;

import java.io.File;
import org.apache.rocketmq.remoting.protocol.LanguageCode;

/**
 * Configurations for runtime.
 */
public class ConnectConfig {

    /**
     * Worker id to distinguish with other workers. Should be unique in a cluster.
     */
    private String workerId = "DEFAULT_WORKER_1";

    /**
     * Storage directory for file store.
     */
    private String storePathRootDir = System.getProperty("user.home") + File.separator + "connectorStore";

    private String namesrvAddr;

    private String rmqProducerGroup = "defaultProducerGroup";

    private int maxMessageSize;

    private LanguageCode language;

    private int operationTimeout = 3000;

    private String rmqConsumerGroup = "defaultConsumerGroup";

    private int rmqMaxRedeliveryTimes;

    private int rmqMessageConsumeTimeout = 3000;

    private int rmqMaxConsumeThreadNums = 32;

    private int rmqMinConsumeThreadNums = 1;

    /**
     * Http port for REST API.
     */
    private int httpPort = 8081;

    /**
     * Source task position persistence interval.
     */
    private int positionPersistInterval = 20 * 1000;

    /**
     * Connector configuration persistence interval.
     */
    private int configPersistInterval = 20 * 1000;

    private String pluginPaths;


    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public String getRmqProducerGroup() {
        return rmqProducerGroup;
    }

    public void setRmqProducerGroup(String rmqProducerGroup) {
        this.rmqProducerGroup = rmqProducerGroup;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public void setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
    }

    public LanguageCode getLanguage() {
        return language;
    }

    public void setLanguage(LanguageCode language) {
        this.language = language;
    }

    public int getOperationTimeout() {
        return operationTimeout;
    }

    public void setOperationTimeout(int operationTimeout) {
        this.operationTimeout = operationTimeout;
    }

    public String getRmqConsumerGroup() {
        return rmqConsumerGroup;
    }

    public void setRmqConsumerGroup(String rmqConsumerGroup) {
        this.rmqConsumerGroup = rmqConsumerGroup;
    }

    public int getRmqMaxRedeliveryTimes() {
        return rmqMaxRedeliveryTimes;
    }

    public void setRmqMaxRedeliveryTimes(int rmqMaxRedeliveryTimes) {
        this.rmqMaxRedeliveryTimes = rmqMaxRedeliveryTimes;
    }

    public int getRmqMessageConsumeTimeout() {
        return rmqMessageConsumeTimeout;
    }

    public void setRmqMessageConsumeTimeout(int rmqMessageConsumeTimeout) {
        this.rmqMessageConsumeTimeout = rmqMessageConsumeTimeout;
    }

    public int getRmqMaxConsumeThreadNums() {
        return rmqMaxConsumeThreadNums;
    }

    public void setRmqMaxConsumeThreadNums(int rmqMaxConsumeThreadNums) {
        this.rmqMaxConsumeThreadNums = rmqMaxConsumeThreadNums;
    }

    public int getRmqMinConsumeThreadNums() {
        return rmqMinConsumeThreadNums;
    }

    public void setRmqMinConsumeThreadNums(int rmqMinConsumeThreadNums) {
        this.rmqMinConsumeThreadNums = rmqMinConsumeThreadNums;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getStorePathRootDir() {
        return storePathRootDir;
    }

    public void setStorePathRootDir(String storePathRootDir) {
        this.storePathRootDir = storePathRootDir;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getPositionPersistInterval() {
        return positionPersistInterval;
    }

    public void setPositionPersistInterval(int positionPersistInterval) {
        this.positionPersistInterval = positionPersistInterval;
    }

    public int getConfigPersistInterval() {
        return configPersistInterval;
    }

    public void setConfigPersistInterval(int configPersistInterval) {
        this.configPersistInterval = configPersistInterval;
    }

    public String getPluginPaths() {
        return pluginPaths;
    }

    public void setPluginPaths(String pluginPaths) {
        this.pluginPaths = pluginPaths;
    }
}
