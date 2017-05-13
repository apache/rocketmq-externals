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

import org.apache.rocketmq.replicator.redis.cmd.CommandListener;
import org.apache.rocketmq.replicator.redis.io.RawByteListener;
import org.apache.rocketmq.replicator.redis.rdb.AuxFieldListener;
import org.apache.rocketmq.replicator.redis.rdb.RdbListener;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public interface ReplicatorListener extends RawByteListener {
    /*
     * Rdb
     */
    boolean addRdbListener(RdbListener listener);

    boolean removeRdbListener(RdbListener listener);

    boolean addAuxFieldListener(AuxFieldListener listener);

    boolean removeAuxFieldListener(AuxFieldListener listener);

    /*
     * Raw byte
     */
    boolean addRawByteListener(RawByteListener listener);

    boolean removeRawByteListener(RawByteListener listener);

    /*
     * Command
     */
    boolean addCommandListener(CommandListener listener);

    boolean removeCommandListener(CommandListener listener);

    /*
     * Close
     */
    boolean addCloseListener(CloseListener listener);

    boolean removeCloseListener(CloseListener listener);

    /*
     * Exception
     */
    boolean addExceptionListener(ExceptionListener listener);

    boolean removeExceptionListener(ExceptionListener listener);
}
