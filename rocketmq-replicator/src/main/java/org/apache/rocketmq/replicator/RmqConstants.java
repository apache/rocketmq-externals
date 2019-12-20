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
package org.apache.rocketmq.replicator;

public class RmqConstants {

    public static final String BROKER_NAME = "brokerName";

    public static final String TOPIC_NAME = "topic";

    public static final String QUEUE_ID = "queueId";

    public static final String NEXT_POSITION = "nextPosition";

    public static final String SOURCE_INSTANCE_NAME = "REPLICATOR_SOURCE_CONSUMER";

    public static String getPartition(String topic, String broker, String queueId) {
        return new StringBuilder().append(broker).append(topic).append(queueId).toString();
    }

    public static String getOffsetTag(String topic, String broker, String queueId, String group) {
        return new StringBuilder().append(broker).append(topic).append(queueId).append(group).toString();
    }
}
