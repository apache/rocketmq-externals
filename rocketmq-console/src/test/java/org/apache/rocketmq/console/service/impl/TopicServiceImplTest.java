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
import java.util.List;
import javax.annotation.Resource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.admin.TopicStatsTable;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.common.protocol.route.TopicRouteData;
import org.apache.rocketmq.console.model.request.SendTopicMessageRequest;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.TopicService;
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
public class TopicServiceImplTest extends RocketMQConsoleTestBase {

    @Resource
    private TopicService topicService;

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
    public void fetchAllTopicList() throws Exception {
        TopicList topicList = topicService.fetchAllTopicList(true);
        Assert.assertNotNull(topicList);
        Assert.assertTrue(CollectionUtils.isNotEmpty(topicList.getTopicList()));
        Assert.assertTrue(topicList.getTopicList().contains(TEST_CONSOLE_TOPIC));
    }

    @Test
    public void stats() throws Exception {
        TopicStatsTable topicStatsTable = topicService.stats(TEST_CONSOLE_TOPIC);
        Assert.assertNotNull(topicStatsTable );
        Assert.assertEquals(topicStatsTable.getOffsetTable().size(),READ_QUEUE_NUM);
    }

    @Test
    public void route() throws Exception {
        TopicRouteData topicRouteData = topicService.route(TEST_CONSOLE_TOPIC);
        Assert.assertNotNull(topicRouteData);
        Assert.assertEquals(topicRouteData.getBrokerDatas().get(0).getBrokerAddrs().get(MixAll.MASTER_ID),TestConstant.BROKER_ADDRESS);
        Assert.assertTrue(CollectionUtils.isNotEmpty(topicRouteData.getQueueDatas()));
    }

    @Test
    public void queryTopicConsumerInfo() throws Exception {
//        GroupList groupList = null; // todo
//        for(int i=0;i<20;i++){
//            sendTestTopicMessage();
//        }
//        for (int i = 0; i < 20; i++) {
//            groupList = topicService.queryTopicConsumerInfo(TEST_CONSOLE_TOPIC);
//            if (CollectionUtils.isNotEmpty(groupList.getGroupList())) {
//                break;
//            }
//            Thread.sleep(1000);
//        }
    }

    @Test
    public void createOrUpdate() throws Exception {
        TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
        topicConfigInfo.setBrokerNameList(Lists.newArrayList(TestConstant.TEST_BROKER_NAME));
        topicConfigInfo.setTopicName(TEST_CREATE_DELETE_TOPIC);
        topicService.createOrUpdate(topicConfigInfo);

        TopicList topicList = topicService.fetchAllTopicList(true);

        Assert.assertNotNull(topicList);
        Assert.assertTrue(CollectionUtils.isNotEmpty(topicList.getTopicList()));
        Assert.assertTrue(topicList.getTopicList().contains(TEST_CREATE_DELETE_TOPIC));
    }

    @Test
    public void examineTopicConfig() throws Exception {
        List<TopicConfigInfo> topicConfigInfoList = topicService.examineTopicConfig(TEST_CONSOLE_TOPIC);
        Assert.assertTrue(CollectionUtils.isNotEmpty(topicConfigInfoList));
    }

    @Test
    public void examineTopicConfigList() throws Exception {
        TopicConfig topicConfig = topicService.examineTopicConfig(TEST_CONSOLE_TOPIC,TestConstant.TEST_BROKER_NAME);
        Assert.assertNotNull(topicConfig);
        Assert.assertEquals(topicConfig.getReadQueueNums(),READ_QUEUE_NUM);
        Assert.assertEquals(topicConfig.getWriteQueueNums(),WRITE_QUEUE_NUM);
    }

    @Test(expected = RuntimeException.class)
    public void deleteTopic() throws Exception {
        Assert.assertTrue(topicService.deleteTopic(TEST_CONSOLE_TOPIC));
        topicService.examineTopicConfig(TEST_CONSOLE_TOPIC);
    }

    @Test(expected = RuntimeException.class)
    public void deleteTopic1() throws Exception {
        Assert.assertTrue(topicService.deleteTopic(TEST_CONSOLE_TOPIC,TestConstant.TEST_CLUSTER_NAME));
        topicService.examineTopicConfig(TEST_CONSOLE_TOPIC);
    }

    @Test
    public void deleteTopicInBroker() throws Exception {
        Assert.assertTrue(topicService.deleteTopic(TestConstant.TEST_BROKER_NAME,TEST_CONSOLE_TOPIC));
    }

    @Test
    public void sendTopicMessageRequest() throws Exception {
        SendTopicMessageRequest sendTopicMessageRequest = new SendTopicMessageRequest();
        sendTopicMessageRequest.setTopic(TEST_CONSOLE_TOPIC);
        sendTopicMessageRequest.setMessageBody("sendTopicMessageRequestMessageBody");
        sendTopicMessageRequest.setKey("sendTopicMessageRequestKey");
        sendTopicMessageRequest.setTag("sendTopicMessageRequestTag");

        SendResult sendResult= topicService.sendTopicMessageRequest(sendTopicMessageRequest);
        Assert.assertNotNull(sendResult);
        Assert.assertTrue(StringUtils.isNoneBlank(sendResult.getMsgId()));
    }

}