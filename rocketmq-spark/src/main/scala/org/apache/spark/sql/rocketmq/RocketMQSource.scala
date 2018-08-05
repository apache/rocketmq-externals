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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaSource.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 * 2. Schema of output dataframe adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.io._
import java.nio.charset.StandardCharsets
import java.{util => ju}

import org.apache.commons.io.IOUtils
import org.apache.rocketmq.common.message.MessageQueue
import org.apache.spark.SparkContext
import org.apache.spark.internal.Logging
import org.apache.spark.scheduler.ExecutorCacheTaskLocation
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.execution.streaming._
import org.apache.spark.sql.rocketmq.RocketMQSource._
import org.apache.spark.sql.types.{StructField, _}
import org.apache.spark.unsafe.types.UTF8String

/**
 * A [[Source]] that reads data from RocketMQ using the following design.
 *
 * - The [[RocketMQSourceOffset]] is the custom [[Offset]] defined for this source that contains
 *   a map of MessageQueue -> offset. Note that this offset is 1 + (available offset). For
 *   example if the last record in a RocketMQ topic "t", partition 2 is offset 5, then
 *   RocketMQSourceOffset will contain MessageQueue("t", 2) -> 6. This is done keep it consistent
 *   with the semantics of `MQPullConsumer.fetchConsumeOffset()`.
 *
 * - The [[RocketMQSource]] written to do the following.
 *
 *  - As soon as the source is created, the pre-configured [[RocketMQOffsetReader]]
 *    is used to query the initial offsets that this source should
 *    start reading from. This is used to create the first batch.
 *
 *   - `getOffset()` uses the [[RocketMQOffsetReader]] to query the latest
 *      available offsets, which are returned as a [[RocketMQSourceOffset]].
 *
 *   - `getBatch()` returns a DF that reads from the 'start offset' until the 'end offset' in
 *     for each partition. The end offset is excluded to be consistent with the semantics of
 *     [[RocketMQSourceOffset]] and `MQPullConsumer.fetchConsumeOffset()`.
 *
 *   - The DF returned is based on [[RocketMQSourceRDD]] which is constructed such that the
 *     data from RocketMQ topic + partition is consistently read by the same executors across
 *     batches, and cached RocketMQConsumers in the executors can be reused efficiently. See the
 *     docs on [[RocketMQSourceRDD]] for more details.
 *
 * Zero data lost is not guaranteed when topics are deleted. If zero data lost is critical, the user
 * must make sure all messages in a topic have been processed when deleting a topic.
 */
