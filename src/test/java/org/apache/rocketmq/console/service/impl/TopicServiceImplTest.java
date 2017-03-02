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

import com.google.common.collect.Lists;
import javax.annotation.Resource;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.testbase.RocketMQConsoleTestBase;
import org.apache.rocketmq.console.testbase.TestConstant;
import org.apache.rocketmq.console.util.JsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext
public class TopicServiceImplTest extends RocketMQConsoleTestBase {
    private static final int WRITE_QUEUE_NUM = 1;
    private static final int READ_QUEUE_NUM = 2;
    private static final int PERM = 6;

    @Resource
    private TopicService topicService;

    @Test
    public void fetchAllTopicList() throws Exception {
        //TODO need time to regist
        TopicList topicList = topicService.fetchAllTopicList();
        System.out.println(JsonUtil.obj2String(topicList));

//        Assert.assertNotNull(topicList);
//        Assert.assertTrue(CollectionUtils.isNotEmpty(topicList.getTopicList()));
//        System.out.println(topicList.getBrokerAddr());
//        Assert.assertTrue();
    }

    @Test
    public void stats() throws Exception {
//        topicService.stats();
    }

    @Test
    public void route() throws Exception {

    }

    @Test
    public void queryTopicConsumerInfo() throws Exception {

    }

    @Test
    public void createOrUpdate() throws Exception {
        TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
        topicConfigInfo.setBrokerNameList(Lists.newArrayList(TestConstant.TEST_BROKER_NAME));
        topicConfigInfo.setTopicName(TestConstant.TEST_CONSOLE_TOPIC);
        topicService.createOrUpdate(topicConfigInfo);
        TopicList topicList = topicService.fetchAllTopicList();
        System.out.println(JsonUtil.obj2String(topicList));
    }

    @Test
    public void examineTopicConfig() throws Exception {

    }

    @Test
    public void examineTopicConfigList() throws Exception {

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