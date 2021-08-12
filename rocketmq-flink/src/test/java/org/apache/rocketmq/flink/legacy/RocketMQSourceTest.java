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

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.MQPullConsumerScheduleService;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.flink.legacy.common.serialization.KeyValueDeserializationSchema;
import org.apache.rocketmq.flink.legacy.common.serialization.SimpleKeyValueDeserializationSchema;

import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.rocketmq.flink.legacy.common.util.TestUtils.setFieldValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Ignore
public class RocketMQSourceTest {

    private RocketMQSourceFunction rocketMQSource;
    private MQPullConsumerScheduleService pullConsumerScheduleService;
    private DefaultMQPullConsumer consumer;
    private KeyValueDeserializationSchema deserializationSchema;
    private String topic = "tpc";

    @Before
    public void setUp() throws Exception {
        deserializationSchema = new SimpleKeyValueDeserializationSchema();
        Properties props = new Properties();
        rocketMQSource = new RocketMQSourceFunction(deserializationSchema, props);

        setFieldValue(rocketMQSource, "topic", topic);
        setFieldValue(rocketMQSource, "runningChecker", new SingleRunningCheck());
        setFieldValue(rocketMQSource, "offsetTable", new ConcurrentHashMap<>());
        setFieldValue(rocketMQSource, "restoredOffsets", new ConcurrentHashMap<>());

        pullConsumerScheduleService = new MQPullConsumerScheduleService("g");

        consumer = mock(DefaultMQPullConsumer.class);
        pullConsumerScheduleService.setDefaultMQPullConsumer(consumer);
        setFieldValue(rocketMQSource, "consumer", consumer);
        setFieldValue(rocketMQSource, "pullConsumerScheduleService", pullConsumerScheduleService);
    }

    @Test
    public void testSource() throws Exception {
        List<MessageExt> msgFoundList = new ArrayList<>();
        MessageExt messageExt = new MessageExt();
        messageExt.setKeys("keys");
        messageExt.setBody("body data".getBytes());
        messageExt.setBornTimestamp(System.currentTimeMillis());
        msgFoundList.add(messageExt);
        PullResult pullResult = new PullResult(PullStatus.FOUND, 3, 1, 5, msgFoundList);

        when(consumer.fetchConsumeOffset(any(MessageQueue.class), anyBoolean())).thenReturn(2L);
        when(consumer.pull(any(MessageQueue.class), anyString(), anyLong(), anyInt()))
                .thenReturn(pullResult);

        SourceContext context = mock(SourceContext.class);
        when(context.getCheckpointLock()).thenReturn(new Object());

        rocketMQSource.run(context);

        // schedule the pull task
        Set<MessageQueue> set = new HashSet();
        set.add(new MessageQueue(topic, "brk", 1));
        pullConsumerScheduleService.putTask(topic, set);

        MessageExt msg = pullResult.getMsgFoundList().get(0);

        // atLeastOnce: re-pulling immediately when messages found before
        verify(context, atLeastOnce()).collectWithTimestamp(msg, msg.getBornTimestamp());
    }

    @Test
    public void close() throws Exception {
        rocketMQSource.close();

        verify(consumer).shutdown();
    }

    class SingleRunningCheck extends RunningChecker {
        @Override
        public boolean isRunning() {
            return false;
        }
    }
}
