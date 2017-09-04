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

import org.apache.rocketmq.replicator.redis.RedisConstants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
//NonThreadSafe
public class ByteBuilder {

    private final ByteBuffer buffer;
    private final List<byte[]> list = new ArrayList<>();
    private int total = 0;

    private ByteBuilder(int cap) {
        this.buffer = ByteBuffer.allocate(cap);
    }

    public static ByteBuilder allocate(int cap) {
        return new ByteBuilder(cap);
    }

    public void put(byte b) {
        total++;
        if (buffer.hasRemaining()) {
            buffer.put(b);
        } else {
            byte[] temp = new byte[buffer.capacity()];
            System.arraycopy(buffer.array(), 0, temp, 0, buffer.capacity());
            list.add(temp);
            buffer.clear();
            buffer.put(b);
        }
    }

    public int length() {
        return total;
    }

    public byte[] array() {
        int len = total;
        if (len < buffer.capacity()) return buffer.array();
        int offset = 0;
        byte[] ary = new byte[len];
        for (byte[] ba : list) {
            System.arraycopy(ba, 0, ary, offset, ba.length);
            offset += ba.length;
            len -= ba.length;
        }
        if (len > 0) System.arraycopy(buffer.array(), 0, ary, offset, len);
        return ary;
    }

    @Override
    public String toString() {
        return new String(array(), 0, total, RedisConstants.CHARSET);
    }
}
