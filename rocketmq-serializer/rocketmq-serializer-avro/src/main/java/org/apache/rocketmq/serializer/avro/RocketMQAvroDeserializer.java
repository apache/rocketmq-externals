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

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.lang.Validate;
import org.apache.rocketmq.serializer.RocketMQDeserializer;

public class RocketMQAvroDeserializer implements RocketMQDeserializer<GenericRecord> {
    private Schema schema;

    public RocketMQAvroDeserializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public GenericRecord deserialize(byte[] bytes) {
        Validate.notNull(bytes);

        DatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(bytes, null);
        try {
            GenericRecord record = reader.read(null, decoder);
            return record;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
