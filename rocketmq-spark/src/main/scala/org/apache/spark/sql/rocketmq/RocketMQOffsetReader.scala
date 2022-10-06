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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaOffsetReader.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.{util => ju}

import org.apache.rocketmq.client.consumer.MQPullConsumer
import org.apache.rocketmq.common.message.MessageQueue
import org.apache.spark.internal.Logging

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
 * This class uses RocketMQ's own [[MQPullConsumer]] API to read data offsets from RocketMQ.
 */
private class RocketMQOffsetReader(
    driverRocketMQParams: ju.Map[String, String],
    readerOptions: Map[String, String],
    driverGroupIdPrefix: String) extends Logging {
  val topic: String = driverRocketMQParams.get(RocketMQConf.CONSUMER_TOPIC)

  /**
   * Place [[groupId]] and [[nextId]] here so that they are initialized before any consumer is
   * created -- see SPARK-19564.
   */
  private var groupId: String = _
  private var nextId = 0

  /**
   * A RocketMQConsumer used in the driver to query the latest RocketMQ offsets. This only queries the
   * offsets and never commits them.
   */
  protected var consumer: MQPullConsumer = createConsumer()

  private val maxOffsetFetchAttempts =
    readerOptions.getOrElse("fetchOffset.numRetries", "3").toInt

  private val offsetFetchAttemptIntervalMs =
    readerOptions.getOrElse("fetchOffset.retryIntervalMs", "1000").toLong

  private def nextGroupId(): String = {
    groupId = driverGroupIdPrefix + "-" + nextId
    nextId += 1
    groupId
  }

  /**
   * Closes the connection to RocketMQ, and cleans up state.
   */
  def close(): Unit = {
    consumer.shutdown()
  }

  /**
   * @return The Set of MessageQueue for a given topic
   */
  def fetchTopicPartitions(): Set[MessageQueue] = {
    val partitions = consumer.fetchSubscribeMessageQueues(topic)
    partitions.asScala.toSet
  }

  /**
   * Resolves the specific offsets based on RocketMQ seek positions.
   * This method resolves offset value -1 to the latest and -2 to the
   * earliest RocketMQ seek position.
   *
   * @param partitionOffsets the specific offsets to resolve
   * @param reportDataLoss callback to either report or log data loss depending on setting
   */
  def fetchSpecificOffsets(
      partitionOffsets: Map[MessageQueue, Long],
      reportDataLoss: String => Unit): RocketMQSourceOffset = {
    val fetched = {
      withRetries {
        val partitions = consumer.fetchSubscribeMessageQueues(topic)
        assert(partitions.asScala == partitionOffsets.keySet,
          "If startingOffsets contains specific offsets, you must specify all TopicPartitions.\n" +
            "Use -1 for latest, -2 for earliest, if you don't care.\n" +
            s"Specified: ${partitionOffsets.keySet} Assigned: ${partitions.asScala}")
        logDebug(s"Partitions assigned to consumer: $partitions. Seeking to $partitionOffsets")

        partitionOffsets.foreach {
          case (mq, RocketMQOffsetRangeLimit.LATEST) =>
            consumer.updateConsumeOffset(mq, consumer.maxOffset(mq))
          case (mq, RocketMQOffsetRangeLimit.EARLIEST) =>
            consumer.updateConsumeOffset(mq, consumer.minOffset(mq))
          case (mq, offset) => consumer.updateConsumeOffset(mq, offset)
        }
        partitionOffsets.map {
          case (mq, _) => mq -> consumer.fetchConsumeOffset(mq, false)
        }
      }
    }

    partitionOffsets.foreach {
      case (tp, off) if off != RocketMQOffsetRangeLimit.LATEST &&
        off != RocketMQOffsetRangeLimit.EARLIEST =>
        if (fetched(tp) != off) {
          reportDataLoss(
            s"startingOffsets for $tp was $off but consumer reset to ${fetched(tp)}")
        }
      case _ =>
        // no real way to check that beginning or end is reasonable
    }
    RocketMQSourceOffset(fetched)
  }

  /**
   * Fetch the earliest offsets for the topic partitions
   */
  def fetchEarliestOffsets(): Map[MessageQueue, Long] = {
    withRetries {
      val partitions = consumer.fetchSubscribeMessageQueues(topic)
      logDebug(s"Partitions assigned to consumer: $partitions. Seeking to the beginning")

      val partitionOffsets = partitions.asScala.map(p => p -> consumer.minOffset(p)).toMap
      logDebug(s"Got earliest offsets for partition : $partitionOffsets")
      partitionOffsets
    }
  }

  /**
   * Fetch the latest offsets for the topic partitions
   */
  def fetchLatestOffsets(): Map[MessageQueue, Long] = {
    withRetries {
      val partitions = consumer.fetchSubscribeMessageQueues(topic)
      logDebug(s"Partitions assigned to consumer: $partitions. Seeking to the end.")

      val partitionOffsets = partitions.asScala.map(p => p -> consumer.maxOffset(p)).toMap
      logDebug(s"Got latest offsets for partition : $partitionOffsets")
      partitionOffsets
    }
  }

  /**
   * Fetch the earliest offsets for specific topic partitions.
   * The return result may not contain some partitions if they are deleted.
   */
  def fetchEarliestOffsets(
      newPartitions: Seq[MessageQueue]): Map[MessageQueue, Long] = {
    if (newPartitions.isEmpty) {
      Map.empty[MessageQueue, Long]
    } else {
      withRetries {
        val partitions = consumer.fetchSubscribeMessageQueues(topic)
        logDebug(s"\tPartitions assigned to consumer: $partitions")

        // Get the earliest offset of each partition
        val partitionOffsets = newPartitions.filter { p =>
          // When deleting topics happen at the same time, some partitions may not be in
          // `partitions`. So we need to ignore them
          partitions.contains(p)
        }.map(p => p -> consumer.minOffset(p)).toMap
        logDebug(s"Got earliest offsets for new partitions: $partitionOffsets")
        partitionOffsets
      }
    }
  }

  /**
   * Helper function that does multiple retries on a body of code that returns offsets.
   * Retries are needed to handle transient failures. For e.g. race conditions between getting
   * assignment and getting position while topics/partitions are deleted can cause NPEs.
   */
  private def withRetries(
      body: => Map[MessageQueue, Long]): Map[MessageQueue, Long] = synchronized {
    var result: Option[Map[MessageQueue, Long]] = None
    var attempt = 1
    var lastException: Throwable = null
    while (result.isEmpty && attempt <= maxOffsetFetchAttempts) {
      try {
        result = Some(body)
      } catch {
        case NonFatal(e) =>
          lastException = e
          logWarning(s"Error in attempt $attempt getting RocketMQ offsets: ", e)
          attempt += 1
          Thread.sleep(offsetFetchAttemptIntervalMs)
          resetConsumer()
      }
    }
    if (result.isEmpty) {
      assert(attempt > maxOffsetFetchAttempts)
      assert(lastException != null)
      throw lastException
    }
    result.get
  }

  /**
   * Create a consumer using the new generated group id. We always use a new consumer to avoid
   * just using a broken consumer to retry on RocketMQ errors, which likely will fail again.
   */
  private def createConsumer(): MQPullConsumer = synchronized {
    val newRocketMQParams = new ju.HashMap[String, String](driverRocketMQParams)
    val groupId = nextGroupId()
    RocketMQSqlUtils.makePullConsumer(groupId, newRocketMQParams)
  }

  private def resetConsumer(): Unit = synchronized {
    consumer.shutdown()
    consumer = createConsumer()
  }
}
