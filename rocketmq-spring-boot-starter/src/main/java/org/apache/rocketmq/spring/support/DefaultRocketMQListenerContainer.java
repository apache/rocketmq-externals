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

package org.apache.rocketmq.spring.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQPushConsumerLifecycleListener;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.MessageSelector;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

@SuppressWarnings("WeakerAccess")
public class DefaultRocketMQListenerContainer implements InitializingBean, RocketMQListenerContainer {
    private final static Logger log = LoggerFactory.getLogger(DefaultRocketMQListenerContainer.class);

    private long suspendCurrentQueueTimeMillis = 1000;

    /**
     * Message consume retry strategy<br> -1,no retry,put into DLQ directly<br> 0,broker control retry frequency<br>
     * >0,client control retry frequency.
     */
    private int delayLevelWhenNextConsume = 0;

    private String nameServer;

    private String consumerGroup;

    private String topic;

    private int consumeThreadMax = 64;

    private String charset = "UTF-8";

    private ObjectMapper objectMapper = new ObjectMapper();

    private RocketMQListener rocketMQListener;

    private RocketMQMessageListener rocketMQMessageListener;

    private DefaultMQPushConsumer consumer;

    private Class messageType;

    private boolean started;

    // The following properties came from @RocketMQMessageListener.
    private ConsumeMode consumeMode;
    private SelectorType selectorType;
    private String selectorExpression;
    private MessageModel messageModel;

    public long getSuspendCurrentQueueTimeMillis() {
        return suspendCurrentQueueTimeMillis;
    }

    public void setSuspendCurrentQueueTimeMillis(long suspendCurrentQueueTimeMillis) {
        this.suspendCurrentQueueTimeMillis = suspendCurrentQueueTimeMillis;
    }

    public int getDelayLevelWhenNextConsume() {
        return delayLevelWhenNextConsume;
    }

    public void setDelayLevelWhenNextConsume(int delayLevelWhenNextConsume) {
        this.delayLevelWhenNextConsume = delayLevelWhenNextConsume;
    }

