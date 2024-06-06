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

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}
import java.{lang => jl, util => ju}
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer
import org.apache.rocketmq.client.consumer.store.ReadOffsetType
import org.apache.rocketmq.common.MixAll
import org.apache.rocketmq.common.message.{MessageExt, MessageQueue}
import org.apache.rocketmq.spark.streaming.MQPullConsumerProvider
import org.apache.rocketmq.spark.{ConsumerStrategy, _}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.{DStream, DStreamCheckpointData, InputDStream}
import org.apache.spark.streaming.scheduler.rate.RateEstimator
import org.apache.spark.streaming.scheduler.{RateController, StreamInputInfo}
import org.apache.spark.util.ThreadUtils

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  *  A DStream where
  * each given RocketMq topic/queueId corresponds to an RDD partition.
  * The configuration pull.max.speed.per.partition gives the maximum number
  *  of messages per second that each '''partition''' will accept.
  *
  * @param groupId it is for rocketMq for identifying the consumer
  * @param topics the topics for the rocketmq
  * @param locationStrategy locationStrategy In most cases, pass in [[LocationStrategy.PreferConsistent]],
  *   see [[LocationStrategy]] for more details.
  * @param consumerStrategy consumerStrategy In most cases, pass in [[ConsumerStrategy.lastest]],
  *   see [[ConsumerStrategy]] for more details
  * @param autoCommit  whether commit the offset to the rocketmq server automatically or not
  * @param forceSpecial Generally if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer
  *  will consume from the checkpoint no matter we specify the offset or not. But if forceSpecial is true,
  *  the rocketmq will start consuming from the specific available offset in any case.
  * @param failOnDataLoss Zero data lost is not guaranteed when topics are deleted. If zero data lost is critical, 
  * the user must make sure all messages in a topic have been processed when deleting a topic.
  */
