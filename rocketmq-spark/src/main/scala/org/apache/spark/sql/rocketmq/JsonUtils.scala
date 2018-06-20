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

import java.{util => ju}

import org.apache.rocketmq.common.message.MessageQueue
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
  * Utilities for converting RocketMQ related objects to and from json.
  */
private object JsonUtils {

  private implicit val formats = Serialization.formats(NoTypeHints)

  /**
    * Read MessageQueues from json string
    */
  def partitions(str: String): Array[MessageQueue] = {
    try {
      Serialization.read[Map[String, Map[String, Seq[Int]]]](str).flatMap { case (topic, broker) =>
        broker.flatMap { bq =>
          bq._2.map(qid => new MessageQueue(topic, bq._1, qid))
        }
      }.toArray
    } catch {
      case NonFatal(x) =>
        throw new IllegalArgumentException(
          s"""Expected e.g. {"topicA":{"broker1":[0,1],"broker2":[0,1]},"topicB":{"broker3":[0,1]}}, got $str""")
    }
  }

  /**
    * Write MessageQueues as json string
    */
  def partitions(mqs: Iterable[MessageQueue]): String = {
    var result = Map[String, Map[String, List[Int]]]()
    mqs.foreach { q =>
      var brokers = result.getOrElse(q.getTopic, Map.empty)
      var queueIds = brokers.getOrElse(q.getBrokerName, List.empty)
      queueIds = queueIds :+ q.getQueueId
      brokers += q.getBrokerName -> queueIds
      result += q.getTopic -> brokers
    }
    Serialization.write(result)
  }

  /**
    * Read per-MessageQueue offsets from json string
    */
  def partitionOffsets(str: String): Map[MessageQueue, Long] = {
    try {
      Serialization.read[Map[String, Map[String, Map[Int, Long]]]](str).flatMap { case (topic, brokers) =>
        brokers.flatMap { case (broker, queues) =>
          queues.map { case (queue, offset) =>
            new MessageQueue(topic, broker, queue) -> offset
          }
        }
      }.toMap
    } catch {
      case NonFatal(x) =>
        throw new IllegalArgumentException(
          s"""Expected e.g. {"topicA":{"broker1":{"0":23,"1":-1},"broker2":{"0":23}},"topicB":{"broker3":{"0":-2}}}, got $str""")
    }
  }

  /**
    * Write per-MessageQueue offsets as json string
    */
  def partitionOffsets(queueOffsets: Map[MessageQueue, Long]): String = {
    var result = Map[String, Map[String, Map[Int, Long]]]()
    val partitions = queueOffsets.keySet.toSeq.sorted // sort for more determinism
    partitions.foreach { q =>
      val offset = queueOffsets(q)
      var brokers = result.getOrElse(q.getTopic, Map.empty)
      var queues = brokers.getOrElse(q.getBrokerName, Map.empty)
      queues += q.getQueueId -> offset
      brokers += q.getBrokerName -> queues
      result += q.getTopic -> brokers
    }
    Serialization.write(result)
  }

  /**
    * Serialize RocketMQ message properties as json string
    */
  def messageProperties(properties: ju.Map[String, String]): String = {
    Serialization.write(properties.asScala)
  }
}
