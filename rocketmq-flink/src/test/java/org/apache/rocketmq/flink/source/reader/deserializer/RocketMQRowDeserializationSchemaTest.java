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

package org.apache.rocketmq.flink.source.reader.deserializer;

import org.apache.rocketmq.common.message.MessageExt;

import org.apache.flink.api.common.serialization.DeserializationSchema.InitializationContext;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;
import org.apache.flink.util.Collector;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

/** Test for {@link RocketMQRowDeserializationSchema}. */
public class RocketMQRowDeserializationSchemaTest {

    @Test
    public void testDeserialize() {
        TableSchema tableSchema =
                new TableSchema.Builder()
                        .field("int", DataTypes.INT())
                        .field("varchar", DataTypes.VARCHAR(100))
                        .field("bool", DataTypes.BOOLEAN())
                        .field("char", DataTypes.CHAR(5))
                        .field("tinyint", DataTypes.TINYINT())
                        .field("decimal", DataTypes.DECIMAL(10, 5))
                        .field("smallint", DataTypes.SMALLINT())
                        .field("bigint", DataTypes.BIGINT())
                        .field("float", DataTypes.FLOAT())
                        .field("double", DataTypes.DOUBLE())
                        .field("date", DataTypes.DATE())
                        .field("time", DataTypes.TIME())
                        .field("timestamp", DataTypes.TIMESTAMP())
                        .build();
        RocketMQRowDeserializationSchema recordDeserializer =
                new RocketMQRowDeserializationSchema(tableSchema, new HashMap<>(), false, null);
        RowDeserializationSchema sourceDeserializer = mock(RowDeserializationSchema.class);
        InitializationContext initializationContext = mock(InitializationContext.class);
        doNothing().when(sourceDeserializer).open(initializationContext);
        Whitebox.setInternalState(recordDeserializer, "deserializationSchema", sourceDeserializer);
        recordDeserializer.open(initializationContext);
        MessageExt firstMsg =
                new MessageExt(
                        1,
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8080),
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8088),
                        "184019387");
        firstMsg.setBody("test_deserializer_raw_messages_1".getBytes());
        MessageExt secondMsg =
                new MessageExt(
                        1,
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8081),
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8087),
                        "284019387");
        secondMsg.setBody("test_deserializer_raw_messages_2".getBytes());
        MessageExt thirdMsg =
                new MessageExt(
                        1,
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8082),
                        System.currentTimeMillis(),
                        InetSocketAddress.createUnresolved("localhost", 8086),
                        "384019387");
        thirdMsg.setBody("test_deserializer_raw_messages_3".getBytes());
        List<MessageExt> messages = Arrays.asList(firstMsg, secondMsg, thirdMsg);
        Collector<RowData> collector = mock(Collector.class);
        recordDeserializer.deserialize(messages, collector);

        assertEquals(3, recordDeserializer.getBytesMessages().size());
        assertEquals(firstMsg.getBody(), recordDeserializer.getBytesMessages().get(0).getData());
        assertEquals(
                String.valueOf(firstMsg.getStoreTimestamp()),
                recordDeserializer.getBytesMessages().get(0).getProperty("__store_timestamp__"));
        assertEquals(
                String.valueOf(firstMsg.getBornTimestamp()),
                recordDeserializer.getBytesMessages().get(0).getProperty("__born_timestamp__"));
        assertEquals(
                String.valueOf(firstMsg.getQueueId()),
                recordDeserializer.getBytesMessages().get(0).getProperty("__queue_id__"));
        assertEquals(
                String.valueOf(firstMsg.getQueueOffset()),
                recordDeserializer.getBytesMessages().get(0).getProperty("__queue_offset__"));
        assertEquals(secondMsg.getBody(), recordDeserializer.getBytesMessages().get(1).getData());
        assertEquals(
                String.valueOf(secondMsg.getStoreTimestamp()),
                recordDeserializer.getBytesMessages().get(1).getProperty("__store_timestamp__"));
        assertEquals(
                String.valueOf(secondMsg.getBornTimestamp()),
                recordDeserializer.getBytesMessages().get(1).getProperty("__born_timestamp__"));
        assertEquals(
                String.valueOf(secondMsg.getQueueId()),
                recordDeserializer.getBytesMessages().get(1).getProperty("__queue_id__"));
        assertEquals(
                String.valueOf(secondMsg.getQueueOffset()),
                recordDeserializer.getBytesMessages().get(1).getProperty("__queue_offset__"));
        assertEquals(thirdMsg.getBody(), recordDeserializer.getBytesMessages().get(2).getData());
        assertEquals(
                String.valueOf(thirdMsg.getStoreTimestamp()),
                recordDeserializer.getBytesMessages().get(2).getProperty("__store_timestamp__"));
        assertEquals(
                String.valueOf(thirdMsg.getBornTimestamp()),
                recordDeserializer.getBytesMessages().get(2).getProperty("__born_timestamp__"));
        assertEquals(
                String.valueOf(thirdMsg.getQueueId()),
                recordDeserializer.getBytesMessages().get(2).getProperty("__queue_id__"));
        assertEquals(
                String.valueOf(thirdMsg.getQueueOffset()),
                recordDeserializer.getBytesMessages().get(2).getProperty("__queue_offset__"));
    }
}
