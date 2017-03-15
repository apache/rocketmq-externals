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

package org.apache.rocketmq.flume.ng.sink;

/**
 *
 */
public class RocketMQSinkConstants {

    public static final String NAME_SERVER_CONFIG = "nameserver";

    public static final String TOPIC_CONFIG = "topic";
    public static final String TOPIC_DEFAULT = "FLUME_TOPIC";

    public static final String TAG_CONFIG = "tag";
    public static final String TAG_DEFAULT = "FLUME_TAG";

    public static final String PRODUCER_GROUP_CONFIG = "producerGroup";
    public static final String PRODUCER_GROUP_DEFAULT = "FLUME_PRODUCER_GROUP";

    public static final String BATCH_SIZE_CONFIG = "batchSize";
    public static final int BATCH_SIZE_DEFAULT = 1;

    public static final String MAX_PROCESS_TIME_CONFIG = "maxProcessTime";
    public static final long MAX_PROCESS_TIME_DEFAULT = 1000;
}
