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

package org.apache.rocketmq.connect.kafka.connector;

import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.Task;
import io.openmessaging.connector.api.source.SourceConnector;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.connect.kafka.config.ConfigDefine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class KafkaSourceConnector extends SourceConnector {
    private static final Logger log = LoggerFactory.getLogger(KafkaSourceConnector.class);

    private KeyValue connectConfig;

    public KafkaSourceConnector() {
        super();
    }

    @Override
    public String verifyAndSetConfig(KeyValue config) {

        log.info("KafkaSourceConnector verifyAndSetConfig enter");
        for (String key : config.keySet()) {
            log.info("connector verifyAndSetConfig: key:{}, value:{}", key, config.getString(key));
        }

        for (String requestKey : ConfigDefine.REQUEST_CONFIG) {
            if (!config.containsKey(requestKey)) {
                return "Request Config key: " + requestKey;
            }
        }
        this.connectConfig = config;
        return "";
    }

    @Override
    public void start() {
        log.info("KafkaSourceConnector start enter");
    }

    @Override
    public void stop() {
        log.info("KafkaSourceConnector stop enter");
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public Class<? extends Task> taskClass() {
        return KafkaSourceTask.class;
    }

    @Override
    public List<KeyValue> taskConfigs() {
        if (connectConfig == null) {
            return new ArrayList<KeyValue>();
        }

        log.info("Source Connector taskConfigs enter");
        List<KeyValue> configs = new ArrayList<>();
        int task_num = connectConfig.getInt(ConfigDefine.TASK_NUM);
        log.info("Source Connector taskConfigs: task_num:" + task_num);
        for (int i = 0; i < task_num; ++i) {
            KeyValue config = new DefaultKeyValue();
            config.put(ConfigDefine.BOOTSTRAP_SERVER, connectConfig.getString(ConfigDefine.BOOTSTRAP_SERVER));
            config.put(ConfigDefine.TOPICS, connectConfig.getString(ConfigDefine.TOPICS));
            config.put(ConfigDefine.GROUP_ID, connectConfig.getString(ConfigDefine.GROUP_ID));

            config.put(ConfigDefine.CONNECTOR_CLASS, connectConfig.getString(ConfigDefine.CONNECTOR_CLASS));
            config.put(ConfigDefine.SOURCE_RECORD_CONVERTER, connectConfig.getString(ConfigDefine.SOURCE_RECORD_CONVERTER));
            configs.add(config);
        }
        return configs;
    }
}
