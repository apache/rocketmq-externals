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
import org.apache.rocketmq.replicator.redis.io.RawByteListener;
import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;
import org.apache.rocketmq.replicator.redis.rdb.datatype.AuxField;
import org.apache.rocketmq.replicator.redis.rdb.datatype.KeyValuePair;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class AbstractReplicatorListener implements ReplicatorListener {
    protected final List<RdbListener> rdbListeners = new CopyOnWriteArrayList<>();
    protected final List<CloseListener> closeListeners = new CopyOnWriteArrayList<>();
    protected final List<CommandListener> commandListeners = new CopyOnWriteArrayList<>();
    protected final List<RawByteListener> rawByteListeners = new CopyOnWriteArrayList<>();
    protected final List<AuxFieldListener> auxFieldListeners = new CopyOnWriteArrayList<>();
    protected final List<ExceptionListener> exceptionListeners = new CopyOnWriteArrayList<>();

    @Override
    public boolean addCommandListener(CommandListener listener) {
        return commandListeners.add(listener);
    }

    @Override
    public boolean removeCommandListener(CommandListener listener) {
        return commandListeners.remove(listener);
    }

    @Override
    public boolean addRdbListener(RdbListener listener) {
        return rdbListeners.add(listener);
    }

    @Override
    public boolean removeRdbListener(RdbListener listener) {
        return rdbListeners.remove(listener);
    }

    @Override
    public boolean addAuxFieldListener(AuxFieldListener listener) {
        return auxFieldListeners.add(listener);
    }

    @Override
    public boolean removeAuxFieldListener(AuxFieldListener listener) {
        return auxFieldListeners.remove(listener);
    }

    @Override
    public boolean addRawByteListener(RawByteListener listener) {
        return this.rawByteListeners.add(listener);
    }

    @Override
    public boolean removeRawByteListener(RawByteListener listener) {
        return this.rawByteListeners.remove(listener);
    }

    @Override
    public boolean addCloseListener(CloseListener listener) {
        return closeListeners.add(listener);
    }

    @Override
    public boolean removeCloseListener(CloseListener listener) {
        return closeListeners.remove(listener);
    }

    @Override
    public boolean addExceptionListener(ExceptionListener listener) {
        return exceptionListeners.add(listener);
    }

    @Override
    public boolean removeExceptionListener(ExceptionListener listener) {
        return exceptionListeners.remove(listener);
    }

    @Override
    public void handle(byte... rawBytes) {
        doRawByteListener(rawBytes);
    }

    protected void doCommandListener(Replicator replicator, Command command) {
        for (CommandListener listener : commandListeners) {
            listener.handle(replicator, command);
        }
    }

    protected void doRdbListener(Replicator replicator, KeyValuePair<?> kv) {
        for (RdbListener listener : rdbListeners) {
            listener.handle(replicator, kv);
        }
    }

    protected void doAuxFieldListener(Replicator replicator, AuxField auxField) {
        for (AuxFieldListener listener : auxFieldListeners) {
            listener.handle(replicator, auxField);
        }
    }

    protected void doPreFullSync(Replicator replicator) {
        for (RdbListener listener : rdbListeners) {
            listener.preFullSync(replicator);
        }
    }

    protected void doPostFullSync(Replicator replicator, final long checksum) {
        for (RdbListener listener : rdbListeners) {
            listener.postFullSync(replicator, checksum);
        }
    }

    protected void doCloseListener(Replicator replicator) {
        for (CloseListener listener : closeListeners) {
            listener.handle(replicator);
        }
    }

    protected void doExceptionListener(Replicator replicator, Throwable throwable, Object event) {
        for (ExceptionListener listener : exceptionListeners) {
            listener.handle(replicator, throwable, event);
        }
    }

    protected void doRawByteListener(byte... bytes) {
        for (RawByteListener listener : rawByteListeners) {
            listener.handle(bytes);
        }
    }
}
