package org.apache.rocketmq.replicator.strategy;/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alibaba.fastjson.JSONObject;
import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.common.consistenthash.ConsistentHashRouter;
import org.apache.rocketmq.common.consistenthash.Node;
import org.apache.rocketmq.replicator.config.DataType;
import org.apache.rocketmq.replicator.config.TaskConfigEnum;
import org.apache.rocketmq.replicator.config.TaskDivideConfig;
import org.apache.rocketmq.replicator.config.TaskTopicInfo;

public class DivideTaskByConsistentHash extends TaskDivideStrategy {
    @Override public List<KeyValue> divide(Map<String, Set<TaskTopicInfo>> topicMap, TaskDivideConfig tdc) {

        List<KeyValue> config = new ArrayList<>();
        int parallelism = tdc.getTaskParallelism();
        Map<Integer, List<TaskTopicInfo>> queueTopicList = new HashMap<>();
        int id = -1;

        Collection<ClientNode> cidNodes = new ArrayList<>();
        for (int i = 0; i < parallelism; i++) {
            cidNodes.add(new ClientNode(i, Integer.toString(i)));
            queueTopicList.put(i, new ArrayList<>());
        }

        ConsistentHashRouter<ClientNode> router = new ConsistentHashRouter<>(cidNodes, cidNodes.size());

        for (String t : topicMap.keySet()) {
            for (TaskTopicInfo queue : topicMap.get(t)) {
                ClientNode clientNode = router.routeNode(queue.toString());
                if (clientNode != null) {
                    queueTopicList.get(clientNode.index).add(queue);
                }
            }
        }

        for (int i = 0; i < parallelism; i++) {
            KeyValue keyValue = new DefaultKeyValue();
            keyValue.put(TaskConfigEnum.TASK_STORE_ROCKETMQ.getKey(), tdc.getStoreTopic());
            keyValue.put(TaskConfigEnum.TASK_SOURCE_ROCKETMQ.getKey(), tdc.getSourceNamesrvAddr());
            keyValue.put(TaskConfigEnum.TASK_DATA_TYPE.getKey(), DataType.COMMON_MESSAGE.ordinal());
            keyValue.put(TaskConfigEnum.TASK_TOPIC_INFO.getKey(), JSONObject.toJSONString(queueTopicList.get(i)));
            keyValue.put(TaskConfigEnum.TASK_SOURCE_RECORD_CONVERTER.getKey(), tdc.getSrcRecordConverter());
            config.add(keyValue);
        }

        return config;
    }

    private static class ClientNode implements Node {
        private final String clientID;
        private final int index;

        public ClientNode(int index, String clientID) {
            this.index = index;
            this.clientID = clientID;
        }

        @Override
        public String getKey() {
            return clientID;
        }
    }
}
