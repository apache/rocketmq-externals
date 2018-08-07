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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaRelation.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 * 2. Schema of output dataframe adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.util.UUID

import org.apache.rocketmq.common.message.MessageQueue
import org.apache.spark.internal.Logging
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.util.DateTimeUtils
import org.apache.spark.sql.sources.{BaseRelation, TableScan}
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.unsafe.types.UTF8String


private[rocketmq] class RocketMQRelation(
    override val sqlContext: SQLContext,
    sourceOptions: Map[String, String],
    optionParams: Map[String, String],
    failOnDataLoss: Boolean,
    startingOffsets: RocketMQOffsetRangeLimit,
    endingOffsets: RocketMQOffsetRangeLimit)
    extends BaseRelation with TableScan with Logging {
  assert(startingOffsets != LatestOffsetRangeLimit,
    "Starting offset not allowed to be set to latest offsets.")
  assert(endingOffsets != EarliestOffsetRangeLimit,
    "Ending offset not allowed to be set to earliest offsets.")

  private val pollTimeoutMs = sourceOptions.getOrElse(
    RocketMQConf.PULL_TIMEOUT_MS,
    sqlContext.sparkContext.conf.getTimeAsMs("spark.network.timeout", "120s").toString
  ).toLong

  override def schema: StructType = RocketMQSource.schema

  override def buildScan(): RDD[Row] = {
    // Each running query should use its own group id. Otherwise, the query may be only assigned
    // partial data since RocketMQ will assign partitions to multiple consumers having the same group
    // id. Hence, we should generate a unique id for each query.
    val uniqueGroupId = s"spark-rocketmq-relation-${UUID.randomUUID}"

    val offsetReader = new RocketMQOffsetReader(
      RocketMQSourceProvider.paramsForDriver(optionParams),
      sourceOptions,
      driverGroupIdPrefix = s"$uniqueGroupId-driver")

    // Leverage the RocketMQReader to obtain the relevant partition offsets
    val (fromPartitionOffsets, untilPartitionOffsets) = {
      try {
        (getPartitionOffsets(offsetReader, startingOffsets),
          getPartitionOffsets(offsetReader, endingOffsets))
      } finally {
        offsetReader.close()
      }
    }

    // Obtain topicPartitions in both from and until partition offset, ignoring
    // topic partitions that were added and/or deleted between the two above calls.
    if (fromPartitionOffsets.keySet != untilPartitionOffsets.keySet) {
      implicit val topicOrdering: Ordering[MessageQueue] = Ordering.by(t => t.getTopic)
      val fromTopics = fromPartitionOffsets.keySet.toList.sorted.mkString(",")
      val untilTopics = untilPartitionOffsets.keySet.toList.sorted.mkString(",")
      throw new IllegalStateException("different topic partitions " +
        s"for starting offsets topics[$fromTopics] and " +
        s"ending offsets topics[$untilTopics]")
    }

    // Calculate offset ranges
    val offsetRanges = untilPartitionOffsets.keySet.map { tp =>
      val fromOffset = fromPartitionOffsets.getOrElse(tp, {
          // This should not happen since messageQueues contains all partitions not in
          // fromPartitionOffsets
          throw new IllegalStateException(s"$tp doesn't have a from offset")
      })
      val untilOffset = untilPartitionOffsets(tp)
      RocketMQSourceRDDOffsetRange(tp, fromOffset, untilOffset, None)
    }.toArray

    logInfo("GetBatch generating RDD of offset range: " +
      offsetRanges.sortBy(_.messageQueue.toString).mkString(", "))

    // Create an RDD that reads from RocketMQ and get the (key, value) pair as byte arrays.
    val executorRocketMQParams =
      RocketMQSourceProvider.paramsForExecutors(optionParams, uniqueGroupId)
    val rdd = new RocketMQSourceRDD(
      sqlContext.sparkContext, executorRocketMQParams, offsetRanges,
      pollTimeoutMs, failOnDataLoss, reuseRocketMQConsumer = false).map { cr =>
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
    sqlContext.internalCreateDataFrame(rdd, schema).rdd
  }

  private def getPartitionOffsets(
      offsetReader: RocketMQOffsetReader,
      offsetRangeLimit: RocketMQOffsetRangeLimit): Map[MessageQueue, Long] = {
    def validateTopicPartitions(partitions: Set[MessageQueue],
      partitionOffsets: Map[MessageQueue, Long]): Map[MessageQueue, Long] = {
      assert(partitions == partitionOffsets.keySet,
        "If startingOffsets contains specific offsets, you must specify all TopicPartitions.\n" +
          "Use -1 for latest, -2 for earliest, if you don't care.\n" +
          s"Specified: ${partitionOffsets.keySet} Assigned: $partitions")
      logDebug(s"Partitions assigned to consumer: $partitions. Seeking to $partitionOffsets")
      partitionOffsets
    }
    val partitions = offsetReader.fetchTopicPartitions()
    // Obtain MessageQueue offsets with late binding support
    offsetRangeLimit match {
      case EarliestOffsetRangeLimit => partitions.map {
        case tp => tp -> RocketMQOffsetRangeLimit.EARLIEST
      }.toMap
      case LatestOffsetRangeLimit => partitions.map {
        case tp => tp -> RocketMQOffsetRangeLimit.LATEST
      }.toMap
      case SpecificOffsetRangeLimit(partitionOffsets) =>
        validateTopicPartitions(partitions, partitionOffsets)
    }
  }

  override def toString: String =
    s"RocketMQRelation(start=$startingOffsets, end=$endingOffsets)"
}
