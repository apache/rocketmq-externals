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

package org.apache.rocketmq.flink.source.common;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/** Includes config options of RocketMQ connector type. */
public class RocketMQOptions {

    public static final ConfigOption<String> TOPIC = ConfigOptions.key("topic").noDefaultValue();

    public static final ConfigOption<String> CONSUMER_GROUP =
            ConfigOptions.key("consumerGroup").noDefaultValue();

    public static final ConfigOption<String> NAME_SERVER_ADDRESS =
            ConfigOptions.key("nameServerAddress").noDefaultValue();

    public static final ConfigOption<String> OPTIONAL_TAG =
            ConfigOptions.key("tag").noDefaultValue();

    public static final ConfigOption<Integer> OPTIONAL_START_MESSAGE_OFFSET =
            ConfigOptions.key("startMessageOffset").defaultValue(-1);

    public static final ConfigOption<Long> OPTIONAL_START_TIME_MILLS =
            ConfigOptions.key("startTimeMs".toLowerCase()).longType().defaultValue(-1L);

    public static final ConfigOption<String> OPTIONAL_START_TIME =
            ConfigOptions.key("startTime".toLowerCase()).stringType().noDefaultValue();

    public static final ConfigOption<String> OPTIONAL_END_TIME =
            ConfigOptions.key("endTime").noDefaultValue();

    public static final ConfigOption<String> OPTIONAL_TIME_ZONE =
            ConfigOptions.key("timeZone".toLowerCase()).stringType().noDefaultValue();

    public static final ConfigOption<Long> OPTIONAL_PARTITION_DISCOVERY_INTERVAL_MS =
            ConfigOptions.key("partitionDiscoveryIntervalMs").longType().defaultValue(30000L);

    public static final ConfigOption<Boolean> OPTIONAL_USE_NEW_API =
            ConfigOptions.key("useNewApi").booleanType().defaultValue(true);

    public static final ConfigOption<String> OPTIONAL_ENCODING =
            ConfigOptions.key("encoding").stringType().defaultValue("UTF-8");

    public static final ConfigOption<String> OPTIONAL_FIELD_DELIMITER =
            ConfigOptions.key("fieldDelimiter").stringType().defaultValue("\u0001");

    public static final ConfigOption<String> OPTIONAL_LINE_DELIMITER =
            ConfigOptions.key("lineDelimiter").stringType().defaultValue("\n");

    public static final ConfigOption<Boolean> OPTIONAL_COLUMN_ERROR_DEBUG =
            ConfigOptions.key("columnErrorDebug").booleanType().defaultValue(true);

    public static final ConfigOption<String> OPTIONAL_LENGTH_CHECK =
            ConfigOptions.key("lengthCheck").stringType().defaultValue("NONE");
}
