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

package org.apache.rocketmq.connect.redis.pojo;

import java.util.HashMap;
import java.util.Map;
import io.openmessaging.connector.api.data.EntryType;
import io.openmessaging.connector.api.data.FieldType;
import org.apache.rocketmq.connect.redis.common.Options;

public class RedisEntry implements KVEntry {
    private Long replOffset;
    private String partition;
    private Map<String, Object> params;
    private String queueName;
    private EntryType entryType;
    private String command;
    private String key;
    private FieldType valueType;
    private Object value;

    public static RedisEntry newEntry(FieldType valueType) {
        return new RedisEntry(valueType);
    }

    public static RedisEntry newEntry(String partition, FieldType valueType) {
        return new RedisEntry(partition, valueType);
    }

    public RedisEntry(FieldType valueType) {
        this(Options.REDIS_PARTITION.name(), valueType);
    }

    public RedisEntry(String partition, FieldType valueType) {
        this.partition = partition;
        this.valueType = valueType;
        this.params = new HashMap<>();
    }

    @Override public KVEntry partition(String partition) {
        this.partition = partition;
        return this;
    }

    @Override public String getPartition() {
        return this.partition;
    }

    @Override public KVEntry queueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    @Override public String getQueueName() {
        return this.queueName;
    }

    @Override public KVEntry entryType(EntryType entryType) {
        this.entryType = entryType;
        return this;
    }

    @Override public EntryType getEntryType() {
        return this.entryType;
    }

    @Override public RedisEntry sourceId(String id) {
        this.params.put(Options.REDIS_REPLID.name(), id);
        return this;
    }

    @Override public String getSourceId() {
        Object ob = this.params.get(Options.REDIS_REPLID.name());
        if (ob != null) {
            return ob.toString();
        }
        return null;
    }

    @Override public RedisEntry offset(Long offset) {
        this.replOffset = offset;
        return this;
    }

    @Override public Long getOffset() {
        return this.replOffset;
    }

    @Override public RedisEntry command(String command) {
        this.command = command;
        return this;
    }

    @Override public String getCommand() {
        return this.command;
    }

    @Override public RedisEntry key(String key) {
        this.key = key;
        return this;
    }

    @Override public String getKey() {
        return this.key;
    }

    @Override public RedisEntry value(Object value) {
        this.value = value;
        return this;
    }

    @Override public Object getValue() {
        return this.value;
    }

    @Override public KVEntry valueType(FieldType valueType) {
        this.valueType = valueType;
        return this;
    }

    @Override public FieldType getValueType() {
        return this.valueType;
    }

    @Override public <T> RedisEntry param(Options<T> k, T v) {
        this.params.put(k.name(), v);
        return this;
    }

    @Override public <T> T getParam(Options<T> k) {
        if(k == null){
            return null;
        }
        return (T) this.params.get(k.name());
    }

    @Override public Map<String, Object> getParams() {
        return this.params;
    }


    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RedisEntry{")
            .append("partition:'").append(this.partition).append("', ")
            .append("queueName:'").append(this.queueName).append("', ")
            .append("entryType:'").append(this.entryType).append("', ")
            .append("command:'").append(this.command).append("', ")
            .append("key:'").append(this.key).append("', ")
            .append("valueType:").append(this.valueType).append("', ")
            .append("value:'").append(this.value).append("', ")
            .append("params:").append(this.params)
            .append("}");
        return sb.toString();
    }
}
