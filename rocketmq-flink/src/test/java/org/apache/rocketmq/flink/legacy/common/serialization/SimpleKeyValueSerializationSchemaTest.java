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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SimpleKeyValueSerializationSchemaTest {
    @Test
    public void serializeKeyAndValue() throws Exception {
        SimpleKeyValueSerializationSchema serializationSchema =
                new SimpleKeyValueSerializationSchema("id", "name");
        SimpleKeyValueDeserializationSchema deserializationSchema =
                new SimpleKeyValueDeserializationSchema("id", "name");

        Map tuple = new HashMap();
        tuple.put("id", "x001");
        tuple.put("name", "vesense");

        assertEquals(
                tuple,
                deserializationSchema.deserializeKeyAndValue(
                        serializationSchema.serializeKey(tuple),
                        serializationSchema.serializeValue(tuple)));
    }
}
