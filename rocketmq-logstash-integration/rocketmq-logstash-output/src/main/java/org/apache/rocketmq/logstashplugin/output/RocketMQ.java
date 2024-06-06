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

package org.apache.rocketmq.logstashplugin.output;

import co.elastic.logstash.api.Codec;
import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.Output;
import co.elastic.logstash.api.PluginConfigSpec;
import co.elastic.logstash.api.PluginHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import static org.apache.rocketmq.client.impl.CommunicationMode.ASYNC;
import static org.apache.rocketmq.client.impl.CommunicationMode.ONEWAY;
import static org.apache.rocketmq.client.impl.CommunicationMode.SYNC;

@LogstashPlugin(name = "rocketmq")
public class RocketMQ implements Output {
    private static final Logger logger = LogManager.getLogger(RocketMQ.class);

    public static final PluginConfigSpec<String> CONFIG_NAMESRV_ADDR = PluginConfigSpec.stringSetting("namesrv_addr", "", false, true);
    public static final PluginConfigSpec<String> CONFIG_GROUP =
            PluginConfigSpec.stringSetting("group", "LOGSTASH_GROUP");

    public static final PluginConfigSpec<String> CONFIG_TOPIC =
            PluginConfigSpec.stringSetting("topic", "", false, true);

    public static final PluginConfigSpec<String> CONFIG_TAG =
            PluginConfigSpec.stringSetting("tag", "");

    public static final String MODE_ONEWAY = ONEWAY.name();
    private static final String MODE_SYNC = SYNC.name();
    private static final String MODE_ASYNC = ASYNC.name();
    public static final PluginConfigSpec<String> CONFIG_SEND_MODE =
            PluginConfigSpec.stringSetting("send_mode", MODE_ONEWAY);

    public static final PluginConfigSpec<Long> CONFIG_TIMEOUT =
            PluginConfigSpec.numSetting("send_timeout_ms", 3000L);

    public static final PluginConfigSpec<Long> CONFIG_RETRY_TIMES =
            PluginConfigSpec.numSetting("retries");
    private static final PluginConfigSpec<Long> CONFIG_BATCH_SIZE = PluginConfigSpec.numSetting("batch_size", 1L);
    public static final PluginConfigSpec<Codec> CONFIG_CODEC = PluginConfigSpec.codecSetting("codec", "java_plain");

    private final String id;
    private final String namesrvAddr;
    private final String topic;
    private final String tag;
    private final Long batchSize;
    private final Long sendTimeoutMillis;
    private final Long retries;
    private final DefaultMQProducer producer;
    private final String group;
    private final String sendMode;
    private final Codec codec;

    private volatile boolean stopped = false;
    private final CountDownLatch done = new CountDownLatch(1);

    public RocketMQ(final String id, final Configuration configuration, final Context context) throws MQClientException {
        this.id = id;
        this.namesrvAddr = configuration.get(CONFIG_NAMESRV_ADDR);
        this.topic = configuration.get(CONFIG_TOPIC);
        this.tag = configuration.get(CONFIG_TAG);
        this.batchSize = configuration.get(CONFIG_BATCH_SIZE);
        this.sendTimeoutMillis = configuration.get(CONFIG_TIMEOUT);
        this.retries = configuration.get(CONFIG_RETRY_TIMES);
        this.sendMode = configuration.get(CONFIG_SEND_MODE);
        this.group = configuration.get(CONFIG_GROUP);
        this.codec = configuration.get(CONFIG_CODEC);

        producer = new DefaultMQProducer(this.group);
        producer.setNamesrvAddr(this.namesrvAddr);
        if (null != this.retries) {
            try {
                final int retryTimes = Math.toIntExact(this.retries);
                producer.setRetryTimesWhenSendFailed(retryTimes);
                producer.setRetryTimesWhenSendAsyncFailed(retryTimes);
            } catch (Exception e) {

            }
        }

        try {
            producer.start();
        } catch (MQClientException e) {
            logger.error("start producer failed", e);
            throw e;
        }
        logger.warn("producer started. {}", producer);

    }

