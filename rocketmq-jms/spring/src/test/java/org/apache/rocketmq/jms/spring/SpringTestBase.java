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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.jms.spring;

import org.apache.rocketmq.jms.domain.CommonConstant;
import org.apache.rocketmq.jms.integration.IntegrationTestBase;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringTestBase extends IntegrationTestBase{

    protected final static ClassPathXmlApplicationContext produceContext;
    protected final static ClassPathXmlApplicationContext consumeContext;

    static {
        String rmqJmsUrl = String.format("rocketmq://xxx?%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
            CommonConstant.PRODUCERID, producerId,
            CommonConstant.CONSUMERID, consumerId,
            CommonConstant.NAMESERVER, nameServer,
            CommonConstant.CONSUME_THREAD_NUMS, consumeThreadNums,
            CommonConstant.SEND_TIMEOUT_MILLIS, 10*1000,
            CommonConstant.INSTANCE_NAME, "JMS_TEST");
        System.setProperty("RMQ_JMS_URL", rmqJmsUrl);
        produceContext = new ClassPathXmlApplicationContext("classpath:producer.xml");
        consumeContext = new ClassPathXmlApplicationContext("classpath:consumer.xml");
    }
}
