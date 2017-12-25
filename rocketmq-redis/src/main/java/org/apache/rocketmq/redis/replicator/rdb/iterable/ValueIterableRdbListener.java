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

package org.apache.rocketmq.redis.replicator.rdb.iterable;

import org.apache.rocketmq.redis.replicator.Replicator;
import org.apache.rocketmq.redis.replicator.rdb.RdbListener;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueModule;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyStringValueString;
import org.apache.rocketmq.redis.replicator.rdb.datatype.KeyValuePair;
import org.apache.rocketmq.redis.replicator.rdb.datatype.Module;
import org.apache.rocketmq.redis.replicator.rdb.datatype.ZSetEntry;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueByteArrayIterator;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueMapEntryIterator;
import org.apache.rocketmq.redis.replicator.rdb.iterable.datatype.KeyStringValueZSetEntryIterator;
import org.apache.rocketmq.redis.replicator.util.ByteArrayMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET;
import static org.apache.rocketmq.redis.replicator.RedisConstants.RDB_TYPE_SET_INTSET;

public abstract class ValueIterableRdbListener extends RdbListener.Adaptor {

    private int batchSize;
    private boolean order;

    public ValueIterableRdbListener() {
        this(64);
    }

    public ValueIterableRdbListener(int batchSize) {
        this(true, batchSize);
    }

    public ValueIterableRdbListener(boolean order, int batchSize) {
        if (batchSize <= 0) throw new IllegalArgumentException(String.valueOf(batchSize));
        this.order = order;
        this.batchSize = batchSize;
    }

    public void handleString(KeyValuePair<byte[]> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public void handleList(KeyValuePair<List<byte[]>> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public void handleSet(KeyValuePair<Set<byte[]>> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public void handleMap(KeyValuePair<Map<byte[], byte[]>> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public void handleZSetEntry(KeyValuePair<Set<ZSetEntry>> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    public void handleModule(KeyValuePair<Module> kv, int batch, boolean last) {
        throw new UnsupportedOperationException("must implement this method.");
    }

    @Override
    public void handle(Replicator replicator, KeyValuePair<?> kv) {
        // Note that:
        // Every Iterator MUST be consumed.
        // Before every it.next() MUST check precondition it.hasNext()
        final int type = kv.getValueRdbType();
        int batch = 0;
        if (kv instanceof KeyStringValueString) {
            KeyStringValueString ksvs = (KeyStringValueString) kv;
            handleString(create(kv, ksvs.getRawValue()), batch, true);
        } else if (kv instanceof KeyStringValueByteArrayIterator) {
            if (type == RDB_TYPE_SET || type == RDB_TYPE_SET_INTSET) {
                Iterator<byte[]> it = ((KeyStringValueByteArrayIterator) kv).getValue();
                Set<byte[]> prev = null, next = create(order, batchSize);
                while (it.hasNext()) {
                    next.add(it.next());
                    if (next.size() == batchSize) {
                        if (prev != null)
                            handleSet(create(kv, prev), batch++, false);
                        prev = next;
                        next = create(order, batchSize);
                    }
                }
                final boolean last = next.isEmpty();
                handleSet(create(kv, prev), batch++, last);
                if (!last) handleSet(create(kv, next), batch++, true);
            } else {
                Iterator<byte[]> it = ((KeyStringValueByteArrayIterator) kv).getValue();
                List<byte[]> prev = null, next = new ArrayList<>(batchSize);
                while (it.hasNext()) {
                    try {
                        next.add(it.next());
                        if (next.size() == batchSize) {
                            if (prev != null)
                                handleList(create(kv, prev), batch++, false);
                            prev = next;
                            next = new ArrayList<>(batchSize);
                        }
                    } catch (IllegalStateException ignore) {
                        // see ValueIterableRdbVisitor.QuickListIter.next().
                    }
                }
                final boolean last = next.isEmpty();
                handleList(create(kv, prev), batch++, last);
                if (!last) handleList(create(kv, next), batch++, true);
            }
        } else if (kv instanceof KeyStringValueMapEntryIterator) {
            Iterator<Map.Entry<byte[], byte[]>> it = ((KeyStringValueMapEntryIterator) kv).getValue();
            Map<byte[], byte[]> prev = null, next = new ByteArrayMap<>(order, batchSize);
            while (it.hasNext()) {
                Map.Entry<byte[], byte[]> entry = it.next();
                next.put(entry.getKey(), entry.getValue());
                if (next.size() == batchSize) {
                    if (prev != null)
                        handleMap(create(kv, prev), batch++, false);
                    prev = next;
                    next = new ByteArrayMap<>(order, batchSize);
                }
            }
            final boolean last = next.isEmpty();
            handleMap(create(kv, prev), batch++, last);
            if (!last) handleMap(create(kv, next), batch++, true);
        } else if (kv instanceof KeyStringValueZSetEntryIterator) {
            Iterator<ZSetEntry> it = ((KeyStringValueZSetEntryIterator) kv).getValue();
            Set<ZSetEntry> prev = null, next = create(order, batchSize);
            while (it.hasNext()) {
                next.add(it.next());
                if (next.size() == batchSize) {
                    if (prev != null)
                        handleZSetEntry(create(kv, prev), batch++, false);
                    prev = next;
                    next = create(order, batchSize);
                }
            }
            final boolean last = next.isEmpty();
            handleZSetEntry(create(kv, prev), batch++, last);
            if (!last) handleZSetEntry(create(kv, next), batch++, true);
        } else if (kv instanceof KeyStringValueModule) {
            handleModule((KeyStringValueModule) kv, batch, true);
        }
    }

    private <T> Set<T> create(boolean order, int batchSize) {
        return order ? new LinkedHashSet<T>(batchSize) : new HashSet<T>(batchSize);
    }

    private <T> KeyValuePair<T> create(KeyValuePair<?> raw, T value) {
        KeyValuePair<T> kv = new KeyValuePair<>();
        kv.setDb(raw.getDb());
        kv.setExpiredType(raw.getExpiredType());
        kv.setExpiredValue(raw.getExpiredValue());
        kv.setValueRdbType(raw.getValueRdbType());
        kv.setKey(raw.getKey());
        kv.setRawKey(raw.getRawKey());
        kv.setValue(value);
        return kv;
    }
}
