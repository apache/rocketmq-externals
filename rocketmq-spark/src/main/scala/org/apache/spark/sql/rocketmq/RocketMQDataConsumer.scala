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

package org.apache.spark.sql.rocketmq

import java.util.concurrent.TimeoutException
import java.{util => ju}

import org.apache.rocketmq.client.consumer.{MQPullConsumer, PullStatus}
import org.apache.rocketmq.common.message.{MessageExt, MessageQueue}
import org.apache.rocketmq.spark.{RocketMQConfig, RocketMqUtils}
import org.apache.spark.internal.Logging
import org.apache.spark.sql.rocketmq.RocketMQDataConsumer.AvailableOffsetRange
import org.apache.spark.sql.rocketmq.RocketMQSource._
import org.apache.spark.util.UninterruptibleThread
import org.apache.spark.{SparkEnv, SparkException, TaskContext}

import scala.collection.mutable

private[rocketmq] sealed trait RocketMQDataConsumer {
  /**
   * Get the record for the given offset if available. Otherwise it will either throw error
   * (if failOnDataLoss = true), or return the next available offset within [offset, untilOffset),
   * or null.
   *
   * @param offset         the offset to fetch.
   * @param untilOffset    the max offset to fetch. Exclusive.
   * @param pollTimeoutMs  timeout in milliseconds to poll data from RocketMQ.
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
      failOnDataLoss: Boolean): MessageExt = {
    internalConsumer.get(offset, untilOffset, pollTimeoutMs, failOnDataLoss)
  }

  /**
   * Return the available offset range of the current partition. It's a pair of the earliest offset
   * and the latest offset.
   */
  def getAvailableOffsetRange(): AvailableOffsetRange = internalConsumer.getAvailableOffsetRange()

  /**
   * Release this consumer from being further used. Depending on its implementation,
   * this consumer will be either finalized, or reset for reuse later.
   */
  def release(): Unit

  /** Reference to the internal implementation that this wrapper delegates to */
  protected def internalConsumer: InternalRocketMQConsumer
}


/**
 * A wrapper around RocketMQ's RocketMQConsumer that throws error when data loss is detected.
 * This is not for direct use outside this file.
 */
