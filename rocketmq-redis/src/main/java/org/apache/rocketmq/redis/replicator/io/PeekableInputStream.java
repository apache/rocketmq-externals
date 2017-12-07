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

public class PeekableInputStream extends InputStream {

    private int peek;
    private InputStream in;
    private boolean peeked;

    public PeekableInputStream(InputStream in) {
        this.in = in;
    }

    public int peek() throws IOException {
        if (!this.peeked) {
            this.peeked = true;
            return this.peek = this.in.read();
        }
        return this.peek;
    }

    @Override
    public int read() throws IOException {
        if (!this.peeked)
            return in.read();
        this.peeked = false;
        return this.peek;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        if (length <= 0)
            return 0;
        if (!this.peeked)
            return in.read(b, offset, length);
        this.peeked = false;
        if (this.peek < 0)
            return this.peek;
        int len = in.read(b, offset + 1, length - 1);
        b[offset] = (byte) this.peek;
        return len < 0 ? 1 : len + 1;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0)
            return 0;
        if (!this.peeked)
            return this.in.skip(n);
        this.peeked = false;
        return this.in.skip(n - 1) + 1;
    }

    @Override
    public int available() throws IOException {
        return (this.peeked ? 1 : 0) + this.in.available();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
        this.peeked = false;
    }
}
