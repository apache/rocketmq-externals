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

import java.util.Properties
import java.{lang => jl, util => ju}
import org.apache.commons.lang.StringUtils
import org.apache.rocketmq.common.message.{Message, MessageExt, MessageQueue}
import org.apache.spark.SparkContext
import org.apache.spark.api.java.{JavaRDD, JavaSparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.api.java.{JavaInputDStream, JavaStreamingContext}
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.{MQPullInputDStream, RocketMqRDD, StreamingContext}
import org.apache.rocketmq.spark.RocketMQConfig._
import org.apache.rocketmq.spark.streaming.{MQPullConsumerProvider, ReliableRocketMQReceiver, RocketMQReceiver, SimpleMQPullConsumerProvider}
import org.apache.spark.storage.StorageLevel

object RocketMqUtils extends Logging {

  /**
    *  Scala constructor for a batch-oriented interface for consuming from rocketmq.
    * Starting and ending offsets are specified in advance,
    * so that you can control exactly-once semantics.
    * @param sc SparkContext
    * @param groupId it is for rocketMq for identifying the consumer
    * @param offsetRanges offset ranges that define the RocketMq data belonging to this RDD
    * @param optionParams optional configs, see [[RocketMQConfig]] for more details.
    * @param locationStrategy map from TopicQueueId to preferred host for processing that partition.
    * In most cases, use [[LocationStrategy.PreferConsistent]]
    * @return RDD[MessageExt]
    */
  def createRDD(
       sc: SparkContext,
       groupId: String,
       offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]],
       optionParams: ju.Map[String, String] = new ju.HashMap,
       locationStrategy: LocationStrategy = PreferConsistent
     ): RDD[MessageExt] = {

    val preferredHosts = locationStrategy match {
      case PreferConsistent => ju.Collections.emptyMap[TopicQueueId, String]()
      case PreferFixed(hostMap) => hostMap
    }
    new RocketMqRDD(sc, groupId, optionParams, offsetRanges, preferredHosts, false)
  }

  /**
    *  Java constructor for a batch-oriented interface for consuming from rocketmq.
    * Starting and ending offsets are specified in advance,
    * so that you can control exactly-once semantics.
    * @param jsc SparkContext
    * @param groupId it is for rocketMq for identifying the consumer
    * @param offsetRanges offset ranges that define the RocketMq data belonging to this RDD
    * @param optionParams optional configs, see [[RocketMQConfig]] for more details.
    * @param locationStrategy map from TopicQueueId to preferred host for processing that partition.
    * In most cases, use [[LocationStrategy.PreferConsistent]]
    * @return JavaRDD[MessageExt]
    */
  def createJavaRDD(
       jsc: JavaSparkContext,
       groupId: String,
       offsetRanges: ju.Map[TopicQueueId, Array[OffsetRange]],
       optionParams: ju.Map[String, String] = new ju.HashMap,
       locationStrategy: LocationStrategy = PreferConsistent
     ): JavaRDD[MessageExt] = {
    new JavaRDD(createRDD(jsc.sc, groupId, offsetRanges, optionParams, locationStrategy))
  }

  /**
    * Scala constructor for a RocketMq DStream
    * @param groupId it is for rocketMq for identifying the consumer
    * @param topics the topics for the rocketmq
    * @param consumerStrategy consumerStrategy In most cases, pass in [[ConsumerStrategy.lastest]],
    *   see [[ConsumerStrategy]] for more details
    * @param autoCommit whether commit the offset to the rocketmq server automatically or not. If the user
    *                   implement the [[OffsetCommitCallback]], the autoCommit must be set false
    * @param forceSpecial Generally if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer
    *  will consume from the checkpoint no matter we specify the offset or not. But if forceSpecial is true,
    *  the rocketmq will start consuming from the specific available offset in any case.
    * @param failOnDataLoss Zero data lost is not guaranteed when topics are deleted. If zero data lost is critical,
    * the user must make sure all messages in a topic have been processed when deleting a topic.
    * @param locationStrategy map from TopicQueueId to preferred host for processing that partition.
    * In most cases, use [[LocationStrategy.PreferConsistent]]
    * @param optionParams optional configs, see [[RocketMQConfig]] for more details.
    * @return InputDStream[MessageExt]
    */
  def createMQPullStream(
       ssc: StreamingContext,
       groupId: String,
       topics: ju.Collection[jl.String],
       consumerStrategy: ConsumerStrategy,
       autoCommit: Boolean,
       forceSpecial: Boolean,
       failOnDataLoss: Boolean,
       locationStrategy: LocationStrategy = PreferConsistent,
       optionParams: ju.Map[String, String] = new ju.HashMap
     ): InputDStream[MessageExt] = {

    new MQPullInputDStream(ssc, groupId, topics, optionParams, locationStrategy, consumerStrategy, autoCommit, forceSpecial,
      failOnDataLoss)
  }

  def createMQPullStream(
        ssc: StreamingContext,
        groupId: String,
        topic: String,
        consumerStrategy: ConsumerStrategy,
        autoCommit: Boolean,
        forceSpecial: Boolean,
        failOnDataLoss: Boolean,
        optionParams: ju.Map[String, String]
      ): InputDStream[MessageExt] = {
    val topics = new ju.ArrayList[String]()
    topics.add(topic)
    new MQPullInputDStream(ssc, groupId, topics, optionParams, PreferConsistent, consumerStrategy, autoCommit, forceSpecial,
      failOnDataLoss)
  }

  /**
    * Java constructor for a RocketMq DStream
    * @param groupId it is for rocketMq for identifying the consumer
    * @param topics the topics for the rocketmq
    * @param consumerStrategy consumerStrategy In most cases, pass in [[ConsumerStrategy.lastest]],
    *   see [[ConsumerStrategy]] for more details
    * @param autoCommit whether commit the offset to the rocketmq server automatically or not. If the user
    *                   implement the [[OffsetCommitCallback]], the autoCommit must be set false
    * @param forceSpecial Generally if the rocketmq server has checkpoint for the [[MessageQueue]], then the consumer
    *  will consume from the checkpoint no matter we specify the offset or not. But if forceSpecial is true,
    *  the rocketmq will start consuming from the specific available offset in any case.
    * @param failOnDataLoss Zero data lost is not guaranteed when topics are deleted. If zero data lost is critical,
    * the user must make sure all messages in a topic have been processed when deleting a topic.
    * @param locationStrategy map from TopicQueueId to preferred host for processing that partition.
    * In most cases, use [[LocationStrategy.PreferConsistent]]
    * @param optionParams optional configs, see [[RocketMQConfig]] for more details.
    * @return JavaInputDStream[MessageExt]
    */
  def createJavaMQPullStream(
     ssc: JavaStreamingContext,
     groupId: String,
     topics: ju.Collection[jl.String],
     consumerStrategy: ConsumerStrategy,
     autoCommit: Boolean,
     forceSpecial: Boolean,
     failOnDataLoss: Boolean,
     locationStrategy: LocationStrategy = PreferConsistent,
     optionParams: ju.Map[String, String] = new ju.HashMap
    ): JavaInputDStream[MessageExt] = {
    val inputDStream = createMQPullStream(ssc.ssc, groupId, topics, consumerStrategy,
      autoCommit, forceSpecial, failOnDataLoss, locationStrategy, optionParams)
    new JavaInputDStream(inputDStream)
  }

  def createJavaMQPullStream(
    ssc: JavaStreamingContext,
    groupId: String,
    topics: ju.Collection[jl.String],
    consumerStrategy: ConsumerStrategy,
    autoCommit: Boolean,
    forceSpecial: Boolean,
    failOnDataLoss: Boolean): JavaInputDStream[MessageExt] = {
    val inputDStream = createMQPullStream(ssc.ssc, groupId, topics, consumerStrategy,
      autoCommit, forceSpecial, failOnDataLoss)
    new JavaInputDStream(inputDStream)
  }

  def mkPullConsumerInstance(
      groupId: String,
      optionParams: ju.Map[String, String],
      instance: String): MQPullConsumerProvider = {
    val consumerProviderFactoryName =
      optionParams.getOrDefault(MQ_PULL_CONSUMER_PROVIDER_FACTORY_NAME, DEFAULT_MQ_PULL_CONSUMER_PROVIDER_FACTORY_NAME)
    val consumer = ConsumerProviderFactory.getPullConsumerProviderByFactoryName(consumerProviderFactoryName)
    consumer.setConsumerGroup(groupId)
    if (optionParams.containsKey(PULL_TIMEOUT_MS))
      consumer.setConsumerTimeoutMillisWhenSuspend(optionParams.get(PULL_TIMEOUT_MS).toLong)
    if (!StringUtils.isBlank(instance)) 
      consumer.setInstanceName(instance)
    if (optionParams.containsKey(NAME_SERVER_ADDR))
      consumer.setNamesrvAddr(optionParams.get(NAME_SERVER_ADDR))
    consumer.setOptionParams(optionParams)

    consumer.start()
    consumer.setOffsetStore(consumer.getOffsetStore)
    consumer
  }

  /**
    * For creating Java push mode unreliable DStream
    * @param jssc
    * @param properties
    * @param level
    * @return
    */
  def createJavaMQPushStream(
    jssc: JavaStreamingContext,
    properties: Properties,
    level: StorageLevel
  ): JavaInputDStream[Message] = createJavaMQPushStream(jssc, properties, level, false)

  /**
    * For creating Java push mode reliable DStream
    * @param jssc
    * @param properties
    * @param level
    * @return
    */
  def createJavaReliableMQPushStream(
    jssc: JavaStreamingContext,
    properties: Properties,
    level: StorageLevel
  ): JavaInputDStream[Message] = createJavaMQPushStream(jssc, properties, level, true)

  /**
    * For creating Java push mode DStream
    * @param jssc
    * @param properties
    * @param level
    * @param reliable
    * @return
    */
  def createJavaMQPushStream(
    jssc: JavaStreamingContext,
    properties: Properties,
    level: StorageLevel,
    reliable: Boolean
  ): JavaInputDStream[Message] = {
    if (jssc == null || properties == null || level == null) return null
    val receiver = if (reliable) new ReliableRocketMQReceiver(properties, level) else new RocketMQReceiver(properties, level)
    val ds = jssc.receiverStream(receiver)
    ds
  }

}
