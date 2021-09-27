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

package org.apache.rocketmq.flink.legacy.example;

import org.apache.rocketmq.client.AccessChannel;
import org.apache.rocketmq.flink.legacy.RocketMQConfig;
import org.apache.rocketmq.flink.legacy.RocketMQSink;
import org.apache.rocketmq.flink.legacy.RocketMQSourceFunction;
import org.apache.rocketmq.flink.legacy.common.serialization.SimpleTupleDeserializationSchema;
import org.apache.rocketmq.flink.legacy.function.SinkMapFunction;
import org.apache.rocketmq.flink.legacy.function.SourceMapFunction;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Properties;

import static org.apache.rocketmq.flink.legacy.RocketMQConfig.CONSUMER_OFFSET_LATEST;
import static org.apache.rocketmq.flink.legacy.RocketMQConfig.DEFAULT_CONSUMER_TAG;

public class RocketMQFlinkExample {

    /**
     * Source Config
     *
     * @return properties
     */
    private static Properties getConsumerProps() {
        Properties consumerProps = new Properties();
        consumerProps.setProperty(
                RocketMQConfig.NAME_SERVER_ADDR,
                "http://${instanceId}.${region}.mq-internal.aliyuncs.com:8080");
        consumerProps.setProperty(RocketMQConfig.CONSUMER_GROUP, "${ConsumerGroup}");
        consumerProps.setProperty(RocketMQConfig.CONSUMER_TOPIC, "${SourceTopic}");
        consumerProps.setProperty(RocketMQConfig.CONSUMER_TAG, DEFAULT_CONSUMER_TAG);
        consumerProps.setProperty(RocketMQConfig.CONSUMER_OFFSET_RESET_TO, CONSUMER_OFFSET_LATEST);
        consumerProps.setProperty(RocketMQConfig.ACCESS_KEY, "${AccessKey}");
        consumerProps.setProperty(RocketMQConfig.SECRET_KEY, "${SecretKey}");
        consumerProps.setProperty(RocketMQConfig.ACCESS_CHANNEL, AccessChannel.CLOUD.name());
        return consumerProps;
    }

    /**
     * Sink Config
     *
     * @return properties
     */
    private static Properties getProducerProps() {
        Properties producerProps = new Properties();
        producerProps.setProperty(
                RocketMQConfig.NAME_SERVER_ADDR,
                "http://${instanceId}.${region}.mq-internal.aliyuncs.com:8080");
        producerProps.setProperty(RocketMQConfig.PRODUCER_GROUP, "${ProducerGroup}");
        producerProps.setProperty(RocketMQConfig.ACCESS_KEY, "${AccessKey}");
        producerProps.setProperty(RocketMQConfig.SECRET_KEY, "${SecretKey}");
        producerProps.setProperty(RocketMQConfig.ACCESS_CHANNEL, AccessChannel.CLOUD.name());
        return producerProps;
    }

    public static void main(String[] args) throws Exception {

        final ParameterTool params = ParameterTool.fromArgs(args);

        // for local
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment();

        // for cluster
        // StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.getConfig().setGlobalJobParameters(params);
        env.setStateBackend(new MemoryStateBackend());
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // start a checkpoint every 10s
        env.enableCheckpointing(10000);
        // advanced options:
        // set mode to exactly-once (this is the default)
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // checkpoints have to complete within one minute, or are discarded
        env.getCheckpointConfig().setCheckpointTimeout(60000);
        // make sure 500 ms of progress happen between checkpoints
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        // allow only one checkpoint to be in progress at the same time
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        // enable externalized checkpoints which are retained after job cancellation
        env.getCheckpointConfig()
                .enableExternalizedCheckpoints(
                        CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        Properties consumerProps = getConsumerProps();
        Properties producerProps = getProducerProps();

        SimpleTupleDeserializationSchema schema = new SimpleTupleDeserializationSchema();

        DataStreamSource<Tuple2<String, String>> source =
                env.addSource(new RocketMQSourceFunction<>(schema, consumerProps))
                        .setParallelism(2);

        source.print();
        source.process(new SourceMapFunction())
                .process(new SinkMapFunction("FLINK_SINK", "*"))
                .addSink(
                        new RocketMQSink(producerProps)
                                .withBatchFlushOnCheckpoint(true)
                                .withBatchSize(32)
                                .withAsync(true))
                .setParallelism(2);

        env.execute("rocketmq-connect-flink");
    }
}
