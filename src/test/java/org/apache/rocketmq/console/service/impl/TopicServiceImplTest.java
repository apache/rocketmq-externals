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

import org.apache.rocketmq.console.testbase.TestRocketMQServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ComponentScan(basePackageClasses = {TestRocketMQServer.class})
@DirtiesContext
public class TopicServiceImplTest {
    private static final int WRITE_QUEUE_NUM = 1;
    private static final int READ_QUEUE_NUM = 2;
    private static final int PERM =6;

    @Test
    public void fetchAllTopicList() throws Exception {

    }

    @Test
    public void stats() throws Exception {

    }

    @Test
    public void route() throws Exception {

    }

    @Test
    public void queryTopicConsumerInfo() throws Exception {

    }

    @Test
    public void createOrUpdate() throws Exception {

    }

    @Test
    public void examineTopicConfig() throws Exception {

    }

    @Test
    public void examineTopicConfig1() throws Exception {

    }

    @Test
    public void deleteTopic() throws Exception {

    }

    @Test
    public void deleteTopic1() throws Exception {

    }

    @Test
    public void deleteTopicInBroker() throws Exception {

    }

    @Test
    public void sendTopicMessageRequest() throws Exception {

    }

}