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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.System.currentTimeMillis;

public class RateLimitInputStream extends InputStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitInputStream.class);

    private static final int DEFAULT_PERMITS = 100 * 1024 * 1000; // 97.65MB/sec

    private final int permits;
    private RateLimiter limiter;
    private final InputStream in;

    public RateLimitInputStream(InputStream in) {
        this(in, DEFAULT_PERMITS);
    }

    public RateLimitInputStream(InputStream in, int permits) {
        if (permits <= 1000)
            permits = 1000;
        else if (permits > 1000)
            permits = permits / 1000 * 1000;
        LOGGER.info("rate limit force set to " + permits);

        this.in = in;
        this.permits = permits;
        this.limiter = new TokenBucketRateLimiter(this.permits);
    }

    @Override
    public int read() throws IOException {
        limiter.acquire(1);
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int offset, int length) throws IOException {
        int total = length, index = offset;
        while (total > 0) {
            int len = Math.min(permits, total);
            limiter.acquire(len);
            int r = in.read(b, index, len);
            index += r;
            total -= r;
            if (r < 0)
                return r;
            if (r < len)
                return length - total;
        }
        assert total == 0;
        return length;
    }

    @Override
    public long skip(long length) throws IOException {
        long total = length;
        while (total > 0) {
            int skip = (int) Math.min(permits, total);
            limiter.acquire(skip);
            long r = in.skip(skip);
            total -= r;
            if (r < skip)
                return length - total;
        }
        assert total == 0;
        return length;
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        this.in.close();
    }

    private interface RateLimiter {
        void acquire(long permits);
    }

    private class TokenBucketRateLimiter implements RateLimiter {

        private long access;
        private long borrow;
        private long permits;
        private final long size;
        private final double sleep;

        private TokenBucketRateLimiter(int permits) {
            this.access = currentTimeMillis();
            this.size = this.permits = permits;
            this.sleep = 1 * this.size / 1000d;
        }

        @Override
        public void acquire(long permits) {
            try {
                while (true) {
                    generate();
                    if (this.permits < permits) {
                        permits -= this.permits;
                    } else {
                        this.permits -= permits;
                        return;
                    }
                    double r = permits / sleep;
                    if (r < 1) {
                        this.borrow += permits;
                        while (this.borrow >= sleep) {
                            Thread.sleep(1);
                            this.borrow -= sleep;
                        }
                        return;
                    } else {
                        Thread.sleep((int) r);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private long generate() {
            long access = currentTimeMillis();
            if (access <= this.access)
                return 0L;
            long p = (access - this.access) * size / 1000L;
            this.permits += p;
            if (this.permits > size)
                this.permits = size;
            this.access = access;
            return p;
        }
    }
}
