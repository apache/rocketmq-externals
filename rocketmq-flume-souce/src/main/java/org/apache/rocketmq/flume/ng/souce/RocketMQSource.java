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

package org.apache.rocketmq.flume.ng.souce;

import com.alibaba.rocketmq.client.consumer.DefaultMQPullConsumer;
import com.alibaba.rocketmq.client.consumer.PullResult;
import com.alibaba.rocketmq.client.consumer.PullStatus;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;
import com.alibaba.rocketmq.common.message.MessageQueue;
import com.alibaba.rocketmq.common.protocol.heartbeat.MessageModel;
import com.google.common.base.Throwables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.FlumeException;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.instrumentation.SourceCounter;
import org.apache.flume.source.AbstractPollableSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.BATCH_SIZE_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.BATCH_SIZE_DEFAULT;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.CONSUMER_GROUP_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.CONSUMER_GROUP_DEFAULT;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.HEADER_TAG_NAME;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.HEADER_TOPIC_NAME;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.MESSAGE_MODEL_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.MESSAGE_MODEL_DEFAULT;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.NAME_SERVER_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.TAG_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.TAG_DEFAULT;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.TOPIC_CONFIG;
import static org.apache.rocketmq.flume.ng.souce.RocketMQSourceConstants.TOPIC_DEFAULT;

/**
 *
 */
public class RocketMQSource extends AbstractPollableSource implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(RocketMQSource.class);

    private String nameServer;
    private String topic;
    private String tag;
    private String consumerGroup;
    private String messageModel;
    private Integer batchSize;

    /** Monitoring counter. */
    private SourceCounter sourceCounter;

    DefaultMQPullConsumer consumer;

    @Override protected void doConfigure(Context context) throws FlumeException {

        nameServer = context.getString(NAME_SERVER_CONFIG);
        if (nameServer == null) {
            throw new ConfigurationException("NameServer must not be null");
        }

        topic = context.getString(TOPIC_CONFIG, TOPIC_DEFAULT);
        tag = context.getString(TAG_CONFIG, TAG_DEFAULT);
        consumerGroup = context.getString(CONSUMER_GROUP_CONFIG, CONSUMER_GROUP_DEFAULT);
        messageModel = context.getString(MESSAGE_MODEL_CONFIG, MESSAGE_MODEL_DEFAULT);
        batchSize = context.getInteger(BATCH_SIZE_CONFIG, BATCH_SIZE_DEFAULT);

        if (sourceCounter == null) {
            sourceCounter = new SourceCounter(getName());
        }
    }

    @Override
    protected void doStart() throws FlumeException {

        consumer = new DefaultMQPullConsumer(consumerGroup);
        consumer.setNamesrvAddr(nameServer);
        consumer.setMessageModel(MessageModel.valueOf(messageModel));
        consumer.registerMessageQueueListener(topic, null);

        try {
            consumer.start();
        } catch (MQClientException e) {
            log.error("RocketMQ consumer start failed", e);
            throw Throwables.propagate(e);
        }

        sourceCounter.start();
    }

    @Override
    protected Status doProcess() throws EventDeliveryException {

        List<Event> events = new ArrayList<Event>();
        Map<MessageQueue, Long> offsets = new HashMap<MessageQueue, Long>();
        Event event;
        Map<String, String> headers;

        try {
            Set<MessageQueue> queues = consumer.fetchSubscribeMessageQueues(topic);
            for (MessageQueue queue : queues) {
                long offset = getMessageQueueOffset(queue);
                PullResult pullResult = consumer.pull(queue, tag, offset, batchSize);

                if (pullResult.getPullStatus() == PullStatus.FOUND) {
                    for (MessageExt msg : pullResult.getMsgFoundList()) {
                        byte[] body = msg.getBody();

                        headers = new HashMap<String, String>();
                        headers.put(HEADER_TOPIC_NAME, topic);
                        headers.put(HEADER_TAG_NAME, tag);
                        if (log.isDebugEnabled()) {
                            log.debug("Processing message,body={}", new String(body, "UTF-8"));
                        }

                        event = EventBuilder.withBody(body, headers);
                        events.add(event);
                    }
                    offsets.put(queue, pullResult.getNextBeginOffset());
                }
            }

            if (events.size() > 0) {
                sourceCounter.incrementAppendBatchReceivedCount();
                sourceCounter.addToEventReceivedCount(events.size());

                getChannelProcessor().processEventBatch(events);

                sourceCounter.incrementAppendBatchAcceptedCount();
                sourceCounter.addToEventAcceptedCount(events.size());

                events.clear();
            }

            for (Map.Entry<MessageQueue, Long> entry : offsets.entrySet()) {
                putMessageQueueOffset(entry.getKey(), entry.getValue());
            }

        } catch (Exception e) {
            log.error("Failed to consumer message", e);
            return Status.BACKOFF;
        }

        return Status.READY;
    }

    @Override
    protected void doStop() throws FlumeException {
        sourceCounter.stop();

        consumer.shutdown();
    }

    private long getMessageQueueOffset(MessageQueue queue) throws MQClientException {
        long offset = consumer.fetchConsumeOffset(queue, false);
        if (offset < 0) {
            offset = 0;
        }

        return offset;
    }

    private void putMessageQueueOffset(MessageQueue queue, long offset) throws MQClientException {
        consumer.updateConsumeOffset(queue, offset);
    }
}
