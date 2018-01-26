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

package org.apache.rocketmq.spring.starter.core;

/**
 * Constants Created by aqlu on 2017/11/16.
 */
public final class DefaultRocketMQListenerContainerConstants {
    public static final String PROP_NAMESERVER = "nameServer";
    public static final String PROP_TOPIC = "topic";
    public static final String PROP_CONSUMER_GROUP = "consumerGroup";
    public static final String PROP_CONSUME_MODE = "consumeMode";
    public static final String PROP_CONSUME_THREAD_MAX = "consumeThreadMax";
    public static final String PROP_MESSAGE_MODEL = "messageModel";
    public static final String PROP_SELECTOR_EXPRESS = "selectorExpress";
    public static final String PROP_SELECTOR_TYPE = "selectorType";
    public static final String PROP_ROCKETMQ_LISTENER = "rocketMQListener";
    public static final String PROP_OBJECT_MAPPER = "objectMapper";
    public static final String METHOD_DESTROY = "destroy";
}
