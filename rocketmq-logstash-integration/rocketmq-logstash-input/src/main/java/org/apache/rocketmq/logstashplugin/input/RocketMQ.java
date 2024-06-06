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

package org.apache.rocketmq.logstashplugin.input;

import co.elastic.logstash.api.Codec;
import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Input;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import co.elastic.logstash.api.PluginHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static java.lang.Math.max;

@LogstashPlugin(name = "rocketmq")
public class RocketMQ implements Input {
    private static final Logger logger = LogManager.getLogger(RocketMQ.class);

    private static final PluginConfigSpec<String> CONFIG_NAMESRV_ADDR = PluginConfigSpec.stringSetting("namesrv_addr", "", false, true);
    private static final PluginConfigSpec<String> CONFIG_GROUP =
            PluginConfigSpec.stringSetting("group", "", false, true);

    private static final PluginConfigSpec<String> CONFIG_TOPIC =
            PluginConfigSpec.stringSetting("topic", "", false, true);


    private static final String CODEC_JSON = "json";
    private static final PluginConfigSpec<Codec> CONFIG_CODEC = PluginConfigSpec.codecSetting("codec", "java_plain");
    private static final int DEFAULT_CONSUME_THREADS = max(Runtime.getRuntime().availableProcessors() / 2, 1);
    private static final PluginConfigSpec<Long> CONFIG_CONSUME_THREADS = PluginConfigSpec.numSetting("consumer_threads", DEFAULT_CONSUME_THREADS);
    private static final PluginConfigSpec<String> CONFIG_SUB_EXPRESSION = PluginConfigSpec.stringSetting("sub_expression", "*");
    private static final PluginConfigSpec<String> CONFIG_CONSUME_FROM_WHERE = PluginConfigSpec.stringSetting("consume_from_where", ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET.name());
    private static final PluginConfigSpec<String> CONFIG_CONSUME_TIMESTAMP = PluginConfigSpec.stringSetting("consume_timestamp", "");
    private static final PluginConfigSpec<String> CONFIG_MESSAGE_MODEL = PluginConfigSpec.stringSetting("message_model", MessageModel.CLUSTERING.name());


    private final String id;
    private final String namesrvAddr;
    private final String topic;


    private final String group;

    private final Codec codec;
    private final Long consumerThreads;
    private final String subExpression;
    private final String consumeFromWhere;
    private final String messageModel;
    private final String consumeTimestamp;

    private DefaultLitePullConsumer rmqConsumer;

    private volatile boolean stopped = false;
    private CountDownLatch done = new CountDownLatch(1);

    public RocketMQ(final String id, final Configuration configuration, final Context context) {
        this.id = id;
        this.codec = configuration.get(CONFIG_CODEC);

        this.namesrvAddr = configuration.get(CONFIG_NAMESRV_ADDR);
        this.topic = configuration.get(CONFIG_TOPIC);
        this.group = configuration.get(CONFIG_GROUP);
        this.consumerThreads = configuration.get(CONFIG_CONSUME_THREADS);
        this.subExpression = configuration.get(CONFIG_SUB_EXPRESSION);
        this.consumeFromWhere = configuration.get(CONFIG_CONSUME_FROM_WHERE);
        this.consumeTimestamp = configuration.get(CONFIG_CONSUME_TIMESTAMP);
        this.messageModel = configuration.get(CONFIG_MESSAGE_MODEL);

    }


    @Override
    public void start(Consumer<Map<String, Object>> consumer) {
        if (this.stopped) {
            return;
        }
        logger.info("Start processing");

        try {
            createAndStartConsumer();

            while (!stopped) {
                final List<MessageExt> messageExtList = this.rmqConsumer.poll();
                if (null == messageExtList || messageExtList.isEmpty()) {
                    continue;
                }
                for (MessageExt messageExt : messageExtList) {
                    this.codec.decode(ByteBuffer.wrap(messageExt.getBody()), m -> {
                        consumer.accept(m);
                    });
                }

            }
        } catch (MQClientException e) {
            logger.error("consumer starts failed", e);
        } finally {
            stopped = true;
            done.countDown();
        }
    }

    private void createAndStartConsumer() throws MQClientException {
        rmqConsumer = new DefaultLitePullConsumer(this.group);
        rmqConsumer.setNamesrvAddr(this.namesrvAddr);
        try {
            final int consumeThreadNum = Math.toIntExact(this.consumerThreads);
            rmqConsumer.setPullThreadNums(consumeThreadNum);
        } catch (Exception e) {
        }
        if (ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET.name().equals(this.consumeFromWhere)) {
            rmqConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);
        } else if (ConsumeFromWhere.CONSUME_FROM_TIMESTAMP.name().equals(this.consumeFromWhere)
                && null != consumeTimestamp && !"".equals(this.consumeTimestamp)) {
            rmqConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_TIMESTAMP);
            rmqConsumer.setConsumeTimestamp(this.consumeTimestamp);
        } else {
            rmqConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        }
        if (MessageModel.BROADCASTING.name().equals(this.messageModel)) {
            rmqConsumer.setMessageModel(MessageModel.BROADCASTING);
        } else {
            rmqConsumer.setMessageModel(MessageModel.CLUSTERING);
        }

        rmqConsumer.subscribe(this.topic, this.subExpression);
        this.rmqConsumer.start();
        logger.warn("LitePullConsumer started: {}", this.rmqConsumer);
    }

    @Override
    public void stop() {
        this.stopped = true;
        this.rmqConsumer.shutdown();
    }

    @Override
    public void awaitStop() throws InterruptedException {
        done.await();
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        return PluginHelper.commonOutputSettings(
                Arrays.asList(CONFIG_NAMESRV_ADDR,
                        CONFIG_GROUP,
                        CONFIG_TOPIC,
                        CONFIG_SUB_EXPRESSION,
                        CONFIG_CONSUME_FROM_WHERE,
                        CONFIG_CONSUME_TIMESTAMP,
                        CONFIG_MESSAGE_MODEL,
                        CONFIG_CONSUME_THREADS,
                        CONFIG_CODEC));
    }

    @Override
    public String getId() {
        return this.id;
    }

}
