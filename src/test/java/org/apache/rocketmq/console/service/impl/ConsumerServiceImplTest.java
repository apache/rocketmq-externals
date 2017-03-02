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
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.model.GroupConsumeInfo;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.DeleteSubGroupRequest;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.testbase.TestConstant;
import org.apache.rocketmq.console.testbase.TestRocketMQServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ComponentScan(basePackageClasses = {TestRocketMQServer.class})
@DirtiesContext
public class ConsumerServiceImplTest {

    @Resource
    private ConsumerService consumerService;
    private static final ConsumerConfigInfo consumerConfigInfo = new ConsumerConfigInfo();
    private static final int RETRY_QUEUE_NUMS = 2;
    private static final String TEST_CONSUMER_GROUP = "CONSOLE_TEST_CONSUMER_GROUP";
    private static final String TEST_CREATE_DELETE_CONSUMER_GROUP = "CREATE_DELETE_CONSUMER_GROUP";

    private DefaultMQPushConsumer consumer;

    @Before
    public void setUp() throws Exception {
        consumer = new DefaultMQPushConsumer(TEST_CONSUMER_GROUP); //test online consumer

        consumerConfigInfo.setBrokerNameList(Lists.newArrayList(TestConstant.TEST_BROKER_NAME));
        SubscriptionGroupConfig subscriptionGroupConfig = new SubscriptionGroupConfig();
        subscriptionGroupConfig.setGroupName(TEST_CONSUMER_GROUP);
        subscriptionGroupConfig.setRetryQueueNums(RETRY_QUEUE_NUMS);
        consumerConfigInfo.setSubscriptionGroupConfig(subscriptionGroupConfig);
        consumerService.createAndUpdateSubscriptionGroupConfig(consumerConfigInfo);

        consumer.setNamesrvAddr(TestConstant.NAME_SERVER_ADDRESS);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        consumer.subscribe("CONSOLE_TEST_CONSUMER_GROUP_NO_TOPIC", "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                ConsumeConcurrentlyContext context) {
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }
    @After
    public void tearDown() throws Exception {
        consumer.shutdown();
    }



    @Test
    public void queryGroupList() throws Exception {
        List<GroupConsumeInfo> consumeInfoList =  consumerService.queryGroupList();
        Assert.assertTrue(CollectionUtils.isNotEmpty(consumeInfoList));
    }

    @Test
    public void queryGroup() throws Exception {
        GroupConsumeInfo consumeInfo =  consumerService.queryGroup(TEST_CONSUMER_GROUP);
        Assert.assertNotNull(consumeInfo);
        Assert.assertEquals(consumeInfo.getGroup(),TEST_CONSUMER_GROUP);
        // todo  mqAdminExt.examineConsumerConnectionInfo(consumerGroup) can't use if don't consume a message
//        Assert.assertTrue(consumeInfo.getCount() == 1);
    }

    @Test
    public void queryConsumeStatsListByGroupName() throws Exception {
        // todo can't use if don't consume a message
//        List<TopicConsumerInfo> topicConsumerInfoList = consumerService.queryConsumeStatsListByGroupName(TEST_CONSUMER_GROUP);
    }

    @Test
    public void queryConsumeStatsList() throws Exception {
//        consumerService.queryConsumeStatsList()
    }

    @Test
    public void queryConsumeStatsListByTopicName() throws Exception {

    }

    @Test
    public void resetOffset() throws Exception {

    }

    @Test
    public void examineSubscriptionGroupConfig() throws Exception {
        List<ConsumerConfigInfo> configInfoList= consumerService.examineSubscriptionGroupConfig(TEST_CONSUMER_GROUP);
        Assert.assertTrue(configInfoList.size()==1);
        Assert.assertTrue(configInfoList.get(0).getSubscriptionGroupConfig().getGroupName().equals(TEST_CONSUMER_GROUP));
        Assert.assertTrue(configInfoList.get(0).getSubscriptionGroupConfig().getRetryQueueNums()==RETRY_QUEUE_NUMS);

    }

    @Test
    public void deleteSubGroup() throws Exception {

        createAndUpdateSubscriptionGroupConfig();
        DeleteSubGroupRequest deleteSubGroupRequest = new DeleteSubGroupRequest();
        deleteSubGroupRequest.setBrokerNameList(Lists.<String>newArrayList(TestConstant.TEST_BROKER_NAME));
        deleteSubGroupRequest.setGroupName(TEST_CREATE_DELETE_CONSUMER_GROUP);
        Assert.assertTrue(consumerService.deleteSubGroup(deleteSubGroupRequest));
        List<ConsumerConfigInfo> groupConsumeInfoList =  consumerService.examineSubscriptionGroupConfig(TEST_CREATE_DELETE_CONSUMER_GROUP);
        Assert.assertTrue(CollectionUtils.isEmpty(groupConsumeInfoList));
    }

    @Test
    public void createAndUpdateSubscriptionGroupConfig() throws Exception {
        ConsumerConfigInfo consumerConfigInfoForCreate = new ConsumerConfigInfo();
        BeanUtils.copyProperties(consumerConfigInfo,consumerConfigInfoForCreate);
        consumerConfigInfoForCreate.getSubscriptionGroupConfig().setGroupName(TEST_CREATE_DELETE_CONSUMER_GROUP);
        Assert.assertTrue(consumerService.createAndUpdateSubscriptionGroupConfig(consumerConfigInfoForCreate));
        Assert.assertTrue(CollectionUtils.isNotEmpty(consumerService.examineSubscriptionGroupConfig(TEST_CREATE_DELETE_CONSUMER_GROUP)));

    }

    @Test
    public void fetchBrokerNameSetBySubscriptionGroup() throws Exception {

    }

    @Test
    public void getConsumerConnection() throws Exception {

    }

}