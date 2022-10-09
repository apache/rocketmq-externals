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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaDataConsumer.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 * 2. Reuse underlying consumer instance for each consumer group
 */

package org.apache.spark.sql.rocketmq

import java.util.concurrent.TimeoutException
import java.{util => ju}

import org.apache.commons.lang3.mutable.MutableInt
import org.apache.rocketmq.client.consumer.{MQPullConsumer, PullStatus}
import org.apache.rocketmq.common.message.{MessageExt, MessageQueue}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.rocketmq.RocketMQSource._
import org.apache.spark.{SparkEnv, SparkException, TaskContext}

import scala.collection.mutable

/**
 * Consumer of single group, intended for cached reuse. Underlying consumer is threadsafe, but processing
 * the same queue and group id in multiple threads is usually bad anyway.
 */
private case class CachedRocketMQConsumer(
    consumer: MQPullConsumer,
    queue: MessageQueue,
    options: ju.Map[String, String]) extends Logging {
  import CachedRocketMQConsumer._

  private val groupId = options.get(RocketMQConf.CONSUMER_GROUP)

  // Since group ID is uniquely generated for each task, the following options must be same for identical group ID
  // so they are not presented in CacheKey
  private val subExpression = options.getOrDefault(RocketMQConf.CONSUMER_SUB_EXPRESSION, "*")
  private val maxBatchSize = options.getOrDefault(RocketMQConf.PULL_MAX_BATCH_SIZE, "32").toInt

  /** indicates whether this consumer is in use or not */
  @volatile var inUse = true

  /** Iterator to the already fetch data */
  @volatile private var fetchedData = ju.Collections.emptyIterator[MessageExt]

  @volatile private var nextOffsetInFetchedData = UNKNOWN_OFFSET

  /**
   * Return the available offset range of the current partition. It's a pair of the earliest offset
   * and the latest offset.
   */
  def getAvailableOffsetRange(): AvailableOffsetRange = {
    val earliestOffset = consumer.minOffset(queue)
    val latestOffset = consumer.maxOffset(queue)
    AvailableOffsetRange(earliestOffset, latestOffset)
  }

  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @param offset the offset to fetch.
   * @param untilOffset the max offset to fetch. Exclusive.
   * @param failOnDataLoss When `failOnDataLoss` is `true`, this method will either return record at
   *                       offset if available, or throw exception.when `failOnDataLoss` is `false`,
   *                       this method will either return record at offset if available, or return
   *                       the next earliest available record less than untilOffset, or null. It
   *                       will not throw any exception.
   */
  def get(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean):
    MessageExt = {
    require(offset < untilOffset,
      s"offset must always be less than untilOffset [offset: $offset, untilOffset: $untilOffset]")
    logDebug(s"Get $groupId $queue requested $offset")
    // The following loop is basically for `failOnDataLoss = false`. When `failOnDataLoss` is
    // `false`, first, we will try to fetch the record at `offset`. If no such record exists, then
    // we will move to the next available offset within `[offset, untilOffset)` and retry.
    // If `failOnDataLoss` is `true`, the loop body will be executed only once.
    var toFetchOffset = offset
    var consumerRecord: MessageExt = null
    // We want to break out of the while loop on a successful fetch to avoid using "return"
    // which may causes a NonLocalReturnControl exception when this method is used as a function.
    var isFetchComplete = false

    while (toFetchOffset != UNKNOWN_OFFSET && !isFetchComplete) {
      try {
        consumerRecord = fetchData(toFetchOffset, untilOffset, pollTimeoutMs, failOnDataLoss)
        isFetchComplete = true
      } catch {
        case e: OffsetIllegalException =>
          // When there is some error thrown, reset all states
          resetFetchedData()
          reportDataLoss(failOnDataLoss, s"Cannot fetch offset $toFetchOffset: ${e.toString}", e)
          toFetchOffset = getEarliestAvailableOffsetBetween(toFetchOffset, untilOffset, e.availableOffsetRange)
      }
    }

    if (isFetchComplete) {
      consumerRecord
    } else {
      resetFetchedData()
      null
    }
  }

  /**
   * Return the next earliest available offset in [offset, untilOffset). If all offsets in
   * [offset, untilOffset) are invalid (e.g., the topic is deleted and recreated), it will return
   * `UNKNOWN_OFFSET`.
   */
  private def getEarliestAvailableOffsetBetween(offset: Long, untilOffset: Long, range: AvailableOffsetRange): Long = {
    logWarning(s"Some data may be lost. Recovering from the earliest offset: ${range.earliest}")
    if (offset >= range.latest || range.earliest >= untilOffset) {
      // [offset, untilOffset) and [earliestOffset, latestOffset) have no overlap,
      // either
      // --------------------------------------------------------
      //         ^                 ^         ^         ^
      //         |                 |         |         |
      //   earliestOffset   latestOffset   offset   untilOffset
      //
      // or
      // --------------------------------------------------------
      //      ^          ^              ^                ^
      //      |          |              |                |
      //   offset   untilOffset   earliestOffset   latestOffset
      val warningMessage =
        s"""
          |The current available offset range is [${range.earliest}, ${range.latest}).
          | Offset $offset is out of range, and records in [$offset, $untilOffset) will be
          | skipped ${additionalMessage(failOnDataLoss = false)}
        """.stripMargin
      logWarning(warningMessage)
      UNKNOWN_OFFSET
    } else if (offset >= range.earliest) {
      // -----------------------------------------------------------------------------
      //         ^            ^                  ^                                 ^
      //         |            |                  |                                 |
      //   earliestOffset   offset   min(untilOffset,latestOffset)   max(untilOffset, latestOffset)
      //
      // This will happen when a topic is deleted and recreated, and new data are pushed very fast,
      // then we will see `offset` disappears first then appears again. Although the parameters
      // are same, the state in RocketMQ cluster is changed, so the outer loop won't be endless.
      logWarning(s"Found a disappeared offset $offset. " +
        s"Some data may be lost ${additionalMessage(failOnDataLoss = false)}")
      offset
    } else {
      // ------------------------------------------------------------------------------
      //      ^           ^                       ^                                 ^
      //      |           |                       |                                 |
      //   offset   earliestOffset   min(untilOffset,latestOffset)   max(untilOffset, latestOffset)
      val warningMessage =
        s"""
           |The current available offset range is [${range.earliest}, ${range.latest}).
           | Offset $offset is out of range, and records in [$offset, ${range.earliest}) will be
           | skipped ${additionalMessage(failOnDataLoss = false)}
        """.stripMargin
      logWarning(warningMessage)
      range.earliest
    }
  }

  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @throws OffsetIllegalException if `offset` is out of range
   * @throws TimeoutException if cannot fetch the record in `pollTimeoutMs` milliseconds.
   */
  private def fetchData(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean): MessageExt = {
    if (offset != nextOffsetInFetchedData || !fetchedData.hasNext) {
      // This is the first fetch, or the last pre-fetched data has been drained.
      val p = consumer.pull(queue, subExpression, offset, maxBatchSize, pollTimeoutMs)
      if (p.getPullStatus == PullStatus.OFFSET_ILLEGAL){
        throw new OffsetIllegalException(AvailableOffsetRange(p.getMinOffset, p.getMaxOffset))
      } else if (p.getPullStatus == PullStatus.NO_MATCHED_MSG || p.getPullStatus == PullStatus.NO_NEW_MSG) {
        throw new IllegalStateException(s"Cannot fetch record for offset $offset in $pollTimeoutMs milliseconds. " +
            s"status = ${p.getPullStatus.toString}")
      }
      fetchedData = p.getMsgFoundList.iterator
      assert(fetchedData.hasNext)
    }

    val record = fetchedData.next()
    assert(record.getQueueOffset == offset,
      s"Got wrong record for $groupId ${queue.toString} even after seeking to offset $offset")
    nextOffsetInFetchedData = record.getQueueOffset + 1
    // In general, RocketMQ uses the specified offset as the start point, and tries to fetch the next
    // available offset. Hence we need to handle offset mismatch.
    if (record.getQueueOffset > offset) {
      // This may happen when some records aged out but their offsets already got verified
      if (failOnDataLoss) {
        reportDataLoss(true, s"Cannot fetch records in [$offset, ${record.getQueueOffset})")
        // Never happen as "reportDataLoss" will throw an exception
        null
      } else {
        if (record.getQueueOffset >= untilOffset) {
          reportDataLoss(false, s"Skip missing records in [$offset, $untilOffset)")
          null
        } else {
          reportDataLoss(false, s"Skip missing records in [$offset, ${record.getQueueOffset})")
          record
        }
      }
    } else if (record.getQueueOffset < offset) {
      // This should not happen. If it does happen, then we probably misunderstand RocketMQ internal
      // mechanism.
      throw new IllegalStateException(
        s"Tried to fetch $offset but the returned record offset was ${record.getQueueOffset}")
    } else {
      record
    }
  }

  /** Reset the internal pre-fetched data. */
  private def resetFetchedData(): Unit = {
    nextOffsetInFetchedData = UNKNOWN_OFFSET
    fetchedData = ju.Collections.emptyIterator[MessageExt]
  }

  /**
   * Return an addition message including useful message and instruction.
   */
  private def additionalMessage(failOnDataLoss: Boolean): String = {
    if (failOnDataLoss) {
      s"(GroupId: $groupId, MessageQueue: $queue). " +
        s"$INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_TRUE"
    } else {
      s"(GroupId: $groupId, MessageQueue: $queue). " +
        s"$INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_FALSE"
    }
  }

  /**
   * Throw an exception or log a warning as per `failOnDataLoss`.
   */
  private def reportDataLoss(
      failOnDataLoss: Boolean,
      message: String,
      cause: Throwable = null): Unit = {
    val finalMessage = s"$message ${additionalMessage(failOnDataLoss)}"
    reportDataLoss0(failOnDataLoss, finalMessage, cause)
  }

  def close(): Unit = {
    // Shutdown the underlying consumer if nobody is using
    val consumerToShutdown = synchronized {
      val useCount = groupIdUseCount(groupId).decrementAndGet()
      if (useCount == 0) {
        groupIdUseCount.remove(groupId)
        groupIdToClient.remove(groupId)
      } else None
    }
    if (consumerToShutdown.isDefined) {
      consumerToShutdown.get.shutdown()
    }
  }
}

