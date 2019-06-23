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
package org.apache.rocketmq.connector.strategy;

import io.openmessaging.KeyValue;
import io.openmessaging.internal.DefaultKeyValue;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connector.config.ConfigDefine;
import org.apache.rocketmq.connector.config.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DivideTaskByTopic extends TaskDivideStrategy {

    public List<KeyValue> divide(Map<String, List<MessageQueue>> topicRouteMap, String source, String storeTopic) {

        List<KeyValue> config = new ArrayList<KeyValue>();

        for (String t: topicRouteMap.keySet()) {
            KeyValue keyValue = new DefaultKeyValue();
            keyValue.put(ConfigDefine.STORE_TOPIC, storeTopic);
            keyValue.put(ConfigDefine.SOURCE_RMQ, source);
            keyValue.put(ConfigDefine.STORE_TOPIC, t);
            keyValue.put(ConfigDefine.BROKER_NAME, "");
            keyValue.put(ConfigDefine.QUEUE_ID, "");
            keyValue.put(ConfigDefine.SOURCE_TOPIC, t);
            keyValue.put(ConfigDefine.DATA_TYPE, DataType.COMMON_MESSAGE.ordinal());
            config.add(keyValue);
        }

        return config;
    }
}
