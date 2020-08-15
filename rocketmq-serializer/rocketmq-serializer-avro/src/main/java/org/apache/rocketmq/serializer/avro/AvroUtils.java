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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang.Validate;

public final class AvroUtils {
    private AvroUtils() {}

    public static GenericRecord newGenericRecord(Schema schema) {
        Validate.notNull(schema);
        GenericRecord record = new GenericData.Record(schema);
        return record;
    }

    public static GenericRecord newGenericRecordFromMap(Schema schema, Map<String, Object> map) {
        Validate.notNull(schema);
        Validate.notNull(map);
        GenericRecord record = new GenericData.Record(schema);
        map.forEach((k, v) -> {
            record.put(k, v);
        });
        return record;
    }

    public static Schema newSchema(String schemaString) {
        Validate.notNull(schemaString);
        return new Parser().parse(schemaString);
    }

    public static Schema newSchema(File schemaFile) {
        Validate.notNull(schemaFile);
        try {
            return new Parser().parse(schemaFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Schema newSchema(InputStream in) {
        Validate.notNull(in);
        try {
            return new Parser().parse(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
