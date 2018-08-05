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

/*
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaSourceOffset.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import org.apache.rocketmq.common.message.MessageQueue
import org.apache.spark.sql.execution.streaming.{Offset, SerializedOffset}
import org.apache.spark.sql.sources.v2.reader.streaming.{PartitionOffset, Offset => OffsetV2}

/**
 * An [[Offset]] for the [[RocketMQSource]]. This one tracks all partitions of subscribed topics and
 * their offsets.
 */
private[rocketmq]
case class RocketMQSourceOffset(queueToOffsets: Map[MessageQueue, Long]) extends OffsetV2 {
  override val json = JsonUtils.partitionOffsets(queueToOffsets)
}

private[rocketmq]
case class RocketMQSourcePartitionOffset(messageQueue: MessageQueue, queueOffset: Long)
  extends PartitionOffset

/** Companion object of the [[RocketMQSourceOffset]] */
private[rocketmq] object RocketMQSourceOffset {

  def getPartitionOffsets(offset: Offset): Map[MessageQueue, Long] = {
    offset match {
      case o: RocketMQSourceOffset => o.queueToOffsets
      case so: SerializedOffset => RocketMQSourceOffset(so).queueToOffsets
      case _ =>
        throw new IllegalArgumentException(
          s"Invalid conversion from offset of ${offset.getClass} to RocketMQSourceOffset")
    }
  }

  /**
   * Returns [[RocketMQSourceOffset]] from a variable sequence of (topic, brokerName, queueId, offset)
   * tuples.
   */
  def apply(offsetTuples: (String, String, Int, Long)*): RocketMQSourceOffset = {
    RocketMQSourceOffset(offsetTuples.map { case(t, b, q, o) => (new MessageQueue(t, b, q), o) }.toMap)
  }

  /**
   * Returns [[RocketMQSourceOffset]] from a JSON [[SerializedOffset]]
   */
  def apply(offset: SerializedOffset): RocketMQSourceOffset =
    RocketMQSourceOffset(JsonUtils.partitionOffsets(offset.json))
}