private case class AvailableOffsetRange(earliest: Long, latest: Long)

private object CachedRocketMQConsumer extends Logging {

  private val UNKNOWN_OFFSET = -2L

  private case class CacheKey(groupId: String, queue: MessageQueue)

  private object CacheKey {
    def from(queue: MessageQueue, options: ju.Map[String, String]): CacheKey = {
      CacheKey(options.get(RocketMQConf.CONSUMER_GROUP), queue)
    }
  }
  private class OffsetIllegalException(val availableOffsetRange: AvailableOffsetRange) extends Exception

  private lazy val cache = {
    val conf = SparkEnv.get.conf
    val capacity = conf.getInt(RocketMQConf.PULL_CONSUMER_CACHE_MAX_CAPACITY, 64)
    new ju.LinkedHashMap[CacheKey, CachedRocketMQConsumer](capacity, 0.75f, true) {
      override def removeEldestEntry(
          entry: ju.Map.Entry[CacheKey, CachedRocketMQConsumer]): Boolean = {

        if (!entry.getValue.inUse && this.size > capacity) {
          logWarning(
            s"RocketMQConsumer cache hitting max capacity of $capacity, " +
                s"removing consumer for ${entry.getKey}")
          try {
            entry.getValue.close()
          } catch {
            case e: SparkException =>
              logError(s"Error closing earliest RocketMQ consumer for ${entry.getKey}", e)
          }
          true
        } else {
          false
        }
      }
    }
  }

