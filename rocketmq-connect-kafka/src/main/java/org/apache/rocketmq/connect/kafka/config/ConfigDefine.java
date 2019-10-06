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

package org.apache.rocketmq.connect.kafka.config;

import java.util.*;

public class ConfigDefine {

    public static String TASK_NUM = "tasks.num";
    public static String TOPICS = "kafka.topics";
    public static String GROUP_ID = "kafka.group.id";
    public static String BOOTSTRAP_SERVER = "kafka.bootstrap.server";
    public static String CONNECTOR_CLASS = "connector-class";
    public static String SOURCE_RECORD_CONVERTER = "source-record-converter";
    public static String ROCKETMQ_TOPIC = "rocketmq.topic";

    private String bootstrapServers;
    private String topics;
    private String groupId;

    public String getTopics() {
        return topics;
    }

    public void setTopics(String topics) {
        this.topics = topics;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public static final Set<String> REQUEST_CONFIG = new HashSet<String>(){
        {
            add(TOPICS);
            add(GROUP_ID);
            add(BOOTSTRAP_SERVER);
        }
    };
}
