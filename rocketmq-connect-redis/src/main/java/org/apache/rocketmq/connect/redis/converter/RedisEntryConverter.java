/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.connect.redis.converter;

import io.openmessaging.connector.api.data.DataEntryBuilder;
import io.openmessaging.connector.api.data.Field;
import io.openmessaging.connector.api.data.FieldType;
import io.openmessaging.connector.api.data.Schema;
import io.openmessaging.connector.api.data.SourceDataEntry;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.connect.redis.common.Options;
import org.apache.rocketmq.connect.redis.converter.KVEntryConverter;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;

public class RedisEntryConverter implements KVEntryConverter {
    private final int maxValueSize = 500;

    @Override public List<SourceDataEntry> kVEntryToDataEntries(KVEntry kvEntry) {
        Schema schema = getRedisSchema(kvEntry.getValueType());

        String partition = kvEntry.getPartition();
        if (partition == null) {
            throw new IllegalStateException("partition info error.");
        }
        List<SourceDataEntry> res = new ArrayList<>();
        List<Object> values = splitValue(kvEntry.getValueType(), kvEntry.getValue(), this.maxValueSize);
        for (int i = 0; i < values.size(); i++) {
            DataEntryBuilder builder = newDataEntryBuilderWithoutValue(schema, kvEntry);

            builder.putFiled(Options.REDIS_VALUE.name(), values.get(i));
            builder.timestamp(System.currentTimeMillis());

            SourceDataEntry entry = builder.buildSourceDataEntry(
                ByteBuffer.wrap(kvEntry.getPartition().getBytes()),
                ByteBuffer.wrap(RedisPositionConverter.longToJson(kvEntry.getOffset()).toJSONString().getBytes())
            );
            res.add(entry);
        }
        return res;
    }


    private List<Object> splitValue(FieldType valueType, Object value, Integer maxValueSize) {
        List<Object> res = new ArrayList<>();
        if (valueType.equals(FieldType.ARRAY) && value instanceof List) {
            List<Object> list = (List)value;
            if (list.size() < maxValueSize) {
                res.add(list);
            } else {
                int num = list.size() / maxValueSize + 1;
                for (int i = 0; i < num; i++) {
                    List<Object> v = new ArrayList<>();
                    for (int j = i * maxValueSize; j < Math.min((i + 1) * maxValueSize, list.size()); j++) {
                        v.add(list.get(j));
                    }
                    if(!v.isEmpty()){
                        res.add(v);
                    }
                }
            }
            return res;
        }

        if (valueType.equals(FieldType.MAP) && value instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>)value;
            if (map.size() < maxValueSize) {
                res.add(map);
            } else {
                AtomicInteger num = new AtomicInteger(0);
                Map<Object, Object> v = new HashMap<>();
                for (Object k : map.keySet()) {
                    v.put(k, map.get(k));
                    if (num.incrementAndGet() == maxValueSize) {
                        res.add(v);
                        v = new HashMap<>();
                        num = new AtomicInteger(0);
                    }
                }
                if(!v.isEmpty()){
                    res.add(v);
                }
            }
            return res;
        }

        res.add(value);
        return res;
    }

    private DataEntryBuilder newDataEntryBuilderWithoutValue(Schema schema, KVEntry kvEntry) {
        DataEntryBuilder dataEntryBuilder = new DataEntryBuilder(schema);
        dataEntryBuilder.queue(kvEntry.getQueueName());
        dataEntryBuilder.entryType(kvEntry.getEntryType());
        dataEntryBuilder.putFiled(Options.REDIS_COMMAND.name(), kvEntry.getCommand());
        dataEntryBuilder.putFiled(Options.REDIS_KEY.name(), kvEntry.getKey());
        dataEntryBuilder.putFiled(Options.REDIS_PARAMS.name(), kvEntry.getParams());
        return dataEntryBuilder;
    }

    private Schema getRedisSchema(FieldType valueType) {
        Schema schema = new Schema();
        schema.setDataSource(Options.REDIS_DATASOURCE.name());
        List<Field> fields = new ArrayList<>();

        fields.add(new Field(0, Options.REDIS_COMMAND.name(), FieldType.STRING));
        fields.add(new Field(1, Options.REDIS_KEY.name(), FieldType.STRING));
        fields.add(new Field(2, Options.REDIS_VALUE.name(), valueType));
        fields.add(new Field(3, Options.REDIS_PARAMS.name(), FieldType.MAP));

        schema.setFields(fields);
        return schema;
    }

}
