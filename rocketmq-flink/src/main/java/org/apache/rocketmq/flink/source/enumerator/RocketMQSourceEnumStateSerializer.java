/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.source.enumerator;

import org.apache.rocketmq.flink.source.split.RocketMQPartitionSplit;
import org.apache.rocketmq.flink.source.split.RocketMQPartitionSplitSerializer;

import org.apache.flink.connector.base.source.utils.SerdeUtils;
import org.apache.flink.core.io.SimpleVersionedSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The {@link SimpleVersionedSerializer Serializer} for the enumerator state of RocketMQ source. */
public class RocketMQSourceEnumStateSerializer
        implements SimpleVersionedSerializer<RocketMQSourceEnumState> {

    private static final int CURRENT_VERSION = 0;

    @Override
    public int getVersion() {
        return CURRENT_VERSION;
    }

    @Override
    public byte[] serialize(RocketMQSourceEnumState enumState) throws IOException {
        return SerdeUtils.serializeSplitAssignments(
                enumState.getCurrentAssignment(), new RocketMQPartitionSplitSerializer());
    }

    @Override
    public RocketMQSourceEnumState deserialize(int version, byte[] serialized) throws IOException {
        // Check whether the version of serialized bytes is supported.
        if (version == getVersion()) {
            Map<Integer, List<RocketMQPartitionSplit>> currentPartitionAssignment =
                    SerdeUtils.deserializeSplitAssignments(
                            serialized, new RocketMQPartitionSplitSerializer(), ArrayList::new);
            return new RocketMQSourceEnumState(currentPartitionAssignment);
        }
        throw new IOException(
                String.format(
                        "The bytes are serialized with version %d, "
                                + "while this deserializer only supports version up to %d",
                        version, getVersion()));
    }
}
