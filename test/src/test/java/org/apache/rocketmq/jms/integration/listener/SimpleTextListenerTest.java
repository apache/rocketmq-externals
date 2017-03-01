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

package org.apache.rocketmq.jms.integration.listener;

import org.apache.commons.lang.time.StopWatch;
import org.apache.rocketmq.jms.integration.AppConfig;
import org.apache.rocketmq.jms.integration.support.ConditionMatcher;
import org.apache.rocketmq.jms.integration.support.TimeLimitAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.rocketmq.jms.integration.listener.SimpleTextListener.DESTINATION;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class SimpleTextListenerTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleTextListenerTest.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private SimpleTextListener simpleTextListener;

    @Test
    public void testListener() throws Exception {
        jmsTemplate.convertAndSend(DESTINATION, "first");
        StopWatch watch = new StopWatch();
        watch.start();

        TimeLimitAssert.doAssert(new ConditionMatcher() {
            @Override public boolean match() {
                return simpleTextListener.getReceivedMsg().size() == 1;
            }
        }, 60);
    }
}
