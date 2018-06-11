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

import org.apache.avro.Schema;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class RocketMQAvroSpecifiedSerializerTest {
    @Test
    public void testSerializeAndDeserialize() throws Exception {
        Schema schema = AvroUtils.newSchema(new File("src/test/avro/user.avsc"));
        RocketMQAvroSpecifiedSerializer<User> avroSpecifiedSerializer = new RocketMQAvroSpecifiedSerializer<>(schema);
        User user = new User();
        user.setName("tom");
        user.setAge(16);
        byte[] result = avroSpecifiedSerializer.serialize(user);

        RocketMQAvroSpecifiedDeserializer<User> avroSpecifiedDeserializer = new RocketMQAvroSpecifiedDeserializer<>(schema);
        assertEquals(user, avroSpecifiedDeserializer.deserialize(result));
    }

}