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

package org.apache.rocketmq.spring.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.starter.annotation.MessageModel;
import org.apache.rocketmq.spring.starter.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.starter.core.RocketMQListener;
import org.apache.rocketmq.spring.starter.annotation.ConsumeMode;
import org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_NAMESERVER;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_TOPIC;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_CONSUMER_GROUP;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_ROCKETMQ_LISTENER;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_OBJECT_MAPPER;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.METHOD_DESTROY;
import static org.apache.rocketmq.spring.starter.support.DefaultRocketMQListenerContainerConstants.PROP_ROCKETMQ_LISTENER_ANNOTATION;


@Configuration
@ConditionalOnClass(name = "org.apache.rocketmq.client.consumer.DefaultMQPushConsumer")
@EnableConfigurationProperties(RocketMQProperties.class)
@ConditionalOnProperty(prefix = "spring.rocketmq", value = "nameServer")
public class ListenerContainerConfiguration implements ApplicationContextAware, SmartInitializingSingleton {
    private final static Logger log = LoggerFactory.getLogger(RocketMQAutoConfiguration.class);
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private ConfigurableApplicationContext applicationContext;

    private AtomicLong counter = new AtomicLong(0);

    private StandardEnvironment environment;

    private RocketMQProperties rocketMQProperties;

    private ObjectMapper objectMapper;

    public ListenerContainerConfiguration() {
    }

    @Autowired
    public ListenerContainerConfiguration(
        @Qualifier("rocketMQMessageObjectMapper") ObjectMapper objectMapper,
        StandardEnvironment environment,
        RocketMQProperties rocketMQProperties) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.rocketMQProperties = rocketMQProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(RocketMQMessageListener.class);

        if (Objects.nonNull(beans)) {
            beans.forEach(this::registerContainer);
        }
    }

    private void registerContainer(String beanName, Object bean) {
        Class<?> clazz = AopUtils.getTargetClass(bean);

        if (!RocketMQListener.class.isAssignableFrom(bean.getClass())) {
            throw new IllegalStateException(clazz + " is not instance of " + RocketMQListener.class.getName());
        }

        RocketMQListener rocketMQListener = (RocketMQListener) bean;
        RocketMQMessageListener annotation = clazz.getAnnotation(RocketMQMessageListener.class);
        validate(annotation);
        BeanDefinitionBuilder beanBuilder = BeanDefinitionBuilder.rootBeanDefinition(DefaultRocketMQListenerContainer.class);
        beanBuilder.addPropertyValue(PROP_TOPIC, environment.resolvePlaceholders(annotation.topic()));
        beanBuilder.addPropertyValue(PROP_CONSUMER_GROUP, environment.resolvePlaceholders(annotation.consumerGroup()));
        beanBuilder.addPropertyValue(PROP_NAMESERVER, rocketMQProperties.getNameServer());
        beanBuilder.addPropertyValue(PROP_ROCKETMQ_LISTENER_ANNOTATION, annotation);
        beanBuilder.addPropertyValue(PROP_ROCKETMQ_LISTENER, rocketMQListener);
        if (Objects.nonNull(objectMapper)) {
            beanBuilder.addPropertyValue(PROP_OBJECT_MAPPER, objectMapper);
        }
        beanBuilder.setDestroyMethodName(METHOD_DESTROY);

        String containerBeanName = String.format("%s_%s", DefaultRocketMQListenerContainer.class.getName(), counter.incrementAndGet());
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.registerBeanDefinition(containerBeanName, beanBuilder.getBeanDefinition());

        DefaultRocketMQListenerContainer container = beanFactory.getBean(containerBeanName, DefaultRocketMQListenerContainer.class);

        if (!container.isStarted()) {
            try {
                container.start();
            } catch (Exception e) {
                log.error("started container failed. {}", container, e);
                throw new RuntimeException(e);
            }
        }

        log.info("register rocketMQ listener to container, listenerBeanName:{}, containerBeanName:{}", beanName, containerBeanName);
    }

    private void validate(RocketMQMessageListener annotation) {
        if (annotation.consumeMode() == ConsumeMode.ORDERLY &&
            annotation.messageModel() == MessageModel.BROADCASTING)
            throw new BeanDefinitionValidationException("Bad annotation definition in @RocketMQMessageListener, messageModel BROADCASTING does not support ORDERLY message!");
    }
}