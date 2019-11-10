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

package org.apache.rocketmq.connect.runtime.config;

import io.openmessaging.connector.api.data.DataEntry;
import java.util.HashSet;
import java.util.Set;

/**
 * Define keys for connector and task configs.
 */
public class RuntimeConfigDefine {

    /**
     * The full class name of a specific connector implements.
     */
    public static final String CONNECTOR_CLASS = "connector-class";

    public static final String TASK_CLASS = "task-class";

    /**
     * Last updated time of the configuration.
     */
    public static final String UPDATE_TIMESATMP = "update-timestamp";

    /**
     * Whether the current config is deleted.
     */
    public static final String CONFIG_DELETED = "config-deleted";

    /**
     * The full class name of record converter. Which is used to parse {@link DataEntry} to/from byte[].
     */
    public static final String SOURCE_RECORD_CONVERTER = "source-record-converter";

    public static final String NAMESRV_ADDR = "namesrv-addr";

    public static final String RMQ_PRODUCER_GROUP = "rmq-producer-group";

    public static final String RMQ_CONSUMNER_GROUP = "rmq-consumer-group";

    public static final String OPERATION_TIMEOUT = "operation-timeout";

    public static final String HASH_FUNC = "consistentHashFunc";

    public static final String VIRTUAL_NODE = "virtualNode";

    public static final String CONNECT_SHARDINGKEY = "connect-shardingkey";

    public static final String CONNECT_TOPICNAME = "connect-topicname";

    public static final String CONNECT_SOURCE_PARTITION = "connect-source-partition";

    public static final String CONNECT_SOURCE_POSITION = "connect-source-position";

    public static final String CONNECT_ENTRYTYPE = "connect-entrytype";

    public static final String CONNECT_TIMESTAMP = "connect-timestamp";

    public static final String CONNECT_SCHEMA = "connect-schema";

    /**
     * The required key for all configurations.
     */
    public static final Set<String> REQUEST_CONFIG = new HashSet<String>() {
        {
            add(CONNECTOR_CLASS);
            add(SOURCE_RECORD_CONVERTER);
        }
    };

    /**
     * Maximum allowed message size in bytes, the default vaule is 4M.
     */
    public static final int MAX_MESSAGE_SIZE = Integer.parseInt(System.getProperty("rocketmq.runtime.max.message.size", "4194304"));

}
