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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;

public class DefaultSchemaRegistry implements SchemaRegistry {
    private Map<String, Schema> schemas = new ConcurrentHashMap<>();

    public void registerSchema(String id, String schemaString) {
        registerSchema(id, AvroUtils.newSchema(schemaString));
    }

    @Override
    public void registerSchema(String id, Schema schema) {
        // override the old value
        schemas.put(id, schema);
    }

    @Override
    public Schema getSchema(String id) {
        return schemas.get(id);
    }
}
