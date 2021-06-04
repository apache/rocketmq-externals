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

package org.apache.rocketmq.flink.source.split;

/** This class extends RocketMQPartitionSplit to track a mutable current offset. */
public class RocketMQPartitionSplitState extends RocketMQPartitionSplit {

    private long currentOffset;

    public RocketMQPartitionSplitState(RocketMQPartitionSplit partitionSplit) {
        super(
                partitionSplit.getTopic(),
                partitionSplit.getBroker(),
                partitionSplit.getPartition(),
                partitionSplit.getStartingOffset(),
                partitionSplit.getStoppingTimestamp());
        this.currentOffset = partitionSplit.getStartingOffset();
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    /**
     * Use the current offset as the starting offset to create a new RocketMQPartitionSplit.
     *
     * @return a new RocketMQPartitionSplit which uses the current offset as its starting offset.
     */
    public RocketMQPartitionSplit toRocketMQPartitionSplit() {
        return new RocketMQPartitionSplit(
                getTopic(),
                getBroker(),
                getPartition(),
                getCurrentOffset(),
                getStoppingTimestamp());
    }
}
