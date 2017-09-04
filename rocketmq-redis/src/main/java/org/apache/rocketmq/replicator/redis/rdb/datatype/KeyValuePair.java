/*
 *
 *   Copyright 2016 leon chen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  modified:
 *    1. rename package from com.moilioncircle.redis.replicator to
 *        org.apache.rocketmq.replicator.redis
 *
 */

package org.apache.rocketmq.replicator.redis.rdb.datatype;

import org.apache.rocketmq.replicator.redis.event.Event;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
@SuppressWarnings("unchecked")
public class KeyValuePair<T> implements Event {
    protected DB db;
    protected int valueRdbType;
    protected ExpiredType expiredType = ExpiredType.NONE;
    protected Long expiredValue;
    protected String key;
    protected T value;

    public int getValueRdbType() {
        return valueRdbType;
    }

    public void setValueRdbType(int valueRdbType) {
        this.valueRdbType = valueRdbType;
    }

    public ExpiredType getExpiredType() {
        return expiredType;
    }

    public void setExpiredType(ExpiredType expiredType) {
        this.expiredType = expiredType;
    }

    public Long getExpiredValue() {
        return expiredValue;
    }

    public void setExpiredValue(Long expiredValue) {
        this.expiredValue = expiredValue;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public DB getDb() {
        return db;
    }

    public void setDb(DB db) {
        this.db = db;
    }

    /**
     * @return expiredValue as Integer
     */
    public Integer getExpiredSeconds() {
        return expiredValue == null ? null : expiredValue.intValue();
    }

    /**
     * @return expiredValue as Long
     */
    public Long getExpiredMs() {
        return expiredValue;
    }

    /**
     * @return RDB_TYPE_STRING
     */
    public String getValueAsString() {
        return (String) value;
    }

    /**
     * @return RDB_TYPE_HASH, RDB_TYPE_HASH_ZIPMAP, RDB_TYPE_HASH_ZIPLIST
     */
    public Map<String, String> getValueAsHash() {
        return (Map<String, String>) value;
    }

    /**
     * @return RDB_TYPE_SET, RDB_TYPE_SET_INTSET
     */
    public Set<String> getValueAsSet() {
        return (Set<String>) value;
    }

    /**
     * @return RDB_TYPE_ZSET, RDB_TYPE_ZSET_2, RDB_TYPE_ZSET_ZIPLIST
     */
    public Set<ZSetEntry> getValueAsZSet() {
        return (Set<ZSetEntry>) value;
    }

    /**
     * @return RDB_TYPE_LIST, RDB_TYPE_LIST_ZIPLIST, RDB_TYPE_LIST_QUICKLIST
     */
    public List<String> getValueAsStringList() {
        return (List<String>) value;
    }

    /**
     * @return RDB_TYPE_MODULE
     */
    public Module getValueAsModule() {
        return (Module) value;
    }

    @Override
    public String toString() {
        return "KeyValuePair{" +
                "db=" + db +
                ", valueRdbType=" + valueRdbType +
                ", expiredType=" + expiredType +
                ", expiredValue=" + expiredValue +
                ", key='" + key + '\'' +
                ", value=" + value +
                '}';
    }
}
