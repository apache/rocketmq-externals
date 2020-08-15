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
package org.apache.rocketmq.connect.jdbc.config;

public class TaskDivideConfig {

    private String dbUrl;

    private String dbPort;

    private String dbUserName;

    private String dbPassword;

    private String srcRecordConverter;

    private int dataType;

    private int taskParallelism;

    private String mode;

    public TaskDivideConfig(String dbUrl, String dbPort, String dbUserName, String dbPassword, String srcRecordConverter,
                            int dataType, int taskParallelism, String mode) {
        this.dbUrl = dbUrl;
        this.dbPort = dbPort;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
        this.srcRecordConverter = srcRecordConverter;
        this.dataType = dataType;
        this.taskParallelism = taskParallelism;
        this.mode = mode;
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
