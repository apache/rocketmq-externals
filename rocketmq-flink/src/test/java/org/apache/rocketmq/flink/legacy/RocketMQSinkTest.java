/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.flink.legacy;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.flink.legacy.common.selector.DefaultTopicSelector;
import org.apache.rocketmq.flink.legacy.common.selector.TopicSelector;
import org.apache.rocketmq.flink.legacy.common.serialization.KeyValueSerializationSchema;
import org.apache.rocketmq.flink.legacy.common.serialization.SimpleKeyValueSerializationSchema;

import org.apache.flink.api.java.tuple.Tuple2;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.apache.rocketmq.flink.legacy.common.util.TestUtils.setFieldValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Ignore
public class RocketMQSinkTest {

    private RocketMQSink rocketMQSink;
    private DefaultMQProducer producer;

    @Before
    public void setUp() throws Exception {
        KeyValueSerializationSchema serializationSchema =
                new SimpleKeyValueSerializationSchema("id", "name");
        TopicSelector topicSelector = new DefaultTopicSelector("tpc");
        Properties props = new Properties();
        props.setProperty(
                RocketMQConfig.MSG_DELAY_LEVEL, String.valueOf(RocketMQConfig.MSG_DELAY_LEVEL04));
        rocketMQSink = new RocketMQSink(props);

        producer = mock(DefaultMQProducer.class);
        setFieldValue(rocketMQSink, "producer", producer);
    }

    @Test
    public void testSink() throws Exception {
        Tuple2<String, String> tuple = new Tuple2<>("id", "province");
        String topic = "testTopic";
        String tag = "testTag";
        Message message = new Message(topic, tag, tuple.f0, tuple.f1.getBytes());
    }

    @Test
    public void close() throws Exception {
        rocketMQSink.close();

        verify(producer).shutdown();
    }
}
