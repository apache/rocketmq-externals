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
 *    2. change commons-logging to slf4j
 *
 */

package org.apache.rocketmq.replicator.redis;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.rocketmq.replicator.redis.cmd.ReplyParser;
import org.apache.rocketmq.replicator.redis.io.RedisInputStream;
import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbVisitor;
import org.apache.rocketmq.replicator.redis.rdb.datatype.Module;
import org.apache.rocketmq.replicator.redis.rdb.module.ModuleParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class RedisAofReplicator extends AbstractReplicator {

    protected static final Logger logger = LoggerFactory.getLogger(RedisAofReplicator.class);
    protected final ReplyParser replyParser;

    public RedisAofReplicator(File file, Configuration configuration) throws FileNotFoundException {
        this(new FileInputStream(file), configuration);
    }

    public RedisAofReplicator(InputStream in, Configuration configuration) {
        this.configuration = configuration;
        this.inputStream = new RedisInputStream(in, this.configuration.getBufferSize());
        this.inputStream.addRawByteListener(this);
        this.replyParser = new ReplyParser(inputStream);
        builtInCommandParserRegister();
        addExceptionListener(new DefaultExceptionListener());
    }

    @Override
    public void open() throws IOException {
        try {
            doOpen();
        }
        catch (EOFException ignore) {
        }
        finally {
            close();
        }
    }

    protected void doOpen() throws IOException {
        while (true) {
            Object obj = replyParser.parse();

            submitObject(obj);
        }
    }

    @Override
    public void close() throws IOException {
        doClose();
    }

    @Override
    public ModuleParser<? extends Module> getModuleParser(String moduleName, int moduleVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Module> void addModuleParser(String moduleName, int moduleVersion, ModuleParser<T> parser) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModuleParser<? extends Module> removeModuleParser(String moduleName, int moduleVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRdbVisitor(RdbVisitor rdbVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RdbVisitor getRdbVisitor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addRdbListener(RdbListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeRdbListener(RdbListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAuxFieldListener(AuxFieldListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAuxFieldListener(AuxFieldListener listener) {
        throw new UnsupportedOperationException();
    }
}