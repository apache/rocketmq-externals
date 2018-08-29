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

package org.apache.rocketmq.spring.starter.annotation;

import org.apache.rocketmq.spring.starter.RocketMQConfigUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * This annotation is used over a class which implements interface
 * org.apache.rocketmq.client.producer.TransactionListener. The class implements
 * two methods for callback of sending transactional message.
 *
 * <p>Note, the annotation is used only on RocketMQ client producer side, it can not be used
 * on consumer side.
 *
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RocketMQTransactionListener {

    /**
     *  The unique identifier of the transaction, a different transName will create a new TransactionProducer instance.
     *  <p>It is suggested to use the default transName if your system only needs to define a TransactionListener class.
     */
    String transName() default RocketMQConfigUtils.ROCKET_MQ_TRANSACTION_DEFAULT_GLOBAL_NAME;

    /**
     * Set ExecutorService params -- corePoolSize
     */
    int corePoolSize() default 1;
    /**
     * Set ExecutorService params -- maximumPoolSize
     */
    int maximumPoolSize() default 1;
    /**
     * Set ExecutorService params -- keepAliveTime
     */
    long keepAliveTime() default 1000*60; //60ms
    /**
     * Set ExecutorService params -- blockingQueueSize
     */
    int blockingQueueSize() default 2000;
}
