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

package org.apache.rocketmq.replicator.redis.util;

import java.util.Iterator;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class ByteArray implements Iterable<byte[]> {

    protected static final int BITS = 30;
    protected static final int MAGIC = 1 << BITS;

    public static final long MIN_VALUE = 0L;
    public static final long MAX_VALUE = 2305843007066210304L; //(Integer.MAX_VALUE - 1) * MAGIC

    protected final int cap;
    protected final long length;
    protected byte[] smallBytes;
    protected byte[][] largeBytes;

    public ByteArray(byte[] smallBytes) {
        this(smallBytes, Integer.MAX_VALUE);
    }

    public ByteArray(long length) {
        this(length, Integer.MAX_VALUE);
    }

    public ByteArray(byte[] smallBytes, int cap) {
        this.cap = cap;
        this.length = smallBytes.length;
        this.smallBytes = smallBytes;
    }

    public ByteArray(long length, int cap) {
        this.cap = cap;
        this.length = length;
        if (length > MAX_VALUE || length < 0) {
            throw new IllegalArgumentException(String.valueOf(length));
        } else if (length <= cap) {
            this.smallBytes = new byte[(int) length];
        } else {
            int x = (int) (length >> BITS);
            int y = (int) (length & (MAGIC - 1));
            largeBytes = new byte[x + 1][];
            for (int i = 0; i < largeBytes.length; i++) {
                if (i == largeBytes.length - 1) {
                    largeBytes[i] = new byte[y];
                } else {
                    largeBytes[i] = new byte[MAGIC];
                }
            }
        }
    }

    public void set(long idx, byte value) {
        if (smallBytes != null) {
            smallBytes[(int) idx] = value;
            return;
        }
        int x = (int) (idx >> BITS);
        int y = (int) (idx & (MAGIC - 1));
        largeBytes[x][y] = value;
    }

    public byte get(long idx) {
        if (smallBytes != null) return smallBytes[(int) idx];
        int x = (int) (idx >> BITS);
        int y = (int) (idx & (MAGIC - 1));
        return largeBytes[x][y];
    }

    public long length() {
        return this.length;
    }

    public byte[] first() {
        Iterator<byte[]> it = this.iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Override
    public Iterator<byte[]> iterator() {
        return new Iter();
    }

    public static void arraycopy(ByteArray src, long srcPos, ByteArray dest, long destPos, long length) {
        if (srcPos + length > src.length || destPos + length > dest.length) {
            throw new IndexOutOfBoundsException();
        }
        if (srcPos + length <= src.cap && destPos + length <= dest.cap) {
            System.arraycopy(src.smallBytes, (int) srcPos, dest.smallBytes, (int) destPos, (int) length);
            return;
        }
        while (length > 0) {
            int x1 = (int) (srcPos >> BITS);
            int y1 = (int) (srcPos & (MAGIC - 1));
            int x2 = (int) (destPos >> BITS);
            int y2 = (int) (destPos & (MAGIC - 1));
            int min = Math.min(MAGIC - y1, MAGIC - y2);
            if (length <= MAGIC) min = Math.min(min, (int) length);
            System.arraycopy(src.largeBytes[x1], y1, dest.largeBytes[x2], y2, min);
            srcPos += min;
            destPos += min;
            length -= min;
        }
        assert length == 0;
    }

    protected class Iter implements Iterator<byte[]> {
        protected int index = 0;

        @Override
        public boolean hasNext() {
            if (smallBytes != null) return index < 1;
            return index < largeBytes.length;
        }

        @Override
        public byte[] next() {
            if (smallBytes != null) {
                index++;
                return smallBytes;
            }
            return largeBytes[index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
