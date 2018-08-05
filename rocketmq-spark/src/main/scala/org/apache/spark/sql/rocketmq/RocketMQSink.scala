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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaSink.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.{util => ju}

import org.apache.spark.internal.Logging
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.sql.execution.streaming.Sink

private[rocketmq] class RocketMQSink(
    sqlContext: SQLContext,
    executorRocketMQParams: ju.Map[String, String],
    topic: Option[String]) extends Sink with Logging {

  @volatile private var latestBatchId = -1L

  override def toString: String = "RocketMQSink"

  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    if (batchId <= latestBatchId) {
      logInfo(s"Skipping already committed batch $batchId")
    } else {
      RocketMQWriter.write(sqlContext.sparkSession,
        data.queryExecution, executorRocketMQParams, topic)
      latestBatchId = batchId
    }
  }
}
