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

package org.apache.rocketmq.console.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import javax.annotation.Resource;
import org.apache.rocketmq.common.protocol.body.Connection;
import org.apache.rocketmq.common.protocol.body.ProducerConnection;
import org.apache.rocketmq.console.service.ProducerService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.apache.rocketmq.console.testbase.TestConstant;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class ProducerServiceImplTest extends RocketMQConsoleTestBase {
    @Resource
    private ProducerService producerService;
    @Before
    public void setUp() throws Exception {
        initMQClientEnv();
        registerTestMQTopic();
        sendTestTopicMessage().getMsgId();
    }

    @After
    public void tearDown() throws Exception {
        destroyMQClientEnv();
    }

    @Test
    public void getProducerConnection() throws Exception {
        ProducerConnection producerConnection=new RetryTempLate<ProducerConnection>() {
            @Override protected ProducerConnection process() throws Exception {
                return  producerService.getProducerConnection(TEST_PRODUCER_GROUP,TEST_CONSOLE_TOPIC);
            }
        }.execute(10,1000);
        Assert.assertNotNull(producerConnection);
        Assert.assertTrue(Lists.transform(Lists.newArrayList(producerConnection.getConnectionSet()), new Function<Connection, String>() {
            @Override public String apply(Connection input) {
                return input.getClientAddr().split(":")[0];
            }
        }).contains(TestConstant.LOCAL_HOST));

    }

}