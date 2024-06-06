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

package org.apache.spark.streaming

import org.apache.rocketmq.common.message.MessageExt
import org.apache.rocketmq.spark._
import org.apache.spark.partial.{BoundedDouble, PartialResult}
import org.apache.spark.rdd.RDD
import org.apache.spark.scheduler.ExecutorCacheTaskLocation
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{Partition, SparkContext, TaskContext}
import java.{util => ju}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


/**
  * A batch-oriented interface for consuming from RocketMq.
  * Starting and ending offsets are specified in advance,
  * so that you can control exactly-once semantics.
  * @param groupId it is for rocketMq for identifying the consumer
  * @param optionParams the configs
  * @param offsetRanges offset ranges that define the RocketMq data belonging to this RDD
  * @param preferredHosts map from TopicQueueId to preferred host for processing that partition.
  * In most cases, use [[LocationStrategy.PreferConsistent]]
  * @param useConsumerCache useConsumerCache whether to use a consumer from a per-jvm cache
  */
class RocketMqRDD (
      sc: SparkContext,
      val groupId: String,
      val optionParams: ju.Map[String, String],
      val offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]],
      val preferredHosts: ju.Map[TopicQueueId, String],
      val useConsumerCache: Boolean
    ) extends RDD[MessageExt](sc, Nil) with HasOffsetRanges {

  private val cacheInitialCapacity =
    optionParams.getOrDefault(RocketMQConfig.PULL_CONSUMER_CACHE_INIT_CAPACITY, "16").toInt
  private val cacheMaxCapacity =
    optionParams.getOrDefault(RocketMQConfig.PULL_CONSUMER_CACHE_MAX_CAPACITY, "64").toInt
  private val cacheLoadFactor =
    optionParams.getOrDefault(RocketMQConfig.PULL_CONSUMER_CACHE_LOAD_FACTOR, "0.75").toFloat

  override def persist(newLevel: StorageLevel): this.type = {
    super.persist(newLevel)
  }

  override def getPartitions: Array[Partition] = {
    offsetRanges.asScala.toArray.zipWithIndex.map{ case ((first, second), i) =>
      new RocketMqRDDPartition(i, first.topic, first.queueId, second)
    }.toArray
  }

  override def count(): Long = offsetRanges.asScala.map(_._2.map(_.count).sum).sum

  override def countApprox(
                            timeout: Long,
                            confidence: Double = 0.95
                          ): PartialResult[BoundedDouble] = {
    val c = count
    new PartialResult(new BoundedDouble(c, 1.0, c, c), true)
  }

  override def isEmpty(): Boolean = count == 0L

  override def take(num: Int): Array[MessageExt] = {
    val nonEmptyPartitions = this.partitions
      .map(_.asInstanceOf[RocketMqRDDPartition])
      .filter(_.count > 0)

    if (num < 1 || nonEmptyPartitions.isEmpty) {
      return new Array[MessageExt](0)
    }

    // Determine in advance how many messages need to be taken from each partition
    val parts = nonEmptyPartitions.foldLeft(Map[Int, Int]()) { (result, part) =>
      val remain = num - result.values.sum
      if (remain > 0) {
        val taken = Math.min(remain, part.count)
        result + (part.index -> taken.toInt)
      } else {
        result
      }
    }

    val buf = new ArrayBuffer[MessageExt]
    val res = context.runJob(
      this,
      (tc: TaskContext, it: Iterator[MessageExt]) =>
        it.take(parts(tc.partitionId)).toArray, parts.keys.toArray
    )
    res.foreach(buf ++= _)
    buf.toArray
  }

  private def executors(): Array[ExecutorCacheTaskLocation] = {
    val bm = sparkContext.env.blockManager
    bm.master.getPeers(bm.blockManagerId).toArray
      .map(x => ExecutorCacheTaskLocation(x.host, x.executorId))
      .sortWith(compareExecutors)
  }

  private def compareExecutors(
     a: ExecutorCacheTaskLocation,
     b: ExecutorCacheTaskLocation): Boolean =
    if (a.host == b.host) {
      a.executorId > b.executorId
    } else {
      a.host > b.host
    }

  /**
    * Non-negative modulus, from java 8 math
    */
  private def floorMod(a: Int, b: Int): Int = ((a % b) + b) % b

  protected override def getPreferredLocations(thePart: Partition): Seq[String] = {
    // The intention is best-effort consistent executor for a given topic partition,
    // so that caching consumers can be effective.
    val part = thePart.asInstanceOf[RocketMqRDDPartition]
    val allExecs = executors()
    val tp = part.topicQueueId()
    val prefHost = preferredHosts.get(tp)
    val prefExecs = if (null == prefHost) allExecs else allExecs.filter(_.host == prefHost)
    val execs = if (prefExecs.isEmpty) allExecs else prefExecs
    if (execs.isEmpty) {
      Seq()
    } else {
      // execs is sorted, tp.hashCode depends only on topic and partition, so consistent index
      val index = this.floorMod(tp.hashCode, execs.length)
      val chosen = execs(index)
      Seq(chosen.toString)
    }
  }

  private def errBeginAfterEnd(part: RocketMqRDDPartition): String =
    s"Beginning offset is after the ending offset ${part.partitionOffsetRanges.mkString(",")} " +
      s"for topic ${part.topic} partition ${part.index}. " +
      "You either provided an invalid fromOffset, or the Kafka topic has been damaged"

  override def compute(thePart: Partition, context: TaskContext): Iterator[MessageExt] = {
    val part = thePart.asInstanceOf[RocketMqRDDPartition]
    val count = part.count()
    assert(count >= 0, errBeginAfterEnd(part))
    if (count == 0) {
      logInfo(s"Beginning offset is the same as ending offset " +
        s"skipping ${part.topic} ${part.queueId}")
      Iterator.empty
    } else {
      new RocketMqRDDIterator(part, context)
    }
  }


  /**
    * An iterator that fetches messages directly from rocketmq for the offsets in partition.
    * Uses a cached consumer where possible to take advantage of prefetching
    */
  private class RocketMqRDDIterator(
    part: RocketMqRDDPartition,
    context: TaskContext) extends Iterator[MessageExt] {

    logDebug(s"Computing topic ${part.topic}, queueId ${part.queueId} " +
      s"offsets ${part.partitionOffsetRanges.mkString(",")}")

    context.addTaskCompletionListener{ context => closeIfNeeded() }


    val consumer = if (useConsumerCache) {
      CachedMQConsumer.init(cacheInitialCapacity, cacheMaxCapacity, cacheLoadFactor)
      if (context.attemptNumber > 5) {
        // just in case the prior attempt failures were cache related
        CachedMQConsumer.remove(groupId, part.topic, part.queueId, part.brokerNames)
      }
      CachedMQConsumer.getOrCreate(groupId, part.topic, part.queueId, part.brokerNames, optionParams)
    } else {
      CachedMQConsumer.getUncached(groupId, part.topic, part.queueId, part.brokerNames, optionParams)
    }

    var logicTotalOffset = 0
    val totalSum = part.partitionOffsetRanges.map(_.count).sum
    var index = 0
    var requestOffset = part.partitionOffsetRanges.apply(index).fromOffset

    def closeIfNeeded(): Unit = {
      if (!useConsumerCache && consumer != null) {
        consumer.client.shutdown
      }
    }

    override def hasNext(): Boolean = {
      totalSum > logicTotalOffset
    }

    override def next(): MessageExt = {
      assert(hasNext(), "Can't call getNext() once untilOffset has been reached")
      val queueRange = part.partitionOffsetRanges.apply(index)
      val r = consumer.get(queueRange.brokerName, requestOffset)
      if (queueRange.untilOffset > (requestOffset + 1))
        requestOffset +=1
      else {
        index +=1
        if (part.partitionOffsetRanges.length > index)
          requestOffset = part.partitionOffsetRanges.apply(index).fromOffset
      }
      logicTotalOffset += 1
      r
    }
  }

  private[RocketMqRDD]
  type OffsetRangeTuple = (String, Int)


}
