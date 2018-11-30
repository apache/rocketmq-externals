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
package org.apache.rocketmq.spring.support;


import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.Assert.assertTrue;

public class RocketMQUtilTest {
    @Test
    public void testMessageBuilder() {
        Message msg = MessageBuilder.withPayload("test").
            setHeader("A", "test1").
            setHeader("B", "test2").
            build();
        System.out.printf("header size=%d  %s %n", msg.getHeaders().size(), msg.getHeaders().toString());
        assertTrue(msg.getHeaders().get("A") != null);
        assertTrue(msg.getHeaders().get("B") != null);
    }
}