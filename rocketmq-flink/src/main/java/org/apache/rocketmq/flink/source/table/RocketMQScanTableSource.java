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

package org.apache.rocketmq.flink.source.table;

import org.apache.rocketmq.flink.legacy.RocketMQConfig;
import org.apache.rocketmq.flink.legacy.RocketMQSourceFunction;
import org.apache.rocketmq.flink.legacy.common.serialization.KeyValueDeserializationSchema;
import org.apache.rocketmq.flink.legacy.common.serialization.RowKeyValueDeserializationSchema;
import org.apache.rocketmq.flink.source.RocketMQSource;
import org.apache.rocketmq.flink.source.reader.deserializer.BytesMessage;
import org.apache.rocketmq.flink.source.reader.deserializer.RocketMQDeserializationSchema;
import org.apache.rocketmq.flink.source.reader.deserializer.RocketMQRowDeserializationSchema;
import org.apache.rocketmq.flink.source.reader.deserializer.RowDeserializationSchema.MetadataConverter;

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.SourceProvider;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.types.DataType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.apache.flink.api.connector.source.Boundedness.BOUNDED;
import static org.apache.flink.api.connector.source.Boundedness.CONTINUOUS_UNBOUNDED;

/** Defines the scan table source of RocketMQ. */
public class RocketMQScanTableSource implements ScanTableSource, SupportsReadingMetadata {

    private final DescriptorProperties properties;
    private final TableSchema schema;

    private final String topic;
    private final String consumerGroup;
    private final String nameServerAddress;
    private final String tag;

    private final long stopInMs;
    private final long partitionDiscoveryIntervalMs;
    private final long startMessageOffset;
    private final long startTime;
    private final boolean useNewApi;

    private List<String> metadataKeys;

    public RocketMQScanTableSource(
            DescriptorProperties properties,
            TableSchema schema,
            String topic,
            String consumerGroup,
            String nameServerAddress,
            String tag,
            long stopInMs,
            long startMessageOffset,
            long startTime,
            long partitionDiscoveryIntervalMs,
            boolean useNewApi) {
        this.properties = properties;
        this.schema = schema;
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.nameServerAddress = nameServerAddress;
        this.tag = tag;
        this.stopInMs = stopInMs;
        this.startMessageOffset = startMessageOffset;
        this.startTime = startTime;
        this.partitionDiscoveryIntervalMs = partitionDiscoveryIntervalMs;
        this.useNewApi = useNewApi;
        this.metadataKeys = Collections.emptyList();
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.insertOnly();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
        if (useNewApi) {
            return SourceProvider.of(
                    new RocketMQSource<>(
                            topic,
                            consumerGroup,
                            nameServerAddress,
                            tag,
                            stopInMs,
                            startTime,
                            startMessageOffset < 0 ? 0 : startMessageOffset,
                            partitionDiscoveryIntervalMs,
                            isBounded() ? BOUNDED : CONTINUOUS_UNBOUNDED,
                            createRocketMQDeserializationSchema()));
        } else {
            return SourceFunctionProvider.of(
                    new RocketMQSourceFunction<>(
                            createKeyValueDeserializationSchema(), getConsumerProps()),
                    isBounded());
        }
    }

    @Override
    public Map<String, DataType> listReadableMetadata() {
        final Map<String, DataType> metadataMap = new LinkedHashMap<>();
        Stream.of(ReadableMetadata.values())
                .forEachOrdered(m -> metadataMap.putIfAbsent(m.key, m.dataType));
        return metadataMap;
    }

    @Override
    public void applyReadableMetadata(List<String> metadataKeys, DataType producedDataType) {
        this.metadataKeys = metadataKeys;
    }

    @Override
    public DynamicTableSource copy() {
        RocketMQScanTableSource tableSource =
                new RocketMQScanTableSource(
                        properties,
                        schema,
                        topic,
                        consumerGroup,
                        nameServerAddress,
                        tag,
                        stopInMs,
                        startMessageOffset,
                        startTime,
                        partitionDiscoveryIntervalMs,
                        useNewApi);
        tableSource.metadataKeys = metadataKeys;
        return tableSource;
    }

    @Override
    public String asSummaryString() {
        return RocketMQScanTableSource.class.getName();
    }

    private RocketMQDeserializationSchema<RowData> createRocketMQDeserializationSchema() {
        final MetadataConverter[] metadataConverters =
                metadataKeys.stream()
                        .map(
                                k ->
                                        Stream.of(ReadableMetadata.values())
                                                .filter(rm -> rm.key.equals(k))
                                                .findFirst()
                                                .orElseThrow(IllegalStateException::new))
                        .map(m -> m.converter)
                        .toArray(MetadataConverter[]::new);
        return new RocketMQRowDeserializationSchema(
                schema, properties.asMap(), metadataKeys.size() > 0, metadataConverters);
    }

    private boolean isBounded() {
        return stopInMs != Long.MAX_VALUE;
    }

    private KeyValueDeserializationSchema<RowData> createKeyValueDeserializationSchema() {
        return new RowKeyValueDeserializationSchema.Builder()
                .setProperties(properties.asMap())
                .setTableSchema(schema)
                .build();
    }

    private Properties getConsumerProps() {
        Properties consumerProps = new Properties();
        consumerProps.setProperty(RocketMQConfig.CONSUMER_TOPIC, topic);
        consumerProps.setProperty(RocketMQConfig.CONSUMER_GROUP, consumerGroup);
        consumerProps.setProperty(RocketMQConfig.NAME_SERVER_ADDR, nameServerAddress);
        consumerProps.setProperty(RocketMQConfig.CONSUMER_TAG, tag);
        return consumerProps;
    }

    // --------------------------------------------------------------------------------------------
    // Metadata handling
    // --------------------------------------------------------------------------------------------

    enum ReadableMetadata {
        TOPIC(
                "topic",
                DataTypes.STRING().notNull(),
                new MetadataConverter() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object read(BytesMessage message) {
                        return StringData.fromString(
                                String.valueOf(message.getProperty("__topic__")));
                    }
                });

        final String key;

        final DataType dataType;

        final MetadataConverter converter;

        ReadableMetadata(String key, DataType dataType, MetadataConverter converter) {
            this.key = key;
            this.dataType = dataType;
            this.converter = converter;
        }
    }
}
