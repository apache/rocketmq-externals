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
package org.apache.rocketmq.console.testbase;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.TopicService;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.Resource;
import java.util.List;

@ComponentScan(basePackageClasses = {TestRocketMQServer.class})
public abstract class RocketMQConsoleTestBase {
    protected static final int RETRY_QUEUE_NUMS = 2;
    protected static final int WRITE_QUEUE_NUM = 1;
    protected static final int READ_QUEUE_NUM = 2;
    protected static final int PERM = 6;
    protected static final String TEST_CONSUMER_GROUP = "CONSOLE_TEST_CONSUMER_GROUP";
    protected static final String TEST_PRODUCER_GROUP = "CONSOLE_TEST_PRODUCER_GROUP";
    protected static final String TEST_CREATE_DELETE_CONSUMER_GROUP = "CREATE_DELETE_CONSUMER_GROUP";
    protected static final String TEST_CREATE_DELETE_TOPIC = "CREATE_DELETE_TOPIC";
    @Resource
    protected ConsumerService consumerService;
    protected ConsumerConfigInfo consumerConfigInfo = new ConsumerConfigInfo();

    private DefaultMQPushConsumer consumer;
    private DefaultMQProducer producer;

    @Resource
    protected TopicService topicService;


    protected void startTestMQProducer() {
        producer = new DefaultMQProducer(TEST_PRODUCER_GROUP);
        producer.setInstanceName(String.valueOf(System.currentTimeMillis()));
        try {
            producer.start();
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    protected void startTestMQConsumer() {
        consumer = new DefaultMQPushConsumer(TEST_CONSUMER_GROUP); //test online consumer
        consumerConfigInfo.setBrokerNameList(Lists.newArrayList(TestConstant.TEST_BROKER_NAME));
        SubscriptionGroupConfig subscriptionGroupConfig = new SubscriptionGroupConfig();
        subscriptionGroupConfig.setGroupName(TEST_CONSUMER_GROUP);
        subscriptionGroupConfig.setRetryQueueNums(RETRY_QUEUE_NUMS);
        consumerConfigInfo.setSubscriptionGroupConfig(subscriptionGroupConfig);
        consumerService.createAndUpdateSubscriptionGroupConfig(consumerConfigInfo);
        consumer.setNamesrvAddr(TestConstant.NAME_SERVER_ADDRESS);
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        try {
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
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    protected void registerTestMQTopic() {
        TopicConfigInfo topicConfigInfo = new TopicConfigInfo();
        topicConfigInfo.setBrokerNameList(Lists.newArrayList(TestConstant.TEST_BROKER_NAME));
        topicConfigInfo.setTopicName(TestConstant.TEST_CONSOLE_TOPIC);
        topicConfigInfo.setWriteQueueNums(WRITE_QUEUE_NUM);
        topicConfigInfo.setReadQueueNums(READ_QUEUE_NUM);
        topicConfigInfo.setPerm(PERM);
        topicService.createOrUpdate(topicConfigInfo);
    }

    protected void initMQClientEnv() {
        startTestMQConsumer();
        startTestMQProducer();
    }

    protected void destroyMQClientEnv() {
        if (consumer != null) {
            consumer.shutdown();
        }
        if (producer != null) {
            producer.shutdown();
        }
        if (topicService.fetchAllTopicList().getTopicList().contains(TestConstant.TEST_CONSOLE_TOPIC)) {
            topicService.deleteTopic(TestConstant.TEST_CONSOLE_TOPIC);
        }
    }
}
