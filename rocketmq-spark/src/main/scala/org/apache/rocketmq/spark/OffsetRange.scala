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

import org.apache.rocketmq.common.message.MessageQueue

trait HasOffsetRanges {
  def offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]]
}

trait CanCommitOffsets {
  /**
    * Queue up offset ranges for commit to rocketmq at a future time.  Threadsafe.
    * This is only needed if you intend to store offsets in rocketmq, instead of your own store.
    * @param offsetRanges The maximum untilOffset for a given partition will be used at commit.
    */
  def commitAsync(offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]]): Unit

  /**
    * Queue up offset ranges for commit to rocketmq at a future time.  Threadsafe.
    * This is only needed if you intend to store offsets in rocketmq, instead of your own store.
    * @param offsetRanges The maximum untilOffset for a given partition will be used at commit.
    * @param callback Only the most recently provided callback will be used at commit.
    */
  def commitAsync(offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]], callback: OffsetCommitCallback): Unit
}

/**
  * Represents a range of offsets from a single rocketmq messageQueue. Instances of this class
  * can be created with `OffsetRange.create()`.
  *
  * @param topic  topic name
  * @param queueId  queueId id
  * @param brokerName the broker name
  * @param fromOffset  Inclusive starting offset
  * @param untilOffset  Exclusive ending offset
  */
final class OffsetRange private(
                                 val topic: String,
                                 val queueId: Int,
                                 val brokerName: String,
                                 val fromOffset: Long,
                                 val untilOffset: Long) extends Serializable {
  import OffsetRange.OffsetRangeTuple

  /** rocketmq topicMessageQueue object, for convenience */
  def topicMessageQueue(): MessageQueue = new MessageQueue(topic, brokerName, queueId)

  /** Number of messages this OffsetRange refers to */

  def count(): Long = {
    val ret = untilOffset - fromOffset
    assert(ret >= 0, s"OffsetRange happened errors form $topic $brokerName $fromOffset to $untilOffset")
    ret
  }

  override def equals(obj: Any): Boolean = obj match {
    case that: OffsetRange =>
      this.topic == that.topic &&
        this.queueId == that.queueId &&
        this.brokerName == that.brokerName &&
        this.fromOffset == that.fromOffset &&
        this.untilOffset == that.untilOffset
    case _ => false
  }

  override def hashCode(): Int = {
    toTuple.hashCode()
  }

  override def toString(): String = {
    s"OffsetRange(topic: '$topic', queueId: $queueId, brokerName: $brokerName, range: [$fromOffset -> $untilOffset])"
  }

  /** this is to avoid ClassNotFoundException during checkpoint restore */
  def toTuple: OffsetRangeTuple = (topic, queueId, brokerName, fromOffset, untilOffset)
}

/**
  * Companion object the provides methods to create instances of [[OffsetRange]].
  */
object OffsetRange {
  def create(topic: String, queueId: Int, brokerName: String, fromOffset: Long, untilOffset: Long): OffsetRange =
    new OffsetRange(topic, queueId, brokerName, fromOffset, untilOffset)

  def create(
              topicMessageQueue: MessageQueue,
              fromOffset: Long,
              untilOffset: Long): OffsetRange =
    new OffsetRange(topicMessageQueue.getTopic, topicMessageQueue.getQueueId, topicMessageQueue.getBrokerName, fromOffset,
      untilOffset)

  def apply(topic: String, queueId: Int, brokerName: String, fromOffset: Long, untilOffset: Long): OffsetRange =
    new OffsetRange(topic, queueId, brokerName, fromOffset, untilOffset)

  def apply(
             topicMessageQueue: MessageQueue,
             fromOffset: Long,
             untilOffset: Long): OffsetRange =
    new OffsetRange(topicMessageQueue.getTopic, topicMessageQueue.getQueueId, topicMessageQueue.getBrokerName, fromOffset,
      untilOffset)

  /** this is to avoid ClassNotFoundException during checkpoint restore */

  type OffsetRangeTuple = (String, Int, String, Long, Long)

  def apply(t: OffsetRangeTuple) =
    new OffsetRange(t._1, t._2, t._3, t._4, t._5)
}