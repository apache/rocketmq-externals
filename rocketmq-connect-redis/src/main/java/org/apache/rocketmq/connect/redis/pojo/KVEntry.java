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

package org.apache.rocketmq.connect.redis.pojo;

import io.openmessaging.connector.api.data.FieldType;
import java.util.List;

import com.moilioncircle.redis.replicator.cmd.Command;
import io.openmessaging.connector.api.data.EntryType;
import java.util.Map;
import org.apache.rocketmq.connect.redis.common.Options;

public interface KVEntry extends Command {

    KVEntry partition(String partition);

    String getPartition();

    KVEntry queueName(String queueName);

    String getQueueName();

    KVEntry entryType(EntryType entryType);

    EntryType getEntryType();

    KVEntry sourceId(String id);

    String getSourceId();

    KVEntry offset(Long offset);

    Long getOffset();

    KVEntry command(String command);

    String getCommand();

    KVEntry key(String key);

    String getKey();

    KVEntry value(Object value);

    Object getValue();

    KVEntry valueType(FieldType valueType);

    FieldType getValueType();

    <T> KVEntry param(Options<T> k, T v);

    <T> T getParam(Options<T> k);

    Map<String, Object> getParams();
}
