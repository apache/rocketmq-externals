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

package org.apache.rocketmq.redis.replicator;

import java.io.IOException;

import static org.apache.rocketmq.redis.replicator.Status.CONNECTED;

abstract class AbstractReplicatorRetrier implements ReplicatorRetrier {
    protected int retries = 0;

    protected abstract boolean open() throws IOException;

    protected abstract boolean connect() throws IOException;

    protected abstract boolean close(IOException reason) throws IOException;

    @Override
    public void retry(Replicator replicator) throws IOException {
        IOException exception = null;
        Configuration configuration = replicator.getConfiguration();
        for (; retries < configuration.getRetries() || configuration.getRetries() <= 0; retries++) {
            exception = null;
            final long interval = configuration.getRetryTimeInterval();
            try {
                if (connect()) {
                    reset();
                }
                if (!open()) {
                    reset();
                    close(null);
                    sleep(interval);
                    continue;
                }
                exception = null;
                break;
            } catch (IOException | UncheckedIOException e) {
                //close manually
                if (replicator.getStatus() != CONNECTED) {
                    exception = null;
                    break;
                }
                exception = translate(e);
                close(exception);
                sleep(interval);
            }
        }
        if (exception != null) throw exception;
    }

    protected void reset() {
        this.retries = 0;
    }

    protected void sleep(long interval) {
        try {
            Thread.sleep(interval);
        } catch (InterruptedException interrupt) {
            Thread.currentThread().interrupt();
        }
    }

    protected IOException translate(Exception e) {
        if (e instanceof UncheckedIOException) {
            return ((UncheckedIOException) e).getCause();
        } else if (e instanceof IOException) {
            return (IOException) e;
        } else {
            return new IOException(e.getMessage());
        }
    }
}