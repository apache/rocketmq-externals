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

import javax.annotation.Resource;
import org.apache.rocketmq.console.service.MonitorService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class MonitorServiceImplTest extends RocketMQConsoleTestBase {
    @Resource
    private MonitorService monitorService;
    @Test
    public void createOrUpdateConsumerMonitor() throws Exception {
//        monitorService.createOrUpdateConsumerMonitor()
    }

    @Test
    public void queryConsumerMonitorConfig() throws Exception {

    }

    @Test
    public void queryConsumerMonitorConfigByGroupName() throws Exception {

    }

    @Test
    public void deleteConsumerMonitor() throws Exception {

    }

}