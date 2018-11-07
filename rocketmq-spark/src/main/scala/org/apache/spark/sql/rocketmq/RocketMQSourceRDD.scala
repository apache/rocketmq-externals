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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaSourceRDD.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.{util => ju}

import org.apache.rocketmq.common.message.{MessageExt, MessageQueue}
import org.apache.spark.partial.{BoundedDouble, PartialResult}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.NextIterator
import org.apache.spark.{Partition, SparkContext, TaskContext}

import scala.collection.mutable.ArrayBuffer


/** Offset range that one partition of the RocketMQSourceRDD has to read */
private[rocketmq] case class RocketMQSourceRDDOffsetRange(
    messageQueue: MessageQueue,
    fromOffset: Long,
    untilOffset: Long,
    preferredLoc: Option[String]) {
  def size: Long = untilOffset - fromOffset
}

/** Partition of the RocketMQSourceRDD */
private[rocketmq] case class RocketMQSourceRDDPartition(index: Int,
    offsetRange: RocketMQSourceRDDOffsetRange) extends Partition

/**
 * An RDD that reads data from RocketMQ based on offset ranges across multiple partitions.
 * Additionally, it allows preferred locations to be set for each topic + partition, so that
 * the [[RocketMQSource]] can ensure the same executor always reads the same topic + partition
 * and cached RocketMQConsumers (see [[CachedRocketMQConsumer]] can be used read data efficiently.
 *
 * @param sc the [[SparkContext]]
 * @param executorRocketMQParams RocketMQ configuration for creating RocketMQConsumer on the executors
 * @param offsetRanges Offset ranges that define the RocketMQ data belonging to this RDD
 */
private[rocketmq] class RocketMQSourceRDD(
    sc: SparkContext,
    executorRocketMQParams: ju.Map[String, String],
    offsetRanges: Seq[RocketMQSourceRDDOffsetRange],
    pollTimeoutMs: Long,
    failOnDataLoss: Boolean,
    reuseRocketMQConsumer: Boolean)
  extends RDD[MessageExt](sc, Nil) {

  override def persist(newLevel: StorageLevel): this.type = {
    logError("RocketMQ ConsumerRecord is not serializable. " +
      "Use .map to extract fields before calling .persist or .window")
    super.persist(newLevel)
  }

  override def getPartitions: Array[Partition] = {
    offsetRanges.zipWithIndex.map { case (o, i) => RocketMQSourceRDDPartition(i, o) }.toArray
  }

  override def count(): Long = offsetRanges.map(_.size).sum

  override def countApprox(timeout: Long, confidence: Double): PartialResult[BoundedDouble] = {
    val c = count()
    new PartialResult(new BoundedDouble(c, 1.0, c, c), true)
  }

  override def isEmpty(): Boolean = count == 0L

  override def take(num: Int): Array[MessageExt] = {
    val nonEmptyPartitions = this.partitions
      .map(_.asInstanceOf[RocketMQSourceRDDPartition])
      .filter(_.offsetRange.size > 0)

    if (num < 1 || nonEmptyPartitions.isEmpty) {
      return new Array[MessageExt](0)
    }

    // Determine in advance how many messages need to be taken from each partition
    val parts = nonEmptyPartitions.foldLeft(Map[Int, Int]()) { (result, part) =>
      val remain = num - result.values.sum
      if (remain > 0) {
        val taken = Math.min(remain, part.offsetRange.size)
        result + (part.index -> taken.toInt)
      } else {
        result
      }
    }

    val buf = new ArrayBuffer[MessageExt]
    val res = context.runJob(
      this,
      (tc: TaskContext, it: Iterator[MessageExt]) =>
      it.take(parts(tc.partitionId())).toArray, parts.keys.toArray
    )
    res.foreach(buf ++= _)
    buf.toArray
  }

  override def getPreferredLocations(split: Partition): Seq[String] = {
    val part = split.asInstanceOf[RocketMQSourceRDDPartition]
    part.offsetRange.preferredLoc.map(Seq(_)).getOrElse(Seq.empty)
  }

  override def compute(
      thePart: Partition,
      context: TaskContext): Iterator[MessageExt] = {
    val sourcePartition = thePart.asInstanceOf[RocketMQSourceRDDPartition]
    val consumer = if (!reuseRocketMQConsumer) {
      CachedRocketMQConsumer.getOrCreate(sourcePartition.offsetRange.messageQueue, executorRocketMQParams)
    } else {
      CachedRocketMQConsumer.createUncached(sourcePartition.offsetRange.messageQueue, executorRocketMQParams)
    }

    val range = resolveRange(consumer, sourcePartition.offsetRange)
    assert(
      range.fromOffset <= range.untilOffset,
      s"Beginning offset ${range.fromOffset} is after the ending offset ${range.untilOffset} for " +
          s"${range.messageQueue}. You either provided an invalid fromOffset, or the RocketMQ topic has been damaged")
    if (range.fromOffset == range.untilOffset) {
      logInfo(s"Beginning offset ${range.fromOffset} is the same as ending offset, " +
          s"skipping ${range.messageQueue}")
      Iterator.empty
    } else {
      val underlying = new NextIterator[MessageExt]() {
        private var requestOffset = range.fromOffset

        override def getNext(): MessageExt = {
          if (requestOffset >= range.untilOffset) {
            // Processed all offsets in this partition.
            finished = true
            null
          } else {
            val r = consumer.get(requestOffset, range.untilOffset, pollTimeoutMs, failOnDataLoss)
            if (r == null) {
              // Losing some data. Skip the rest offsets in this partition.
              finished = true
              null
            } else {
              requestOffset = r.getQueueOffset + 1
              // The MessageExt structure does not contains any field of `brokerName`, so put one into properties
              r.putUserProperty(RocketMQSource.PROP_BROKER_NAME, sourcePartition.offsetRange.messageQueue.getBrokerName)
              r
            }
          }
        }

        override protected def close(): Unit = {
          if (!reuseRocketMQConsumer) {
            consumer.close()
          } else {
            CachedRocketMQConsumer.releaseConsumer(sourcePartition.offsetRange.messageQueue, executorRocketMQParams)
          }
        }
      }
      // Release consumer, either by removing it or indicating we're no longer using it
      context.addTaskCompletionListener { _ =>
        underlying.closeIfNeeded()
      }
      underlying
    }
  }

  /**
   * Resolve the EARLIEST/LATEST placeholder in range
   * @return the range with actual boundary
   */
  private def resolveRange(consumer: CachedRocketMQConsumer, range: RocketMQSourceRDDOffsetRange) = {
    if (range.fromOffset < 0 || range.untilOffset < 0) {
      // Late bind the offset range
      val availableOffsetRange = consumer.getAvailableOffsetRange()
      val fromOffset = if (range.fromOffset < 0) {
        assert(range.fromOffset == RocketMQOffsetRangeLimit.EARLIEST,
          s"earliest offset ${range.fromOffset} does not equal ${RocketMQOffsetRangeLimit.EARLIEST}")
        availableOffsetRange.earliest
      } else {
        range.fromOffset
      }
      val untilOffset = if (range.untilOffset < 0) {
        assert(range.untilOffset == RocketMQOffsetRangeLimit.LATEST,
          s"latest offset ${range.untilOffset} does not equal ${RocketMQOffsetRangeLimit.LATEST}")
        availableOffsetRange.latest
      } else {
        range.untilOffset
      }
      RocketMQSourceRDDOffsetRange(range.messageQueue, fromOffset, untilOffset, range.preferredLoc)
    } else {
      range
    }
  }
}
