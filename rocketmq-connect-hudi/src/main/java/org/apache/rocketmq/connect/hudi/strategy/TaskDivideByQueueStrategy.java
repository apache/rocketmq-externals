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

package org.apache.rocketmq.connect.hudi.strategy;

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connect.hudi.config.HudiConnectConfig;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class TaskDivideByQueueStrategy implements ITaskDivideStrategy {
    @Override
    public List<KeyValue> divide(KeyValue source) {
        List<KeyValue> config = new ArrayList<KeyValue>();
        int parallelism = source.getInt(HudiConnectConfig.CONN_TASK_PARALLELISM);
        Map<String, MessageQueue> topicRouteInfos = (Map<String, MessageQueue>) JSONObject.parse(source.getString(HudiConnectConfig.CONN_TOPIC_ROUTE_INFO));
        int id = 0;
        List<List<String>> taskTopicQueues = new ArrayList<>(parallelism);
        for (Map.Entry<String, MessageQueue> topicQueue : topicRouteInfos.entrySet()) {
            MessageQueue messageQueue = topicQueue.getValue();
            String topicQueueStr = messageQueue.getTopic() + "," + messageQueue.getBrokerName() + "," + messageQueue.getQueueId();
            int ind = ++id % parallelism;
            if (taskTopicQueues.get(ind) != null) {
                List<String> taskTopicQueue = new LinkedList<>();
                taskTopicQueue.add(topicQueueStr);
                taskTopicQueues.add(ind, taskTopicQueue);
            } else {
                List<String> taskTopicQueue = taskTopicQueues.get(ind);
                taskTopicQueue.add(topicQueueStr);
            }
        }

        for (int i = 0; i < parallelism; i++) {
            // build single task queue config; format is topicName1,brokerName1,queueId1;topicName1,brokerName1,queueId2
            String singleTaskTopicQueueStr = "";
            List<String> singleTaskTopicQueues = taskTopicQueues.get(i);
            for (String singleTopicQueue : singleTaskTopicQueues) {
                singleTaskTopicQueueStr += singleTopicQueue + ";";
            }
            singleTaskTopicQueueStr = singleTaskTopicQueueStr.substring(0, singleTaskTopicQueueStr.length() - 1);
            // fill connect config;
            KeyValue keyValue = new DefaultKeyValue();
            keyValue.put(HudiConnectConfig.CONN_TOPIC_QUEUES, singleTaskTopicQueueStr);
            keyValue.put(HudiConnectConfig.CONN_HUDI_TABLE_PATH, source.getString(HudiConnectConfig.CONN_HUDI_TABLE_PATH));
            keyValue.put(HudiConnectConfig.CONN_HUDI_TABLE_NAME, source.getString(HudiConnectConfig.CONN_HUDI_TABLE_NAME));
            keyValue.put(HudiConnectConfig.CONN_HUDI_INSERT_SHUFFLE_PARALLELISM, source.getInt(HudiConnectConfig.CONN_HUDI_INSERT_SHUFFLE_PARALLELISM));
            keyValue.put(HudiConnectConfig.CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM, source.getInt(HudiConnectConfig.CONN_HUDI_UPSERT_SHUFFLE_PARALLELISM));
            keyValue.put(HudiConnectConfig.CONN_HUDI_DELETE_PARALLELISM, source.getInt(HudiConnectConfig.CONN_HUDI_DELETE_PARALLELISM));
            keyValue.put(HudiConnectConfig.CONN_SOURCE_RECORD_CONVERTER, source.getString(HudiConnectConfig.CONN_SOURCE_RECORD_CONVERTER));
            keyValue.put(HudiConnectConfig.CONN_SCHEMA_PATH, source.getString(HudiConnectConfig.CONN_SCHEMA_PATH));
            keyValue.put(HudiConnectConfig.CONN_TASK_PARALLELISM, source.getInt(HudiConnectConfig.CONN_TASK_PARALLELISM));
            keyValue.put(HudiConnectConfig.CONN_SCHEMA_PATH, source.getString(HudiConnectConfig.CONN_SCHEMA_PATH));
            config.add(keyValue);
        }

        return config;
    }
}
