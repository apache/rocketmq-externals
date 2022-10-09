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
 * This file was taken from Apache Spark org/apache/spark/sql/kafka010/CachedKafkaProducer.scala
 *
 * There are some modifications:
 * 1. Parameters and API were adapted to RocketMQ
 * 2. Reuse underlying producer instance for each producer group
 */

package org.apache.spark.sql.rocketmq

import java.util.concurrent._
import java.{util => ju}

import com.google.common.cache._
import com.google.common.util.concurrent.{ExecutionError, UncheckedExecutionException}
import org.apache.rocketmq.client.producer.DefaultMQProducer
import org.apache.spark.SparkEnv
import org.apache.spark.internal.Logging

import scala.util.control.NonFatal

private[rocketmq] object CachedRocketMQProducer extends Logging {

  private type Producer = DefaultMQProducer

  private lazy val cacheExpireTimeout: Long =
    SparkEnv.get.conf.getTimeAsMs(RocketMQConf.PRODUCER_CACHE_TIMEOUT, "10m")
  
  private val removalListener = new RemovalListener[String, Producer]() {
    override def onRemoval(
        notification: RemovalNotification[String, Producer]): Unit = {
      val group: String = notification.getKey
      val producer: Producer = notification.getValue
      logDebug(
        s"Evicting RocketMQ producer $producer for group $group, due to ${notification.getCause}")
      close(group, producer)
    }
  }

  private lazy val guavaCache: Cache[String, Producer] =
    CacheBuilder.newBuilder().expireAfterAccess(cacheExpireTimeout, TimeUnit.MILLISECONDS)
        .removalListener(removalListener)
        .build[String, Producer]()
  
  /**
    * Get a cached RocketMQProducer for a given configuration. If matching RocketMQProducer doesn't
    * exist, a new RocketMQProducer will be created. RocketMQProducer is thread safe, it is best to keep
    * one instance per specified options.
    */
  def getOrCreate(options: ju.Map[String, String]): Producer = {
    val group = options.get(RocketMQConf.PRODUCER_GROUP)
    try {
      guavaCache.get(group, new Callable[Producer] {
        override def call(): Producer = {
          val producer = RocketMQSqlUtils.makeProducer(group, options)
          logDebug(s"Created a new instance of RocketMQ producer for group $group.")
          producer
        }
      })
    } catch {
      case e @ (_: ExecutionException | _: UncheckedExecutionException | _: ExecutionError)
        if e.getCause != null =>
        throw e.getCause
    }
  }

  /** For explicitly closing RocketMQ producer */
  private def close(options: ju.Map[String, String]): Unit = {
    val group = options.get(RocketMQConf.PRODUCER_GROUP)
    guavaCache.invalidate(group)
  }

  /** Auto close on cache evict */
  private def close(group: String, producer: Producer): Unit = {
    try {
      logInfo(s"Closing the RocketMQ producer of group $group")
      producer.shutdown()
    } catch {
      case NonFatal(e) => logWarning("Error while closing RocketMQ producer.", e)
    }
  }

  private def clear(): Unit = {
    logInfo("Cleaning up guava cache.")
    guavaCache.invalidateAll()
  }

  // Intended for testing purpose only.
  private def getAsMap: ConcurrentMap[String, Producer] = guavaCache.asMap()
}