private[rocketmq] case class InternalRocketMQConsumer(consumer: MQPullConsumer,
                                                      queue: MessageQueue,
                                                      optionParams: ju.Map[String, String]) extends Logging {
  import InternalRocketMQConsumer._

  private val groupId = optionParams.get(RocketMQConfig.CONSUMER_GROUP)
  private val tags = optionParams.getOrDefault(RocketMQConfig.CONSUMER_TAG, RocketMQConfig.DEFAULT_TAG)
  private val maxBatchSize = optionParams.getOrDefault(RocketMQConfig.PULL_MAX_BATCH_SIZE, "32").toInt

  /** indicates whether this consumer is in use or not */
  @volatile var inUse = true

  /** indicate whether this consumer is going to be stopped in the next release */
  @volatile var markedForClose = false

  /** Iterator to the already fetch data */
  @volatile private var fetchedData = ju.Collections.emptyIterator[MessageExt]

  @volatile private var nextOffsetInFetchedData = UNKNOWN_OFFSET

  private def runUninterruptiblyIfPossible[T](body: => T): T = Thread.currentThread match {
    case ut: UninterruptibleThread =>
      ut.runUninterruptibly(body)
    case _ =>
      logWarning("CachedRocketMQConsumer is not running in UninterruptibleThread. " +
        "It may hang when CachedRocketMQConsumer's methods are interrupted because of KAFKA-1894")
      body
  }

  /**
   * Return the available offset range of the current partition. It's a pair of the earliest offset
   * and the latest offset.
   */
  def getAvailableOffsetRange(): AvailableOffsetRange = runUninterruptiblyIfPossible {
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
   * @param pollTimeoutMs timeout in milliseconds to poll data from RocketMQ.
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
    MessageExt = runUninterruptiblyIfPossible {
    require(offset < untilOffset,
      s"offset must always be less than untilOffset [offset: $offset, untilOffset: $untilOffset]")
    logDebug(s"Get $groupId $queue nextOffset $nextOffsetInFetchedData requested $offset")
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
        case e: OffsetOutOfRangeException =>
          // When there is some error thrown, it's better to use a new consumer to drop all cached
          // states in the old consumer. We don't need to worry about the performance because this
          // is not a common path.
          resetConsumer()
          reportDataLoss(failOnDataLoss, s"Cannot fetch offset $toFetchOffset", e)
          toFetchOffset = getEarliestAvailableOffsetBetween(toFetchOffset, untilOffset)
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
  private def getEarliestAvailableOffsetBetween(offset: Long, untilOffset: Long): Long = {
    val range = getAvailableOffsetRange()
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
          |The current available offset range is $range.
          | Offset ${offset} is out of range, and records in [$offset, $untilOffset) will be
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
           |The current available offset range is $range.
           | Offset ${offset} is out of range, and records in [$offset, ${range.earliest}) will be
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
   * @throws OffsetOutOfRangeException if `offset` is out of range
   * @throws TimeoutException if cannot fetch the record in `pollTimeoutMs` milliseconds.
   */
  private def fetchData(
      offset: Long,
      untilOffset: Long,
      pollTimeoutMs: Long,
      failOnDataLoss: Boolean): MessageExt = {
    if (offset != nextOffsetInFetchedData || !fetchedData.hasNext) {
      // This is the first fetch, or the last pre-fetched data has been drained.
      // Seek to the offset because we may call seekToBeginning or seekToEnd before this.
      seek(offset)
      poll(pollTimeoutMs)
    }

    if (!fetchedData.hasNext) {
      // We cannot fetch anything after `poll`. Two possible cases:
      // - `offset` is out of range so that RocketMQ returns nothing
      // - Cannot fetch any data before timeout
      throw new IllegalStateException(s"Cannot fetch record for offset $offset in $pollTimeoutMs milliseconds")
    } else {
      val record = fetchedData.next()
      assert(record.getQueueOffset == offset,
        s"Got wrong record for $groupId ${queue.getTopic} ${queue.getQueueId} ${queue.getBrokerName} even after seeking to offset $offset")
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
  }

  /** Create a new consumer and reset cached states */
  private def resetConsumer(): Unit = {
    // do not shutdown RocketMQ client because it is shared by multiple instances of CachedMQConsumer
    resetFetchedData()
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
    // do not shutdown RocketMQ client because it is shared by multiple instances of CachedMQConsumer
  }

  private def seek(offset: Long): Unit = {
    logDebug(s"Seeking to $groupId $queue $offset")
    consumer.updateConsumeOffset(queue, offset)
  }

  private def poll(pollTimeoutMs: Long): Unit = {
    val offset = consumer.fetchConsumeOffset(queue, false)
    var p = consumer.pull(queue, tags, offset, maxBatchSize, pollTimeoutMs)
    var i = 0
    if (p.getPullStatus == PullStatus.OFFSET_ILLEGAL){
      throw new OffsetOutOfRangeException(s"Failed to get records for $groupId ${queue.getTopic} ${queue.getQueueId} ${queue.getBrokerName} $offset after polling, due to ${p.toString}")
    }
    fetchedData = p.getMsgFoundList.iterator
  }
}


private[rocketmq] object RocketMQDataConsumer extends Logging {

  case class AvailableOffsetRange(earliest: Long, latest: Long)

  private case class CachedRocketMQDataConsumer(internalConsumer: InternalRocketMQConsumer)
    extends RocketMQDataConsumer {
    assert(internalConsumer.inUse) // make sure this has been set to true
    override def release(): Unit = { RocketMQDataConsumer.release(internalConsumer) }
  }

  private case class NonCachedRocketMQDataConsumer(internalConsumer: InternalRocketMQConsumer)
    extends RocketMQDataConsumer {
    override def release(): Unit = { internalConsumer.close() }
  }

  private case class CacheKey(groupId: String, queue: MessageQueue) {
    def this(queue: MessageQueue, optionParams: ju.Map[String, String]) =
      this(optionParams.get(RocketMQConfig.CONSUMER_GROUP), queue)
  }

  // This cache has the following important properties.
  // - We make a best-effort attempt to maintain the max size of the cache as configured capacity.
  //   The capacity is not guaranteed to be maintained, especially when there are more active
  //   tasks simultaneously using consumers than the capacity.
  private lazy val cache = {
    val conf = SparkEnv.get.conf
    val capacity = conf.getInt(s"spark.sql.rocketmq.${RocketMQConfig.PULL_CONSUMER_CACHE_MAX_CAPACITY}", 64)
    new ju.LinkedHashMap[CacheKey, InternalRocketMQConsumer](capacity, 0.75f, true) {
      override def removeEldestEntry(
        entry: ju.Map.Entry[CacheKey, InternalRocketMQConsumer]): Boolean = {

        // Try to remove the least-used entry if its currently not in use.
        //
        // If you cannot remove it, then the cache will keep growing. In the worst case,
        // the cache will grow to the max number of concurrent tasks that can run in the executor,
        // (that is, number of tasks slots) after which it will never reduce. This is unlikely to
        // be a serious problem because an executor with more than 64 (default) tasks slots is
        // likely running on a beefy machine that can handle a large number of simultaneously
        // active consumers.

        if (entry.getValue.inUse == false && this.size > capacity) {
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

  private val groupIdToClient = mutable.Map[String, MQPullConsumer]()

  /**
   * Get a cached consumer for groupId, assigned to topic and partition.
   * If matching consumer doesn't already exist, will be created using optionParams.
   * The returned consumer must be released explicitly using [[RocketMQDataConsumer.release()]].
   *
   * Note: This method guarantees that the consumer returned is not currently in use by any one
   * else. Within this guarantee, this method will make a best effort attempt to re-use consumers by
   * caching them and tracking when they are in use.
   */
  def acquire(
      queue: MessageQueue,
      optionParams: ju.Map[String, String],
      useCache: Boolean): RocketMQDataConsumer = synchronized {
    val key = new CacheKey(queue, optionParams)
    val existingInternalConsumer = cache.get(key)

    val groupId = optionParams.get(RocketMQConfig.CONSUMER_GROUP)

    // The client (MQPullConsumer) is shared by multiple instances of InternalRocketMQConsumer
    lazy val client = groupIdToClient.getOrElseUpdate(groupId, {
      RocketMqUtils.mkPullConsumerInstance(groupId, optionParams, s"instance-$groupId")
    })

    lazy val newInternalConsumer = new InternalRocketMQConsumer(client, queue, optionParams)

    if (TaskContext.get != null && TaskContext.get.attemptNumber >= 1) {
      // If this is reattempt at running the task, then invalidate cached consumer if any and
      // start with a new one.
      if (existingInternalConsumer != null) {
        // Consumer exists in cache. If its in use, mark it for closing later, or close it now.
        if (existingInternalConsumer.inUse) {
          existingInternalConsumer.markedForClose = true
        } else {
          existingInternalConsumer.close()
        }
      }
      cache.remove(key)  // Invalidate the cache in any case
      NonCachedRocketMQDataConsumer(newInternalConsumer)

    } else if (!useCache) {
      // If planner asks to not reuse consumers, then do not use it, return a new consumer
      NonCachedRocketMQDataConsumer(newInternalConsumer)

    } else if (existingInternalConsumer == null) {
      // If consumer is not already cached, then put a new in the cache and return it
      cache.put(key, newInternalConsumer)
      newInternalConsumer.inUse = true
      CachedRocketMQDataConsumer(newInternalConsumer)

    } else if (existingInternalConsumer.inUse) {
      // If consumer is already cached but is currently in use, then return a new consumer
      NonCachedRocketMQDataConsumer(newInternalConsumer)

    } else {
      // If consumer is already cached and is currently not in use, then return that consumer
      existingInternalConsumer.inUse = true
      CachedRocketMQDataConsumer(existingInternalConsumer)
    }
  }

  private def release(intConsumer: InternalRocketMQConsumer): Unit = {
    synchronized {

      // Clear the consumer from the cache if this is indeed the consumer present in the cache
      val key = new CacheKey(intConsumer.queue, intConsumer.optionParams)
      val cachedIntConsumer = cache.get(key)
      if (intConsumer.eq(cachedIntConsumer)) {
        // The released consumer is the same object as the cached one.
        if (intConsumer.markedForClose) {
          intConsumer.close()
          cache.remove(key)
        } else {
          intConsumer.inUse = false
        }
      } else {
        // The released consumer is either not the same one as in the cache, or not in the cache
        // at all. This may happen if the cache was invalidate while this consumer was being used.
        // Just close this consumer.
        intConsumer.close()
        logInfo(s"Released a supposedly cached consumer that was not found in the cache")
      }
    }
  }
}

private[rocketmq] object InternalRocketMQConsumer extends Logging {

  private val UNKNOWN_OFFSET = -2L

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

private[rocketmq] class OffsetOutOfRangeException(message: String) extends Exception {
  override def toString: String = "OffsetOutOfRange: " + message
}
