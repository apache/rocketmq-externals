
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

package org.apache.rocketmq.connect.cassandra.config;

import io.openmessaging.KeyValue;
import org.apache.rocketmq.connect.cassandra.strategy.TaskDivideStrategy;

public abstract class DbConnectorConfig {
    public TaskDivideStrategy taskDivideStrategy;
    public String dbUrl;
    public String dbPort;
    public String dbUserName;
    public String dbPassword;
    public String localDataCenter;
    public String converter;
    public int taskParallelism;
    public String mode;

    public abstract void validate(KeyValue config);

    public abstract <T> T getWhiteTopics();

    public TaskDivideStrategy getTaskDivideStrategy() {
        return taskDivideStrategy;
    }

    public void setTaskDivideStrategy(TaskDivideStrategy taskDivideStrategy) {
        this.taskDivideStrategy = taskDivideStrategy;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbPort() {
        return dbPort;
    }

    public void setDbPort(String dbPort) {
        this.dbPort = dbPort;
    }

    public String getDbUserName() {
        return dbUserName;
    }

    public void setDbUserName(String dbUserName) {
        this.dbUserName = dbUserName;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getLocalDataCenter() {
        return localDataCenter;
    }

    public void setLocalDataCenter(String localDataCenter) {
        this.localDataCenter = localDataCenter;
    }

    public String getConverter() {
        return converter;
    }

    public void setConverter(String converter) {
        this.converter = converter;
    }

    public int getTaskParallelism() {
        return taskParallelism;
    }

    public void setTaskParallelism(int taskParallelism) {
        this.taskParallelism = taskParallelism;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
