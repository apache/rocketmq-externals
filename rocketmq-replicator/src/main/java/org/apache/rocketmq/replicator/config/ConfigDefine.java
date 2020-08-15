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
package org.apache.rocketmq.replicator.config;

import java.util.HashSet;
import java.util.Set;

public class ConfigDefine {

    public static final String CONN_SOURCE_RMQ = "source-rocketmq";
    public static final String CONN_SOURCE_CLUSTER = "source-cluster";

    public static final String CONN_STORE_TOPIC = "replicator-store-topic";

    public static final String CONN_TARGET_RMQ = "target-rocketmq";
    public static final String CONN_TARGET_CLUSTER = "target-cluster";

    public static final String CONN_SOURCE_GROUP = "source-group";

    public static final String CONN_DATA_TYPE = "data-type";

    public static final String CONN_TASK_DIVIDE_STRATEGY = "task-divide-strategy";

    public static final String CONN_WHITE_LIST = "white-list";

    public static final String CONN_SOURCE_RECORD_CONVERTER = "source-record-converter";

    public static final String CONN_TASK_PARALLELISM = "task-parallelism";

    public static final String CONN_TOPIC_RENAME_FMT = "topic.rename.format";

    public static final String REFRESH_INTERVAL = "refresh.interval";

    public static final String OFFSET_SYNC_TOPIC = "offset.sync.topic";

    /**
     * The required key for all configurations.
     */
    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add(CONN_SOURCE_RMQ);
            add(CONN_TARGET_RMQ);
            add(CONN_STORE_TOPIC);
            add(CONN_WHITE_LIST);
            add(CONN_SOURCE_RECORD_CONVERTER);
        }
    };
}
