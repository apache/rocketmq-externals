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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.lang.Validate;
import org.apache.rocketmq.serializer.RocketMQSerializer;

public class RocketMQAvroSpecifiedSerializer<T> implements RocketMQSerializer<T> {
    private Schema schema;

    public RocketMQAvroSpecifiedSerializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public byte[] serialize(T obj) {
        Validate.notNull(obj);

        DatumWriter<T> datumWriter = new SpecificDatumWriter<T>(schema);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(out, null);
        try {
            datumWriter.write(obj, encoder);
            encoder.flush();
            byte[] bytes = out.toByteArray();
            out.close();
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
