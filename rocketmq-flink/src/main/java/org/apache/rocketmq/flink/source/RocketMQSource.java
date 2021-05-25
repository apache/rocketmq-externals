/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.source;

import org.apache.rocketmq.flink.source.enumerator.RocketMQSourceEnumState;
import org.apache.rocketmq.flink.source.enumerator.RocketMQSourceEnumStateSerializer;
import org.apache.rocketmq.flink.source.enumerator.RocketMQSourceEnumerator;
import org.apache.rocketmq.flink.source.reader.RocketMQPartitionSplitReader;
import org.apache.rocketmq.flink.source.reader.RocketMQRecordEmitter;
import org.apache.rocketmq.flink.source.reader.RocketMQSourceReader;
import org.apache.rocketmq.flink.source.reader.deserializer.RocketMQDeserializationSchema;
import org.apache.rocketmq.flink.source.split.RocketMQPartitionSplit;
import org.apache.rocketmq.flink.source.split.RocketMQPartitionSplitSerializer;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.Boundedness;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.source.SourceReader;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitReader;
import org.apache.flink.connector.base.source.reader.synchronization.FutureCompletingBlockingQueue;
import org.apache.flink.core.io.SimpleVersionedSerializer;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.util.UserCodeClassLoader;

import java.util.function.Supplier;

/** The Source implementation of RocketMQ. */
public class RocketMQSource<OUT>
        implements Source<OUT, RocketMQPartitionSplit, RocketMQSourceEnumState>,
                ResultTypeQueryable<OUT> {
    private static final long serialVersionUID = -1L;

    private final String topic;
    private final String consumerGroup;
    private final String nameServerAddress;
    private final String tag;
    private final long stopInMs;
    private final long startTime;
    private final long startOffset;
    private final long partitionDiscoveryIntervalMs;

    // Boundedness
    private final Boundedness boundedness;
    private final RocketMQDeserializationSchema<OUT> deserializationSchema;

    public RocketMQSource(
            String topic,
            String consumerGroup,
            String nameServerAddress,
            String tag,
            long stopInMs,
            long startTime,
            long startOffset,
            long partitionDiscoveryIntervalMs,
            Boundedness boundedness,
            RocketMQDeserializationSchema<OUT> deserializationSchema) {
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.nameServerAddress = nameServerAddress;
        this.tag = tag;
        this.stopInMs = stopInMs;
        this.startTime = startTime;
        this.startOffset = startOffset;
        this.partitionDiscoveryIntervalMs = partitionDiscoveryIntervalMs;
        this.boundedness = boundedness;
        this.deserializationSchema = deserializationSchema;
    }

    @Override
    public Boundedness getBoundedness() {
        return this.boundedness;
    }

    @Override
    public SourceReader<OUT, RocketMQPartitionSplit> createReader(
            SourceReaderContext readerContext) {
        FutureCompletingBlockingQueue<RecordsWithSplitIds<Tuple3<OUT, Long, Long>>> elementsQueue =
                new FutureCompletingBlockingQueue<>();
        deserializationSchema.open(
                new DeserializationSchema.InitializationContext() {
                    @Override
                    public MetricGroup getMetricGroup() {
                        return readerContext.metricGroup();
                    }

                    @Override
                    public UserCodeClassLoader getUserCodeClassLoader() {
                        return null;
                    }
                });

        Supplier<SplitReader<Tuple3<OUT, Long, Long>, RocketMQPartitionSplit>> splitReaderSupplier =
                () ->
                        new RocketMQPartitionSplitReader<>(
                                topic,
                                consumerGroup,
                                nameServerAddress,
                                tag,
                                stopInMs,
                                startTime,
                                startOffset,
                                deserializationSchema);
        RocketMQRecordEmitter<OUT> recordEmitter = new RocketMQRecordEmitter<>();

        return new RocketMQSourceReader<>(
                elementsQueue,
                splitReaderSupplier,
                recordEmitter,
                new Configuration(),
                readerContext);
    }

    @Override
    public SplitEnumerator<RocketMQPartitionSplit, RocketMQSourceEnumState> createEnumerator(
            SplitEnumeratorContext<RocketMQPartitionSplit> enumContext) {
        return new RocketMQSourceEnumerator(
                topic,
                consumerGroup,
                nameServerAddress,
                stopInMs,
                startOffset,
                partitionDiscoveryIntervalMs,
                boundedness,
                enumContext);
    }

    @Override
    public SplitEnumerator<RocketMQPartitionSplit, RocketMQSourceEnumState> restoreEnumerator(
            SplitEnumeratorContext<RocketMQPartitionSplit> enumContext,
            RocketMQSourceEnumState checkpoint) {
        return new RocketMQSourceEnumerator(
                topic,
                consumerGroup,
                nameServerAddress,
                stopInMs,
                startOffset,
                partitionDiscoveryIntervalMs,
                boundedness,
                enumContext,
                checkpoint.getCurrentAssignment());
    }

    @Override
    public SimpleVersionedSerializer<RocketMQPartitionSplit> getSplitSerializer() {
        return new RocketMQPartitionSplitSerializer();
    }

    @Override
    public SimpleVersionedSerializer<RocketMQSourceEnumState> getEnumeratorCheckpointSerializer() {
        return new RocketMQSourceEnumStateSerializer();
    }

    @Override
    public TypeInformation<OUT> getProducedType() {
        return deserializationSchema.getProducedType();
    }
}
