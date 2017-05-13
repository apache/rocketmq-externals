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

package org.apache.rocketmq.replicator.redis.io;

import org.apache.rocketmq.replicator.redis.Constants;
import org.apache.rocketmq.replicator.redis.util.ByteArray;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisInputStream extends InputStream {
    protected int head = 0;
    protected int tail = 0;
    protected long total = 0;
    protected long markLen = 0;
    protected final byte[] buf;
    protected boolean mark = false;
    protected final InputStream in;
    protected final List<RawByteListener> listeners = new CopyOnWriteArrayList<>();

    public RedisInputStream(final InputStream in) {
        this(in, 8192);
    }

    public RedisInputStream(final InputStream in, int len) {
        this.in = in;
        this.buf = new byte[len];
    }

    public void addRawByteListener(RawByteListener listener) {
        this.listeners.add(listener);
    }

    public void removeRawByteListener(RawByteListener listener) {
        this.listeners.remove(listener);
    }

    protected void notify(byte... bytes) {
        for (RawByteListener listener : listeners) {
            listener.handle(bytes);
        }
    }

    public int head() {
        return head;
    }

    public int tail() {
        return tail;
    }

    public int bufSize() {
        return buf.length;
    }

    public void mark() {
        if (!mark) {
            mark = true;
            return;
        }
        throw new AssertionError("already marked");
    }

    public long unmark() {
        if (mark) {
            long rs = markLen;
            markLen = 0;
            mark = false;
            return rs;
        }
        throw new AssertionError("must mark first");
    }

    public long total() {
        return total;
    }

    public ByteArray readBytes(long len) throws IOException {
        ByteArray bytes = new ByteArray(len);
        this.read(bytes, 0, len);
        if (mark) markLen += len;
        return bytes;
    }

    public int readInt(int len) throws IOException {
        return readInt(len, true);
    }

    public long readLong(int len) throws IOException {
        return readLong(len, true);
    }

    public int readInt(int length, boolean littleEndian) throws IOException {
        int r = 0;
        for (int i = 0; i < length; ++i) {
            final int v = this.read();
            if (littleEndian) {
                r |= (v << (i << 3));
            } else {
                r = (r << 8) | v;
            }
        }
        int c;
        return r << (c = (4 - length << 3)) >> c;
    }

    public long readUInt(int length) throws IOException {
        return readUInt(length, true);
    }

    public long readUInt(int length, boolean littleEndian) throws IOException {
        return readInt(length, littleEndian) & 0xFFFFFFFFL;
    }

    public int readInt(byte[] bytes) {
        return readInt(bytes, true);
    }

    public int readInt(byte[] bytes, boolean littleEndian) {
        int r = 0;
        int length = bytes.length;
        for (int i = 0; i < length; ++i) {
            final int v = bytes[i] & 0xff;
            if (littleEndian) {
                r |= (v << (i << 3));
            } else {
                r = (r << 8) | v;
            }
        }
        int c;
        return r << (c = (4 - length << 3)) >> c;
    }

    public long readLong(int length, boolean littleEndian) throws IOException {
        long r = 0;
        for (int i = 0; i < length; ++i) {
            final long v = this.read();
            if (littleEndian) {
                r |= (v << (i << 3));
            } else {
                r = (r << 8) | v;
            }
        }
        return r;
    }

    public String readString(int len) throws IOException {
        return readString(len, Constants.CHARSET);
    }

    public String readString(int len, Charset charset) throws IOException {
        byte[] original = readBytes(len).first();
        return new String(original, charset);
    }

    @Override
    public int read() throws IOException {
        if (head >= tail) fill();
        if (mark) markLen += 1;
        byte b = buf[head++];
        notify(b);
        return b & 0xff;
    }

    public long read(ByteArray bytes, long offset, long len) throws IOException {
        long total = len;
        long index = offset;
        while (total > 0) {
            int available = tail - head;
            if (available >= total) {
                ByteArray.arraycopy(new ByteArray(buf), head, bytes, index, total);
                head += total;
                break;
            } else {
                ByteArray.arraycopy(new ByteArray(buf), head, bytes, index, available);
                index += available;
                total -= available;
                fill();
            }
        }
        for (byte[] b : bytes) {
            notify(b);
        }
        return len;
    }

    @Override
    public int available() throws IOException {
        return tail - head + in.available();
    }

    @Override
    public long skip(long len) throws IOException {
        long total = len;
        while (total > 0) {
            int available = tail - head;
            if (available >= total) {
                notify(Arrays.copyOfRange(buf, head, head + (int) total));
                head += total;
                break;
            } else {
                notify(Arrays.copyOfRange(buf, head, tail));
                total -= available;
                fill();
            }
        }
        return len;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    protected void fill() throws IOException {
        tail = in.read(buf, 0, buf.length);
        if (tail == -1) throw new EOFException("end of file or stream.");
        total += tail;
        head = 0;
    }
}