private class RocketMQSource(
    sqlContext: SQLContext,
    offsetReader: RocketMQOffsetReader,
    executorRocketMQParams: ju.Map[String, String],
    sourceOptions: Map[String, String],
    metadataPath: String,
    startingOffsets: RocketMQOffsetRangeLimit,
    failOnDataLoss: Boolean)
  extends Source with Logging {

  private val sc = sqlContext.sparkContext

  private val pollTimeoutMs = sourceOptions.getOrElse(
    RocketMQConf.PULL_TIMEOUT_MS,
    sc.conf.getTimeAsMs("spark.network.timeout", "120s").toString
  ).toLong

  private val maxOffsetsPerTrigger =
    sourceOptions.get("maxOffsetsPerTrigger").map(_.toLong)

  /**
   * Lazily initialize `initialPartitionOffsets` to make sure that `RocketMQConsumer.pull` is only
   * called in StreamExecutionThread.
   */
  private lazy val initialPartitionOffsets = {
    val metadataLog =
      new HDFSMetadataLog[RocketMQSourceOffset](sqlContext.sparkSession, metadataPath) {
        override def serialize(metadata: RocketMQSourceOffset, out: OutputStream): Unit = {
          out.write(0) // A zero byte is written to support Spark 2.1.0 (SPARK-19517)
          val writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))
          writer.write("v" + VERSION + "\n")
          writer.write(metadata.json)
          writer.flush()
        }

        override def deserialize(in: InputStream): RocketMQSourceOffset = {
          in.read() // A zero byte is read to support Spark 2.1.0 (SPARK-19517)
          val content = IOUtils.toString(new InputStreamReader(in, StandardCharsets.UTF_8))
          // HDFSMetadataLog guarantees that it never creates a partial file.
          assert(content.length != 0)
          if (content(0) == 'v') {
            val indexOfNewLine = content.indexOf("\n")
            if (indexOfNewLine > 0) {
              val version = parseVersion(content.substring(0, indexOfNewLine), VERSION)
              RocketMQSourceOffset(SerializedOffset(content.substring(indexOfNewLine + 1)))
            } else {
              throw new IllegalStateException(
                s"Log file was malformed: failed to detect the log file version line.")
            }
          } else {
            // The log was generated by Spark 2.1.0
            RocketMQSourceOffset(SerializedOffset(content))
          }
        }
      }

    metadataLog.get(0).getOrElse {
      val offsets = startingOffsets match {
        case EarliestOffsetRangeLimit => RocketMQSourceOffset(offsetReader.fetchEarliestOffsets())
        case LatestOffsetRangeLimit => RocketMQSourceOffset(offsetReader.fetchLatestOffsets())
        case SpecificOffsetRangeLimit(p) => offsetReader.fetchSpecificOffsets(p, reportDataLoss)
      }
      metadataLog.add(0, offsets)
      logInfo(s"Initial offsets: $offsets")
      offsets
    }.queueToOffsets
  }

  private var currentPartitionOffsets: Option[Map[MessageQueue, Long]] = None

  override def schema: StructType = RocketMQSource.schema

  /** Returns the maximum available offset for this source. */
  override def getOffset: Option[Offset] = {
    // Make sure initialPartitionOffsets is initialized
    initialPartitionOffsets

    val latest = offsetReader.fetchLatestOffsets()
    val offsets = maxOffsetsPerTrigger match {
      case None =>
        latest
      case Some(limit) if currentPartitionOffsets.isEmpty =>
        rateLimit(limit, initialPartitionOffsets, latest)
      case Some(limit) =>
        rateLimit(limit, currentPartitionOffsets.get, latest)
    }

    currentPartitionOffsets = Some(offsets)
    logDebug(s"GetOffset: ${offsets.toSeq.map(_.toString).sorted}")
    Some(RocketMQSourceOffset(offsets))
  }

  /** Proportionally distribute limit number of offsets among message queues */
  private def rateLimit(
      limit: Long,
      from: Map[MessageQueue, Long],
      until: Map[MessageQueue, Long]): Map[MessageQueue, Long] = {
    val fromNew = offsetReader.fetchEarliestOffsets(until.keySet.diff(from.keySet).toSeq)
    val sizes = until.flatMap {
      case (tp, end) =>
        // If begin isn't defined, something's wrong, but let alert logic in getBatch handle it
        from.get(tp).orElse(fromNew.get(tp)).flatMap { begin =>
          val size = end - begin
          logDebug(s"rateLimit $tp size is $size")
          if (size > 0) Some(tp -> size) else None
        }
    }
    val total = sizes.values.sum.toDouble
    if (total < 1) {
      until
    } else {
      until.map {
        case (tp, end) =>
          tp -> sizes.get(tp).map { size =>
            val begin = from.get(tp).getOrElse(fromNew(tp))
            val prorate = limit * (size / total)
            logDebug(s"rateLimit $tp prorated amount is $prorate")
            // Don't completely starve small topicpartitions
            val off = begin + (if (prorate < 1) Math.ceil(prorate) else Math.floor(prorate)).toLong
            logDebug(s"rateLimit $tp new offset is $off")
            // Paranoia, make sure not to return an offset that's past end
            Math.min(end, off)
          }.getOrElse(end)
      }
    }
  }

  /**
   * Returns the data that is between the offsets
   * [`start.get.partitionToOffsets`, `end.partitionToOffsets`), i.e. end.partitionToOffsets is
   * exclusive.
   */
  override def getBatch(start: Option[Offset], end: Offset): DataFrame = {
    // Make sure initialPartitionOffsets is initialized
    initialPartitionOffsets

    logInfo(s"GetBatch called with start = $start, end = $end")
    val untilPartitionOffsets = RocketMQSourceOffset.getPartitionOffsets(end)
    // On recovery, getBatch will get called before getOffset
    if (currentPartitionOffsets.isEmpty) {
      currentPartitionOffsets = Some(untilPartitionOffsets)
    }
    if (start.isDefined && start.get == end) {
      return sqlContext.internalCreateDataFrame(
        sqlContext.sparkContext.emptyRDD, schema, isStreaming = true)
    }
    val fromPartitionOffsets = start match {
      case Some(prevBatchEndOffset) =>
        RocketMQSourceOffset.getPartitionOffsets(prevBatchEndOffset)
      case None =>
        initialPartitionOffsets
    }

    // Find the new partitions, and get their earliest offsets
    val newPartitions = untilPartitionOffsets.keySet.diff(fromPartitionOffsets.keySet)
    val newPartitionOffsets = offsetReader.fetchEarliestOffsets(newPartitions.toSeq)
    if (newPartitionOffsets.keySet != newPartitions) {
      // We cannot get from offsets for some partitions. It means they got deleted.
      val deletedPartitions = newPartitions.diff(newPartitionOffsets.keySet)
      reportDataLoss(
        s"Cannot find earliest offsets of $deletedPartitions. Some data may have been missed")
    }
    logInfo(s"Partitions added: $newPartitionOffsets")
    newPartitionOffsets.filter(_._2 != 0).foreach { case (p, o) =>
      reportDataLoss(
        s"Added partition $p starts from $o instead of 0. Some data may have been missed")
    }

    val deletedPartitions = fromPartitionOffsets.keySet.diff(untilPartitionOffsets.keySet)
    if (deletedPartitions.nonEmpty) {
      reportDataLoss(s"$deletedPartitions are gone. Some data may have been missed")
    }

    // Use the until partitions to calculate offset ranges to ignore partitions that have
    // been deleted
    val topicPartitions = untilPartitionOffsets.keySet.filter { tp =>
      // Ignore partitions that we don't know the from offsets.
      newPartitionOffsets.contains(tp) || fromPartitionOffsets.contains(tp)
    }.toSeq
    logDebug("TopicPartitions: " + topicPartitions.mkString(", "))

    val sortedExecutors = getSortedExecutorList(sc)
    val numExecutors = sortedExecutors.length
    logDebug("Sorted executors: " + sortedExecutors.mkString(", "))

    // Calculate offset ranges
    val offsetRanges = topicPartitions.map { tp =>
      val fromOffset = fromPartitionOffsets.getOrElse(tp, {
        newPartitionOffsets.getOrElse(tp, {
          // This should not happen since newPartitionOffsets contains all partitions not in
          // fromPartitionOffsets
          throw new IllegalStateException(s"$tp doesn't have a from offset")
        })
      })
      val untilOffset = untilPartitionOffsets(tp)
      val preferredLoc = if (numExecutors > 0) {
        // This allows cached RocketMQConsumers in the executors to be re-used to read the same
        // partition in every batch.
        Some(sortedExecutors(Math.floorMod(tp.hashCode, numExecutors)))
      } else None
      RocketMQSourceRDDOffsetRange(tp, fromOffset, untilOffset, preferredLoc)
    }.filter { range =>
      if (range.untilOffset < range.fromOffset) {
        reportDataLoss(s"Partition ${range.messageQueue}'s offset was changed from " +
          s"${range.fromOffset} to ${range.untilOffset}, some data may have been missed")
        false
      } else {
        true
      }
    }.toArray

    // Create an RDD that reads from RocketMQ and get the (key, value) pair as byte arrays.
    val rdd = new RocketMQSourceRDD(
      sc, executorRocketMQParams, offsetRanges, pollTimeoutMs, failOnDataLoss,
      reuseRocketMQConsumer = true).map { cr =>
      // Remove the `brokerName` property which was added by us. See `RocketMQSourceRDD.compute`
      val brokerName = cr.getProperties.remove(RocketMQSource.PROP_BROKER_NAME)
      InternalRow(
        UTF8String.fromString(cr.getTopic), // topic
        cr.getFlag, // flag
        cr.getBody, // body
        UTF8String.fromString(JsonUtils.messageProperties(cr.getProperties)), // properties
        UTF8String.fromString(brokerName), // brokerName
        cr.getQueueId, // queueId
        cr.getQueueOffset, // queueOffset
        DateTimeUtils.fromJavaTimestamp(new java.sql.Timestamp(cr.getBornTimestamp)), // bornTimestamp
        DateTimeUtils.fromJavaTimestamp(new java.sql.Timestamp(cr.getStoreTimestamp)) // storeTimestamp
      )
    }

    logInfo("GetBatch generating RDD of offset range: " +
      offsetRanges.sortBy(_.messageQueue.toString).mkString(", "))

    sqlContext.internalCreateDataFrame(rdd, schema, isStreaming = true)
  }

  /** Stop this source and free any resources it has allocated. */
  override def stop(): Unit = synchronized {
    offsetReader.close()
  }

  override def toString: String = s"RocketMQSource[$offsetReader]"

  /**
   * If `failOnDataLoss` is true, this method will throw an `IllegalStateException`.
   * Otherwise, just log a warning.
   */
  private def reportDataLoss(message: String): Unit = {
    if (failOnDataLoss) {
      throw new IllegalStateException(message + s". $INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_TRUE")
    } else {
      logWarning(message + s". $INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_FALSE")
    }
  }
}

