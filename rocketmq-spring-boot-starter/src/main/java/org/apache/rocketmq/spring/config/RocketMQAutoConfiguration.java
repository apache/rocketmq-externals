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

package org.apache.rocketmq.spring.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Role;
import org.springframework.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@ConditionalOnClass(name = "org.apache.rocketmq.client.impl.MQClientAPIImpl")
@EnableConfigurationProperties(RocketMQProperties.class)
@ConditionalOnProperty(prefix = "spring.rocketmq", value = "nameServer")
@Import(ListenerContainerConfiguration.class)
public class RocketMQAutoConfiguration {
    private final static Logger log = LoggerFactory.getLogger(RocketMQAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(name = "defaultMQProducer")
    @ConditionalOnProperty(prefix = "spring.rocketmq", value = {"nameServer", "producer.group"})
    public DefaultMQProducer mqProducer(RocketMQProperties rocketMQProperties) {
        RocketMQProperties.Producer producerConfig = rocketMQProperties.getProducer();
        String nameServer = rocketMQProperties.getNameServer();
        String groupName = producerConfig.getGroup();
        Assert.hasText(nameServer, "[spring.rocketmq.nameServer] must not be null");
        Assert.hasText(groupName, "[spring.rocketmq.producer.group] must not be null");

        DefaultMQProducer producer = new DefaultMQProducer(groupName);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(producerConfig.getSendMessageTimeout());
        producer.setRetryTimesWhenSendFailed(producerConfig.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(producerConfig.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(producerConfig.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(producerConfig.getCompressMessageBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(producerConfig.isRetryAnotherBrokerWhenNotStoreOk());

        return producer;
    }

    @Bean
    @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
    @ConditionalOnMissingBean(name = "rocketMQMessageObjectMapper")
    public ObjectMapper rocketMQMessageObjectMapper() {
        return new ObjectMapper();
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnBean(DefaultMQProducer.class)
    @ConditionalOnMissingBean(name = "rocketMQTemplate")
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer mqProducer,
        @Autowired(required = false)
        @Qualifier("rocketMQMessageObjectMapper")
            ObjectMapper objectMapper) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        rocketMQTemplate.setProducer(mqProducer);
        if (Objects.nonNull(objectMapper)) {
            log.debug("set objectMapper to rocketMRTemplate");
            rocketMQTemplate.setObjectMapper(objectMapper);
        } else {
            // Throw exception
            throw new IllegalStateException("can not inject objectMapper to rocketMQTemplate, the objectMapper is null!!");
        }

        return rocketMQTemplate;
    }

    @Bean
    @ConditionalOnBean(RocketMQTemplate.class)
    @ConditionalOnMissingBean(TransactionHandlerRegistry.class)
    public TransactionHandlerRegistry transactionHandlerRegistry(RocketMQTemplate template) {
        return new TransactionHandlerRegistry(template);
    }

    @SuppressWarnings("rawtypes")
    @ConditionalOnBean(RocketMQTemplate.class)
    @Bean(name = RocketMQConfigUtils.ROCKETMQ_TRANSACTION_ANNOTATION_PROCESSOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public RocketMQTransactionAnnotationProcessor RocketMQTransactionAnnotationProcessor() {
        return new RocketMQTransactionAnnotationProcessor();
    }
}
