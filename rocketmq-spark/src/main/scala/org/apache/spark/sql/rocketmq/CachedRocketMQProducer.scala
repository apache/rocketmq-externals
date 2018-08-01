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

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap, ExecutionException, TimeUnit}
import java.{util => ju}

import com.google.common.cache._
import com.google.common.util.concurrent.{ExecutionError, UncheckedExecutionException}
import org.apache.rocketmq.client.producer.DefaultMQProducer
import org.apache.spark.SparkEnv
import org.apache.spark.internal.Logging

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

private[rocketmq] object CachedRocketMQProducer extends Logging {

  private type Producer = DefaultMQProducer

  // The MQProducer client is shared by multiple instances of CachedRocketMQProducer
  // because RocketMQ claims there should not be more than one instance for a groupId
  private lazy val groupIdToClient = new ConcurrentHashMap[String, Producer]().asScala
  
  private lazy val cacheExpireTimeout: Long =
    SparkEnv.get.conf.getTimeAsMs(s"spark.rocketmq.producer.cache.timeout", "10m")

  private val cacheLoader = new CacheLoader[Seq[(String, String)], Producer] {
    override def load(config: Seq[(String, String)]): Producer = {
      val configMap = config.map(x => x._1 -> x._2).toMap.asJava
      createRocketMQProducer(configMap)
    }
  }

  private val removalListener = new RemovalListener[Seq[(String, String)], Producer]() {
    override def onRemoval(
        notification: RemovalNotification[Seq[(String, String)], Producer]): Unit = {
      val paramsSeq: Seq[(String, String)] = notification.getKey
      val producer: Producer = notification.getValue
      logDebug(
        s"Evicting RocketMQ producer $producer params: $paramsSeq, due to ${notification.getCause}")
      close(paramsSeq, producer)
    }
  }

  private lazy val guavaCache: LoadingCache[Seq[(String, String)], Producer] =
    CacheBuilder.newBuilder().expireAfterAccess(cacheExpireTimeout, TimeUnit.MILLISECONDS)
        .removalListener(removalListener)
        .build[Seq[(String, String)], Producer](cacheLoader)

  private def createRocketMQProducer(optionParams: ju.Map[String, String]): Producer = {
    val groupId = optionParams.get(RocketMQProducerConfig.PRODUCER_GROUP)
    groupIdToClient.getOrElseUpdate(groupId, {
      val rocketmqProducer = RocketMQUtils.makeProducer(groupId, optionParams)
      logDebug(s"Created a new instance of RocketMQProducer for $optionParams.")
      rocketmqProducer
    })
  }

  /**
    * Get a cached RocketMQProducer for a given configuration. If matching RocketMQProducer doesn't
    * exist, a new RocketMQProducer will be created. RocketMQProducer is thread safe, it is best to keep
    * one instance per specified rocketmqParams.
    */
  private[rocketmq] def getOrCreate(rocketmqParams: ju.Map[String, String]): Producer = {
    val paramsSeq: Seq[(String, String)] = paramsToSeq(rocketmqParams)
    try {
      guavaCache.get(paramsSeq)
    } catch {
      case e @ (_: ExecutionException | _: UncheckedExecutionException | _: ExecutionError)
        if e.getCause != null =>
        throw e.getCause
    }
  }

  private def paramsToSeq(rocketmqParams: ju.Map[String, String]): Seq[(String, String)] = {
    val paramsSeq: Seq[(String, String)] = rocketmqParams.asScala.toSeq.sortBy(x => x._1)
    paramsSeq
  }

  /** For explicitly closing rocketmq producer */
  private[rocketmq] def close(rocketmqParams: ju.Map[String, String]): Unit = {
    val paramsSeq = paramsToSeq(rocketmqParams)
    guavaCache.invalidate(paramsSeq)
  }

  /** Auto close on cache evict */
  private def close(paramsSeq: Seq[(String, String)], producer: Producer): Unit = {
    try {
      logInfo(s"Closing the RocketMQProducer with params: ${paramsSeq.mkString("\n")}.")
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
  private def getAsMap: ConcurrentMap[Seq[(String, String)], Producer] = guavaCache.asMap()
}
