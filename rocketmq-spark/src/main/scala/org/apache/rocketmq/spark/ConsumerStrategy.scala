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

import java.{util => ju}

import org.apache.rocketmq.common.UtilAll
import org.apache.rocketmq.common.message.MessageQueue
import org.apache.spark.streaming.MQPullInputDStream
import scala.collection.JavaConverters._

/**
  * Specify the start available offset for the rocketmq consumer
  */
sealed abstract class  ConsumerStrategy

/**
  * Specify the earliest available offset for the rocketmq consumer to start consuming..
  * But if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer will consume from
  * the checkpoint.
  */
case object EarliestStrategy extends ConsumerStrategy

/**
  * Specify the lastest available offset for the rocketmq consumer to start consuming.
  * But if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer will consume from
  * the checkpoint.
  */
case object LatestStrategy extends ConsumerStrategy

/**
  * Specify the specific available offset for the rocketmq consumer to start consuming.
  * Generally if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer will consume from
  * the checkpoint. But if the [[MQPullInputDStream.forceSpecial]] is true, the rocketmq will start consuming from
  * the specific available offset in any case. Of course, the consumer will use the min available offset if a message
  * queue is not specified.
  */
case class SpecificOffsetStrategy(
    queueToOffset: Map[MessageQueue, Long]) extends ConsumerStrategy


object ConsumerStrategy {
  /**
    * Used to denote offset range limits that are resolved via rocketmq
    */
  val LATEST = -1L // indicates resolution to the latest offset
  val EARLIEST = -2L // indicates resolution to the earliest offset

  def earliest: ConsumerStrategy =
    org.apache.rocketmq.spark.EarliestStrategy

  def lastest: ConsumerStrategy =
  org.apache.rocketmq.spark.LatestStrategy

  def specificOffset(queueToOffset: ju.Map[MessageQueue, Long]): ConsumerStrategy = {
    val scalaMapOffset = queueToOffset.asScala.map{ case (q, o) =>
      (q, o)
    }.toMap
    SpecificOffsetStrategy(scalaMapOffset)
  }

  def specificTime(queueToTime: ju.Map[MessageQueue, String]): ConsumerStrategy = {
    val queueToOffset = queueToTime.asScala.map{ case (q, t) =>
      val offset = UtilAll.parseDate(t, UtilAll.YYYY_MM_DD_HH_MM_SS).getTime
      (q, offset)
    }.toMap
    SpecificOffsetStrategy(queueToOffset)
  }

}