    @Override
    public void output(Collection<Event> collection) {
        if (MODE_ONEWAY.equals(this.sendMode)) {
            sendEvent(collection, new EventOnewayConsumer(this.producer));
        } else if (MODE_SYNC.equals(this.sendMode)) {
            sendEvent(collection, new EventSyncConsumer(this.producer, this.sendTimeoutMillis));
        } else if (MODE_ASYNC.equals(this.sendMode)) {
            sendEvent(collection, new EventAsyncConsumer(this.producer, this.sendTimeoutMillis));
        } else {
            logger.error("Unknown send mode: {}, skip event", this.sendMode);
        }
    }

    class EventAsyncConsumer implements BiConsumer<Event, Message> {
        private DefaultMQProducer producer;
        private long sendTimeoutMillis;

        public EventAsyncConsumer(DefaultMQProducer producer, long sendTimeoutMillis) {
            this.producer = producer;
            this.sendTimeoutMillis = sendTimeoutMillis;
        }

        @Override
        public void accept(Event event, Message message) {
            try {
                this.producer.send(message, new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {

                    }

                    @Override
                    public void onException(Throwable e) {
                        logger.error("Send event receiving exception", e);
                        logger.warn("Failed event: " + event.toString());
                    }
                }, this.sendTimeoutMillis);
            } catch (MQClientException e) {
                logger.error("Send event failed", e);
            } catch (RemotingException e) {
                logger.error("Send event failed", e);
            } catch (InterruptedException e) {
                logger.error("Send event failed", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void stop() {
        this.stopped = true;
        this.producer.shutdown();
        done.countDown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return PluginHelper.commonOutputSettings(
                Arrays.asList(CONFIG_BATCH_SIZE, CONFIG_GROUP, CONFIG_TAG, CONFIG_TOPIC, CONFIG_TIMEOUT,
                        CONFIG_NAMESRV_ADDR, CONFIG_RETRY_TIMES,
                        CONFIG_SEND_MODE, CONFIG_CODEC));
    }

    @Override
    public String getId() {
        return this.id;
    }

    private void sendEvent(Collection<Event> collection, BiConsumer<Event, Message> eventSender) {
        for (Event event : collection) {
            if (this.stopped) {
                return;
            }

            byte[] eventBytes = null;
            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                this.codec.encode(event, os);
                eventBytes = os.toByteArray();
            } catch (IOException e) {
                logger.warn("Encode event error: {}", event);
                continue;
            }

            if (null == eventBytes) {
                continue;
            }

            final Message message = new Message(this.topic, this.tag, eventBytes);
            eventSender.accept(event, message);
        }
    }


    private class EventSyncConsumer implements BiConsumer<Event, Message> {
        private DefaultMQProducer producer;
        private long sendTimeoutMillis;

        public EventSyncConsumer(DefaultMQProducer producer, long sendTimeoutMillis) {
            this.producer = producer;
            this.sendTimeoutMillis = sendTimeoutMillis;
        }

        @Override
        public void accept(Event event, Message message) {
            try {
                this.producer.send(message, this.sendTimeoutMillis);
            } catch (MQClientException e) {
                logger.error("Send event failed", e);
            } catch (RemotingException e) {
                logger.error("Send event failed", e);
            } catch (MQBrokerException e) {
                logger.error("Send event failed", e);
            } catch (InterruptedException e) {
                logger.error("Send event failed", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private class EventOnewayConsumer implements BiConsumer<Event, Message> {
        private DefaultMQProducer producer;

        public EventOnewayConsumer(DefaultMQProducer producer) {
            this.producer = producer;
        }

        @Override
        public void accept(Event event, Message message) {
            try {
                this.producer.sendOneway(message);
            } catch (MQClientException e) {
                logger.error("Send event failed", e);
            } catch (RemotingException e) {
                logger.error("Send event failed", e);
            } catch (InterruptedException e) {
                logger.error("Send event failed", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