/** Companion object for the [[RocketMQSource]]. */
private object RocketMQSource {
  val INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_FALSE =
    """
      |Some data may have been lost because they are not available in RocketMQ any more; either the
      | data was aged out by RocketMQ or the topic may have been deleted before all the data in the
      | topic was processed. If you want your streaming query to fail on such cases, set the source
      | option "failOnDataLoss" to "true".
    """.stripMargin

  val INSTRUCTION_FOR_FAIL_ON_DATA_LOSS_TRUE =
    """
      |Some data may have been lost because they are not available in RocketMQ any more; either the
      | data was aged out by RocketMQ or the topic may have been deleted before all the data in the
      | topic was processed. If you don't want your streaming query to fail on such cases, set the
      | source option "failOnDataLoss" to "false".
    """.stripMargin

  val VERSION = 1

  val PROP_BROKER_NAME = "_brokerName"

  def getSortedExecutorList(sc: SparkContext): Array[String] = {
    val bm = sc.env.blockManager
    bm.master.getPeers(bm.blockManagerId).toArray
      .map(x => ExecutorCacheTaskLocation(x.host, x.executorId))
      .sortWith(compare)
      .map(_.toString)
  }

  private def compare(a: ExecutorCacheTaskLocation, b: ExecutorCacheTaskLocation): Boolean = {
    if (a.host == b.host) { a.executorId > b.executorId } else { a.host > b.host }
  }

  def schema: StructType = StructType(Seq(
    // fields of `Message`
    StructField("topic", StringType),
    StructField("flag", IntegerType),
    StructField("body", BinaryType),
    StructField("properties", StringType),
    // fields of `MessageExt`
    StructField("brokerName", StringType),
    StructField("queueId", IntegerType),
    StructField("queueOffset", LongType),
    StructField("bornTimestamp", TimestampType),
    StructField("storeTimestamp", TimestampType)
  ))
}

