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

import java.util.{Locale, UUID}
import java.{util => ju}

import org.apache.rocketmq.spark.RocketMQConfig
import org.apache.spark.internal.Logging
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.execution.streaming.Source
import org.apache.spark.sql.sources._
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConverters._

/**
 * The provider class for the [[RocketMQSource]]. This provider is designed such that it throws
 * IllegalArgumentException when the RocketMQ Dataset is created, so that it can catch
 * missing options even before the query is started.
 */
class RocketMQSourceProvider extends DataSourceRegister
    with StreamSourceProvider
    with Logging {
  import RocketMQSourceProvider._

  override def shortName(): String = "rocketmq"

  /**
   * Returns the name and schema of the source. In addition, it also verifies whether the options
   * are correct and sufficient to create the [[RocketMQSource]] when the query is started.
   */
  override def sourceSchema(
      sqlContext: SQLContext,
      schema: Option[StructType],
      providerName: String,
      parameters: Map[String, String]): (String, StructType) = {
    validateStreamOptions(parameters)
    require(schema.isEmpty, "RocketMQ source has a fixed schema and cannot be set with a custom one")
    (shortName(), RocketMQOffsetReader.schema)
  }

  override def createSource(
      sqlContext: SQLContext,
      metadataPath: String,
      schema: Option[StructType],
      providerName: String,
      parameters: Map[String, String]): Source = {
    validateStreamOptions(parameters)
    // Each running query should use its own group id. Otherwise, the query may be only assigned
    // partial data since RocketMQ will assign partitions to multiple consumers having the same group
    // id. Hence, we should generate a unique id for each query.
    val uniqueGroupId = s"spark-rocketmq-source-${UUID.randomUUID}-${metadataPath.hashCode.toHexString}"

    val caseInsensitiveParams = parameters.map { case (k, v) => (k.toLowerCase(Locale.ROOT), v) }

    val startingStreamOffsets = RocketMQSourceProvider.getRocketMQOffsetRangeLimit(caseInsensitiveParams,
      RocketMQConfig.CONSUMER_OFFSET_RESET_TO, EarliestOffsetRangeLimit) // Kafka uses `Latest`

    val offsetReader = new RocketMQOffsetReader(
      rocketmqParamsForDriver(caseInsensitiveParams),
      parameters,
      driverGroupIdPrefix = s"$uniqueGroupId-driver")

    new RocketMQSource(
      sqlContext,
      offsetReader,
      rocketmqParamsForExecutors(caseInsensitiveParams, uniqueGroupId),
      parameters,
      metadataPath,
      startingStreamOffsets,
      failOnDataLoss(caseInsensitiveParams))
  }

  private def failOnDataLoss(caseInsensitiveParams: Map[String, String]) =
    caseInsensitiveParams.getOrElse(FAIL_ON_DATA_LOSS_OPTION_KEY, "true").toBoolean

  private def validateGeneralOptions(caseInsensitiveParams: Map[String, String]) {
    // Validate source options
    if (!caseInsensitiveParams.contains(RocketMQConfig.CONSUMER_TOPIC)) {
      throw new IllegalArgumentException(s"Option '${RocketMQConfig.CONSUMER_TOPIC}' must be specified for RocketMQ source")
    }
  }

  private def validateStreamOptions(caseInsensitiveParams: Map[String, String]) {
    // Stream specific options
    validateGeneralOptions(caseInsensitiveParams)
  }

  private def validateBatchOptions(caseInsensitiveParams: Map[String, String]) {
    // Batch specific options
    RocketMQSourceProvider.getRocketMQOffsetRangeLimit(
      caseInsensitiveParams, STARTING_OFFSETS_OPTION_KEY, EarliestOffsetRangeLimit) match {
      case EarliestOffsetRangeLimit => // good to go
      case LatestOffsetRangeLimit =>
        throw new IllegalArgumentException("starting offset can't be latest " +
          "for batch queries on RocketMQ")
      case SpecificOffsetRangeLimit(partitionOffsets) =>
        partitionOffsets.foreach {
          case (mq, off) if off == RocketMQOffsetRangeLimit.LATEST =>
            throw new IllegalArgumentException(s"starting offsets for $mq can't be latest for batch queries on RocketMQ")
          case _ => // ignore
        }
    }

    RocketMQSourceProvider.getRocketMQOffsetRangeLimit(
      caseInsensitiveParams, ENDING_OFFSETS_OPTION_KEY, LatestOffsetRangeLimit) match {
      case EarliestOffsetRangeLimit =>
        throw new IllegalArgumentException("ending offset can't be earliest " +
          "for batch queries on RocketMQ")
      case LatestOffsetRangeLimit => // good to go
      case SpecificOffsetRangeLimit(partitionOffsets) =>
        partitionOffsets.foreach {
          case (mq, off) if off == RocketMQOffsetRangeLimit.EARLIEST =>
            throw new IllegalArgumentException(s"ending offset for $mq can't be " +
              "earliest for batch queries on RocketMQ")
          case _ => // ignore
        }
    }

    validateGeneralOptions(caseInsensitiveParams)

    // Don't want to throw an error, but at least log a warning.
    if (caseInsensitiveParams.get("maxoffsetspertrigger").isDefined) {
      logWarning("maxOffsetsPerTrigger option ignored in batch queries")
    }
  }
}

