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

package org.apache.rocketmq.connect.activemq;

import java.lang.reflect.Field;

import javax.jms.Message;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.rocketmq.connect.activemq.pattern.PatternProcessor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.junit.Assert;

public class ReplicatorTest {

    Replicator replicator;

    PatternProcessor patternProcessor;

    Config config;

    @Before
    public void before() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        config = new Config();
        replicator = new Replicator(config);

        patternProcessor = Mockito.mock(PatternProcessor.class);

        Field processor = Replicator.class.getDeclaredField("processor");
        processor.setAccessible(true);
        processor.set(replicator, patternProcessor);
    }

    @Test(expected = RuntimeException.class)
    public void startTest() throws Exception {
        replicator.start();
    }

    @Test
    public void stop() throws Exception {
        replicator.stop();
        Mockito.verify(patternProcessor, Mockito.times(1)).stop();
    }

    @Test
    public void commitAddGetQueueTest() {
        Message message = new ActiveMQTextMessage();
        replicator.commit(message, false);
        Assert.assertEquals(replicator.getQueue().poll(), message);
    }

    @Test
    public void getConfigTest() {
        Assert.assertEquals(replicator.getConfig(), config);
    }
}
