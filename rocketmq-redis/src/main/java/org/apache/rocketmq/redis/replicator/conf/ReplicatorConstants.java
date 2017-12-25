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

package org.apache.rocketmq.redis.replicator.conf;

public class ReplicatorConstants {

    public static final String ROOT_DIR = "/redis-replicator";

    public static final String CONFIG_FILE_SYS_PROP_NAME = "conf";

    public static final String CONFIG_FILE = "/replicator.conf";

    public static final String CONFIG_PROP_ZK_ADDRESS = "zookeeper.address";

    public static final String REDIS_URI = "redis.uri";

    public static final String ROCKETMQ_NAMESERVER_ADDRESS = "rocketmq.nameserver.address";

    public static final String ROCKETMQ_PRODUCER_GROUP_NAME = "rocketmq.producer.groupname";

    public static final String ROCKETMQ_CONSUMER_GROUP_NAME = "rocketmq.consumer.groupname";

    public static final String ROCKETMQ_DATA_TOPIC = "rocketmq.data.topic";

    public static final String DEPLOY_MODEL = "deploy.model";

    public static final String DEPLOY_MODEL_CLUSTER = "cluster";

    public static final String DEPLOY_MODEL_SINGLE = "single";
}
