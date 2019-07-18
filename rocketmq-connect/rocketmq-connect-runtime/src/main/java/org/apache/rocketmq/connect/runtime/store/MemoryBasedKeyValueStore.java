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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.store;

import java.util.HashMap;
import java.util.Map;

public class MemoryBasedKeyValueStore<K, V> implements KeyValueStore<K, V> {

    protected Map<K, V> data;

    public MemoryBasedKeyValueStore() {
        this.data = new HashMap<>();
    }

    @Override
    public V put(K key, V value) {
        return this.data.put(key, value);
    }

    @Override public void putAll(Map<K, V> map) {
        data.putAll(map);
    }

    @Override
    public V remove(K key) {
        return this.data.remove(key);
    }

    @Override
    public int size() {
        return this.data.size();
    }

    @Override
    public boolean containsKey(K key) {
        return this.data.containsKey(key);
    }

    @Override
    public V get(K key) {
        return this.data.get(key);
    }

    @Override
    public Map<K, V> getKVMap() {
        return this.data;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public void persist() {

    }
}
