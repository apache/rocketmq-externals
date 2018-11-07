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

/**
 * Options for RocketMQ consumer or producer client.
 *
 * See also [[RocketMQSourceProvider]]
 */
object RocketMQConf {

  //*******************************
  //        Shared Options
  //*******************************

  val NAME_SERVER_ADDR = "nameserver"

  //*******************************
  //  Source (Consumer) Options
  //*******************************

  val CONSUMER_GROUP = "group"

  val CONSUMER_TOPIC = "topic"

  // What point should be consuming from (options: "earliest", "latest", default: "latest")
  val CONSUMER_OFFSET = "startingoffsets"

  // Subscription expression (default: "*")
  val CONSUMER_SUB_EXPRESSION = "subexpression"

  // To pick up the consume speed, the consumer can pull a batch of messages at a time (default: 32)
  val PULL_MAX_BATCH_SIZE = "pullbatchsize"

  // Pull timeout for the consumer (default: 3000)
  val PULL_TIMEOUT_MS = "pulltimeoutms"

  //*******************************
  //   Sink (Producer) Options
  //*******************************

  val PRODUCER_GROUP = "group"

  // Default topic of produced messages if `topic` is not among the attributes
  val PRODUCER_TOPIC = "topic"

  //*******************************
  //     Spark Context Options
  //*******************************

  // Max number of cached pull consumer (default: 64)
  val PULL_CONSUMER_CACHE_MAX_CAPACITY = "spark.sql.rocketmq.pull.consumer.cache.maxCapacity"

  // Producer cache timeout (default: "10m")
  val PRODUCER_CACHE_TIMEOUT = "spark.rocketmq.producer.cache.timeout"
}
