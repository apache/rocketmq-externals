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

package org.apache.rocketmq.connect.cassandra.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import java.util.ArrayList;
import java.util.List;
import org.apache.rocketmq.connect.cassandra.common.DataType;
import org.apache.rocketmq.connect.cassandra.config.Config;
import org.apache.rocketmq.connect.cassandra.config.DbConnectorConfig;
import org.apache.rocketmq.connect.cassandra.config.SourceDbConnectorConfig;
import org.apache.rocketmq.connect.cassandra.config.TaskDivideConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraSourceConnector extends SourceConnector {
    private static final Logger log = LoggerFactory.getLogger(org.apache.rocketmq.connect.cassandra.connector.CassandraSourceConnector.class);
    private DbConnectorConfig dbConnectorConfig;
    private volatile boolean configValid = false;

    public CassandraSourceConnector() {
        dbConnectorConfig = new SourceDbConnectorConfig();
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {

        log.info("CassandraSourceConnector verifyAndSetConfig enter");
        for (String requestKey : Config.REQUEST_CONFIG) {

            if (!config.containsKey(requestKey)) {
                return "Request config key: " + requestKey;
            }
        }
        try {
            this.dbConnectorConfig.validate(config);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        this.configValid = true;

        return "";
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return CassandraSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        log.info("List.start");
        if (!configValid) {
            return new ArrayList<KeyValue>();
        }

        TaskDivideConfig tdc = new TaskDivideConfig(
                this.dbConnectorConfig.getDbUrl(),
                this.dbConnectorConfig.getDbPort(),
                this.dbConnectorConfig.getDbUserName(),
                this.dbConnectorConfig.getDbPassword(),
                this.dbConnectorConfig.getLocalDataCenter(),
                this.dbConnectorConfig.getConverter(),
                DataType.COMMON_MESSAGE.ordinal(),
                this.dbConnectorConfig.getTaskParallelism(),
                this.dbConnectorConfig.getMode()
        );
        return this.dbConnectorConfig.getTaskDivideStrategy().divide(this.dbConnectorConfig, tdc);
    }

}