object RocketMQSourceProvider extends Logging {
  private[rocketmq] val STARTING_OFFSETS_OPTION_KEY = "startingoffsets"
  private[rocketmq] val ENDING_OFFSETS_OPTION_KEY = "endingoffsets"
  private val FAIL_ON_DATA_LOSS_OPTION_KEY = "failondataloss"

  def getRocketMQOffsetRangeLimit(
      params: Map[String, String],
      offsetOptionKey: String,
      defaultOffsets: RocketMQOffsetRangeLimit): RocketMQOffsetRangeLimit = {
    // TODO: support specify timestamp
    params.get(offsetOptionKey).map(_.trim) match {
      case Some(offset) if offset.toLowerCase(Locale.ROOT) == "latest" =>
        LatestOffsetRangeLimit
      case Some(offset) if offset.toLowerCase(Locale.ROOT) == "earliest" =>
        EarliestOffsetRangeLimit
      case Some(json) => SpecificOffsetRangeLimit(JsonUtils.partitionOffsets(json))
      case None => defaultOffsets
    }
  }

  def rocketmqParamsForDriver(specifiedRocketMQParams: Map[String, String]): ju.Map[String, String] =
    ConfigUpdater("source", specifiedRocketMQParams)
      // Set to "earliest" to avoid exceptions. However, RocketMQSource will fetch the initial
      // offsets by itself instead of counting on RocketMQConsumer.
      .set(RocketMQConfig.CONSUMER_OFFSET_RESET_TO, "earliest")

      // So that the driver does not pull too much data
      .set(RocketMQConfig.PULL_MAX_BATCH_SIZE, "1")

      .build()

  def rocketmqParamsForExecutors(
      specifiedRocketMQParams: Map[String, String],
      uniqueGroupId: String): ju.Map[String, String] =
    ConfigUpdater("executor", specifiedRocketMQParams)
      // So that consumers in executors do not mess with any existing group id
      .set(RocketMQConfig.CONSUMER_GROUP, s"$uniqueGroupId-executor")

      .build()

  /** Class to conveniently update RocketMQ config params, while logging the changes */
  private case class ConfigUpdater(module: String, params: Map[String, String]) {
    private val map = new ju.HashMap[String, String](params.asJava)

    def set(key: String, value: String): this.type = {
      map.put(key, value)
      logDebug(s"$module: Set $key to $value, earlier value: ${params.getOrElse(key, "")}")
      this
    }

    def setIfUnset(key: String, value: String): ConfigUpdater = {
      if (!map.containsKey(key)) {
        map.put(key, value)
        logDebug(s"$module: Set $key to $value")
      }
      this
    }

    def build(): ju.Map[String, String] = map
  }
}
