/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.serializer.avro;

import java.io.File;
import java.util.HashMap;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RocketMQAvroSerializerTest {
    @Test
    public void testSerializeAndDeserialize() throws Exception {
        Schema schema = AvroUtils.newSchema(new File("src/test/avro/user.avsc"));
        RocketMQAvroSerializer avroSerializer = new RocketMQAvroSerializer();
        HashMap<String,Object> map = new HashMap<>();
        map.put("name", "tom");
        map.put("age", 16);
        GenericRecord obj = AvroUtils.newGenericRecordFromMap(schema, map);
        byte[] result = avroSerializer.serialize(obj);

        RocketMQAvroDeserializer avroDeserializer = new RocketMQAvroDeserializer(schema);
        assertEquals(obj, avroDeserializer.deserialize(result));
    }

}