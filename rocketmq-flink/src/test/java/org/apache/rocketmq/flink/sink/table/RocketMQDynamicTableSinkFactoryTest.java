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

import org.apache.rocketmq.flink.common.RocketMQOptions;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.ResolvedCatalogTable;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.FactoryUtil;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.flink.table.api.DataTypes.STRING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests for {@link org.apache.rocketmq.flink.sink.table.RocketMQDynamicTableSinkFactory}. */
public class RocketMQDynamicTableSinkFactoryTest {

    private static final ResolvedSchema SCHEMA =
            new ResolvedSchema(
                    Collections.singletonList(Column.physical("name", STRING().notNull())),
                    new ArrayList<>(),
                    null);

    private static final String IDENTIFIER = "rocketmq";
    private static final String TOPIC = "test_sink";
    private static final String PRODUCER_GROUP = "test_producer";
    private static final String NAME_SERVER_ADDRESS =
            "http://${instanceId}.${region}.mq-internal.aliyuncs.com:8080";

    @Test
    public void testRocketMQDynamicTableSinkWithLegalOption() {
        final Map<String, String> options = new HashMap<>();
        options.put("connector", IDENTIFIER);
        options.put(RocketMQOptions.TOPIC.key(), TOPIC);
        options.put(RocketMQOptions.PRODUCER_GROUP.key(), PRODUCER_GROUP);
        options.put(RocketMQOptions.NAME_SERVER_ADDRESS.key(), NAME_SERVER_ADDRESS);
        final DynamicTableSink tableSink = createDynamicTableSink(options);
        assertTrue(tableSink instanceof RocketMQDynamicTableSink);
        assertEquals(RocketMQDynamicTableSink.class.getName(), tableSink.asSummaryString());
    }

    @Test(expected = ValidationException.class)
    public void testRocketMQDynamicTableSinkWithoutRequiredOption() {
        final Map<String, String> options = new HashMap<>();
        options.put("connector", IDENTIFIER);
        options.put(RocketMQOptions.TOPIC.key(), TOPIC);
        options.put(RocketMQOptions.PRODUCER_GROUP.key(), PRODUCER_GROUP);
        options.put(RocketMQOptions.OPTIONAL_TAG.key(), "test_tag");
        createDynamicTableSink(options);
    }

    @Test(expected = ValidationException.class)
    public void testRocketMQDynamicTableSinkWithUnknownOption() {
        final Map<String, String> options = new HashMap<>();
        options.put(RocketMQOptions.TOPIC.key(), TOPIC);
        options.put(RocketMQOptions.PRODUCER_GROUP.key(), PRODUCER_GROUP);
        options.put(RocketMQOptions.NAME_SERVER_ADDRESS.key(), NAME_SERVER_ADDRESS);
        options.put("unknown", "test_option");
        createDynamicTableSink(options);
    }

    private static DynamicTableSink createDynamicTableSink(Map<String, String> options) {
        return FactoryUtil.createTableSink(
                null,
                ObjectIdentifier.of("default", "default", "mq"),
                new ResolvedCatalogTable(
                        CatalogTable.of(
                                Schema.newBuilder().fromResolvedSchema(SCHEMA).build(),
                                "mock sink",
                                Collections.emptyList(),
                                options),
                        SCHEMA),
                new Configuration(),
                RocketMQDynamicTableSinkFactory.class.getClassLoader(),
                false);
    }
}