    public String getNameServer() {
        return nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getConsumeThreadMax() {
        return consumeThreadMax;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public RocketMQListener getRocketMQListener() {
        return rocketMQListener;
    }

    public void setRocketMQListener(RocketMQListener rocketMQListener) {
        this.rocketMQListener = rocketMQListener;
    }

    public RocketMQMessageListener getRocketMQMessageListener() {
        return rocketMQMessageListener;
    }

    public void setRocketMQMessageListener(RocketMQMessageListener anno) {
        this.rocketMQMessageListener = anno;

        this.consumeMode = anno.consumeMode();
        this.consumeThreadMax = anno.consumeThreadMax();
        this.messageModel = anno.messageModel();
        this.selectorExpression = anno.selectorExpression();
        this.selectorType = anno.selectorType();
    }

    public ConsumeMode getConsumeMode() {
        return consumeMode;
    }

    public SelectorType getSelectorType() {
        return selectorType;
    }

    public String getSelectorExpression() {
        return selectorExpression;
    }

    public MessageModel getMessageModel() {
        return messageModel;
    }

    public DefaultMQPushConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(DefaultMQPushConsumer consumer) {
        this.consumer = consumer;
    }

    public void setupMessageListener(RocketMQListener rocketMQListener) {
        this.rocketMQListener = rocketMQListener;
    }

    @Override
    public void destroy() {
        this.setStarted(false);
        if (Objects.nonNull(consumer)) {
            consumer.shutdown();
        }
        log.info("container destroyed, {}", this.toString());
    }

    public synchronized void start() throws MQClientException {

        if (this.isStarted()) {
            throw new IllegalStateException("container already started. " + this.toString());
        }

        initRocketMQPushConsumer();

        this.messageType = getMessageType();
        log.debug("msgType: {}", messageType.getName());

        consumer.start();
        this.setStarted(true);

        log.info("started container: {}", this.toString());
    }

    public class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {

        @SuppressWarnings("unchecked")
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            for (MessageExt messageExt : msgs) {
                log.debug("received msg: {}", messageExt);
                try {
                    long now = System.currentTimeMillis();
                    rocketMQListener.onMessage(doConvertMessage(messageExt));
                    long costTime = System.currentTimeMillis() - now;
                    log.debug("consume {} cost: {} ms", messageExt.getMsgId(), costTime);
                } catch (Exception e) {
                    log.warn("consume message failed. messageExt:{}", messageExt, e);
                    context.setDelayLevelWhenNextConsume(delayLevelWhenNextConsume);
                    return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                }
            }

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }

    public class DefaultMessageListenerOrderly implements MessageListenerOrderly {

        @SuppressWarnings("unchecked")
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            for (MessageExt messageExt : msgs) {
                log.debug("received msg: {}", messageExt);
                try {
                    long now = System.currentTimeMillis();
                    rocketMQListener.onMessage(doConvertMessage(messageExt));
                    long costTime = System.currentTimeMillis() - now;
                    log.info("consume {} cost: {} ms", messageExt.getMsgId(), costTime);
                } catch (Exception e) {
                    log.warn("consume message failed. messageExt:{}", messageExt, e);
                    context.setSuspendCurrentQueueTimeMillis(suspendCurrentQueueTimeMillis);
                    return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                }
            }

            return ConsumeOrderlyStatus.SUCCESS;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public String toString() {
        return "DefaultRocketMQListenerContainer{" +
            "consumerGroup='" + consumerGroup + '\'' +
            ", nameServer='" + nameServer + '\'' +
            ", topic='" + topic + '\'' +
            ", consumeMode=" + consumeMode +
            ", selectorType=" + selectorType +
            ", selectorExpression='" + selectorExpression + '\'' +
            ", messageModel=" + messageModel +
            '}';
    }

    @SuppressWarnings("unchecked")
    private Object doConvertMessage(MessageExt messageExt) {
        if (Objects.equals(messageType, MessageExt.class)) {
            return messageExt;
        } else {
            String str = new String(messageExt.getBody(), Charset.forName(charset));
            if (Objects.equals(messageType, String.class)) {
                return str;
            } else {
                // If msgType not string, use objectMapper change it.
                try {
                    return objectMapper.readValue(str, messageType);
                } catch (Exception e) {
                    log.info("convert failed. str:{}, msgType:{}", str, messageType);
                    throw new RuntimeException("cannot convert message to " + messageType, e);
                }
            }
        }
    }

    private Class getMessageType() {
        Type[] interfaces = AopUtils.getTargetClass(rocketMQListener).getGenericInterfaces();
        if (Objects.nonNull(interfaces)) {
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    if (Objects.equals(parameterizedType.getRawType(), RocketMQListener.class)) {
                        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                        if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
                            return (Class) actualTypeArguments[0];
                        } else {
                            return Object.class;
                        }
                    }
                }
            }

            return Object.class;
        } else {
            return Object.class;
        }
    }

    private void initRocketMQPushConsumer() throws MQClientException {

        Assert.notNull(rocketMQListener, "Property 'rocketMQListener' is required");
        Assert.notNull(consumerGroup, "Property 'consumerGroup' is required");
        Assert.notNull(nameServer, "Property 'nameServer' is required");
        Assert.notNull(topic, "Property 'topic' is required");

        consumer = new DefaultMQPushConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumeThreadMax(consumeThreadMax);
        if (consumeThreadMax < consumer.getConsumeThreadMin()) {
            consumer.setConsumeThreadMin(consumeThreadMax);
        }

        switch (messageModel) {
            case BROADCASTING:
                consumer.setMessageModel(org.apache.rocketmq.common.protocol.heartbeat.MessageModel.BROADCASTING);
                break;
            case CLUSTERING:
                consumer.setMessageModel(org.apache.rocketmq.common.protocol.heartbeat.MessageModel.CLUSTERING);
                break;
        }

        switch (selectorType) {
            case TAG:
                consumer.subscribe(topic, selectorExpression);
                break;
            case SQL92:
                consumer.subscribe(topic, MessageSelector.bySql(selectorExpression));
                break;
            default:
                throw new IllegalArgumentException("Property 'selectorType' was wrong.");
        }

        switch (consumeMode) {
            case ORDERLY:
                consumer.setMessageListener(new DefaultMessageListenerOrderly());
                break;
            case CONCURRENTLY:
                consumer.setMessageListener(new DefaultMessageListenerConcurrently());
                break;
            default:
                throw new IllegalArgumentException("Property 'consumeMode' was wrong.");
        }

        if (rocketMQListener instanceof RocketMQPushConsumerLifecycleListener) {
            ((RocketMQPushConsumerLifecycleListener) rocketMQListener).prepareStart(consumer);
        }

    }

}
