/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.samples.springboot.consumer;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.starter.annotation.RocketMQTransactionListener;

/**
 * Nagitive testing to tell @RocketMQTransactionListener can not be used on consumer side!!!
 * You can try this after uncomment the annotation declaration.
 *
 */
//@RocketMQTransactionListener
public class Checker implements TransactionListener  {
  @Override
  public LocalTransactionState executeLocalTransaction(Message message, Object o) {
    return null;
  }

  @Override
  public LocalTransactionState checkLocalTransaction(MessageExt messageExt) {
    return null;
  }
}
