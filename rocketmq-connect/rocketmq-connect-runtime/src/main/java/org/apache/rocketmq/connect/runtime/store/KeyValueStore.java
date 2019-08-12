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

import java.util.Map;

/**
 * Key value based store interface.
 *
 * @param <K>
 * @param <V>
 */
public interface KeyValueStore<K, V> {

    /**
     * Put a key/value into the store.
     *
     * @param key
     * @param value
     * @return
     */
    V put(K key, V value);

    /**
     * Put a set of key/value into the store.
     *
     * @param map
     */
    void putAll(Map<K, V> map);

    /**
     * Remove a specified key.
     *
     * @param key
     * @return
     */
    V remove(K key);

    /**
     * Get the size of current key/value store.
     *
     * @return
     */
    int size();

    /**
     * Whether a key is contained in current store.
     *
     * @param key
     * @return
     */
    boolean containsKey(K key);

    /**
     * Get the value of a key.
     *
     * @param key
     * @return
     */
    V get(K key);

    /**
     * Get all data from the current store. Not recommend to use this method when the data set is large.
     *
     * @return
     */
    Map<K, V> getKVMap();

    /**
     * Load the data from back store.
     *
     * @return
     */
    boolean load();

    /**
     * Persist all data into the store.
     */
    void persist();
}
