/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.spark

import org.apache.spark.Partition


/**
  * the Partition for RocketMqRDD
  * @param index the partition id for rdd
  * @param topic the rockermq topic
  * @param queueId the rocketmq queue id
  * @param partitionOffsetRanges Represents a range of offsets from a single partition
  *
  */
class RocketMqRDDPartition(
                         val index: Int,
                         val topic: String,
                         val queueId: Int,
                         val partitionOffsetRanges: Array[OffsetRange]
                       ) extends Partition {
  /** Number of messages this partition refers to */
  def count(): Long = {
    if (!partitionOffsetRanges.isEmpty)
      partitionOffsetRanges.map(_.count).sum
    else 0L
  }


  /** rocketmq TopicQueueId object, for convenience */
  def topicQueueId(): TopicQueueId = new TopicQueueId(topic, queueId)

  def brokerNames(): Set[String] = {
    partitionOffsetRanges.map(_.brokerName).sorted.toSet
  }

  override def toString: String = {
    s"$index $topic $queueId ${partitionOffsetRanges.mkString(",")}"
  }
}