/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.spark;

import org.apache.commons.lang.Validate;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.remoting.common.RemotingUtil;

import java.util.Properties;
import java.util.UUID;

/**
 * RocketMQConfig for Consumer
 */
public class RocketMQConfig {

    public static final String MQ_PULL_CONSUMER_PROVIDER_FACTORY_NAME = "mq.pull.consumer.provider.factory.name";
    public static final String DEFAULT_MQ_PULL_CONSUMER_PROVIDER_FACTORY_NAME = "DefaultSimpleFactory";

    // ------- the following is for common usage -------
    /**
     * RocketMq name server address
     */
    public static final String NAME_SERVER_ADDR = "nameserver.addr"; // Required

    public static final String CLIENT_NAME = "client.name";

    public static final String CLIENT_IP = "client.ip";
    public static final String DEFAULT_CLIENT_IP = RemotingUtil.getLocalAddress();

    public static final String CLIENT_CALLBACK_EXECUTOR_THREADS = "client.callback.executor.threads";
    public static final int DEFAULT_CLIENT_CALLBACK_EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();

    public static final String NAME_SERVER_POLL_INTERVAL = "nameserver.poll.interval";
    public static final int DEFAULT_NAME_SERVER_POLL_INTERVAL = 30000; // 30 seconds

    public static final String BROKER_HEART_BEAT_INTERVAL = "brokerserver.heartbeat.interval";
    public static final int DEFAULT_BROKER_HEART_BEAT_INTERVAL = 30000; // 30 seconds


    // ------- the following is for push consumer mode -------
    /**
     * RocketMq consumer group
      */
    public static final String CONSUMER_GROUP = "consumer.group"; // Required

    /**
     * RocketMq consumer topic
     */
    public static final String CONSUMER_TOPIC = "consumer.topic"; // Required

    public static final String CONSUMER_TAG = "consumer.tag";
    public static final String DEFAULT_TAG = "*";

    public static final String CONSUMER_OFFSET_RESET_TO = "consumer.offset.reset.to";
    public static final String CONSUMER_OFFSET_LATEST = "latest";
    public static final String CONSUMER_OFFSET_EARLIEST = "earliest";
    public static final String CONSUMER_OFFSET_TIMESTAMP = "timestamp";

    public static final String CONSUMER_MESSAGES_ORDERLY = "consumer.messages.orderly";

    public static final String CONSUMER_OFFSET_PERSIST_INTERVAL = "consumer.offset.persist.interval";
    public static final int DEFAULT_CONSUMER_OFFSET_PERSIST_INTERVAL = 5000; // 5 seconds

    public static final String CONSUMER_MIN_THREADS = "consumer.min.threads";
    public static final int DEFAULT_CONSUMER_MIN_THREADS = 20;

    public static final String CONSUMER_MAX_THREADS = "consumer.max.threads";
    public static final int DEFAULT_CONSUMER_MAX_THREADS = 64;


    // ------- the following is for reliable Receiver -------
    public static final String QUEUE_SIZE = "spout.queue.size";
    public static final int DEFAULT_QUEUE_SIZE = 500;

    public static final String MESSAGES_MAX_RETRY = "spout.messages.max.retry";
    public static final int DEFAULT_MESSAGES_MAX_RETRY = 3;

    public static final String MESSAGES_TTL = "spout.messages.ttl";
    public static final int DEFAULT_MESSAGES_TTL = 300000;  // 5min


    // ------- the following is for pull consumer mode -------

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

    // the following configs for consumer cache
    public static final String PULL_CONSUMER_CACHE_INIT_CAPACITY = "pull.consumer.cache.initialCapacity";
    public static final String PULL_CONSUMER_CACHE_MAX_CAPACITY = "pull.consumer.cache.maxCapacity";
    public static final String PULL_CONSUMER_CACHE_LOAD_FACTOR = "pull.consumer.cache.loadFactor";


    public static void buildConsumerConfigs(Properties props, DefaultMQPushConsumer consumer) {
        buildCommonConfigs(props, consumer);

        String group = props.getProperty(CONSUMER_GROUP);
        Validate.notEmpty(group);
        consumer.setConsumerGroup(group);

        consumer.setPersistConsumerOffsetInterval(getInteger(props,
                CONSUMER_OFFSET_PERSIST_INTERVAL, DEFAULT_CONSUMER_OFFSET_PERSIST_INTERVAL));
        consumer.setConsumeThreadMin(getInteger(props,
                CONSUMER_MIN_THREADS, DEFAULT_CONSUMER_MIN_THREADS));
        consumer.setConsumeThreadMax(getInteger(props,
                CONSUMER_MAX_THREADS, DEFAULT_CONSUMER_MAX_THREADS));

        String initOffset = props.getProperty(CONSUMER_OFFSET_RESET_TO, CONSUMER_OFFSET_LATEST);
        switch (initOffset) {
            case CONSUMER_OFFSET_EARLIEST:
                consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
                break;
            case CONSUMER_OFFSET_LATEST:
                consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
                break;
            case CONSUMER_OFFSET_TIMESTAMP:
                consumer.setConsumeTimestamp(initOffset);
                break;
            default:
                consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        }

        String topic = props.getProperty(CONSUMER_TOPIC);
        Validate.notEmpty(topic);
        try {
            consumer.subscribe(topic, props.getProperty(CONSUMER_TAG, DEFAULT_TAG));
        } catch (MQClientException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void buildCommonConfigs(Properties props, ClientConfig client) {
        String namesvr = props.getProperty(NAME_SERVER_ADDR);
        Validate.notEmpty(namesvr);
        client.setNamesrvAddr(namesvr);

        client.setClientIP(props.getProperty(CLIENT_IP, DEFAULT_CLIENT_IP));
        // use UUID for client name by default
        String defaultClientName = UUID.randomUUID().toString();
        client.setInstanceName(props.getProperty(CLIENT_NAME, defaultClientName));

        client.setClientCallbackExecutorThreads(getInteger(props,
                CLIENT_CALLBACK_EXECUTOR_THREADS, DEFAULT_CLIENT_CALLBACK_EXECUTOR_THREADS));
        client.setPollNameServerInterval(getInteger(props,
                NAME_SERVER_POLL_INTERVAL, DEFAULT_NAME_SERVER_POLL_INTERVAL));
        client.setHeartbeatBrokerInterval(getInteger(props,
                BROKER_HEART_BEAT_INTERVAL, DEFAULT_BROKER_HEART_BEAT_INTERVAL));
    }

    public static int getInteger(Properties props, String key, int defaultValue) {
        return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
    }

    public static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(defaultValue)));
    }

}
