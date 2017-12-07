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

package org.apache.rocketmq.redis.replicator.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//@NonThreadSafe
public class ByteArrayMap<V> implements Map<byte[], V>, Serializable {
    private static final long serialVersionUID = 1L;

    protected final Map<Key, V> map;

    public ByteArrayMap(Map<? extends byte[], ? extends V> m) {
        this(true, m);
    }

    public ByteArrayMap(boolean ordered, Map<? extends byte[], ? extends V> m) {
        this(ordered, m == null ? 0 : m.size(), 0.75f);
        putAll(m);
    }

    public ByteArrayMap() {
        this(true);
    }

    public ByteArrayMap(boolean ordered) {
        this(ordered, 16);
    }

    public ByteArrayMap(boolean ordered, int initialCapacity) {
        this(ordered, initialCapacity, 0.75f);
    }

    public ByteArrayMap(boolean ordered, int initialCapacity, float loadFactor) {
        if (ordered)
            map = new LinkedHashMap<>(initialCapacity, loadFactor);
        else
            map = new HashMap<>(initialCapacity, loadFactor);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key != null && !(key instanceof byte[]))
            return false;
        return map.containsKey(new Key((byte[]) key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if (key != null && !(key instanceof byte[]))
            return null;
        return map.get(new Key((byte[]) key));
    }

    @Override
    public V put(byte[] key, V value) {
        return map.put(new Key(key), value);
    }

    @Override
    public void putAll(Map<? extends byte[], ? extends V> m) {
        if (m == null)
            return;
        for (Entry<? extends byte[], ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        if (key != null && !(key instanceof byte[]))
            return null;
        return map.remove(new Key((byte[]) key));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<byte[]> keySet() {
        return new KeySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<byte[], V>> entrySet() {
        return new EntrySet();
    }

    private static final class Key implements Serializable {
        private static final long serialVersionUID = 1L;

        private final byte[] bytes;

        private Key(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Key key = (Key) o;
            return Arrays.equals(bytes, key.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    private final class EntrySet extends AbstractSet<Entry<byte[], V>> {

        @Override
        public final int size() {
            return ByteArrayMap.this.size();
        }

        @Override
        public final void clear() {
            ByteArrayMap.this.clear();
        }

        @Override
        public final Iterator<Entry<byte[], V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Entry<?, ?> e = (Entry<?, ?>) o;
            Object obj = e.getKey();
            if (obj != null && !(obj instanceof byte[]))
                return false;
            byte[] key = (byte[]) obj;
            if (!ByteArrayMap.this.containsKey(key))
                return false;
            V v = ByteArrayMap.this.get(key);
            return v != null ? v.equals(e.getValue()) : e.getValue() == v;
        }

        @Override
        public final boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Entry<?, ?> e = (Entry<?, ?>) o;
            Object obj = e.getKey();
            Object value = e.getValue();
            if (obj != null && !(obj instanceof byte[]))
                return false;
            byte[] key = (byte[]) obj;
            if (!ByteArrayMap.this.containsKey(key))
                return false;
            V v = ByteArrayMap.this.get(key);
            if ((value == null && value == v) || (value != null && value.equals(v)))
                return ByteArrayMap.this.remove(key) != null;
            return false;
        }
    }

    private final class KeySet extends AbstractSet<byte[]> {

        @Override
        public final int size() {
            return ByteArrayMap.this.size();
        }

        @Override
        public final void clear() {
            ByteArrayMap.this.clear();
        }

        @Override
        public final Iterator<byte[]> iterator() {
            return new KeyIterator();
        }

        @Override
        public final boolean contains(Object o) {
            return ByteArrayMap.this.containsKey(o);
        }

        @Override
        public final boolean remove(Object key) {
            return ByteArrayMap.this.remove(key) != null;
        }
    }

    private final class KeyIterator implements Iterator<byte[]> {

        private final Iterator<Key> iterator = map.keySet().iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public byte[] next() {
            return iterator.next().bytes;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private final class EntryIterator implements Iterator<Entry<byte[], V>> {

        private final Iterator<Entry<Key, V>> iterator = map.entrySet().iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<byte[], V> next() {
            Entry<Key, V> v = iterator.next();
            return new Node(v.getKey().bytes, v.getValue());
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    private final class Node implements Entry<byte[], V> {

        private V value;
        private final byte[] bytes;

        private Node(byte[] bytes, V value) {
            this.bytes = bytes;
            this.value = value;
        }

        @Override
        public byte[] getKey() {
            return bytes;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Node node = (Node) o;
            if (value != null ? !value.equals(node.value) : node.value != null)
                return false;
            return Arrays.equals(bytes, node.bytes);
        }

        @Override
        public int hashCode() {
            int result = value != null ? value.hashCode() : 0;
            result = 31 * result + Arrays.hashCode(bytes);
            return result;
        }
    }
}
