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

package org.apache.rocketmq.flink.common.serialization;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SimpleKeyValueSerializationSchema implements KeyValueSerializationSchema<Map> {
    public static final String DEFAULT_KEY_FIELD = "key";
    public static final String DEFAULT_VALUE_FIELD = "value";

    public String keyField;
    public String valueField;

    public SimpleKeyValueSerializationSchema() {
        this(DEFAULT_KEY_FIELD, DEFAULT_VALUE_FIELD);
    }

    /**
     * SimpleKeyValueSerializationSchema Constructor.
     * @param keyField tuple field for selecting the key
     * @param valueField  tuple field for selecting the value
     */
    public SimpleKeyValueSerializationSchema(String keyField, String valueField) {
        this.keyField = keyField;
        this.valueField = valueField;
    }

    @Override
    public byte[] serializeKey(Map tuple) {
        if (tuple == null || keyField == null) {
            return null;
        }
        Object key = tuple.get(keyField);
        return key != null ? key.toString().getBytes(StandardCharsets.UTF_8) : null;
    }

    @Override
    public byte[] serializeValue(Map tuple) {
        if (tuple == null || valueField == null) {
            return null;
        }
        Object value = tuple.get(valueField);
        return value != null ? value.toString().getBytes(StandardCharsets.UTF_8) : null;
    }

}
