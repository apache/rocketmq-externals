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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/KafkaWriteTask.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 */

package org.apache.spark.sql.rocketmq

import java.{util => ju}

import org.apache.rocketmq.client.producer.{DefaultMQProducer, SendCallback, SendResult}
import org.apache.rocketmq.common.message.Message
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.{Attribute, Cast, Literal, UnsafeProjection}
import org.apache.spark.sql.types.{BinaryType, StringType}

/**
 * Writes out data in a single Spark task, without any concerns about how
 * to commit or abort tasks. Exceptions thrown by the implementation of this class will
 * automatically trigger task aborts.
 */
private[rocketmq] class RocketMQWriteTask(
    options: ju.Map[String, String],
    inputSchema: Seq[Attribute],
    topic: Option[String]) extends RocketMQRowWriter(inputSchema, topic) {
  // used to synchronize with RocketMQ callbacks
  private var producer: DefaultMQProducer = _

  /**
   * Writes key value data out to topics.
   */
  def execute(iterator: Iterator[InternalRow]): Unit = {
    producer = CachedRocketMQProducer.getOrCreate(options)
    while (iterator.hasNext && failedWrite == null) {
      val currentRow = iterator.next()
      sendRow(currentRow, producer)
    }
  }

  def close(): Unit = {
    checkForErrors()
    if (producer != null) {
      checkForErrors()
      producer = null
    }
  }
}

private[rocketmq] abstract class RocketMQRowWriter(
    inputSchema: Seq[Attribute], topic: Option[String]) {

  // used to synchronize with RocketMQ callbacks
  @volatile protected var failedWrite: Throwable = _
  protected val projection = createProjection

  private val callback = new SendCallback {
    override def onSuccess(sendResult: SendResult): Unit = {}

    override def onException(e: Throwable): Unit = {
      if (failedWrite == null) failedWrite = e
    }
  }

  /**
    * Send the specified row to the producer, with a callback that will save any exception
    * to failedWrite. Note that send is asynchronous; subclasses must flush() their producer before
    * assuming the row is in RocketMQ.
    */
  protected def sendRow(
      row: InternalRow, producer: DefaultMQProducer): Unit = {
    val projectedRow = projection(row)
    val topic = projectedRow.getString(0)
    val keys = if (projectedRow.isNullAt(1)) null else projectedRow.getString(1)
    val body = projectedRow.getBinary(2)
    if (topic == null) {
      throw new NullPointerException(s"null topic present in the data. Use the " +
          s"${RocketMQConf.PRODUCER_TOPIC} option for setting a default topic.")
    }
    val record = new Message(topic, keys, body)
    producer.send(record, callback) // send asynchronously
  }

  protected def checkForErrors(): Unit = {
    if (failedWrite != null) {
      throw failedWrite
    }
  }

  private def createProjection = {
    val topicExpression = topic.map(Literal(_)).orElse {
      inputSchema.find(_.name == RocketMQWriter.TOPIC_ATTRIBUTE_NAME)
    }.getOrElse {
      throw new IllegalStateException(s"topic option required when no " +
          s"'${RocketMQWriter.TOPIC_ATTRIBUTE_NAME}' attribute is present")
    }
    topicExpression.dataType match {
      case StringType => // good
      case t =>
        throw new IllegalStateException(s"${RocketMQWriter.TOPIC_ATTRIBUTE_NAME} " +
            s"attribute unsupported type $t. ${RocketMQWriter.TOPIC_ATTRIBUTE_NAME} " +
            "must be a StringType")
    }
    val tagsExpression = inputSchema.find(_.name == RocketMQWriter.TAGS_ATTRIBUTE_NAME)
        .getOrElse(Literal(null, StringType))
    tagsExpression.dataType match {
      case StringType => // good
      case t =>
        throw new IllegalStateException(s"${RocketMQWriter.TAGS_ATTRIBUTE_NAME} " +
            s"attribute unsupported type $t")
    }
    val bodyExpression = inputSchema.find(_.name == RocketMQWriter.BODY_ATTRIBUTE_NAME).getOrElse(
      throw new IllegalStateException(s"Required attribute '${RocketMQWriter.BODY_ATTRIBUTE_NAME}' not found")
    )
    bodyExpression.dataType match {
      case StringType | BinaryType => // good
      case t =>
        throw new IllegalStateException(s"${RocketMQWriter.BODY_ATTRIBUTE_NAME} " +
            s"attribute unsupported type $t")
    }
    UnsafeProjection.create(
      Seq(topicExpression, tagsExpression, Cast(bodyExpression, BinaryType)), inputSchema)
  }
}
