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

package org.apache.rocketmq.connect.redis.handler;

import com.moilioncircle.redis.replicator.cmd.Command;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.iterable.datatype.BatchedKeyValuePair;
import org.apache.rocketmq.connect.redis.pojo.KVEntry;

public interface RedisEventHandler {

    /**
     * handle redis command
     *
     * @param replId
     * @param replOffset
     * @param command
     * @return
     * @throws Exception
     */
    KVEntry handleCommand(String replId, Long replOffset, Command command) throws Exception;

    /**
     * handle data from RDB file
     *
     * @param replId
     * @param replOffset
     * @param keyValuePair
     * @return
     * @throws Exception
     */
    KVEntry handleKVString(String replId, Long replOffset, KeyValuePair keyValuePair) throws Exception;

    /**
     * handle redis batch kv data
     *
     * @param replId
     * @param replOffset
     * @param batchedKeyValuePair
     * @return
     * @throws Exception
     */
    KVEntry handleBatchKVString(String replId, Long replOffset, BatchedKeyValuePair batchedKeyValuePair) throws Exception;

    /**
     * handle other redis command
     *
     * @param replId
     * @param replOffset
     * @param event
     * @return
     * @throws Exception
     */
    KVEntry handleOtherEvent(String replId, Long replOffset, Event event) throws Exception;

}
