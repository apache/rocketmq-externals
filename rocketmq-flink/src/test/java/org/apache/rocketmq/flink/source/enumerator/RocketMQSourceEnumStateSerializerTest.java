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

import org.apache.flink.api.connector.source.SplitsAssignment;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/** Test for {@link RocketMQSourceEnumStateSerializer}. */
public class RocketMQSourceEnumStateSerializerTest {

    @Test
    public void testSerializeDeserializeSourceEnumState() throws IOException {
        RocketMQSourceEnumStateSerializer serializer = new RocketMQSourceEnumStateSerializer();
        RocketMQSourceEnumState expected = prepareSourceEnumeratorState();
        RocketMQSourceEnumState actual = serializer.deserialize(0, serializer.serialize(expected));
        assertEquals(expected.getCurrentAssignment(), actual.getCurrentAssignment());
    }

    private RocketMQSourceEnumState prepareSourceEnumeratorState() {
        SplitsAssignment<RocketMQPartitionSplit> pendingAssignment =
                new SplitsAssignment<>(new HashMap<>());
        pendingAssignment
                .assignment()
                .put(
                        0,
                        Arrays.asList(
                                new RocketMQPartitionSplit(
                                        "0", "taobaodaily-01", 1, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "3", "taobaodaily-01", 2, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "6", "taobaodaily-01", 3, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "9", "taobaodaily-01", 4, 0, System.currentTimeMillis())));
        pendingAssignment
                .assignment()
                .put(
                        1,
                        Arrays.asList(
                                new RocketMQPartitionSplit(
                                        "1", "taobaodaily-02", 5, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "4", "taobaodaily-02", 6, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "7", "taobaodaily-02", 7, 0, System.currentTimeMillis())));
        pendingAssignment
                .assignment()
                .put(
                        2,
                        Arrays.asList(
                                new RocketMQPartitionSplit(
                                        "2", "taobaodaily-03", 8, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "5", "taobaodaily-03", 9, 0, System.currentTimeMillis()),
                                new RocketMQPartitionSplit(
                                        "8", "taobaodaily-03", 10, 0, System.currentTimeMillis())));
        return new RocketMQSourceEnumState(pendingAssignment.assignment());
    }
}
