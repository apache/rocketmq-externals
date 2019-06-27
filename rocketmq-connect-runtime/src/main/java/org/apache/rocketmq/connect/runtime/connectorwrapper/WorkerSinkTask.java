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
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.rocketmq.connect.runtime.connectorwrapper;

import com.alibaba.fastjson.JSON;
import io.openmessaging.KeyValue;
import io.openmessaging.connector.api.data.Converter;
import io.openmessaging.connector.api.data.SinkDataEntry;
import io.openmessaging.connector.api.data.SourceDataEntry;
import io.openmessaging.connector.api.sink.SinkTask;
import io.openmessaging.connector.api.sink.SinkTaskContext;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.client.consumer.PullStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.connect.runtime.common.ConnectKeyValue;
import org.apache.rocketmq.connect.runtime.common.LoggerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper of {@link SinkTask} for runtime.
 */
public class WorkerSinkTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(LoggerName.ROCKETMQ_RUNTIME);

    /**
     * The configuration key that provides the list of topicNames that are inputs for this SinkTask.
     */
    public static final String QUEUENAMES_CONFIG = "topicNames";

    /**
     * Connector name of current task.
     */
    private String connectorName;

    /**
     * The implements of the sink task.
     */
    private SinkTask sinkTask;

    /**
     * The configs of current sink task.
     */
    private ConnectKeyValue taskConfig;

    /**
     * A switch for the sink task.
     */
    private AtomicBoolean isStopping;

    /**
     * A RocketMQ consumer to pull message from MQ.
     */
    private DefaultMQPullConsumer consumer;

    /**
     * A converter to parse sink data entry to object.
     */
    private Converter recordConverter;

    private final ConcurrentHashMap<MessageQueue, Long> messageQueuesOffsetMap;

    private final ConcurrentHashMap<MessageQueue, QueueStatus> messageQueuesStatusMap;

    public WorkerSinkTask(String connectorName,
        SinkTask sinkTask,
        ConnectKeyValue taskConfig,
        Converter recordConverter,
        DefaultMQPullConsumer consumer) {
        this.connectorName = connectorName;
        this.sinkTask = sinkTask;
        this.taskConfig = taskConfig;
        this.isStopping = new AtomicBoolean(false);
        this.consumer = consumer;
        this.recordConverter = recordConverter;
        this.messageQueuesOffsetMap = new ConcurrentHashMap<>(256);
        this.messageQueuesStatusMap = new ConcurrentHashMap<>(256);
    }

    /**
     * Start a sink task, and receive data entry from MQ cyclically.
     */
    @Override
    public void run() {
        try {
            sinkTask.initialize(new SinkTaskContext() {
                @Override
                public void resetOffset(String topicName, Long offset) {
                    //TODO
                    MessageQueue messageQueue = new MessageQueue(topicName, "", 0);
                    messageQueuesOffsetMap.put(messageQueue, offset);
                }

                @Override
                public void resetOffset(Map<String, Long> offsets) {
                    //TODO
                    for (Map.Entry<String, Long> entry : offsets.entrySet()) {
                        MessageQueue messageQueue = new MessageQueue(entry.getKey(), "", 0);
                        messageQueuesOffsetMap.put(messageQueue, entry.getValue());
                    }
                }

                @Override
                public void pause(List<String> topicNames) {
                    //TODO
                    if (null != topicNames && topicNames.size() > 0) {
                        for (String topicName : topicNames) {
                            MessageQueue messageQueue = new MessageQueue(topicName, "", 0);
                            messageQueuesStatusMap.put(messageQueue, QueueStatus.PAUSE);
                        }
                    }
                }

                @Override
                public void resume(List<String> topicNames) {
                    //TODO
                    if (null != topicNames && topicNames.size() > 0) {
                        for (String topicName : topicNames) {
                            MessageQueue messageQueue = new MessageQueue(topicName, "", 0);
                            messageQueuesStatusMap.remove(messageQueue);
                        }
                    }
                }

                @Override
                public KeyValue configs() {
                    return taskConfig;
                }
            });
            String topicNamesStr = taskConfig.getString(QUEUENAMES_CONFIG);
            if (!StringUtils.isEmpty(topicNamesStr)) {
                String[] topicNames = topicNamesStr.split(",");
                for (String topicName : topicNames) {
                    //TODO 获取offset信息（持久化到本地)
                    final Set<MessageQueue> messageQueues = consumer.fetchMessageQueuesInBalance(topicName);
                    for (MessageQueue messageQueue : messageQueues) {
                        final long offset = consumer.searchOffset(messageQueue, 3 * 1000);
                        messageQueuesOffsetMap.put(messageQueue, offset);
                    }
                    messageQueues.addAll(messageQueues);
                }
                log.debug("{} Initializing and starting task for topicNames {}", this, topicNames);
            } else {
                log.error("Lack of sink comsume topicNames config");
            }
            sinkTask.start(taskConfig);

            while (!isStopping.get()) {
                for (Map.Entry<MessageQueue, Long> entry : messageQueuesOffsetMap.entrySet()) {
                    final PullResult pullResult = consumer.pullBlockIfNotFound(entry.getKey(), "*", entry.getValue(), 32);
                    if (pullResult.getPullStatus().equals(PullStatus.FOUND)) {
                        final List<MessageExt> messages = pullResult.getMsgFoundList();
                        checkPause(messages);
                        receiveMessages(messages);
                        for (MessageExt message1 : messages) {
                            String msgId = message1.getMsgId();
                            log.info("Received one message success : {}", msgId);
                            //TODO 更新queue offset
//                            consumer.ack(msgId);
                        }
                        messageQueuesOffsetMap.put(entry.getKey(), pullResult.getNextBeginOffset());
                    }
                }

            }
            log.info("Task stop, config:{}", JSON.toJSONString(taskConfig));
        } catch (Exception e) {
            log.error("Run task failed {}.", e);
        }
    }

    private void checkPause(List<MessageExt> messages) {
        final Iterator<MessageExt> iterator = messages.iterator();
        while (iterator.hasNext()) {
            final MessageExt message = iterator.next();
            String topicName = messages.get(0).getTopic();
            //TODO 缺失partition
            MessageQueue messageQueue = new MessageQueue(topicName, "", 0);
            if (null != messageQueuesStatusMap.get(messageQueue)) {
                String msgId = message.getMsgId();
                log.info("TopicName {}, queueId {} pause, Discard the message {}", topicName, 0, msgId);
                iterator.remove();
            }
        }
    }

    public void stop() {
        isStopping.set(true);
        consumer.shutdown();
        sinkTask.stop();
    }

    /**
     * receive message from MQ.
     *
     * @param messages
     */
    private void receiveMessages(List<MessageExt> messages) {
        List<SinkDataEntry> sinkDataEntries = new ArrayList<>(8);
        for (MessageExt message : messages) {
            SinkDataEntry sinkDataEntry = convertToSinkDataEntry(message);
            sinkDataEntries.add(sinkDataEntry);
        }
        sinkTask.put(sinkDataEntries);
    }

    private SinkDataEntry convertToSinkDataEntry(Message message) {
        String topicName = message.getTopic();
        final byte[] messageBody = message.getBody();
        final SourceDataEntry sourceDataEntry = JSON.parseObject(new String(messageBody), SourceDataEntry.class);
        final Object[] payload = sourceDataEntry.getPayload();
        final byte[] decodeBytes = Base64.getDecoder().decode((String) payload[0]);
        final Object recodeObject = recordConverter.byteToObject(decodeBytes);
        Object[] newObject = new Object[1];
        newObject[0] = recodeObject;
        //TODO
        SinkDataEntry sinkDataEntry = new SinkDataEntry(10L, sourceDataEntry.getTimestamp(), sourceDataEntry.getEntryType(), topicName, sourceDataEntry.getSchema(), newObject);
        sinkDataEntry.setPayload(newObject);
        return sinkDataEntry;
    }

    public String getConnectorName() {
        return connectorName;
    }

    public ConnectKeyValue getTaskConfig() {
        return taskConfig;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("connectorName:" + connectorName)
            .append("\nConfigs:" + JSON.toJSONString(taskConfig));
        return sb.toString();
    }

    private enum QueueStatus {
        PAUSE
    }
}
