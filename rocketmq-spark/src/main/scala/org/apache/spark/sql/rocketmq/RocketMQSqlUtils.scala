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

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer
import org.apache.rocketmq.client.producer.DefaultMQProducer

/**
 * Some helper methods of RocketMQ
 */
object RocketMQSqlUtils {

  def makePullConsumer(groupId: String, optionParams: ju.Map[String, String]): DefaultMQPullConsumer = {
    val consumer = new DefaultMQPullConsumer(groupId)
    if (optionParams.containsKey(RocketMQConf.NAME_SERVER_ADDR)) {
      consumer.setNamesrvAddr(optionParams.get(RocketMQConf.NAME_SERVER_ADDR))
    }
    consumer.start()
    consumer.setOffsetStore(consumer.getDefaultMQPullConsumerImpl.getOffsetStore)
    consumer
  }

  def makeProducer(groupId: String, optionParams: ju.Map[String, String]): DefaultMQProducer = {
    val producer = new DefaultMQProducer(groupId)
    if (optionParams.containsKey(RocketMQConf.NAME_SERVER_ADDR)) {
      producer.setNamesrvAddr(optionParams.get(RocketMQConf.NAME_SERVER_ADDR))
    }
    producer.start()
    producer
  }

}
