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

package org.apache.rocketmq.spring.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.starter.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.starter.core.DefaultRocketMQListenerContainer;
import org.apache.rocketmq.spring.starter.core.RocketMQListener;
import org.apache.rocketmq.spring.starter.core.RocketMQTemplate;
import org.apache.rocketmq.spring.starter.enums.ConsumeMode;
import org.apache.rocketmq.spring.starter.enums.SelectorType;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.starter.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RocketMQAutoConfigurationTests {

    private static final String TEST_CONSUMER_GROUP = "my_consumer";

    private static final String TEST_TOPIC = "test-topic";

    private AnnotationConfigApplicationContext context;

    @Test
    public void rocketMQTemplate() {

        load("spring.rocketmq.nameServer=127.0.0.1:9876",
            "spring.rocketmq.producer.group=my_group",
            "spring.rocketmq.producer.send-msg-timeout=30000",
            "spring.rocketmq.producer.retry-times-when-send-async-failed=1",
            "spring.rocketmq.producer.compress-msg-body-over-howmuch=1024",
            "spring.rocketmq.producer.max-message-size=10240",
            "spring.rocketmq.producer.retry-another-broker-when-not-store-ok=true",
            "spring.rocketmq.producer.retry-times-when-send-failed=1");

        assertThat(this.context.containsBean("rocketMQMessageObjectMapper")).isTrue();
        assertThat(this.context.containsBean("mqProducer")).isTrue();
        assertThat(this.context.containsBean("rocketMQTemplate")).isTrue();
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();

        RocketMQTemplate rocketMQTemplate = this.context.getBean(RocketMQTemplate.class);
        ObjectMapper objectMapper = this.context.getBean("rocketMQMessageObjectMapper", ObjectMapper.class);
        assertThat(rocketMQTemplate.getObjectMapper()).isEqualTo(objectMapper);

        DefaultMQProducer defaultMQProducer = rocketMQTemplate.getProducer();

        assertThat(defaultMQProducer.getNamesrvAddr()).isEqualTo("127.0.0.1:9876");
        assertThat(defaultMQProducer.getProducerGroup()).isEqualTo("my_group");
        assertThat(defaultMQProducer.getSendMsgTimeout()).isEqualTo(30000);
        assertThat(defaultMQProducer.getRetryTimesWhenSendAsyncFailed()).isEqualTo(1);
        assertThat(defaultMQProducer.getCompressMsgBodyOverHowmuch()).isEqualTo(1024);
        assertThat(defaultMQProducer.getMaxMessageSize()).isEqualTo(10240);
        assertThat(defaultMQProducer.isRetryAnotherBrokerWhenNotStoreOK()).isTrue();
        assertThat(defaultMQProducer.getRetryTimesWhenSendFailed()).isEqualTo(1);

        try {
            // create txProducer
            rocketMQTemplate.createAndStartTransactionMQProducer("test",
                new TransactionListener() {
                    @Override
                    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                        return LocalTransactionState.UNKNOW;
                    }

                    @Override
                    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                        return LocalTransactionState.COMMIT_MESSAGE;
                    }
                }, null);

            // send transactional message with the txProducer
            // test sending as follows when the nameserver and broker is started.
            //rocketMQTemplate.sendMessageInTransaction("test", new Message(TEST_TOPIC, "Hello".getBytes()), null);
        } catch (MQClientException e) {
            e.printStackTrace(System.out);
            fail("failed to create txProducer and send transactional msg!");
        }
    }

    @Test
    public void enableProducer() {
        load();
        assertThat(this.context.containsBean("mqProducer")).isFalse();
        assertThat(this.context.containsBean("rocketMQTemplate")).isFalse();
        closeContext();

        load("spring.rocketmq.nameServer=127.0.0.1:9876");
        assertThat(this.context.containsBean("mqProducer")).isFalse();
        assertThat(this.context.containsBean("rocketMQTemplate")).isFalse();
        closeContext();

        load("spring.rocketmq.producer.group=my_group");
        assertThat(this.context.containsBean("mqProducer")).isFalse();
        assertThat(this.context.containsBean("rocketMQTemplate")).isFalse();
        closeContext();

        load("spring.rocketmq.nameServer=127.0.0.1:9876", "spring.rocketmq.producer.group=my_group");
        assertThat(this.context.containsBean("mqProducer")).isTrue();
        assertThat(this.context.containsBean("rocketMQTemplate")).isEqualTo(true);
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();
    }

    @Test
    public void enableConsumer() {
        load();
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();
        closeContext();

        load("spring.rocketmq.nameServer=127.0.0.1:9876");
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();
        closeContext();

        load(false);
        this.context.registerBeanDefinition("myListener",
            BeanDefinitionBuilder.rootBeanDefinition(MyListener.class).getBeanDefinition());
        this.context.refresh();
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isEmpty();
        closeContext();

        load(false, "spring.rocketmq.nameServer=127.0.0.1:9876");
        this.context.registerBeanDefinition("myListener",
            BeanDefinitionBuilder.rootBeanDefinition(MyListener.class).getBeanDefinition());
        this.context.refresh();
        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isNotEmpty();
        assertThat(this.context.containsBean(DefaultRocketMQListenerContainer.class.getName() + "_1")).isTrue();
        assertThat(this.context.containsBean("mqProducer")).isFalse();
        assertThat(this.context.containsBean("rocketMQTemplate")).isFalse();

    }

    @Test
    public void listenerContainer() {
        load(false, "spring.rocketmq.nameServer=127.0.0.1:9876");
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(MyListener.class);
        this.context.registerBeanDefinition("myListener", beanBuilder.getBeanDefinition());
        this.context.refresh();

        assertThat(this.context.getBeansOfType(DefaultRocketMQListenerContainer.class)).isNotEmpty();
        assertThat(this.context.containsBean(DefaultRocketMQListenerContainer.class.getName() + "_1")).isTrue();

        DefaultRocketMQListenerContainer listenerContainer =
            this.context.getBean(DefaultRocketMQListenerContainer.class.getName() + "_1",
                DefaultRocketMQListenerContainer.class);
        ObjectMapper objectMapper = this.context.getBean("rocketMQMessageObjectMapper", ObjectMapper.class);
        assertThat(listenerContainer.getObjectMapper()).isEqualTo(objectMapper);
        assertThat(listenerContainer.getConsumeMode()).isEqualTo(ConsumeMode.CONCURRENTLY);
        assertThat(listenerContainer.getSelectorType()).isEqualTo(SelectorType.TAG);
        assertThat(listenerContainer.getSelectorExpress()).isEqualTo("*");
        assertThat(listenerContainer.getConsumerGroup()).isEqualTo(TEST_CONSUMER_GROUP);
        assertThat(listenerContainer.getTopic()).isEqualTo(TEST_TOPIC);
        assertThat(listenerContainer.getNameServer()).isEqualTo("127.0.0.1:9876");
        assertThat(listenerContainer.getMessageModel()).isEqualTo(MessageModel.CLUSTERING);
        assertThat(listenerContainer.getConsumeThreadMax()).isEqualTo(1);
    }

    @After
    public void closeContext() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @RocketMQMessageListener(consumerGroup = TEST_CONSUMER_GROUP, topic = TEST_TOPIC, consumeThreadMax = 1)
    private static class MyListener implements RocketMQListener<String> {

        @Override
        public void onMessage(String message) {
            System.out.println(message);
        }
    }


    //@Test
    //run the case when nameserver and broker is started !!
    public void enableTxProducer() {
        load(false, "spring.rocketmq.nameServer=127.0.0.1:9876",
            "spring.rocketmq.producer.group=my_group",
            "spring.rocketmq.producer.send-msg-timeout=30000",
            "spring.rocketmq.producer.retry-times-when-send-async-failed=1",
            "spring.rocketmq.producer.compress-msg-body-over-howmuch=1024",
            "spring.rocketmq.producer.max-message-size=10240",
            "spring.rocketmq.producer.retry-another-broker-when-not-store-ok=true",
            "spring.rocketmq.producer.retry-times-when-send-failed=1");


        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(TransactionListenerImpl.class);
        this.context.registerBeanDefinition("myListener1", beanBuilder.getBeanDefinition());
        this.context.refresh();
        assertThat(this.context.containsBean("rocketMQTemplate")).isTrue();
        assertThat(this.context.getBeansOfType(TransactionListenerImpl.class)).isNotEmpty();

        RocketMQTemplate rocketMQTemplate = this.context.getBean(RocketMQTemplate.class);
        try {
            rocketMQTemplate.sendMessageInTransaction(null, new Message(TEST_TOPIC, "Hello".getBytes()), null);
            rocketMQTemplate.removeTransactionMQProducer(null);
        } catch (MQClientException e) {
            e.printStackTrace(System.out);
            fail("failed to get TransactionListenerImpl and send transactional msg!");
        }
    }

    @RocketMQTransactionListener
    private static class TransactionListenerImpl implements TransactionListener {
        @Override
        public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            return LocalTransactionState.UNKNOW;
        }

        @Override
        public LocalTransactionState checkLocalTransaction(MessageExt msg) {
            return LocalTransactionState.COMMIT_MESSAGE;
        }
    }

    private void load(boolean refresh, String... environment) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(RocketMQAutoConfiguration.class);
        EnvironmentTestUtils.addEnvironment(ctx, environment);
        if (refresh) {
            ctx.refresh();
        }
        this.context = ctx;
    }

    private void load(String... environment) {
        load(true, environment);
    }
}

