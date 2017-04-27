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

import org.slf4j.{Logger, LoggerFactory}

/**
  * Utility trait for classes that want to log data.
  */
trait Logging {
  // Make the log field transient so that objects with Logging can
  // be serialized and used on another machine
  @transient private var log_ : Logger = null

  // Method to get or create the logger
  def log: Logger = {
    if (log_ == null)
      log_ = LoggerFactory.getLogger(this.getClass.getName.stripSuffix("$"))
    return log_
  }

  // Log methods that take only a String
  def logInfo(msg: => String) = if (log.isInfoEnabled) log.info(msg)

  def logDebug(msg: => String) = if (log.isDebugEnabled) log.debug(msg)

  def logWarning(msg: => String) = if (log.isWarnEnabled) log.warn(msg)

  def logError(msg: => String) = if (log.isErrorEnabled) log.error(msg)

  // Log methods that take Throwable (Exceptions/Errors) too
  def logInfo(msg: => String, throwable: Throwable) {
    if (log.isInfoEnabled) log.info(msg, throwable)
  }

  def logDebug(msg: => String, throwable: Throwable) {
    if (log.isDebugEnabled) log.debug(msg, throwable)
  }

  def logWarning(msg: => String, throwable: Throwable) =
    if (log.isWarnEnabled) log.warn(msg, throwable)

  def logError(msg: => String, throwable: Throwable) =
    if (log.isErrorEnabled) log.error(msg, throwable)
}
