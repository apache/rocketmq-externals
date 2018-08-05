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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaOffsetRangeLimit.scala
 *
 * There are some modifications:
 * 1. Adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import org.apache.rocketmq.common.message.MessageQueue

/**
 * Objects that represent desired offset range limits for starting,
 * ending, and specific offsets.
 */
private[rocketmq] sealed trait RocketMQOffsetRangeLimit

/**
 * Represents the desire to bind to the earliest offsets in RocketMQ
 */
private[rocketmq] case object EarliestOffsetRangeLimit extends RocketMQOffsetRangeLimit

/**
 * Represents the desire to bind to the latest offsets in RocketMQ
 */
private[rocketmq] case object LatestOffsetRangeLimit extends RocketMQOffsetRangeLimit

/**
 * Represents the desire to bind to specific offsets. A offset == -1 binds to the
 * latest offset, and offset == -2 binds to the earliest offset.
 */
private[rocketmq] case class SpecificOffsetRangeLimit(
    partitionOffsets: Map[MessageQueue, Long]) extends RocketMQOffsetRangeLimit

private[rocketmq] object RocketMQOffsetRangeLimit {
  /**
   * Used to denote offset range limits that are resolved via RocketMQ
   */
  val LATEST = -1L // indicates resolution to the latest offset
  val EARLIEST = -2L // indicates resolution to the earliest offset
}
