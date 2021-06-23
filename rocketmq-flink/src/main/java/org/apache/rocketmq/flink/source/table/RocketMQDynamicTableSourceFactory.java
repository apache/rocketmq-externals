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

package org.apache.rocketmq.flink.source.table;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.utils.TableSchemaUtils;
import org.apache.flink.util.Preconditions;
import org.apache.flink.util.StringUtils;

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.apache.flink.table.factories.FactoryUtil.createTableFactoryHelper;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.CONSUMER_GROUP;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.NAME_SERVER_ADDRESS;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_COLUMN_ERROR_DEBUG;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_ENCODING;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_END_TIME;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_FIELD_DELIMITER;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_LENGTH_CHECK;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_LINE_DELIMITER;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_PARTITION_DISCOVERY_INTERVAL_MS;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_START_MESSAGE_OFFSET;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_START_TIME;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_START_TIME_MILLS;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_TAG;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.OPTIONAL_TIME_ZONE;
import static org.apache.rocketmq.flink.source.common.RocketMQOptions.TOPIC;

/**
 * Defines the {@link DynamicTableSourceFactory} implementation to create {@link
 * RocketMQScanTableSource}.
 */
public class RocketMQDynamicTableSourceFactory implements DynamicTableSourceFactory {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    @Override
    public String factoryIdentifier() {
        return "rocketmq";
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(TOPIC);
        requiredOptions.add(CONSUMER_GROUP);
        requiredOptions.add(NAME_SERVER_ADDRESS);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(OPTIONAL_TAG);
        optionalOptions.add(OPTIONAL_START_MESSAGE_OFFSET);
        optionalOptions.add(OPTIONAL_START_TIME_MILLS);
        optionalOptions.add(OPTIONAL_START_TIME);
        optionalOptions.add(OPTIONAL_END_TIME);
        optionalOptions.add(OPTIONAL_TIME_ZONE);
        optionalOptions.add(OPTIONAL_PARTITION_DISCOVERY_INTERVAL_MS);
        optionalOptions.add(OPTIONAL_ENCODING);
        optionalOptions.add(OPTIONAL_FIELD_DELIMITER);
        optionalOptions.add(OPTIONAL_LINE_DELIMITER);
        optionalOptions.add(OPTIONAL_COLUMN_ERROR_DEBUG);
        optionalOptions.add(OPTIONAL_LENGTH_CHECK);
        return optionalOptions;
    }

    @Override
    public DynamicTableSource createDynamicTableSource(Context context) {
        transformContext(this, context);
        FactoryUtil.TableFactoryHelper helper = createTableFactoryHelper(this, context);
        helper.validate();
        Map<String, String> rawProperties = context.getCatalogTable().getOptions();
        Configuration configuration = Configuration.fromMap(rawProperties);
        String topic = configuration.getString(TOPIC);
        String consumerGroup = configuration.getString(CONSUMER_GROUP);
        String nameServerAddress = configuration.getString(NAME_SERVER_ADDRESS);
        String tag = configuration.getString(OPTIONAL_TAG);
        int startMessageOffset = configuration.getInteger(OPTIONAL_START_MESSAGE_OFFSET);
        long startTimeMs = configuration.getLong(OPTIONAL_START_TIME_MILLS);
        String startDateTime = configuration.getString(OPTIONAL_START_TIME);
        String timeZone = configuration.getString(OPTIONAL_TIME_ZONE);
        long startTime = startTimeMs;
        if (startTime == -1) {
            if (!StringUtils.isNullOrWhitespaceOnly(startDateTime)) {
                try {
                    startTime = parseDateString(startDateTime, timeZone);
                } catch (ParseException e) {
                    throw new RuntimeException(
                            String.format(
                                    "Incorrect datetime format: %s, pls use ISO-8601 "
                                            + "complete date plus hours, minutes and seconds format:%s.",
                                    startDateTime, DATE_FORMAT),
                            e);
                }
            }
        }
        long stopInMs = Long.MAX_VALUE;
        String endDateTime = configuration.getString(OPTIONAL_END_TIME);
        if (!StringUtils.isNullOrWhitespaceOnly(endDateTime)) {
            try {
                stopInMs = parseDateString(endDateTime, timeZone);
            } catch (ParseException e) {
                throw new RuntimeException(
                        String.format(
                                "Incorrect datetime format: %s, pls use ISO-8601 "
                                        + "complete date plus hours, minutes and seconds format:%s.",
                                endDateTime, DATE_FORMAT),
                        e);
            }
            Preconditions.checkArgument(
                    stopInMs >= startTime, "Start time should be less than stop time.");
        }
        long partitionDiscoveryIntervalMs =
                configuration.getLong(OPTIONAL_PARTITION_DISCOVERY_INTERVAL_MS);
        DescriptorProperties descriptorProperties = new DescriptorProperties();
        descriptorProperties.putProperties(rawProperties);
        TableSchema physicalSchema =
                TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
        descriptorProperties.putTableSchema("schema", physicalSchema);
        return new RocketMQScanTableSource(
                descriptorProperties,
                physicalSchema,
                topic,
                consumerGroup,
                nameServerAddress,
                tag,
                stopInMs,
                startMessageOffset,
                startMessageOffset < 0 ? startTime : -1L,
                partitionDiscoveryIntervalMs);
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

    private Long parseDateString(String dateString, String timeZone) throws ParseException {
        FastDateFormat simpleDateFormat =
                FastDateFormat.getInstance(DATE_FORMAT, TimeZone.getTimeZone(timeZone));
        return simpleDateFormat.parse(dateString).getTime();
    }
}
