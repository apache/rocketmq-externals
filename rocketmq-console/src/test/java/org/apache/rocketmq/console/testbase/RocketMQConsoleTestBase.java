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
import java.util.List;
import javax.annotation.Resource;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.subscription.SubscriptionGroupConfig;
import org.apache.rocketmq.console.model.request.ConsumerConfigInfo;
import org.apache.rocketmq.console.model.request.TopicConfigInfo;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackageClasses = {TestRocketMQServer.class})
public abstract class RocketMQConsoleTestBase {
    private Logger consoleTestBaseLog = LoggerFactory.getLogger(RocketMQConsoleTestBase.class);
    protected static final int RETRY_QUEUE_NUMS = 2;
    protected static final int WRITE_QUEUE_NUM = 16;
    protected static final int READ_QUEUE_NUM = 16;
    protected static final int PERM = 6;
    protected static final String TEST_CONSUMER_GROUP = "CONSOLE_TEST_CONSUMER_GROUP";
    protected static final String TEST_PRODUCER_GROUP = "CONSOLE_TEST_PRODUCER_GROUP";
    protected static final String TEST_CREATE_DELETE_CONSUMER_GROUP = "CREATE_DELETE_CONSUMER_GROUP";
    protected static final String TEST_CREATE_DELETE_TOPIC = "CREATE_DELETE_TOPIC";
    protected static final String TEST_TOPIC_MESSAGE_BODY = "hello world";
    protected static final String TEST_TOPIC_MESSAGE_KEY = "TEST_TOPIC_KEY";
    protected static final String TEST_CONSOLE_TOPIC = "TEST_CONSOLE_TOPIC";
    @Resource
    protected ConsumerService consumerService;
    protected ConsumerConfigInfo consumerConfigInfo = new ConsumerConfigInfo();

    private DefaultMQPushConsumer consumer;
    private DefaultMQProducer producer;

    @Resource
    protected TopicService topicService;

    public static abstract class RetryTempLate<T> {
        protected abstract T process() throws Exception;
        public T execute(int times, long waitTime) throws Exception {
            Exception exception = null;
            for (int i = 0; i < times; i++) {
                try {
                    return process();
                }
                catch (Exception ignore) {
                    exception = ignore;
                    if (waitTime > 0) {
                        Thread.sleep(waitTime);
                    }
                }
            }
            throw Throwables.propagate(exception);
        }
    }



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

    protected SendResult sendTestTopicMessage() throws InterruptedException {
        if (producer == null) {
            startTestMQProducer();
        }

        Message message = new Message(TEST_CONSOLE_TOPIC, TEST_TOPIC_MESSAGE_BODY.getBytes());
        message.setKeys(Lists.<String>newArrayList(TEST_TOPIC_MESSAGE_KEY));
        for (int i = 0; i < 3; i++) {
            try {
                return producer.send(message);
            }
            catch (Exception ignore) {
                Thread.sleep(500);
            }
        }
        throw new RuntimeException("send message error");
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
            consumer.subscribe(TEST_CONSOLE_TOPIC, "*");

            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                    ConsumeConcurrentlyContext context) {
                    consoleTestBaseLog.info("op=consumeMessage message={}", JsonUtil.obj2String(msgs));
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
        topicConfigInfo.setTopicName(TEST_CONSOLE_TOPIC);
        topicConfigInfo.setWriteQueueNums(WRITE_QUEUE_NUM);
        topicConfigInfo.setReadQueueNums(READ_QUEUE_NUM);
        topicConfigInfo.setPerm(PERM);
        topicService.createOrUpdate(topicConfigInfo);
    }

    protected void initMQClientEnv() {
        startTestMQProducer();
        startTestMQConsumer();

    }

    protected void destroyMQClientEnv() {
        if (consumer != null) {
            consumer.shutdown();
        }
        if (producer != null) {
            producer.shutdown();
        }
    }
}
