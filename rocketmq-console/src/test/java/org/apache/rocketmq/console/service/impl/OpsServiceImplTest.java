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

import java.util.List;
import javax.annotation.Resource;
import org.apache.rocketmq.console.service.OpsService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.apache.rocketmq.console.testbase.TestConstant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class OpsServiceImplTest extends RocketMQConsoleTestBase {
    @Resource
    private OpsService opsService;

    @SuppressWarnings("unchecked")
    @Test
    public void homePageInfo() throws Exception {
        List<String> namesvrAddrList= (List<String>)opsService.homePageInfo().get("namesvrAddrList");
        Assert.assertTrue(namesvrAddrList.contains(TestConstant.NAME_SERVER_ADDRESS));
    }

    @Test
    public void updateNameSvrAddrList() throws Exception {
        String testChangeNameSvrAddr = "110.110.100.110:1234";
        opsService.updateNameSvrAddrList(testChangeNameSvrAddr);
        Assert.assertEquals(opsService.getNameSvrList(),testChangeNameSvrAddr);

        opsService.updateNameSvrAddrList(TestConstant.NAME_SERVER_ADDRESS);
    }

    @Test
    public void getNameSvrList() throws Exception {
        Assert.assertEquals(opsService.getNameSvrList(),TestConstant.NAME_SERVER_ADDRESS);
    }

    @Test
    public void rocketMqStatusCheck() throws Exception {
        // need enhance in milestone 2
        opsService.rocketMqStatusCheck();
    }

}