  // The MQPullConsumer client is shared by multiple instances of CachedRocketMQConsumer
  // because RocketMQ claims there should not be more than one instance for a groupId
  private val groupIdToClient = mutable.Map[String, MQPullConsumer]()
  // For cleaning unused clients
  private val groupIdUseCount = mutable.Map[String, MutableInt]()

  def releaseConsumer(
      queue: MessageQueue,
      options: ju.Map[String, String]): Unit = {
    val key = CacheKey.from(queue, options)

    synchronized {
      val consumer = cache.get(key)
      if (consumer != null) {
        consumer.inUse = false
      } else {
        logWarning(s"Attempting to release consumer that does not exist")
      }
    }
  }

  /**
    * Removes (and closes) the RocketMQ Consumer for the given MessageQueue and groupId.
    */
  def removeConsumer(
      queue: MessageQueue,
      options: ju.Map[String, String]): Unit = {
    val key = CacheKey.from(queue, options)

    synchronized {
      val removedConsumer = cache.remove(key)
      if (removedConsumer != null) {
        removedConsumer.close()
      }
    }
  }

  /**
    * Get a cached consumer for groupId, assigned to topic and partition.
    * If matching consumer doesn't already exist, will be created using options.
    */
  def getOrCreate(
      queue: MessageQueue,
      options: ju.Map[String, String]): CachedRocketMQConsumer = synchronized {
    val key = CacheKey.from(queue, options)

    // The MQPullConsumer client is shared by multiple instances of CachedRocketMQConsumer
    // because RocketMQ claims there should not be more than one instance for a groupId
    val groupId = options.get(RocketMQConf.CONSUMER_GROUP)
    val client = synchronized {
      groupIdUseCount.getOrElseUpdate(groupId, new MutableInt(0)).increment()
      groupIdToClient.getOrElseUpdate(groupId, RocketMQSqlUtils.makePullConsumer(groupId, options))
    }

    // If this is reattempt at running the task, then invalidate cache and start with
    // a new consumer
    if (TaskContext.get != null && TaskContext.get.attemptNumber >= 1) {
      removeConsumer(queue, options)
      val consumer = new CachedRocketMQConsumer(client, queue, options)
      consumer.inUse = true
      cache.put(key, consumer)
      consumer
    } else {
      if (!cache.containsKey(key)) {
        cache.put(key, new CachedRocketMQConsumer(client, queue, options))
      }
      val consumer = cache.get(key)
      consumer.inUse = true
      consumer
    }
  }

  /** Create an [[CachedRocketMQConsumer]] but don't put it into cache. */
  def createUncached(
      queue: MessageQueue,
      options: ju.Map[String, String]): CachedRocketMQConsumer = {
    val groupId = options.get(RocketMQConf.CONSUMER_GROUP)
    val client = synchronized {
      groupIdUseCount.getOrElseUpdate(groupId, new MutableInt(0)).increment()
      groupIdToClient.getOrElseUpdate(groupId, RocketMQSqlUtils.makePullConsumer(groupId, options))
    }

    new CachedRocketMQConsumer(client, queue, options)
  }

  private def reportDataLoss0(
      failOnDataLoss: Boolean,
      finalMessage: String,
      cause: Throwable = null): Unit = {
    if (failOnDataLoss) {
      if (cause != null) {
        throw new IllegalStateException(finalMessage, cause)
      } else {
        throw new IllegalStateException(finalMessage)
      }
    } else {
      if (cause != null) {
        logWarning(finalMessage, cause)
      } else {
        logWarning(finalMessage)
      }
    }
  }
}
