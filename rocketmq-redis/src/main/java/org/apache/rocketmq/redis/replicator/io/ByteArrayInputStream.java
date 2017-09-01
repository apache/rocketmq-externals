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

package org.apache.rocketmq.redis.replicator.io;

import java.io.IOException;
import java.io.InputStream;
import org.apache.rocketmq.redis.replicator.util.ByteArray;

public class ByteArrayInputStream extends InputStream {

    protected long pos;
    protected long count;
    protected long mark = 0;
    protected ByteArray buf;

    public ByteArrayInputStream(ByteArray buf) {
        this.pos = 0;
        this.buf = buf;
        this.count = buf.length();
    }

    @Override
    public int read() {
        return (pos < count) ? (buf.get(pos++) & 0xff) : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }

        if (pos >= count)
            return -1;
        int avail = (int) (count - pos);
        if (len > avail)
            len = avail;
        if (len <= 0)
            return 0;
        ByteArray.arraycopy(buf, pos, new ByteArray(b), off, len);
        pos += len;
        return len;
    }

    @Override
    public long skip(long n) {
        long k = count - pos;
        if (n < k)
            k = n < 0 ? 0 : n;
        pos += k;
        return k;
    }

    @Override
    public int available() {
        return (int) (this.count - this.pos);
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    @Override
    public void reset() {
        pos = mark;
    }

    @Override
    public void close() throws IOException {
    }
}
