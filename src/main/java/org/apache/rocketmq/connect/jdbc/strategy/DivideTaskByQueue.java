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
package org.apache.rocketmq.connect.jdbc.strategy;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import java.util.Set;
import org.apache.rocketmq.connect.jdbc.config.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DivideTaskByQueue extends TaskDivideStrategy {

    @Override
    public List<KeyValue> divide(DbConnectorConfig dbConnectorConfig, TaskDivideConfig tdc) {
        if (dbConnectorConfig instanceof SinkDbConnectorConfig){
            return divideSinkTaskByQueue(dbConnectorConfig, tdc);
        }
        return null;
    }

    public List<KeyValue> divideSinkTaskByQueue(DbConnectorConfig dbConnectorConfig, TaskDivideConfig tdc) {

        List<KeyValue> config = new ArrayList<KeyValue>();
        int parallelism = tdc.getTaskParallelism();
        Map<Integer, List<TaskTopicInfo>> queueTopicList = new HashMap<Integer, List<TaskTopicInfo>>();
        Map<String, Set<TaskTopicInfo>> topicRouteMap = ((SinkDbConnectorConfig)dbConnectorConfig).getTopicRouteMap();
        int id = -1;
        for (String t : topicRouteMap.keySet()) {
            for (TaskTopicInfo taskTopicInfo : topicRouteMap.get(t)) {
                int ind = ++id % parallelism;
                if (!queueTopicList.containsKey(ind)) {
                    queueTopicList.put(ind, new ArrayList<TaskTopicInfo>());
                }
                queueTopicList.get(ind).add(taskTopicInfo);
            }
        }

        for (int i = 0; i < parallelism; i++) {
            KeyValue keyValue = new DefaultKeyValue();
            keyValue.put(Config.CONN_DB_IP, tdc.getDbUrl());
            keyValue.put(Config.CONN_DB_PORT, tdc.getDbPort());
            keyValue.put(Config.CONN_DB_USERNAME, tdc.getDbUserName());
            keyValue.put(Config.CONN_DB_PASSWORD, tdc.getDbPassword());
            keyValue.put(Config.CONN_TOPIC_NAMES, JSONObject.toJSONString(queueTopicList.get(i)));
            keyValue.put(Config.CONN_DATA_TYPE, tdc.getDataType());
            keyValue.put(Config.CONN_SOURCE_RECORD_CONVERTER, tdc.getSrcRecordConverter());
            keyValue.put(Config.CONN_DB_MODE, tdc.getMode());
            config.add(keyValue);
        }

        return config;
    }
}
