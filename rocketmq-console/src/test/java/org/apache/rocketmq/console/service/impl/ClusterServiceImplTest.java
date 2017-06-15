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

import java.util.Map;
import java.util.Properties;
import javax.annotation.Resource;
import org.apache.rocketmq.common.protocol.body.ClusterInfo;
import org.apache.rocketmq.console.service.ClusterService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.apache.rocketmq.console.testbase.TestConstant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.rocketmq.console.testbase.TestConstant.BROKER_ADDRESS;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class ClusterServiceImplTest extends RocketMQConsoleTestBase {
    @Resource
    private ClusterService clusterService;

    @Test
    @SuppressWarnings("unchecked")
    public void list() throws Exception {
        Map<String, Object> clusterMap = clusterService.list();
        ClusterInfo clusterInfo = (ClusterInfo)clusterMap.get("clusterInfo");
        Map<String/*brokerName*/, Map<Long/* brokerId */, Object/* brokerDetail */>> brokerServerMap = (Map<String, Map<Long, Object>>)clusterMap.get("brokerServer");
        Assert.assertNotNull(clusterInfo);
        Assert.assertNotNull(brokerServerMap);
        Assert.assertNotNull(clusterInfo.getBrokerAddrTable().get(TestConstant.TEST_BROKER_NAME));
        Assert.assertNotNull(brokerServerMap.get(TestConstant.TEST_BROKER_NAME));
    }

    @Test
    public void getBrokerConfig() throws Exception {
        Properties properties = clusterService.getBrokerConfig(BROKER_ADDRESS);
        Assert.assertNotNull(properties);
    }

}