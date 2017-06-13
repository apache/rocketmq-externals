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

import java.{util => ju}

import scala.collection.JavaConverters._


/**
  *  :: Experimental ::
  * Choice of how to schedule consumers for a given [[TopicQueueId]] on an executor.
  * See [[LocationStrategy]] to obtain instances.
  * RocketMq consumers prefetch messages, so it's important for performance
  * to keep cached consumers on appropriate executors, not recreate them for every partition.
  * Choice of location is only a preference, not an absolute; partitions may be scheduled elsewhere.
  */

sealed abstract class LocationStrategy

case object PreferConsistent extends LocationStrategy

case class PreferFixed(hostMap: ju.Map[TopicQueueId, String]) extends LocationStrategy

/**
  * object to obtain instances of [[LocationStrategy]]
  *
  */
object LocationStrategy {

  /**
    *
    * Use this in most cases, it will consistently distribute partitions across all executors.
    */
  def PreferConsistent: LocationStrategy =
  org.apache.rocketmq.spark.PreferConsistent

  /**
    * Use this to place particular TopicQueueIds on particular hosts if your load is uneven.
    * Any TopicQueueId not specified in the map will use a consistent location.
    */
  def PreferFixed(hostMap: collection.Map[TopicQueueId, String]): LocationStrategy =
  new PreferFixed(new ju.HashMap[TopicQueueId, String](hostMap.asJava))

  /**
    * Use this to place particular TopicQueueIds on particular hosts if your load is uneven.
    * Any TopicQueueId not specified in the map will use a consistent location.
    */
  def PreferFixed(hostMap: ju.Map[TopicQueueId, String]): LocationStrategy =
  new PreferFixed(hostMap)
}
