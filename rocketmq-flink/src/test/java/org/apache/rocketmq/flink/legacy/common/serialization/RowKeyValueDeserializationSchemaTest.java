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

package org.apache.rocketmq.flink.legacy.common.serialization;

import org.apache.rocketmq.common.message.MessageExt;

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.data.RowData;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/** Test for {@link RowKeyValueDeserializationSchema}. */
public class RowKeyValueDeserializationSchemaTest {

    @Test
    public void testDeserializeKeyAndValue() {
        TableSchema tableSchema =
                new TableSchema.Builder().field("varchar", DataTypes.VARCHAR(100)).build();
        RowKeyValueDeserializationSchema deserializationSchema =
                new RowKeyValueDeserializationSchema.Builder()
                        .setTableSchema(tableSchema)
                        .setProperties(new HashMap<>())
                        .build();
        MessageExt messageExt = new MessageExt();
        messageExt.setBody("test_deserialize_key_and_value".getBytes());
        RowData rowData = deserializationSchema.deserializeKeyAndValue(null, messageExt.getBody());
        assertEquals(new String(messageExt.getBody()), rowData.getString(0).toString());
    }
}
