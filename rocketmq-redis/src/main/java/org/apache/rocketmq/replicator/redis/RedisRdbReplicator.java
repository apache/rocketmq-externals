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

package org.apache.rocketmq.replicator.redis;

import org.apache.rocketmq.replicator.redis.cmd.Command;
import org.apache.rocketmq.replicator.redis.cmd.CommandListener;
import org.apache.rocketmq.replicator.redis.cmd.CommandName;
import org.apache.rocketmq.replicator.redis.cmd.CommandParser;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.RdbParser;

import java.io.*;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisRdbReplicator extends AbstractReplicator {

    public RedisRdbReplicator(File file, Configuration configuration) throws FileNotFoundException {
        //bug fix http://git.oschina.net/leonchen83/redis-replicator/issues/2
        this(new FileInputStream(file), configuration);
    }

    public RedisRdbReplicator(InputStream in, Configuration configuration) {
        this.configuration = configuration;
        this.inputStream = new RedisInputStream(in, this.configuration.getBufferSize());
        this.inputStream.addRawByteListener(this);
        addExceptionListener(new DefaultExceptionListener());
    }

    @Override
    public void open() throws IOException {
        try {
            doOpen();
        } catch (EOFException ignore) {
        } finally {
            close();
        }
    }

    protected void doOpen() throws IOException {
        RdbParser parser = new RdbParser(inputStream, this);
        parser.parse();
    }

    @Override
    public void close() throws IOException {
        doClose();
    }

    @Override
    public boolean addCommandListener(CommandListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeCommandListener(CommandListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandParser<? extends Command> getCommandParser(CommandName command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Command> void addCommandParser(CommandName command, CommandParser<T> parser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommandParser<? extends Command> removeCommandParser(CommandName command) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void builtInCommandParserRegister() {
        throw new UnsupportedOperationException();
    }
}
