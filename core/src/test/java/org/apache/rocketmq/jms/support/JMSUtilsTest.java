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

package org.apache.rocketmq.jms.support;

import org.apache.rocketmq.jms.RocketMQQueue;
import org.apache.rocketmq.jms.RocketMQTopic;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class JMSUtilsTest {

    @Test
    public void getTopicName() throws Exception {
        RocketMQTopic topic = new RocketMQTopic("topic");
        assertThat(JMSUtils.getDestinationName(topic), is("topic"));

        RocketMQQueue queue = new RocketMQQueue("queue");
        assertThat(JMSUtils.getDestinationName(queue), is("queue"));
    }

    @Test
    public void uuid() throws Exception {
        assertThat(JMSUtils.uuid(), notNullValue());
    }

}