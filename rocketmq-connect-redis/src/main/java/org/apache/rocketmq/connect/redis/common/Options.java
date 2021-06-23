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

package org.apache.rocketmq.connect.redis.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.moilioncircle.redis.replicator.rdb.datatype.EvictType;
import com.moilioncircle.redis.replicator.rdb.datatype.ExpiredType;

public class Options<T>{
    private static ConcurrentMap<String, Options<Object>> pool = new ConcurrentHashMap<>(32);

    public static final Options<Void> REDIS_PARTITION = newOption("DEFAULT_PARTITION");
    public static final Options<Void> REDIS_QEUEUE = newOption("redis");
    public static final Options<Void> REDIS_DATASOURCE = newOption("redis");
    public static final Options<Void> REDIS_COMMAND = newOption("command");
    public static final Options<Void> REDIS_KEY = newOption("key");
    public static final Options<Void> REDIS_VALUE = newOption("value");
    public static final Options<Void> REDIS_PARAMS = newOption("params");

    public static final Options<String> REDIS_REPLID = newOption("replId");

    public static final Options<Long> EXPIRED_TIME = newOption("EXPIRED_TIME");
    public static final Options<ExpiredType> EXPIRED_TYPE = newOption("EXPIRED_TYPE");
    public static final Options<Long> EVICT_VALUE = newOption("EVICT_VALUE");
    public static final Options<EvictType> EVICT_TYPE = newOption("EVICT_TYPE");

    public static final Options<Integer> REDIS_EX = newOption("EX");
    public static final Options<Long> REDIS_PX = newOption("PX");
    public static final Options<Boolean> REDIS_NX = newOption("NX");
    public static final Options<Boolean> REDIS_XX = newOption("XX");
    public static final Options<Long> REDIS_INDEX = newOption("INDEX");
    public static final Options<Long> REDIS_OFFSET = newOption("CUSTOM_OFFSET");
    public static final Options<Long> REDIS_COUNT = newOption("COUNT");
    public static final Options<Long> REDIS_INCREMENT = newOption("LAST_OFFSET");
    public static final Options<Integer> REDIS_DB_INDEX = newOption("DB_INDEX");
    public static final Options<Long> REDIS_TIMEOUT = newOption("TIMEOUT");
    public static final Options<Integer> REDIS_EX_TIMESTAMP = newOption("EX_TIMESTAMP");
    public static final Options<Long> REDIS_PX_TIMESTAMP = newOption("PX_TIMESTAMP");
    public static final Options<String> REDIS_GROUP = newOption("GROUP");
    public static final Options<String> REDIS_ID = newOption("ID");
    public static final Options<Boolean> REDIS_CH = newOption("CH");
    public static final Options<Boolean> REDIS_INCR = newOption("INCR");
    public static final Options<Boolean> REDIS_WEIGHTS = newOption("WEIGHTS");
    public static final Options<String> REDIS_AGGREGATE = newOption("AGGREGATE");
    public static final Options<Boolean> REDIS_BEFORE = newOption("BEFORE");
    public static final Options<Boolean> REDIS_AFTER = newOption("AFTER");
    public static final Options<Long> REDIS_TTL = newOption("TTL");
    public static final Options<Boolean> REDIS_REPLACE = newOption("REPLACE");
    public static final Options<Boolean> REDIS_XTRIM = newOption("~");
    public static final Options<String> REDIS_CONSUMER = newOption("CONSUMER");
    public static final Options<Long> REDIS_MIN_IDLE_TIME = newOption("MIN-IDLE-TIME");
    public static final Options<Long> REDIS_IDLE = newOption("IDLE");
    public static final Options<Long> REDIS_TIME = newOption("TIME");
    public static final Options<Integer> REDIS_RETRYCOUNT = newOption("RETRYCOUNT");
    public static final Options<Boolean> REDIS_FORCE = newOption("FORCE");
    public static final Options<Boolean> REDIS_JUSTID = newOption("JUSTID");
    public static final Options<Boolean> REDIS_KEEPTTL = newOption("KEEPTTL");


    private final String name;

    public Options(String name) {
        this.name = name;
    }

    public String name(){
        return this.name;
    }

    public static <T> Options<T> valueOf(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("empty name");
        }
        return (Options<T>)get(name);
    }


    private static <T> Options<T> newOption(String name){
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("empty name");
        }
        return (Options<T>)getOrCreate(name);
    }

    private static Options<Object> getOrCreate(String name){
        Options<Object> option = get(name);
        if(option == null){
            final Options<Object> tempOption = new Options<>(name);
            option = save(name, tempOption);
            if(option == null){
                return tempOption;
            }
        }
        return option;
    }

    private static Options<Object> get(String name){
        Options<Object> option = pool.get(name);
        return option;
    }

    private static Options<Object> save(String name, Options<Object> options){
        return pool.putIfAbsent(name, options);
    }

}


