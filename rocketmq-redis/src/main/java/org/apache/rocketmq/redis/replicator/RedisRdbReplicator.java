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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.rocketmq.redis.replicator.io.RedisInputStream;
import org.apache.rocketmq.redis.replicator.rdb.RdbParser;

import static org.apache.rocketmq.redis.replicator.Status.CONNECTED;
import static org.apache.rocketmq.redis.replicator.Status.DISCONNECTED;

public class RedisRdbReplicator extends AbstractReplicator {

    public RedisRdbReplicator(File file, Configuration configuration) throws FileNotFoundException {
        this(new FileInputStream(file), configuration);
    }

    public RedisRdbReplicator(InputStream in, Configuration configuration) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(configuration);
        this.configuration = configuration;
        this.inputStream = new RedisInputStream(in, this.configuration.getBufferSize());
        this.inputStream.setRawByteListeners(this.rawByteListeners);
        if (configuration.isUseDefaultExceptionListener())
            addExceptionListener(new DefaultExceptionListener());
    }

    @Override
    public void open() throws IOException {
        if (!this.connected.compareAndSet(DISCONNECTED, CONNECTED)) return;
        try {
            doOpen();
        } catch (EOFException ignore) {
        } catch (UncheckedIOException e) {
            if (!(e.getCause() instanceof EOFException))
                throw e.getCause();
        } finally {
            doClose();
            doCloseListener(this);
        }
    }

    protected void doOpen() throws IOException {
        RdbParser parser = new RdbParser(inputStream, this);
        parser.parse();
    }
}
