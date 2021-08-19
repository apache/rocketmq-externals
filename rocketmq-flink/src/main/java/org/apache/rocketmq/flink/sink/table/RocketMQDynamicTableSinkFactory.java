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

package org.apache.rocketmq.flink.sink.table;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.factories.DynamicTableSinkFactory;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.flink.table.factories.FactoryUtil.createTableFactoryHelper;
import static org.apache.rocketmq.flink.common.RocketMQOptions.NAME_SERVER_ADDRESS;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_ENCODING;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_FIELD_DELIMITER;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_TAG;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN_WRITE_INCLUDED;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_IS_DYNAMIC_TAG;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_KEYS_TO_BODY;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_KEY_COLUMNS;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_RETRY_TIMES;
import static org.apache.rocketmq.flink.common.RocketMQOptions.OPTIONAL_WRITE_SLEEP_TIME_MS;
import static org.apache.rocketmq.flink.common.RocketMQOptions.PRODUCER_GROUP;
import static org.apache.rocketmq.flink.common.RocketMQOptions.TOPIC;

/**
 * Defines the {@link DynamicTableSinkFactory} implementation to create {@link
 * RocketMQDynamicTableSink}.
 */
public class RocketMQDynamicTableSinkFactory implements DynamicTableSinkFactory {

    @Override
    public String factoryIdentifier() {
        return "rocketmq";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(TOPIC);
        requiredOptions.add(PRODUCER_GROUP);
        requiredOptions.add(NAME_SERVER_ADDRESS);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(OPTIONAL_TAG);
        optionalOptions.add(OPTIONAL_WRITE_RETRY_TIMES);
        optionalOptions.add(OPTIONAL_WRITE_SLEEP_TIME_MS);
        optionalOptions.add(OPTIONAL_WRITE_IS_DYNAMIC_TAG);
        optionalOptions.add(OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN);
        optionalOptions.add(OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN_WRITE_INCLUDED);
        optionalOptions.add(OPTIONAL_WRITE_KEYS_TO_BODY);
        optionalOptions.add(OPTIONAL_WRITE_KEY_COLUMNS);
        optionalOptions.add(OPTIONAL_ENCODING);
        optionalOptions.add(OPTIONAL_FIELD_DELIMITER);
        return optionalOptions;
    }

    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        transformContext(this, context);
        FactoryUtil.TableFactoryHelper helper = createTableFactoryHelper(this, context);
        helper.validate();
        Map<String, String> rawProperties = context.getCatalogTable().getOptions();
        Configuration properties = Configuration.fromMap(rawProperties);
        String topicName = properties.getString(TOPIC);
        String producerGroup = properties.getString(PRODUCER_GROUP);
        String nameServerAddress = properties.getString(NAME_SERVER_ADDRESS);
        String tag = properties.getString(OPTIONAL_TAG);
        String dynamicColumn = properties.getString(OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN);
        String encoding = properties.getString(OPTIONAL_ENCODING);
        String fieldDelimiter = properties.getString(OPTIONAL_FIELD_DELIMITER);
        int retryTimes = properties.getInteger(OPTIONAL_WRITE_RETRY_TIMES);
        long sleepTimeMs = properties.getLong(OPTIONAL_WRITE_SLEEP_TIME_MS);
        boolean isDynamicTag = properties.getBoolean(OPTIONAL_WRITE_IS_DYNAMIC_TAG);
        boolean isDynamicTagIncluded =
                properties.getBoolean(OPTIONAL_WRITE_DYNAMIC_TAG_COLUMN_WRITE_INCLUDED);
        boolean writeKeysToBody = properties.getBoolean(OPTIONAL_WRITE_KEYS_TO_BODY);
        String keyColumnsConfig = properties.getString(OPTIONAL_WRITE_KEY_COLUMNS);
        String[] keyColumns = new String[0];
        if (keyColumnsConfig != null && keyColumnsConfig.length() > 0) {
            keyColumns = keyColumnsConfig.split(",");
        }
        DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(rawProperties);
        TableSchema physicalSchema =
                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
        return new RocketMQDynamicTableSink(
                descriptorProperties,
                physicalSchema,
                topicName,
                producerGroup,
                nameServerAddress,
                tag,
                dynamicColumn,
                fieldDelimiter,
                encoding,
                sleepTimeMs,
                retryTimes,
                isDynamicTag,
                isDynamicTagIncluded,
                writeKeysToBody,
                keyColumns);
    }

    private void transformContext(
            DynamicTableFactory factory, DynamicTableFactory.Context context) {
        Map<String, String> catalogOptions = context.getCatalogTable().getOptions();
        Map<String, String> convertedOptions =
                normalizeOptionCaseAsFactory(factory, catalogOptions);
        catalogOptions.clear();
        for (Map.Entry<String, String> entry : convertedOptions.entrySet()) {
            catalogOptions.put(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, String> normalizeOptionCaseAsFactory(
            Factory factory, Map<String, String> options) {
        Map<String, String> normalizedOptions = new HashMap<>();
        Map<String, String> requiredOptionKeysLowerCaseToOriginal =
                factory.requiredOptions().stream()
                        .collect(
                                Collectors.toMap(
                                        option -> option.key().toLowerCase(), ConfigOption::key));
        Map<String, String> optionalOptionKeysLowerCaseToOriginal =
                factory.optionalOptions().stream()
                        .collect(
                                Collectors.toMap(
                                        option -> option.key().toLowerCase(), ConfigOption::key));
        for (Map.Entry<String, String> entry : options.entrySet()) {
            final String catalogOptionKey = entry.getKey();
            final String catalogOptionValue = entry.getValue();
            normalizedOptions.put(
                    requiredOptionKeysLowerCaseToOriginal.containsKey(
                                    catalogOptionKey.toLowerCase())
                            ? requiredOptionKeysLowerCaseToOriginal.get(
                                    catalogOptionKey.toLowerCase())
                            : optionalOptionKeysLowerCaseToOriginal.getOrDefault(
                                    catalogOptionKey.toLowerCase(), catalogOptionKey),
                    catalogOptionValue);
        }
        return normalizedOptions;
    }
}
