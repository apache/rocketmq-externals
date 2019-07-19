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
package org.apache.rocketmq.connector.config;

import java.util.HashSet;
import java.util.Set;

public class ConfigDefine {

    public static final String SOURCE_RMQ = "sourceRocketmq";

    public static final String STORE_TOPIC = "storeTopic";

    public static final String TARGET_RMQ = "targetRocketmq";

    public static final String DATA_TYPE = "dataType";

    public static final String QUEUE_ID = "queueId";

    public static final String TASK_DIVIDE_STRATEGY = "taskDivideStrategy";

    public static final String BROKER_NAME = "brokerName";

    public static final String SOURCE_TOPIC = "sourceTopic";

    public static final Set<String> REQUEST_CONFIG = new HashSet<String>(){
        {
            add("sourceRocketmq");
            add("targetRocketmq");
            add("storeTopic");
            add("taskDivideStrategy");
        }
    };
}