class MQPullInputDStream(
    _ssc: StreamingContext,
    groupId: String,
    topics: ju.Collection[jl.String],
    optionParams: ju.Map[String, String],
    locationStrategy: LocationStrategy,
    consumerStrategy: ConsumerStrategy,
    autoCommit: Boolean,
    forceSpecial: Boolean,
    failOnDataLoss: Boolean
  ) extends InputDStream[MessageExt](_ssc) with CanCommitOffsets {

  private var currentOffsets = mutable.Map[TopicQueueId, Map[String, Long]]()

  private val commitQueue = new ConcurrentLinkedQueue[OffsetRange]

  private val commitCallback = new AtomicReference[OffsetCommitCallback]

  private val maxRateLimitPerPartition = optionParams.getOrDefault(RocketMQConfig.MAX_PULL_SPEED_PER_PARTITION,
    "-1").toInt
  
  @transient private var kc: MQPullConsumerProvider = null

  /**
    * start up timer thread to persis the OffsetStore
    */
  @transient private val scheduledExecutorService = ThreadUtils.newDaemonSingleThreadScheduledExecutor(
    "Driver-Commit-Thread")

  private def consumer() = this.synchronized {
    if (null == kc) {
      kc = RocketMqUtils.mkPullConsumerInstance(groupId, optionParams, "driver")
      val messageQueues = fetchSubscribeMessageQueues(topics)
      val iter = messageQueues.iterator
      while (iter.hasNext){
        val messageQueue = iter.next
        val offset = computePullFromWhere(messageQueue)
        val topicQueueId = new TopicQueueId(messageQueue.getTopic, messageQueue.getQueueId)
        if (!currentOffsets.contains(topicQueueId)) {
          currentOffsets += topicQueueId -> Map(messageQueue.getBrokerName -> offset)
        } else {
          if (!currentOffsets(topicQueueId).contains(messageQueue.getBrokerName)){
            currentOffsets(topicQueueId) += messageQueue.getBrokerName -> offset
          }
        }
      }

      // TODO should not persist here if autoCommit is off
      // timer persist
      this.scheduledExecutorService.scheduleAtFixedRate(
        new Runnable() {
          def run() {
            try {
              kc.getOffsetStore.persistAll(fetchSubscribeMessageQueues(topics))
            } catch {
              case e: Exception => {
                log.error("ScheduledTask persistAllConsumerOffset exception", e)
              }
            }
          }
        }, 1000 * 10, 1000 * 5, TimeUnit.MILLISECONDS)
    }
    kc
  }

  private def fetchSubscribeMessageQueues(topics : ju.Collection[jl.String]): ju.HashSet[MessageQueue] = {
    val messageQueueSet = new ju.HashSet[MessageQueue]

    val iter = topics.iterator
    while (iter.hasNext){
      messageQueueSet.addAll(kc.fetchSubscribeMessageQueues(iter.next))
    }
    messageQueueSet
  }

  private def computePullFromWhere(mq: MessageQueue): Long = {
    var result = -1L
    val offsetStore = kc.getOffsetStore
    val minOffset = kc.minOffset(mq)
    val checkpointOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE)

    consumerStrategy match {
      case LatestStrategy => {
        if (checkpointOffset >= 0) {
          //consider the checkpoint offset first
          if (checkpointOffset < minOffset) {
            reportDataLoss(s"MessageQueue $mq's checkpointOffset $checkpointOffset is smaller than minOffset $minOffset")
            result = kc.maxOffset(mq)
          } else {
            result = checkpointOffset
          }
        } else {
          // First start,no offset
          if (mq.getTopic.startsWith(MixAll.RETRY_GROUP_TOPIC_PREFIX)) {
            result = 0
          } else {
            result = kc.maxOffset(mq)
          }
        }
      }
      case EarliestStrategy => {
        if (checkpointOffset >= 0) {
          //consider the checkpoint offset first
          if (checkpointOffset < minOffset) {
            reportDataLoss(s"MessageQueue $mq's checkpointOffset $checkpointOffset is smaller than minOffset $minOffset")
            result = minOffset
          } else {
            result = checkpointOffset
          }
        } else {
          // First start,no offset
          result = minOffset
        }
      }
      case SpecificOffsetStrategy(queueToOffset) => {

        val specificOffset = queueToOffset.get(mq)

        if (checkpointOffset >= 0 && !forceSpecial) {
          if (checkpointOffset < minOffset) {
            reportDataLoss(s"MessageQueue $mq's checkpointOffset $checkpointOffset is smaller than minOffset $minOffset")
            result = minOffset
          } else {
            result = checkpointOffset
          }
        } else {
          specificOffset match {
            case Some(ConsumerStrategy.LATEST) => {
              result = kc.maxOffset(mq)
            }
            case Some(ConsumerStrategy.EARLIEST) => {
              result = kc.minOffset(mq)
            }
            case Some(offset) => {
              if (offset < minOffset) {
                reportDataLoss(s"MessageQueue $mq's specific offset $offset is smaller than minOffset $minOffset")
                result = minOffset
              } else {
                result = offset
              }
            }
            case None => {
              if (checkpointOffset >= 0) {
                //consider the checkpoint offset first
                if (checkpointOffset < minOffset) {
                  reportDataLoss(s"MessageQueue $mq's checkpointOffset $checkpointOffset is smaller than minOffset $minOffset")
                  result = minOffset
                } else {
                  result = checkpointOffset
                }
              } else {
                logWarning(s"MessageQueue $mq's specific offset and checkpointOffset are none, then use the minOffset")
                result = kc.minOffset(mq)
              }
            }
          }
        }
      }
    }
    result
  }

  private def firstConsumerOffset(mq: MessageQueue): Long = {
    val offsetStore = kc.getOffsetStore
    val lastOffset = offsetStore.readOffset(mq, ReadOffsetType.READ_FROM_STORE)
    val minOffset = kc.minOffset(mq)
    if (lastOffset < minOffset) {
      reportDataLoss(s"MessageQueue $mq's checkpoint offset $lastOffset is smaller than minOffset $minOffset")
      minOffset
    } else {
      lastOffset
    }
  }


  override def persist(newLevel: StorageLevel): DStream[MessageExt] = {
    logError("rocketmq MessageExt is not serializable. " +
      "Use .map to extract fields before calling .persist or .window")
    super.persist(newLevel)
  }

  protected def getPreferredHosts: ju.Map[TopicQueueId, String] = {
    locationStrategy match {
      case PreferConsistent => ju.Collections.emptyMap[TopicQueueId, String]()
      case PreferFixed(hostMap) => hostMap
    }
  }

  // Keep this consistent with how other streams are named (e.g. "Flume polling stream [2]")
  private[streaming] override def name: String = s"RocketMq polling stream [$id]"

  protected[streaming] override val checkpointData =
    new MQInputDStreamCheckpointData


  /**
    * Asynchronously maintains & sends new rate limits to the receiver through the receiver tracker.
    */
  override protected[streaming] val rateController: Option[RateController] = {
    if (RateController.isBackPressureEnabled(_ssc.conf)) {
      Some(new DirectMQRateController(id,
        RateEstimator.create(_ssc.conf, context.graph.batchDuration)))
    } else {
      None
    }
  }

  /**
    * calculate the until-offset per partition in theory
    */
  private def maxMessagesPerPartition(
     offsets: Map[TopicQueueId, Map[String, Long]]): Option[Map[TopicQueueId, Map[String, Long]]] = {
    val estimatedRateLimit = rateController.map(_.getLatestRate().toInt)

    var lagPerPartition = Map[TopicQueueId, Long]()
    var totalLag = 0L
    val lagPerPartitionPerQueue = offsets.map{ case (tp, value) =>
      val partitionTotal = value.map{ case (name, maxOffset) =>
        var count = Math.max(maxOffset - currentOffsets(tp)(name), 0)
        totalLag += count
        (name, count)
      }
      lagPerPartition += tp -> partitionTotal.values.sum
      tp -> partitionTotal
    }

    val effectiveRateLimitPerPartition = estimatedRateLimit.filter(_ > 0) match {
      case Some(rate) =>
        lagPerPartitionPerQueue.map { case (tp, queues) =>
          val backPressRate = Math.round(lagPerPartition(tp) / totalLag.toFloat * rate)
          val partitionMessages = (if (maxRateLimitPerPartition > 0) {
            Math.min(backPressRate, maxRateLimitPerPartition)} else backPressRate)
          tp -> queues.map{ case (name, count) =>
            (name, Math.ceil(count / lagPerPartition(tp).toFloat * partitionMessages))
          }
        }
      case None =>

        lagPerPartitionPerQueue.map { case (tp, queues) =>
          val partitionMessages = maxRateLimitPerPartition
          tp -> queues.map{ case (name, count) =>
            (name, Math.ceil(count / lagPerPartition(tp).toFloat * partitionMessages))
          }
        }
    }

    if (effectiveRateLimitPerPartition.flatMap(_._2).map(_._2).sum > 0) {
      val secsPerBatch = context.graph.batchDuration.milliseconds.toDouble / 1000
      Some(effectiveRateLimitPerPartition.map {
        case (tp, limit) => tp -> limit.map{ case (name, count) =>
          name -> (count * secsPerBatch).toLong
        }
      })
    } else {
      None
    }
  }


  /**
    * Returns the latest (highest) available offsets, taking new partitions into account.
    */
  protected def latestOffsets(): Map[TopicQueueId, Map[String, Long]] = {
    val c = consumer

    val messageQueues = fetchSubscribeMessageQueues(topics)

    var maxOffsets = Map[TopicQueueId, Map[String, Long]]()

    val lastTopicQueues = currentOffsets.keySet
    val fetchTopicQueues = mutable.Set[TopicQueueId]()
    val iter = messageQueues.iterator
    while (iter.hasNext) {
      val messageQueue = iter.next
      logDebug(s"${messageQueue.toString} min: ${c.minOffset(messageQueue)}  max: ${c.maxOffset(messageQueue)}")
      val topicQueueId = new TopicQueueId(messageQueue.getTopic, messageQueue.getQueueId)
      fetchTopicQueues.add(topicQueueId)
      if (!currentOffsets.contains(topicQueueId)){
        currentOffsets += topicQueueId -> Map(messageQueue.getBrokerName -> firstConsumerOffset(messageQueue))
      }else{
        if (!currentOffsets(topicQueueId).contains(messageQueue.getBrokerName))
          currentOffsets(topicQueueId) += messageQueue.getBrokerName -> firstConsumerOffset(messageQueue)
      }
      if (!maxOffsets.contains(topicQueueId)) {
        maxOffsets += topicQueueId -> Map(messageQueue.getBrokerName -> c.maxOffset(messageQueue))
      }else{
        if (!maxOffsets(topicQueueId).contains(messageQueue.getBrokerName)) {
          val tempMap = maxOffsets(topicQueueId) + (messageQueue.getBrokerName -> c.maxOffset(messageQueue))
          maxOffsets += topicQueueId -> tempMap
        }
      }
    }

    val deletedPartitions = lastTopicQueues.diff(fetchTopicQueues)
    if (deletedPartitions.size > 0){
      reportDataLoss(
        s"Cannot find offsets of ${deletedPartitions}. Some data may have been missed")
    }
    maxOffsets
  }

  /**
    * limits the maximum number of messages per partition
    */
  protected def clamp(offsets: Map[TopicQueueId, Map[String, Long]]): Map[TopicQueueId, Map[String, Long]] = {
    maxMessagesPerPartition(offsets).map { mmp =>
      mmp.map { case (tp, partitionsOffsets) =>
        tp -> partitionsOffsets.map{case (name, messages) =>
          name -> Math.min(currentOffsets(tp)(name) + messages, offsets(tp)(name))}
      }
    }.getOrElse(offsets)
  }


  override def compute(validTime: Time): Option[RocketMqRDD] = {

    val untilOffsets = clamp(latestOffsets())

    val offsetRangeRdd: ju.Map[TopicQueueId, Array[OffsetRange]] = new ju.HashMap()

    untilOffsets.foreach { case (tp, uo) =>
      val values = uo.map { case (name, until) =>
        val fo = currentOffsets(tp)(name)
        OffsetRange(tp.topic, tp.queueId, name, fo, until)
      }.filter(item => {
        item.count() > 0
      }).toArray

      if (values != null && values.length > 0) {
        offsetRangeRdd.put(tp, values)
      }
    }

    val rdd = new RocketMqRDD(
      context.sparkContext, groupId, optionParams, offsetRangeRdd, getPreferredHosts, true)

    // Report the record number and metadata of this batch interval to InputInfoTracker.
    val description = offsetRangeRdd.asScala.flatMap { case (tp, arrayRange) =>
      // Don't display empty ranges.
      arrayRange
    }.filter { offsetRange =>
      offsetRange.fromOffset != offsetRange.untilOffset
    }.map { offsetRange =>
      s"topic: ${offsetRange.topic}\tqueueId: ${offsetRange.queueId}\t" +
        s"brokerName: ${offsetRange.brokerName}\t" +
        s"offsets: ${offsetRange.fromOffset} to ${offsetRange.untilOffset}"
    }.mkString("\n")
    // Copy offsetRanges to immutable.List to prevent from being modified by the user
    val metadata = Map(
      "offsets" -> offsetRangeRdd,
      StreamInputInfo.METADATA_KEY_DESCRIPTION -> description)
    val inputInfo = StreamInputInfo(id, rdd.count, metadata)
    ssc.scheduler.inputInfoTracker.reportInfo(validTime, inputInfo)

    currentOffsets = collection.mutable.Map() ++ untilOffsets

    if (autoCommit) {
      currentOffsets.foreach { case (tp, uo) =>
        uo.map { case (name, until) =>
          val offset = currentOffsets(tp)(name) - 1
          val mq = new MessageQueue(tp.topic, name, tp.queueId)
          kc.commitConsumeOffset(mq, offset)
        }
      }
    } else {
      commitAll()
    }
    Some(rdd)
  }

  private def reportDataLoss(message: String): Unit = {
    if (failOnDataLoss) {
      throw new IllegalStateException(message)
    } else {
      logWarning(message)
    }
  }

  /**
    * Queue up offset ranges for commit to rocketmq at a future time.  Threadsafe.
    *
    * @param offsetRanges The maximum untilOffset for a given partition will be used at commit.
    */
  def commitAsync(offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]]): Unit = {
    commitAsync(offsetRanges, null)
  }

  /**
    * Queue up offset ranges for commit to rocketmq at a future time.  Threadsafe.
    *
    * @param offsetRanges The maximum untilOffset for a given partition will be used at commit.
    * @param callback Only the most recently provided callback will be used at commit.
    */
  def commitAsync(offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]], callback: OffsetCommitCallback): Unit = {
    commitCallback.set(callback)
    offsetRanges.values.asScala.foreach{ value =>
      commitQueue.addAll(ju.Arrays.asList(value: _*))
    }
  }

  protected def commitAll(): Unit = {
    val m = new ju.HashMap[MessageQueue, jl.Long]
    var osr = commitQueue.poll()
    try {
      while (null != osr) {
        //Exclusive ending offset
        val mq = new MessageQueue(osr.topic, osr.brokerName, osr.queueId)
        kc.commitConsumeOffset(mq, osr.untilOffset - 1)
        m.put(mq, osr.untilOffset - 1)
        osr = commitQueue.poll()
      }
      if (commitCallback.get != null) {
        commitCallback.get.onComplete(m, null)
      }
    } catch {
      case e: Exception => {
        if (commitCallback.get != null)
          commitCallback.get.onComplete(m, e)
      }
    }
  }


  override def start(): Unit = {
     consumer
  }

  override def stop(): Unit = this.synchronized {
    if (kc != null) {
      kc.shutdown()
    }
  }

  private[streaming]
  class MQInputDStreamCheckpointData extends DStreamCheckpointData(this) {
    def batchForTime: mutable.HashMap[Time, mutable.HashMap[TopicQueueId, Array[(String, Int, String, Long, Long)]]] = {
      data.asInstanceOf[mutable.HashMap[Time, mutable.HashMap[TopicQueueId, Array[OffsetRange.OffsetRangeTuple]]]]
    }

    override def update(time: Time): Unit = {
      batchForTime.clear()
      generatedRDDs.foreach { kv =>
        val values = new mutable.HashMap[TopicQueueId, Array[OffsetRange.OffsetRangeTuple]]
        kv._2.asInstanceOf[RocketMqRDD].offsetRanges.asScala.foreach{ case (k, v) =>
          values.put(k, v.map(_.toTuple))
        }
        batchForTime += kv._1 ->values
      }
    }

    override def cleanup(time: Time): Unit = { }

    override def restore(): Unit = {
      batchForTime.toSeq.sortBy(_._1)(Time.ordering).foreach { case (t, b) =>
        logInfo(s"Restoring RocketMqRDD for time $t $b")

        val offsetRanges = new ju.HashMap[TopicQueueId, Array[OffsetRange]]()
        b.foreach{ case (i, j) =>
          offsetRanges.put(i, j.map(OffsetRange(_)))
        }

        generatedRDDs += t -> new RocketMqRDD(
          context.sparkContext,
          groupId,
          optionParams,
          offsetRanges,
          getPreferredHosts,
          // during restore, it's possible same partition will be consumed from multiple
          // threads, so dont use cache
          false
        )
      }
    }
  }

  /**
    * A RateController to retrieve the rate from RateEstimator.
    */
  private class DirectMQRateController(id: Int, estimator: RateEstimator)
    extends RateController(id, estimator) {
    override def publish(rate: Long): Unit = ()
  }

}
