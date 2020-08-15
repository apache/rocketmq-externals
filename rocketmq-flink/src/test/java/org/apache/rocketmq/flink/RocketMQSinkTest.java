/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.flink.common.selector.DefaultTopicSelector;
import org.apache.rocketmq.flink.common.selector.TopicSelector;
import org.apache.rocketmq.flink.common.serialization.KeyValueSerializationSchema;
import org.apache.rocketmq.flink.common.serialization.SimpleKeyValueSerializationSchema;
import org.junit.Before;
import org.junit.Test;

import static org.apache.rocketmq.flink.TestUtils.setFieldValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RocketMQSinkTest {

    private RocketMQSink rocketMQSink;
    private DefaultMQProducer producer;

    @Before
    public void setUp() throws Exception {
        KeyValueSerializationSchema serializationSchema = new SimpleKeyValueSerializationSchema("id", "name");
        TopicSelector topicSelector = new DefaultTopicSelector("tpc");
        Properties props = new Properties();
        props.setProperty(RocketMQConfig.MSG_DELAY_LEVEL,String.valueOf(RocketMQConfig.MSG_DELAY_LEVEL04));
        rocketMQSink = new RocketMQSink(serializationSchema, topicSelector, props);

        producer = mock(DefaultMQProducer.class);
        setFieldValue(rocketMQSink, "producer", producer);
    }

    @Test
    public void testSink() throws Exception {
        Map tuple = new HashMap();
        tuple.put("id", "x001");
        tuple.put("name", "vesense");
        tuple.put("tpc", "tpc1");

        rocketMQSink.invoke(tuple, null);

        verify(producer).send(any(Message.class));

    }

    @Test
    public void close() throws Exception {
        rocketMQSink.close();

        verify(producer).shutdown();
    }

}