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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaSourceProvider.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 * 2. Schema of output dataframe adapted to RocketMQ
 * 3. Trait `StreamWriteSupport` and `ContinuousReadSupport` is not supported yet
 */

package org.apache.spark.sql.rocketmq

import java.util.{Locale, UUID}
import java.{util => ju}

import org.apache.spark.internal.Logging
import org.apache.spark.sql.{AnalysisException, DataFrame, SQLContext, SaveMode}
import org.apache.spark.sql.execution.streaming.{Sink, Source}
import org.apache.spark.sql.sources._
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.StructType

import scala.collection.JavaConverters._

/**
 * The provider class for the [[RocketMQSource]]. This provider is designed such that it throws
 * IllegalArgumentException when the RocketMQ Dataset is created, so that it can catch
 * missing options even before the query is started.
 */
class RocketMQSourceProvider extends DataSourceRegister
    with StreamSourceProvider
    with RelationProvider
    with CreatableRelationProvider
    with StreamSinkProvider
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
    (shortName(), RocketMQSource.schema)
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
      RocketMQConf.CONSUMER_OFFSET, LatestOffsetRangeLimit)

    val offsetReader = new RocketMQOffsetReader(
      paramsForDriver(caseInsensitiveParams),
      parameters,
      driverGroupIdPrefix = s"$uniqueGroupId-driver")

    new RocketMQSource(
      sqlContext,
      offsetReader,
      paramsForExecutors(caseInsensitiveParams, uniqueGroupId),
      parameters,
      metadataPath,
      startingStreamOffsets,
      failOnDataLoss(caseInsensitiveParams))
  }

  /**
    * Returns a new base relation with the given parameters.
    *
    * @note The parameters' keywords are case insensitive and this insensitivity is enforced
    *       by the Map that is passed to the function.
    */
  override def createRelation(
      sqlContext: SQLContext,
      parameters: Map[String, String]): BaseRelation = {
    validateBatchOptions(parameters)
    val caseInsensitiveParams = parameters.map { case (k, v) => (k.toLowerCase(Locale.ROOT), v) }

    val startingRelationOffsets = RocketMQSourceProvider.getRocketMQOffsetRangeLimit(caseInsensitiveParams,
      STARTING_OFFSETS_OPTION_KEY, EarliestOffsetRangeLimit)
    assert(startingRelationOffsets != LatestOffsetRangeLimit)

    val endingRelationOffsets = RocketMQSourceProvider.getRocketMQOffsetRangeLimit(caseInsensitiveParams,
      ENDING_OFFSETS_OPTION_KEY, LatestOffsetRangeLimit)
    assert(endingRelationOffsets != EarliestOffsetRangeLimit)

    new RocketMQRelation(
      sqlContext,
      sourceOptions = parameters,
      optionParams = caseInsensitiveParams,
      failOnDataLoss = failOnDataLoss(caseInsensitiveParams),
      startingOffsets = startingRelationOffsets,
      endingOffsets = endingRelationOffsets)
  }

  override def createRelation(
      sqlContext: SQLContext,
      mode: SaveMode,
      parameters: Map[String, String],
      data: DataFrame): BaseRelation = {
    mode match {
      case SaveMode.Overwrite | SaveMode.Ignore =>
        throw new AnalysisException(s"Save mode $mode not allowed for RocketMQ. " +
            s"Allowed save modes are ${SaveMode.Append} and " +
            s"${SaveMode.ErrorIfExists} (default).")
      case _ => // good
    }
    val caseInsensitiveParams = parameters.map { case (k, v) => (k.toLowerCase(Locale.ROOT), v) }
    val defaultTopic = parameters.get(RocketMQConf.PRODUCER_TOPIC).map(_.trim)
    val uniqueGroupId = s"spark-rocketmq-sink-${UUID.randomUUID}"

    val specifiedKafkaParams = paramsForProducer(caseInsensitiveParams, uniqueGroupId)
    RocketMQWriter.write(sqlContext.sparkSession, data.queryExecution, specifiedKafkaParams, defaultTopic)

    /* This method is suppose to return a relation that reads the data that was written.
     * We cannot support this for RocketMQ. Therefore, in order to make things consistent,
     * we return an empty base relation.
     */
    new BaseRelation {
      override def sqlContext: SQLContext = unsupportedException
      override def schema: StructType = unsupportedException
      override def needConversion: Boolean = unsupportedException
      override def sizeInBytes: Long = unsupportedException
      override def unhandledFilters(filters: Array[Filter]): Array[Filter] = unsupportedException
      private def unsupportedException =
        throw new UnsupportedOperationException("BaseRelation from RocketMQ write " +
            "operation is not usable.")
    }
  }

  private def failOnDataLoss(caseInsensitiveParams: Map[String, String]) =
    caseInsensitiveParams.getOrElse(FAIL_ON_DATA_LOSS_OPTION_KEY, "true").toBoolean

  private def validateGeneralOptions(caseInsensitiveParams: Map[String, String]) {
    // Validate source options
    if (!caseInsensitiveParams.contains(RocketMQConf.CONSUMER_TOPIC)) {
      throw new IllegalArgumentException(s"Option '${RocketMQConf.CONSUMER_TOPIC}' must be specified for RocketMQ source")
    }
  }

  override def createSink(
      sqlContext: SQLContext,
      parameters: Map[String, String],
      partitionColumns: Seq[String],
      outputMode: OutputMode): Sink = {
    val caseInsensitiveParams = parameters.map { case (k, v) => (k.toLowerCase(Locale.ROOT), v) }

    val defaultTopic = parameters.get(RocketMQConf.PRODUCER_TOPIC).map(_.trim)
    val uniqueGroupId = s"spark-rocketmq-sink-${UUID.randomUUID}"

    new RocketMQSink(sqlContext, paramsForProducer(caseInsensitiveParams, uniqueGroupId), defaultTopic)
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
  private[rocketmq] val FAIL_ON_DATA_LOSS_OPTION_KEY = "failondataloss"

  def getRocketMQOffsetRangeLimit(
      params: Map[String, String],
      offsetOptionKey: String,
      defaultOffsets: RocketMQOffsetRangeLimit): RocketMQOffsetRangeLimit = {
    params.get(offsetOptionKey).map(_.trim) match {
      case Some(offset) if offset.toLowerCase(Locale.ROOT) == "latest" =>
        LatestOffsetRangeLimit
      case Some(offset) if offset.toLowerCase(Locale.ROOT) == "earliest" =>
        EarliestOffsetRangeLimit
      case Some(json) => SpecificOffsetRangeLimit(JsonUtils.partitionOffsets(json))
      case None => defaultOffsets
    }
  }

  def paramsForDriver(specifiedRocketMQParams: Map[String, String]): ju.Map[String, String] = {
    if (specifiedRocketMQParams.contains(RocketMQConf.CONSUMER_GROUP)) {
      throw new IllegalArgumentException(
        s"Option '${RocketMQConf.CONSUMER_GROUP}' can not be specified")
    }
    ConfigUpdater("source", specifiedRocketMQParams)
        // Set to "earliest" to avoid exceptions. However, RocketMQSource will fetch the initial
        // offsets by itself instead of counting on RocketMQConsumer.
        .set(RocketMQConf.CONSUMER_OFFSET, "earliest")
        // So that the driver does not pull too much data
        .set(RocketMQConf.PULL_MAX_BATCH_SIZE, "1")
        .build()
  }

  def paramsForExecutors(
      specifiedRocketMQParams: Map[String, String],
      uniqueGroupId: String): ju.Map[String, String] = {
    if (specifiedRocketMQParams.contains(RocketMQConf.CONSUMER_GROUP)) {
      throw new IllegalArgumentException(
        s"Option '${RocketMQConf.CONSUMER_GROUP}' can not be specified")
    }
    ConfigUpdater("executor", specifiedRocketMQParams)
        // So that consumers in executors do not mess with any existing group id
        .set(RocketMQConf.CONSUMER_GROUP, s"$uniqueGroupId-executor")
        .build()
  }

  def paramsForProducer(
      specifiedRocketMQParams: Map[String, String],
      uniqueGroupId: String): ju.Map[String, String] = {
    if (specifiedRocketMQParams.contains(RocketMQConf.PRODUCER_GROUP)) {
      throw new IllegalArgumentException(
        s"Option '${RocketMQConf.PRODUCER_GROUP}' can not be specified")
    }
    ConfigUpdater("executor", specifiedRocketMQParams)
        // So that consumers in executors do not mess with any existing group id
        .set(RocketMQConf.PRODUCER_GROUP, uniqueGroupId)
        .build()
  }

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
