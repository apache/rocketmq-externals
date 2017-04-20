/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.spark;

public class ConsumerConfig {

    /**
     * Maximum rate (number of records per second) at which data will be read from each RocketMq partition ,
     * and the default value is "-1", it means consumer can pull message from rocketmq as fast as the consumer can.
     * Other that, you also enables or disables Spark Streaming's internal backpressure mechanism by the config
     *  "spark.streaming.backpressure.enabled".
     */
    public static final String  MAX_PULL_SPEED_PER_PARTITION = "pull.max.speed.per.partition";

    /**
     * To pick up the consume speed, the consumer can pull a batch of messages at a time. And the default
     * value is "32"
     */
    public static final String PULL_MAX_BATCH_SIZE = "pull.max.batch.size";

    /**
     * pull timeout for the consumer, and the default time is "3000".
     */
    public static final String PULL_TIMEOUT_MS = "pull.timeout.ms";


    /**
     * the configs for consumer cache
     */
    public static final String PULL_CONSUMER_CACHE_INIT_CAPACITY = "pull.consumer.cache.initialCapacity";

    public static final String PULL_CONSUMER_CACHE_MAX_CAPACITY = "pull.consumer.cache.maxCapacity";

    public static final String PULL_CONSUMER_CACHE_LOAD_FACTOR = "pull.consumer.cache.loadFactor";

    /**
     * name server address
     */
    public static final String ROCKETMQ_NAME_SERVER = "rocket.mq.name.server";


}